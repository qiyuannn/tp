// @@author -
// Unassigned sections are adapted from SE-EDU AddressBook-Level3.
// @@author Austin-Chia
package trackflow.storage;
// @@author -

import static org.junit.jupiter.api.Assertions.assertEquals;
// @@author Austin-Chia
import static trackflow.testutil.Assert.assertThrows;
// @@author -

import java.nio.file.Path;
import java.nio.file.Paths;
// @@author qiyuannn
import java.util.Collections;
// @@author Austin-Chia
import java.util.List;
// @@author -

import org.junit.jupiter.api.Test;

// @@author Austin-Chia
import trackflow.commons.exceptions.IllegalValueException;
import trackflow.commons.util.JsonUtil;
import trackflow.model.AthleteRoster;
import trackflow.model.person.Person;
import trackflow.testutil.PersonBuilder;
import trackflow.testutil.TypicalPersons;
// @@author -

// @@author Austin-Chia
public class JsonSerializableRosterTest {
    // @@author -

    // @@author Austin-Chia
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonSerializableRosterTest");
    // @@author -
    private static final Path TYPICAL_PERSONS_FILE = TEST_DATA_FOLDER.resolve("typicalRoster.json");
    // @@author Austin-Chia
    private static final Path INVALID_PERSON_FILE = TEST_DATA_FOLDER.resolve("invalidPersonAthleteRoster.json");
    private static final Path DUPLICATE_PERSON_FILE = TEST_DATA_FOLDER.resolve("duplicatePersonAthleteRoster.json");
    // @@author -

    // EP: roster containing duplicate persons with same identity
    // @@author Austin-Chia
    @Test
    public void toModelType_sameNameSharedContact_throwsIllegalValueException() {
        Person original = new PersonBuilder().build();
        Person duplicate = new PersonBuilder(original).withEmail("changed@example.com")
                .withAgeCategory("Under 14").build();
        JsonSerializableRoster saved = new JsonSerializableRoster(List.of(
                new JsonAdaptedPerson(original), new JsonAdaptedPerson(duplicate)));
        assertThrows(IllegalValueException.class, JsonSerializableRoster.MESSAGE_DUPLICATE_PERSON,
                saved::toModelType);
    }

    // EP: valid typical roster JSON file
    // @@author -
    @Test
    public void toModelType_typicalPersonsFile_success() throws Exception {
        // @@author Austin-Chia
        JsonSerializableRoster dataFromFile = JsonUtil.readJsonFile(TYPICAL_PERSONS_FILE,
                JsonSerializableRoster.class).get();
        AthleteRoster rosterFromFile = dataFromFile.toModelType();
        // @@author -
        AthleteRoster typicalRoster = TypicalPersons.getTypicalRoster();
        assertEquals(rosterFromFile, typicalRoster);
    }

    // EP: roster JSON file containing invalid person details
    @Test
    public void toModelType_invalidPersonFile_throwsIllegalValueException() throws Exception {
        // @@author Austin-Chia
        JsonSerializableRoster dataFromFile = JsonUtil.readJsonFile(INVALID_PERSON_FILE,
                JsonSerializableRoster.class).get();
        // @@author -
        assertThrows(IllegalValueException.class, dataFromFile::toModelType);
    }

    // EP: roster JSON file with duplicate person entries
    @Test
    public void toModelType_duplicatePersons_throwsIllegalValueException() throws Exception {
        // @@author Austin-Chia
        JsonSerializableRoster dataFromFile = JsonUtil.readJsonFile(DUPLICATE_PERSON_FILE,
                JsonSerializableRoster.class).get();
        assertThrows(IllegalValueException.class, JsonSerializableRoster.MESSAGE_DUPLICATE_PERSON,
                // @@author -
                dataFromFile::toModelType);
    }

    // EP: null persons list (missing athlete list)
    // @@author qiyuannn
    @Test
    public void toModelType_nullPersonsList_throwsIllegalValueException() {
        JsonSerializableRoster roster = new JsonSerializableRoster((List<JsonAdaptedPerson>) null); // Boundary value
        assertThrows(IllegalValueException.class, "Athlete list is missing!", roster::toModelType);
    }

    // EP: persons list containing a null element
    @Test
    public void toModelType_nullPersonElement_throwsIllegalValueException() {
        JsonSerializableRoster roster = new JsonSerializableRoster(Collections.singletonList(null)); // Boundary value
        assertThrows(IllegalValueException.class, "Athlete list contains a null athlete!", roster::toModelType);
    }

    // @@author -
}
// @@author
