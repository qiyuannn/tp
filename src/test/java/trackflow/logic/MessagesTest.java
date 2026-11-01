package trackflow.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import trackflow.model.person.Person;
import trackflow.testutil.PersonBuilder;

/**
 * Checks that shared-contact warnings distinguish records and identify the matching contact fields.
 */
public class MessagesTest {

    @Test
    public void getPossibleDuplicateWarning_noSharedContacts_returnsEmpty() {
        Person added = new PersonBuilder().withName("Blake Tan")
                .withPhone("91111111").withEmail("blake@example.com").build();
        Person other = new PersonBuilder().withName("Alex Tan")
                .withPhone("92222222").withEmail("alex@example.com").build();

        // EP: duplicate check against empty athlete roster
        assertEquals("", Messages.getPossibleDuplicateWarning(added, List.of())); // Boundary value
        // EP: duplicate check against non-empty roster with no shared contact details
        assertEquals("", Messages.getPossibleDuplicateWarning(added, List.of(other)));
    }

    @Test
    public void getPossibleDuplicateWarning_sameNamedMatches_identifiesEachRecordAndField() {
        // EP: multiple records with identical names matching different contact fields
        Person phoneMatch = new PersonBuilder().withName("Alex Tan")
                .withPhone("91111111").withEmail("alex1@example.com").build();
        Person emailMatch = new PersonBuilder().withName("Alex Tan")
                .withPhone("92222222").withEmail("alex2@example.com").build();
        Person unrelated = new PersonBuilder().withName("Other Athlete")
                .withPhone("93333333").withEmail("other@example.com").build();
        Person added = new PersonBuilder().withName("Blake Tan")
                .withPhone("91111111").withEmail("alex2@example.com").build();

        String expected = "\nWarning: possible duplicate. Shared contact details:\n"
                + "- Alex Tan; Age category: Open; Phone: 91111111; Email: alex1@example.com; Matches: phone\n"
                + "- Alex Tan; Age category: Open; Phone: 92222222; Email: alex2@example.com; Matches: email\n"
                + "Please review the records.";
        assertEquals(expected, Messages.getPossibleDuplicateWarning(added,
                List.of(phoneMatch, unrelated, emailMatch)));
    }

    @Test
    public void getPossibleDuplicateWarning_formattedPhoneWithNumericNotes_identifiesPhoneMatch() {
        // EP: phone match with differing formatting and office notes
        Person added = new PersonBuilder().withName("Blake Tan")
                .withPhone("(+65) 9123 4567 (Office 1)").withEmail("blake@example.com").build();
        Person other = new PersonBuilder().withName("Alex Tan")
                .withPhone("+65 91234567 (Office 2)").withEmail("alex@example.com").build();

        assertEquals(String.format(Messages.MESSAGE_POSSIBLE_DUPLICATE,
                "- Alex Tan; Age category: Open; Phone: +65 91234567 (Office 2); "
                        + "Email: alex@example.com; Matches: phone"),
                Messages.getPossibleDuplicateWarning(added, List.of(other)));
    }

    @Test
    public void getPossibleDuplicateWarning_emailCaseDiffers_identifiesEmailMatch() {
        // EP: email match across differing case variations
        Person added = new PersonBuilder().withName("Blake Tan")
                .withPhone("91111111").withEmail("COACH@example.com").build();
        Person other = new PersonBuilder().withName("Alex Tan")
                .withPhone("92222222").withEmail("coach@example.com").build();

        assertEquals(String.format(Messages.MESSAGE_POSSIBLE_DUPLICATE,
                "- Alex Tan; Age category: Open; Phone: 92222222; Email: coach@example.com; Matches: email"),
                Messages.getPossibleDuplicateWarning(added, List.of(other)));
    }

    @Test
    public void getPossibleDuplicateWarning_bothContactsMatch_listsRecordOnceWithBothFields() {
        // EP: single record matching both phone and email contact fields
        Person added = new PersonBuilder().withName("Blake Tan")
                .withPhone("9123 4567 (HP)").withEmail("COACH@example.com").build();
        Person other = new PersonBuilder().withName("Alex Tan")
                .withPhone("91234567").withEmail("coach@example.com").build();

        assertEquals(String.format(Messages.MESSAGE_POSSIBLE_DUPLICATE,
                "- Alex Tan; Age category: Open; Phone: 91234567; Email: coach@example.com; Matches: phone and email"),
                Messages.getPossibleDuplicateWarning(added, List.of(other)));
    }

    @Test
    public void getPossibleDuplicateWarning_differentCountryCode_doesNotMatchPhone() {
        // EP: phone numbers with different country codes treated as non-matching
        Person added = new PersonBuilder().withName("Blake Tan")
                .withPhone("(+65) 91234567").withEmail("blake@example.com").build();
        Person other = new PersonBuilder().withName("Alex Tan")
                .withPhone("91234567").withEmail("alex@example.com").build();

        assertEquals("", Messages.getPossibleDuplicateWarning(added, List.of(other)));
    }

    @Test
    public void getPossibleDuplicateWarning_duplicateIdentity_doesNotWarn() {
        // EP: candidate matching athlete with identical person identity excluded from warning
        Person added = new PersonBuilder().withName("Alex Tan")
                .withPhone("91234567").withEmail("alex@example.com").build();
        Person duplicate = new PersonBuilder(added).withName("ALEX   TAN").build();

        assertEquals("", Messages.getPossibleDuplicateWarning(added, List.of(added, duplicate)));
    }
}
