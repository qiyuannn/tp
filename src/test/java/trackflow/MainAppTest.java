package trackflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.commons.core.GuiSettings;
import trackflow.commons.exceptions.DataLoadingException;
import trackflow.logic.Logic;
import trackflow.logic.LogicManager;
import trackflow.logic.parser.exceptions.ParseException;
import trackflow.model.AthleteRoster;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.ReadOnlyRoster;
import trackflow.model.UserPrefs;
import trackflow.model.util.SampleDataUtil;
import trackflow.storage.JsonRosterStorage;
import trackflow.storage.JsonUserPrefsStorage;
import trackflow.storage.Storage;
import trackflow.storage.StorageManager;

public class MainAppTest {
    @TempDir
    public Path testFolder;

    @Test
    public void initModelManager_unreadableRoster_preservesEachOriginalBeforeReplacement() throws Exception {
        Path rosterPath = testFolder.resolve("trackflow.json");
        Storage storage = createStorage(rosterPath);
        // EP: unreadable or corrupted roster JSON formats
        List<String> invalidRosters = List.of("{invalid JSON", "null", "{}", "{\"persons\":null}",
                "{\"persons\":[]} GARBAGE", "{\"persons\":[]} {\"persons\":[{}]}",
                "{\"persons\":[null]}", """
                {"persons": [{"name": "Avery Tan", "phone": "91234567",
                "email": "avery.tan@example.com", "address": "", "remark": "", "tags": [null]}]}
                """);
        for (String invalidRoster : invalidRosters) {
            Files.writeString(rosterPath, invalidRoster);
            Model model = new MainApp().initModelManager(storage, new UserPrefs());
            // EP: fallback to empty roster upon reading corrupted data
            assertEquals(new AthleteRoster(), model.getRoster()); // Boundary value
            assertEquals(invalidRoster, Files.readString(rosterPath));

            // EP: subsequent valid save overwrites roster without error
            storage.saveRoster(getTypicalRoster());
            assertEquals(getTypicalRoster(), new MainApp().initModelManager(storage, new UserPrefs()).getRoster());
        }

        // EP: backup files created for every unreadable roster
        try (Stream<Path> files = Files.list(testFolder)) {
            List<Path> backups = files.filter(path -> path.getFileName().toString().endsWith(".bak")).toList();
            assertEquals(invalidRosters.size(), backups.size());
            for (String original : invalidRosters) {
                boolean found = false;
                for (Path backup : backups) {
                    found |= original.equals(Files.readString(backup));
                }
                assertTrue(found);
            }
        }
    }

    // EP: unreadable roster with backup creation failure stops startup and preserves original roster
    @Test
    public void initModelManager_backupFailure_stopsStartupAndPreservesOriginal() throws Exception {
        Path rosterPath = testFolder.resolve("trackflow.json");
        Files.writeString(rosterPath, "{\"persons\":[null]}"); // Boundary value
        // This path cannot be copied: its parent is a regular file. Avoid platform-dependent permission checks.
        Storage storage = new StorageManager(new JsonRosterStorage(rosterPath.resolve("unreadable.json")),
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))) {
            @Override
            public Optional<ReadOnlyRoster> readRoster() throws DataLoadingException {
                throw new DataLoadingException(new IOException("Cannot read roster"));
            }
        };
        assertThrows(IOException.class, () -> new MainApp().initModelManager(storage, new UserPrefs()));
        assertEquals("{\"persons\":[null]}", Files.readString(rosterPath));
    }

    // EP: unreadable roster sets non-null roster load warning message
    @Test
    public void initModelManager_unreadableRoster_recordsLoadWarning() throws Exception {
        Path rosterPath = testFolder.resolve("trackflow.json");
        Files.writeString(rosterPath, "{invalid JSON"); // Boundary value
        MainApp app = new MainApp();
        app.initModelManager(createStorage(rosterPath), new UserPrefs());
        assertNotNull(app.getRosterLoadWarning());
    }

    // EP: missing roster file uses sample data and leaves load warning null
    @Test
    public void initModelManager_validRoster_noLoadWarning() throws Exception {
        Path rosterPath = testFolder.resolve("missing.json"); // Boundary value
        MainApp app = new MainApp();
        app.initModelManager(createStorage(rosterPath), new UserPrefs());
        assertNull(app.getRosterLoadWarning());
    }

    // EP: legacy tag exceeding 30-char limit loaded without backup and preserved across unrelated mutations
    @Test
    public void initModelManager_legacyLongTag_preservesRosterAcrossUpdatesAndRemoval() throws Exception {
        String legacyTag = "A".repeat(31); // Boundary value
        Path rosterPath = testFolder.resolve("legacy.json");
        String original = """
                {"persons":[{"name":"Legacy Athlete","phone":"91234567","email":"legacy@example.com",
                "address":"Home","remark":"Original note","tags":["%s","100m"],"ageCategory":"Open"}]}
                """.formatted(legacyTag);
        Files.writeString(rosterPath, original);
        Storage storage = createStorage(rosterPath);
        MainApp app = new MainApp();
        Model model = app.initModelManager(storage, new UserPrefs());
        // EP: legacy roster loaded with single athlete and no backup created
        assertEquals(1, model.getRoster().getPersonList().size()); // Boundary value
        assertNull(app.getRosterLoadWarning());
        assertEquals(original, Files.readString(rosterPath));
        try (Stream<Path> files = Files.list(testFolder)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".bak")));
        }

        // EP: mutations not touching legacy tag preserve legacy tag
        Logic logic = new LogicManager(model, storage);
        logic.execute("remark 1 r/Updated note");
        logic.execute("edit 1 addr/New home");
        logic.execute("tag 1 t/200m");
        Model reloaded = new MainApp().initModelManager(storage, new UserPrefs());
        assertEquals(model.getRoster(), reloaded.getRoster());
        assertTrue(reloaded.getRoster().getPersonList().getFirst().getTags().stream()
                .anyMatch(tag -> tag.tagName.equals(legacyTag)));
        String beforeRejectedTag = Files.readString(rosterPath);
        // EP: adding tag exceeding length limit (31 chars) rejected by parser
        assertThrows(ParseException.class, () -> logic.execute("tag 1 t/" + legacyTag));
        assertEquals(beforeRejectedTag, Files.readString(rosterPath));

        // EP: untag command removes legacy tag case-insensitively
        logic.execute("untag 1 t/" + legacyTag.toLowerCase(Locale.ROOT));
        Model afterRemoval = new MainApp().initModelManager(storage, new UserPrefs());
        assertEquals(2, afterRemoval.getRoster().getPersonList().getFirst().getTags().size());
        assertFalse(afterRemoval.getRoster().getPersonList().getFirst().getTags().stream()
                .anyMatch(tag -> tag.tagName.equals(legacyTag)));
    }

    // EP: missing roster file uses sample data without creating file on disk
    @Test
    public void initModelManager_missingRoster_usesSampleWithoutCreatingFile() throws Exception {
        Path rosterPath = testFolder.resolve("missing.json"); // Boundary value
        Model model = new MainApp().initModelManager(createStorage(rosterPath), new UserPrefs());
        assertEquals(SampleDataUtil.getSampleRoster(), model.getRoster());
        assertFalse(Files.exists(rosterPath));
    }

    // EP: valid saved empty roster loaded and preserved without backup
    @Test
    public void initModelManager_savedEmptyRoster_remainsEmptyWithoutBackup() throws Exception {
        Path rosterPath = testFolder.resolve("trackflow.json");
        Storage storage = createStorage(rosterPath);
        storage.saveRoster(new AthleteRoster()); // Boundary value
        assertEquals(new AthleteRoster(), new MainApp().initModelManager(storage, new UserPrefs()).getRoster());
        try (Stream<Path> files = Files.list(testFolder)) {
            assertEquals(1, files.count()); // Boundary value
        }
    }

    private Storage createStorage(Path rosterPath) {
        return new StorageManager(new JsonRosterStorage(rosterPath),
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json")));
    }

    @Test
    public void initPrefs_invalidSettings_recoversDefaultsWithoutChangingRoster() throws Exception {
        Path prefsPath = testFolder.resolve("preferences.json");
        Path rosterPath = testFolder.resolve("trackflow.json");
        String originalRoster = "roster bytes must stay untouched by preference recovery";
        Files.writeString(rosterPath, originalRoster);
        JsonUserPrefsStorage prefsStorage = new JsonUserPrefsStorage(prefsPath);
        // EP: malformed, corrupted, or boundary-violating preferences recovering default UserPrefs
        List<String> invalidPreferences = List.of("null", "{invalid JSON", "{\"guiSettings\":null}",
                "{\"guiSettings\":{\"windowWidth\":0}}", "{\"guiSettings\":{\"windowHeight\":-1}}",
                "{\"guiSettings\":{\"windowWidth\":1e309}}", "{} {}", "{} GARBAGE");
        for (String json : invalidPreferences) {
            Files.writeString(prefsPath, json);
            UserPrefs loaded = new MainApp().initPrefs(prefsStorage);
            assertEquals(new UserPrefs(), loaded, json); // Boundary value
            assertEquals(new UserPrefs(), prefsStorage.readUserPrefs().orElseThrow(), json);
            assertEquals(originalRoster, Files.readString(rosterPath), json);
            new ModelManager(new AthleteRoster(), loaded);
        }
    }

    @Test
    public void initPrefs_missingOrValidPreferences_createsDefaultsOrRetainsCustomSettings() throws Exception {
        Path prefsPath = testFolder.resolve("preferences.json");
        JsonUserPrefsStorage prefsStorage = new JsonUserPrefsStorage(prefsPath);
        // EP: missing preferences file creates default UserPrefs on disk
        assertEquals(new UserPrefs(), new MainApp().initPrefs(prefsStorage));
        assertTrue(Files.exists(prefsPath));
        // EP: existing valid custom preferences loaded and retained
        UserPrefs custom = new UserPrefs();
        custom.setGuiSettings(new GuiSettings(900, 450, -300, 100));
        prefsStorage.saveUserPrefs(custom);
        assertEquals(custom, new MainApp().initPrefs(prefsStorage));
        assertEquals(custom, prefsStorage.readUserPrefs().orElseThrow());
    }
}
