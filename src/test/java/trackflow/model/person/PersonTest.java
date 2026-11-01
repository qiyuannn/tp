// @@author qiyuannn
package trackflow.model.person;
// @@author

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
// @@author qiyuannn
import static trackflow.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static trackflow.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static trackflow.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static trackflow.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static trackflow.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalPersons.ALICE;
import static trackflow.testutil.TypicalPersons.BOB;
// @@author

import org.junit.jupiter.api.Test;

// @@author qiyuannn
import trackflow.testutil.PersonBuilder;
// @@author

public class PersonTest {

    // EP: immutable tag set (modifying returned set throws UnsupportedOperationException)
    @Test
    public void getTags_modifySet_throwsUnsupportedOperationException() {
        Person person = new PersonBuilder().build();
        assertThrows(UnsupportedOperationException.class, () -> person.getTags().clear());
    }

    @Test
    public void isSamePerson() {
        // EP: reflexive identity (same object instance)
        assertTrue(ALICE.isSamePerson(ALICE));

        // EP: null person
        assertFalse(ALICE.isSamePerson(null)); // Boundary value

        // EP: same name and one matching contact detail (other contact detail differs)
        assertTrue(ALICE.isSamePerson(new PersonBuilder(ALICE).withPhone(VALID_PHONE_BOB).build()));
        assertTrue(ALICE.isSamePerson(new PersonBuilder(ALICE).withEmail(VALID_EMAIL_BOB).build()));

        // EP: different name with identical contact details
        assertFalse(ALICE.isSamePerson(new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build()));

        // EP: same name and contact details with different non-identity attributes (age category)
        assertTrue(ALICE.isSamePerson(new PersonBuilder(ALICE).withAgeCategory("Under 14").build()));

        // EP: same name with all contact details different (neither phone nor email matches)
        assertFalse(ALICE.isSamePerson(new PersonBuilder(ALICE).withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_BOB).build()));

        // EP: same identity with case variation and redundant whitespace in name or email
        Person normalizedAlice = new PersonBuilder(ALICE).withName("Alice   Pauline ")
                .withEmail(ALICE.getEmail().value.toUpperCase()).build();
        assertTrue(ALICE.isSamePerson(normalizedAlice));

        // EP: same identity with different non-identity fields (address, remark, tags)
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB)
                .withRemark("Different remark")
                .withTags(VALID_TAG_HUSBAND).build();
        assertTrue(ALICE.isSamePerson(editedAlice));
    }

    @Test
    public void hasSameContactDetails() {
        // EP: null person
        assertFalse(ALICE.hasSameContactDetails(null)); // Boundary value

        // EP: reflexive identity (same object instance)
        assertTrue(ALICE.hasSameContactDetails(ALICE));

        // EP: same phone number with different name
        assertTrue(ALICE.hasSameContactDetails(new PersonBuilder(BOB).withPhone(ALICE.getPhone().value).build()));

        // EP: same email (case-insensitive) with different name
        assertTrue(ALICE.hasSameContactDetails(
                new PersonBuilder(BOB).withEmail(ALICE.getEmail().value.toUpperCase()).build()));

        // EP: completely different contact details (both phone and email differ)
        assertFalse(ALICE.hasSameContactDetails(BOB));

        // EP: phone matching via significant digits with notes and international country code formatting
        Person local = new PersonBuilder(ALICE).withPhone("81234567").withEmail("local@example.com").build();
        Person withNote = new PersonBuilder(local).withPhone("81234567 (Office 2)")
                .withEmail("note@example.com").build();
        Person withCountryCode = new PersonBuilder(local).withPhone("(+65) 81234567")
                .withEmail("country@example.com").build();
        assertTrue(local.isSamePerson(withNote));
        assertFalse(local.isSamePerson(withCountryCode));
        assertTrue(withCountryCode.isSamePerson(new PersonBuilder(withCountryCode)
                .withPhone("6581234567 (Office 2)").withEmail("other@example.com").build()));

        // EP: phone numbers where digits inside parenthesized notes are excluded from significant digits
        Person withNumericNote = new PersonBuilder(local).withPhone("200 (5)").withEmail("x1@example.com")
                .build();
        assertFalse(withNumericNote.isSamePerson(new PersonBuilder(withNumericNote).withPhone("2005")
                .withEmail("x2@example.com").build()));
    }

    @Test
    public void equals() {
        // EP: symmetric equality (different instance with same field values and hash code)
        Person aliceCopy = new PersonBuilder(ALICE).build();
        assertTrue(ALICE.equals(aliceCopy));
        assertEquals(ALICE.hashCode(), aliceCopy.hashCode());

        // EP: reflexive equality (same object instance)
        assertTrue(ALICE.equals(ALICE));

        // EP: null comparison
        assertFalse(ALICE.equals(null)); // Boundary value

        // EP: different object type
        assertFalse(ALICE.equals(5));

        // EP: different person instance with completely different fields
        assertFalse(ALICE.equals(BOB));

        // EP: different name
        Person editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // EP: different phone
        editedAlice = new PersonBuilder(ALICE).withPhone(VALID_PHONE_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // EP: different email
        editedAlice = new PersonBuilder(ALICE).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // EP: different address
        editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // EP: different age category
        editedAlice = new PersonBuilder(ALICE).withAgeCategory("Under 14").build();
        assertFalse(ALICE.equals(editedAlice));

        // EP: different remark
        editedAlice = new PersonBuilder(ALICE).withRemark("Different remark").build();
        assertFalse(ALICE.equals(editedAlice));

        // EP: different tags
        editedAlice = new PersonBuilder(ALICE).withTags(VALID_TAG_HUSBAND).build();
        assertFalse(ALICE.equals(editedAlice));
    }

    // EP: valid string representation of Person containing all attributes
    @Test
    public void toStringMethod() {
        String expected = Person.class.getCanonicalName() + "{name=" + ALICE.getName()
                + ", ageCategory=" + ALICE.getAgeCategory()
                + ", phone=" + ALICE.getPhone()
                + ", email=" + ALICE.getEmail() + ", address=" + ALICE.getAddress()
                + ", remark=" + ALICE.getRemark() + ", tags=" + ALICE.getTags() + "}";
        assertEquals(expected, ALICE.toString());
    }
}
