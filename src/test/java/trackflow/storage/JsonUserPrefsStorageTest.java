package trackflow.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static trackflow.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.commons.core.GuiSettings;
import trackflow.commons.exceptions.DataLoadingException;
import trackflow.model.UserPrefs;

public class JsonUserPrefsStorageTest {

    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonUserPrefsStorageTest");

    @TempDir
    public Path testFolder;

    // EP: null file path
    @Test
    public void readUserPrefs_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readUserPrefs(null)); // Boundary value
    }

    private Optional<UserPrefs> readUserPrefs(String userPrefsFileInTestDataFolder) throws DataLoadingException {
        Path prefsFilePath = addToTestDataPathIfNotNull(userPrefsFileInTestDataFolder);
        return new JsonUserPrefsStorage(prefsFilePath).readUserPrefs(prefsFilePath);
    }

    // EP: non-existent file path
    @Test
    public void readUserPrefs_missingFile_emptyResult() throws DataLoadingException {
        assertFalse(readUserPrefs("NonExistentFile.json").isPresent()); // Boundary value
    }

    // EP: invalid file format (non-JSON content)
    @Test
    public void readUserPrefs_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readUserPrefs("NotJsonFormatUserPrefs.json"));
    }

    // EP: user prefs JSON with null GuiSettings field
    @Test
    public void readUserPrefs_nullGuiSettings_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readUserPrefs("NullGuiSettingsUserPref.json"));
    }

    // EP: user prefs JSON with trailing extraneous content
    @Test
    public void readUserPrefs_trailingContent_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readUserPrefs("TrailingContentUserPref.json"));
    }

    // EP: user prefs JSON with duplicate keys
    @Test
    public void readUserPrefs_duplicateKeys_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readUserPrefs("DuplicateKeysUserPref.json"));
    }

    private Path addToTestDataPathIfNotNull(String userPrefsFileInTestDataFolder) {
        return userPrefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(userPrefsFileInTestDataFolder)
                : null;
    }

    // EP: valid user prefs JSON file with all fields present
    @Test
    public void readUserPrefs_fileInOrder_successfullyRead() throws DataLoadingException {
        UserPrefs expected = getTypicalUserPrefs();
        UserPrefs actual = readUserPrefs("TypicalUserPref.json").get();
        assertEquals(expected, actual);
    }

    // EP: empty user prefs JSON file (defaults applied)
    @Test
    public void readUserPrefs_valuesMissingFromFile_defaultValuesUsed() throws DataLoadingException {
        UserPrefs actual = readUserPrefs("EmptyUserPrefs.json").get(); // Boundary value
        assertEquals(new UserPrefs(), actual);
    }

    // EP: user prefs JSON with unrecognized extra fields
    @Test
    public void readUserPrefs_extraValuesInFile_extraValuesIgnored() throws DataLoadingException {
        UserPrefs expected = getTypicalUserPrefs();
        UserPrefs actual = readUserPrefs("ExtraValuesUserPref.json").get();

        assertEquals(expected, actual);
    }

    // EP: invalid GUI settings values (null, zero, negative, overflow, and non-numeric dimensions)
    @Test
    public void readUserPrefs_invalidSettings_throwsWithoutRewritingFile() throws Exception {
        Path prefsPath = testFolder.resolve("invalid-settings.json");
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(prefsPath);
        List<String> invalidSettings = List.of(
                "{\"guiSettings\":null}", // Boundary value
                "{\"guiSettings\":{\"windowWidth\":null}}", // Boundary value
                "{\"guiSettings\":{\"windowHeight\":0}}", // Boundary value
                "{\"guiSettings\":{\"windowWidth\":-1}}", // Boundary value
                "{\"guiSettings\":{\"windowHeight\":1e309}}", // Boundary value
                "{\"guiSettings\":{\"windowWidth\":\"NaN\"}}",
                "{\"guiSettings\":{\"windowCoordinates\":{\"x\":1e20,\"y\":0}}}"); // Boundary value
        for (String json : invalidSettings) {
            Files.writeString(prefsPath, json);
            assertThrows(DataLoadingException.class, storage::readUserPrefs);
            assertEquals(json, Files.readString(prefsPath), json);
        }
    }

    private UserPrefs getTypicalUserPrefs() {
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setGuiSettings(new GuiSettings(1000, 500, 300, 100));
        return userPrefs;
    }

    // EP: null user prefs object
    @Test
    public void savePrefs_nullPrefs_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveUserPrefs(null, "SomeFile.json")); // Boundary value
    }

    // EP: null file path when saving user prefs
    @Test
    public void saveUserPrefs_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveUserPrefs(new UserPrefs(), null)); // Boundary value
    }

    /**
     * Saves {@code userPrefs} at the specified {@code prefsFileInTestDataFolder} filepath.
     */
    private void saveUserPrefs(UserPrefs userPrefs, String prefsFileInTestDataFolder) {
        try {
            new JsonUserPrefsStorage(addToTestDataPathIfNotNull(prefsFileInTestDataFolder))
                    .saveUserPrefs(userPrefs);
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file", ioe);
        }
    }

    // EP: save and overwrite user prefs workflows
    @Test
    public void saveUserPrefs_allInOrder_success() throws DataLoadingException, IOException {

        UserPrefs original = new UserPrefs();
        original.setGuiSettings(new GuiSettings(1200, 200, 0, 2)); // Boundary value

        Path pefsFilePath = testFolder.resolve("TempPrefs.json");
        JsonUserPrefsStorage jsonUserPrefsStorage = new JsonUserPrefsStorage(pefsFilePath);

        // EP: save user prefs to new file (file does not exist initially)
        //Try writing when the file doesn't exist
        jsonUserPrefsStorage.saveUserPrefs(original);
        UserPrefs readBack = jsonUserPrefsStorage.readUserPrefs().get();
        assertEquals(original, readBack);

        // EP: overwrite existing user prefs file
        //Try saving when the file exists
        original.setGuiSettings(new GuiSettings(5, 5, 5, 5));
        jsonUserPrefsStorage.saveUserPrefs(original);
        readBack = jsonUserPrefsStorage.readUserPrefs().get();
        assertEquals(original, readBack);
    }

}
