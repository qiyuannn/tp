// @@author -
// Unassigned sections are adapted from SE-EDU AddressBook-Level3.
// @@author qiyuannn
package trackflow.storage;
// @@author -

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
// @@author qiyuannn
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalPersons.ALICE;
import static trackflow.testutil.TypicalPersons.HOON;
import static trackflow.testutil.TypicalPersons.IDA;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;
// @@author -

import java.io.IOException;
// @@author qiyuannn
import java.nio.file.Files;
// @@author -
import java.nio.file.Path;
import java.nio.file.Paths;
// @@author qiyuannn
import java.util.List;
// @@author -

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// @@author qiyuannn
import trackflow.commons.exceptions.DataLoadingException;
import trackflow.model.AthleteRoster;
import trackflow.model.ReadOnlyRoster;
// @@author -

// @@author qiyuannn
public class JsonRosterStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonRosterStorageTest");
    // @@author -

    @TempDir
    public Path testFolder;

    // EP: null file path
    @Test
    // @@author qiyuannn
    public void readRoster_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readRoster(null)); // Boundary value
        // @@author -
    }

    // @@author qiyuannn
    private java.util.Optional<ReadOnlyRoster> readRoster(String filePath) throws Exception {
        return new JsonRosterStorage(Paths.get(filePath)).readRoster(addToTestDataPathIfNotNull(filePath));
        // @@author -
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    // EP: non-existent file path
    @Test
    public void read_missingFile_emptyResult() throws Exception {
        // @@author qiyuannn
        assertFalse(readRoster("NonExistentFile.json").isPresent()); // Boundary value
        // @@author -
    }

    // EP: invalid file format (non-JSON content)
    @Test
    public void read_notJsonFormat_exceptionThrown() {
        // @@author qiyuannn
        assertThrows(DataLoadingException.class, () -> readRoster("notJsonFormatAthleteRoster.json"));
        // @@author -
    }

    // EP: valid JSON syntax but invalid athlete data
    @Test
    // @@author qiyuannn
    public void readRoster_invalidPersonAthleteRoster_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readRoster("invalidPersonAthleteRoster.json"));
        // @@author -
    }

    // EP: malformed JSON structures and schema violations
    // @@author qiyuannn
    @Test
    public void readRoster_malformedStructure_throwsDataLoadingException() throws Exception {
        Path filePath = testFolder.resolve("malformed.json");
        List<String> malformedRosters = List.of(
                "", // Boundary value
                "null",
                "{}", // Boundary value
                "{\"persons\":null}",
                "{\"persons\":[null]}",
                """
                {"persons": [{"name": "Avery Tan", "phone": "91234567",
                "email": "avery.tan@example.com", "address": "", "remark": "", "tags": [null]}]}
                """);
        for (String json : malformedRosters) {
            Files.writeString(filePath, json);
            assertThrows(DataLoadingException.class, new JsonRosterStorage(filePath)::readRoster);
            assertEquals(json, Files.readString(filePath));
        }
    }

    // EP: save and read roster workflows
    // @@author -
    @Test
    // @@author qiyuannn
    public void readAndSaveAthleteRoster_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempAthleteRoster.json");
        AthleteRoster original = getTypicalRoster();
        JsonRosterStorage jsonRosterStorage = new JsonRosterStorage(filePath);
        // @@author -

        // EP: save and read roster with explicit file path (new file)
        // Save in new file and read back
        // @@author qiyuannn
        jsonRosterStorage.saveRoster(original, filePath);
        ReadOnlyRoster readBack = jsonRosterStorage.readRoster(filePath).get();
        assertEquals(original, new AthleteRoster(readBack));
        // @@author -

        // EP: overwrite existing file with modified roster
        // Modify data, overwrite existing file, and read back
        original.addPerson(HOON);
        original.removePerson(ALICE);
        // @@author qiyuannn
        jsonRosterStorage.saveRoster(original, filePath);
        readBack = jsonRosterStorage.readRoster(filePath).get();
        assertEquals(original, new AthleteRoster(readBack));
        // @@author -

        // EP: save and read roster using default file path
        // Save and read without specifying file path
        original.addPerson(IDA);
        // @@author qiyuannn
        jsonRosterStorage.saveRoster(original); // file path not specified
        readBack = jsonRosterStorage.readRoster().get(); // file path not specified
        assertEquals(original, new AthleteRoster(readBack));
        // @@author -

    }

    // EP: null roster object
    @Test
    // @@author qiyuannn
    public void saveRoster_nullAthleteRoster_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveRoster(null, "SomeFile.json")); // Boundary value
        // @@author -
    }

    /**
     * Saves {@code roster} at the specified {@code filePath}.
     */
    // @@author qiyuannn
    private void saveRoster(ReadOnlyRoster roster, String filePath) {
        // @@author -
        try {
            // @@author qiyuannn
            new JsonRosterStorage(Paths.get(filePath))
                    .saveRoster(roster, addToTestDataPathIfNotNull(filePath));
            // @@author -
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    // EP: null file path when saving roster
    @Test
    // @@author qiyuannn
    public void saveRoster_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveRoster(new AthleteRoster(), null)); // Boundary value
        // @@author -
    }
}
// @@author
