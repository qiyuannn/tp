// @@author qiyuannn
package trackflow.storage;
// @@author

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
// @@author qiyuannn
import static trackflow.storage.JsonAdaptedPerson.MISSING_FIELD_MESSAGE_FORMAT;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalPersons.BENSON;
// @@author

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

// @@author qiyuannn
import trackflow.commons.exceptions.IllegalValueException;
import trackflow.model.person.Address;
import trackflow.model.person.AgeCategory;
import trackflow.model.person.Email;
import trackflow.model.person.Name;
import trackflow.model.person.Person;
import trackflow.model.person.Phone;
import trackflow.model.tag.Tag;
import trackflow.testutil.PersonBuilder;
// @@author

public class JsonAdaptedPersonTest {
    private static final String INVALID_NAME = "R@chel";
    private static final String INVALID_PHONE = "911a";
    private static final String INVALID_ADDRESS = " ";
    private static final String INVALID_EMAIL = "example.com";
    private static final String INVALID_TAG = "#friend";

    private static final String VALID_NAME = BENSON.getName().toString();
    private static final String VALID_PHONE = BENSON.getPhone().toString();
    private static final String VALID_EMAIL = BENSON.getEmail().toString();
    private static final String VALID_ADDRESS = BENSON.getAddress().toString();
    private static final String VALID_REMARK = BENSON.getRemark().toString();
    private static final List<JsonAdaptedTag> VALID_TAGS = BENSON.getTags().stream()
            .map(JsonAdaptedTag::new)
            .collect(Collectors.toList());

    // EP: valid person details with all fields present
    @Test
    public void toModelType_validPersonDetails_returnsPerson() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(BENSON);
        assertEquals(BENSON, person.toModelType());
    }

    // EP: legacy JSON record omitting category field (defaults to Open)
    @Test
    public void toModelType_legacyCategory_defaultsToOpen() throws Exception {
        JsonAdaptedPerson legacy = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL,
                VALID_ADDRESS, VALID_REMARK, VALID_TAGS);
        assertEquals(BENSON, legacy.toModelType());
    }

    // EP: explicit age category and empty address string
    @Test
    public void toModelType_explicitCategoryAndEmptyAddress_roundTrip() throws Exception {
        Person athlete = new PersonBuilder(BENSON).withAgeCategory("Under 16")
                .withAddress("").build(); // Boundary value
        assertEquals(athlete, new JsonAdaptedPerson(athlete).toModelType());
    }

    // EP: invalid age category (empty string and unsupported category)
    @Test
    public void toModelType_invalidCategory_throwsIllegalValueException() {
        for (String category : new String[] {
            "", // Boundary value
            "Under 15"}) {
            JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL,
                    VALID_ADDRESS, VALID_REMARK, VALID_TAGS, category);
            assertThrows(IllegalValueException.class, AgeCategory.MESSAGE_CONSTRAINTS, person::toModelType);
        }
    }

    // EP: invalid name containing invalid characters
    @Test
    public void toModelType_invalidName_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(INVALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_REMARK, VALID_TAGS);
        String expectedMessage = Name.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    // EP: null name field (missing field)
    @Test
    public void toModelType_nullName_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(null, // Boundary value
                        VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_REMARK, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    // EP: invalid phone containing non-numeric characters
    @Test
    public void toModelType_invalidPhone_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, INVALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_REMARK, VALID_TAGS);
        String expectedMessage = Phone.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    // EP: null phone field (missing field)
    @Test
    public void toModelType_nullPhone_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, null, // Boundary value
                        VALID_EMAIL, VALID_ADDRESS, VALID_REMARK, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    // EP: invalid email format (missing local part and domain separation)
    @Test
    public void toModelType_invalidEmail_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, INVALID_EMAIL, VALID_ADDRESS, VALID_REMARK, VALID_TAGS);
        String expectedMessage = Email.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    // EP: pathological email with extreme input lengths
    @Test
    public void toModelType_pathologicalEmail_throwsIllegalValueExceptionQuickly() {
        Duration timeout = Duration.ofMillis(200);

        // EP: extremely long email local part
        JsonAdaptedPerson personLongLocal = new JsonAdaptedPerson(
                VALID_NAME, VALID_PHONE, "a".repeat(5000), // Boundary value
                VALID_ADDRESS, VALID_REMARK, VALID_TAGS);
        assertTimeoutPreemptively(timeout, () ->
                assertThrows(IllegalValueException.class, Email.MESSAGE_CONSTRAINTS, personLongLocal::toModelType));

        // EP: extremely long email domain part
        JsonAdaptedPerson personLongDomain = new JsonAdaptedPerson(
                VALID_NAME, VALID_PHONE, "a@" + "b".repeat(5000), // Boundary value
                VALID_ADDRESS, VALID_REMARK, VALID_TAGS);
        assertTimeoutPreemptively(timeout, () ->
                assertThrows(IllegalValueException.class, Email.MESSAGE_CONSTRAINTS, personLongDomain::toModelType));

        // EP: deeply nested email subdomains
        JsonAdaptedPerson personStackOverflow = new JsonAdaptedPerson(
                VALID_NAME, VALID_PHONE, "a@" + "a.".repeat(2500), // Boundary value
                VALID_ADDRESS, VALID_REMARK, VALID_TAGS);
        assertTimeoutPreemptively(timeout, () ->
                assertThrows(IllegalValueException.class, Email.MESSAGE_CONSTRAINTS, personStackOverflow::toModelType));
    }

    // EP: null email field (missing field)
    @Test
    public void toModelType_nullEmail_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, null, // Boundary value
                        VALID_ADDRESS, VALID_REMARK, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Email.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    // EP: invalid address containing whitespace only
    @Test
    public void toModelType_invalidAddress_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, INVALID_ADDRESS, // Boundary value
                        VALID_REMARK, VALID_TAGS);
        String expectedMessage = Address.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    // EP: null address field defaults to empty address
    @Test
    public void toModelType_nullAddress_defaultsToEmptyAddress() throws Exception {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, null, // Boundary value
                        VALID_REMARK, VALID_TAGS);
        assertEquals("", person.toModelType().getAddress().value); // Boundary value
    }

    // EP: null remark field defaults to empty remark
    @Test
    public void toModelType_nullRemark_defaultsToEmptyRemark() throws Exception {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, null, // Boundary value
                        VALID_TAGS);
        assertEquals("", person.toModelType().getRemark().value); // Boundary value
    }

    // EP: invalid tag in tag list (violates tag syntax)
    @Test
    public void toModelType_invalidTags_throwsIllegalValueException() {
        List<JsonAdaptedTag> invalidTags = new ArrayList<>(VALID_TAGS);
        invalidTags.add(new JsonAdaptedTag(INVALID_TAG));
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_REMARK, invalidTags);
        assertThrows(IllegalValueException.class, Tag.MESSAGE_CONSTRAINTS, person::toModelType);
    }

    // EP: tag list containing null element
    @Test
    public void toModelType_nullTagElement_throwsIllegalValueException() {
        List<JsonAdaptedTag> tagsWithNull = Collections.singletonList(null); // Boundary value
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_REMARK, tagsWithNull);
        assertThrows(IllegalValueException.class, "Athlete's tags contain a null tag!", person::toModelType);
    }

}
