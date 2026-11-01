package trackflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.logic.Logic;
import trackflow.logic.LogicManager;
import trackflow.logic.commands.exceptions.CommandException;
import trackflow.model.AthleteRoster;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.ReadOnlyRoster;
import trackflow.model.UserPrefs;
import trackflow.model.person.Person;
import trackflow.model.util.SampleDataUtil;
import trackflow.storage.JsonRosterStorage;
import trackflow.storage.JsonUserPrefsStorage;
import trackflow.storage.Storage;
import trackflow.storage.StorageManager;

/**
 * Exercises actual filesystem failures and byte-level corruption using isolated installation data.
 */
public class StorageAdversarialTest {
    @TempDir
    public Path testFolder;

    // EP: startup with invalid UTF-8 bytes recovers empty roster and preserves bytes in backup
    @Test
    public void startup_invalidUtf8_preservesOriginalBytesBeforeAnyReplacement() throws Exception {
        Path rosterPath = testFolder.resolve("trackflow.json");
        byte[] corruptBytes = {(byte) 0xc3, (byte) 0x28, (byte) 0xff, 0}; // Boundary value
        Files.write(rosterPath, corruptBytes);
        Storage storage = createStorage(rosterPath);
        MainApp app = new MainApp();

        Model model = app.initModelManager(storage, new UserPrefs());

        assertEquals(new AthleteRoster(), model.getRoster()); // Boundary value
        assertNotNull(app.getRosterLoadWarning());
        assertArrayEquals(corruptBytes, Files.readAllBytes(rosterPath));
        Path backup;
        try (Stream<Path> files = Files.list(testFolder)) {
            List<Path> backups = files.filter(path -> path.toString().endsWith(".bak")).toList();
            assertEquals(1, backups.size()); // Boundary value
            backup = backups.getFirst();
        }
        assertArrayEquals(corruptBytes, Files.readAllBytes(backup));

        // EP: modifying roster after UTF-8 recovery preserves original bytes in backup
        Logic logic = new LogicManager(model, storage);
        logic.execute("add n/Recovered Athlete a/Open p/123 e/recovered@example.com addr/Home");

        assertArrayEquals(corruptBytes, Files.readAllBytes(backup));
        assertEquals(model.getRoster(), storage.readRoster().orElseThrow());
    }

    // EP: atomic filesystem replacement failure preserves model state across mutating commands
    @Test
    public void execute_actualReplacementFailure_preservesAllStateAndCanRetryAfterRepair() throws Exception {
        Path rosterPath = testFolder.resolve("trackflow.json");
        Storage storage = createStorage(rosterPath);
        Model model = new ModelManager(SampleDataUtil.getSampleRoster(), new UserPrefs());
        storage.saveRoster(model.getRoster());
        byte[] originalBytes = Files.readAllBytes(rosterPath);
        Logic logic = new LogicManager(model, storage);
        logic.execute("find n/Avery");
        logic.execute("sort name desc");

        ReadOnlyRoster originalRoster = new AthleteRoster(model.getRoster());
        List<Person> originalDisplay = List.copyOf(model.getFilteredPersonList());
        var originalPredicate = model.getPersonListPredicate();
        var originalComparator = model.getPersonListComparator();
        int[] publishedChanges = {0};
        model.getRoster().getPersonList().addListener(
                (javafx.collections.ListChangeListener<Person>) change -> publishedChanges[0]++);

        // A nonempty directory forces the real final replacement to fail on every supported OS.
        Path preservedRoster = testFolder.resolve("preserved-roster.json");
        Files.move(rosterPath, preservedRoster);
        Files.createDirectory(rosterPath);
        Path blockingFile = rosterPath.resolve("keep.txt");
        Files.writeString(blockingFile, "Do not replace");

        // EP: mutating commands roll back state when storage replacement is blocked
        for (String command : List.of("add n/New Athlete a/Open p/123 e/new@example.com addr/Home",
                "edit 1 n/Changed Athlete", "remark 1 r/Changed note", "tag 1 t/800m", "untag 1 t/100m",
                "delete 1", "y")) {
            if (command.equals("y")) {
                logic.execute("clear");
            }
            CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command), command);
            assertTrue(failure.getMessage().startsWith("Could not save data"), command);
            assertEquals(originalRoster, model.getRoster(), command);
            assertEquals(originalDisplay, model.getFilteredPersonList(), command);
            assertSame(originalPredicate, model.getPersonListPredicate(), command);
            assertSame(originalComparator, model.getPersonListComparator(), command);
            assertEquals(0, publishedChanges[0], command); // Boundary value
            assertArrayEquals(originalBytes, Files.readAllBytes(preservedRoster), command);
            assertEquals("Do not replace", Files.readString(blockingFile), command);
            try (Stream<Path> files = Files.list(testFolder)) {
                assertFalse(files.anyMatch(path -> path.toString().endsWith(".tmp")), command);
            }
        }

        // EP: successful command retry after removing filesystem blockage
        Files.delete(blockingFile);
        Files.delete(rosterPath);
        Files.move(preservedRoster, rosterPath);
        logic.execute("remark 1 r/Successfully retried");
        assertEquals("Successfully retried", model.getRoster().getPersonList().getFirst().getRemark().value);
        assertEquals(model.getRoster(), storage.readRoster().orElseThrow());
    }

    private Storage createStorage(Path rosterPath) {
        return new StorageManager(new JsonRosterStorage(rosterPath),
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json")));
    }
}
