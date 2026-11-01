package trackflow.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.commons.core.GuiSettings;
import trackflow.model.AthleteRoster;
import trackflow.model.ReadOnlyRoster;
import trackflow.model.UserPrefs;

public class StorageManagerTest {

    @TempDir
    public Path testFolder;

    private StorageManager storageManager;

    @BeforeEach
    public void setUp() {
        JsonRosterStorage rosterStorage = new JsonRosterStorage(getTempFilePath("ab"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(getTempFilePath("prefs"));
        storageManager = new StorageManager(rosterStorage, userPrefsStorage);
    }

    private Path getTempFilePath(String fileName) {
        return testFolder.resolve(fileName);
    }

    // EP: delegation to user prefs storage (save and read preferences)
    @Test
    public void prefsReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonUserPrefsStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonUserPrefsStorageTest} class.
         */
        UserPrefs original = new UserPrefs();
        original.setGuiSettings(new GuiSettings(300, 600, 4, 6));
        storageManager.saveUserPrefs(original);
        UserPrefs retrieved = storageManager.readUserPrefs().get();
        assertEquals(original, retrieved);
    }

    // EP: delegation to roster storage (save and read roster)
    @Test
    public void rosterReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonRosterStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonRosterStorageTest} class.
         */
        AthleteRoster original = getTypicalRoster();
        storageManager.saveRoster(original);
        ReadOnlyRoster retrieved = storageManager.readRoster().get();
        assertEquals(original, new AthleteRoster(retrieved));
    }

    @Test
    public void getRosterFilePath() {
        // EP: retrieve athlete roster file path
        assertEquals(getTempFilePath("ab"), storageManager.getRosterFilePath());

        // EP: retrieve user prefs file path
        assertEquals(getTempFilePath("prefs"), storageManager.getUserPrefsFilePath());
    }

}
