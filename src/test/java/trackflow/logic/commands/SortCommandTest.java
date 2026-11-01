package trackflow.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.logic.commands.SortCommand.SortField.ADDRESS;
import static trackflow.logic.commands.SortCommand.SortField.AGE;
import static trackflow.logic.commands.SortCommand.SortField.EMAIL;
import static trackflow.logic.commands.SortCommand.SortField.NAME;
import static trackflow.logic.commands.SortCommand.SortField.PHONE;
import static trackflow.logic.commands.SortCommand.SortOrder.ASCENDING;
import static trackflow.logic.commands.SortCommand.SortOrder.DESCENDING;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import trackflow.commons.core.index.Index;
import trackflow.logic.commands.SortCommand.SortField;
import trackflow.logic.commands.SortCommand.SortOrder;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.UserPrefs;
import trackflow.model.person.Person;
import trackflow.testutil.AthleteRosterBuilder;
import trackflow.testutil.PersonBuilder;

public class SortCommandTest {

    private static final Person ZOE = new PersonBuilder()
            .withName("zoe Tan")
            .withAgeCategory("Under 18")
            .withPhone("999")
            .withEmail("zulu@example.com")
            .withAddress("Zebra Road")
            .build();
    private static final Person AMY = new PersonBuilder()
            .withName("Amy Lim")
            .withAgeCategory("Under 14")
            .withPhone("1000")
            .withEmail("alpha@example.com")
            .withAddress("")
            .build();
    private static final Person MIA = new PersonBuilder()
            .withName("Mia Ong")
            .withAgeCategory("Open")
            .withPhone("10000")
            .withEmail("MIKE@example.com")
            .withAddress("Main Street")
            .build();
    private static final Person IAN = new PersonBuilder()
            .withName("Ian Koh")
            .withAgeCategory("Under 16")
            .withPhone("2000")
            .withEmail("ian@example.com")
            .withAddress("Hill Road")
            .build();
    private static final Person UMA = new PersonBuilder()
            .withName("Uma Lee")
            .withAgeCategory("Under 20")
            .withPhone("3000")
            .withEmail("uma@example.com")
            .withAddress("River Road")
            .build();

    private Model model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(new AthleteRosterBuilder()
                .withPerson(ZOE)
                .withPerson(MIA)
                .withPerson(AMY)
                .build(), new UserPrefs());
    }

    // EP: sort by name in ascending order (case-insensitive)
    @Test
    public void execute_nameAscending_sortsAlphabeticallyIgnoringCase() {
        assertSortSuccess(NAME, ASCENDING, List.of(AMY, MIA, ZOE));
    }

    // EP: sort by name in descending order (reverse alphabetical)
    @Test
    public void execute_nameDescending_sortsReverseAlphabetically() {
        assertSortSuccess(NAME, DESCENDING, List.of(ZOE, MIA, AMY));
    }

    // EP: sort by name with repeated spaces normalizes spacing for ordering while preserving stored names
    @Test
    public void execute_nameWithRepeatedSpaces_sortsWithoutChangingNames() {
        Person leeAnn = new PersonBuilder(AMY).withName("Lee Ann").build();
        Person leeBob = new PersonBuilder(MIA).withName("Lee  Bob").build();
        Person leeAmy = new PersonBuilder(ZOE).withName("lee   Amy").build();
        model = new ModelManager(new AthleteRosterBuilder().withPerson(leeBob)
                .withPerson(leeAnn).withPerson(leeAmy).build(), new UserPrefs());

        assertSortSuccess(NAME, ASCENDING, List.of(leeAmy, leeAnn, leeBob));
        assertSortSuccess(NAME, DESCENDING, List.of(leeBob, leeAnn, leeAmy));
        assertEquals(List.of("Lee  Bob", "Lee Ann", "lee   Amy"), model.getRoster().getPersonList().stream()
                .map(person -> person.getName().fullName).toList());
    }

    // EP: comparator treats names differing only in case and spacing as equal
    @Test
    public void execute_namesDifferingOnlyInCaseAndSpacing_compareAsEqual() {
        Person leeAnn = new PersonBuilder(AMY).withName("Lee Ann").build();
        Person equivalentName = new PersonBuilder(MIA).withName("LEE   Ann").build();
        model = new ModelManager(new AthleteRosterBuilder().withPerson(leeAnn)
                .withPerson(equivalentName).build(), new UserPrefs());

        new SortCommand(NAME, ASCENDING).execute(model);

        assertEquals(0, model.getPersonListComparator().compare(leeAnn, equivalentName));
    }

    // EP: sort by age category in ascending order (Under 14 to Open)
    @Test
    public void execute_ageAscending_sortsByCompetitionCategory() {
        useModelWithAllAgeCategories();
        assertSortSuccess(AGE, ASCENDING, List.of(AMY, IAN, ZOE, UMA, MIA));
    }

    // EP: sort by age category in descending order (Open to Under 14)
    @Test
    public void execute_ageDescending_sortsByReverseCompetitionCategory() {
        useModelWithAllAgeCategories();
        assertSortSuccess(AGE, DESCENDING, List.of(MIA, UMA, ZOE, IAN, AMY));
    }

    // EP: sort by phone in ascending order (numerical order)
    @Test
    public void execute_phoneAscending_sortsNumerically() {
        assertSortSuccess(PHONE, ASCENDING, List.of(ZOE, AMY, MIA));
    }

    // EP: sort by phone in descending order (reverse numerical order)
    @Test
    public void execute_phoneDescending_sortsReverseNumerically() {
        assertSortSuccess(PHONE, DESCENDING, List.of(MIA, AMY, ZOE));
    }

    // EP: sort by formatted phone numbers strips formatting for comparison while preserving stored values
    @Test
    public void execute_formattedPhones_sortsDigitsWithoutChangingStoredValues() {
        for (String phone : List.of("1 000", "+1-000", "(+1) 000", "1000 (HP)", "000 1000")) {
            Person formattedAmy = new PersonBuilder(AMY).withPhone(phone).build();
            Person longPhoneMia = new PersonBuilder(MIA).withPhone("9".repeat(100)).build(); // Boundary value
            model = new ModelManager(new AthleteRosterBuilder().withPerson(longPhoneMia)
                    .withPerson(formattedAmy).withPerson(ZOE).build(), new UserPrefs());

            assertSortSuccess(PHONE, ASCENDING, List.of(ZOE, formattedAmy, longPhoneMia));
            assertSortSuccess(PHONE, DESCENDING, List.of(longPhoneMia, formattedAmy, ZOE));
            assertEquals(phone, formattedAmy.getPhone().value);
        }
    }

    // EP: sort by phone with notes extracts significant digits and retains country codes
    @Test
    public void execute_phoneNotes_sortsSignificantDigitsAndKeepsCountryCode() {
        for (String phone : List.of("200 (5)", "2 00 (Office 9)", "000200 (1234)")) {
            Person withNote = new PersonBuilder(ZOE).withPhone(phone).build();
            Person withoutNote = new PersonBuilder(AMY).withPhone("300").build();
            Person withCountryCode = new PersonBuilder(MIA).withPhone("(+65) 200").build();
            model = new ModelManager(new AthleteRosterBuilder().withPerson(withCountryCode)
                    .withPerson(withoutNote).withPerson(withNote).build(), new UserPrefs());

            assertSortSuccess(PHONE, ASCENDING, List.of(withNote, withoutNote, withCountryCode));
            assertSortSuccess(PHONE, DESCENDING, List.of(withCountryCode, withoutNote, withNote));
            assertEquals(phone, withNote.getPhone().value);
            assertEquals("(+65) 200", withCountryCode.getPhone().value);
        }
    }

    // EP: sort phone with numeric note and zero digits distinguishes note from country code
    @Test
    public void execute_numericNoteWithZeroNumber_doesNotTreatNoteAsCountryCode() {
        Person zeroNumber = new PersonBuilder(AMY).withPhone("(1) 000").build();
        model = new ModelManager(new AthleteRosterBuilder().withPerson(ZOE)
                .withPerson(zeroNumber).build(), new UserPrefs());

        assertSortSuccess(PHONE, ASCENDING, List.of(zeroNumber, ZOE));
        assertSortSuccess(PHONE, DESCENDING, List.of(ZOE, zeroNumber));
    }

    // EP: sort by email in ascending and descending order (case-insensitive)
    @Test
    public void execute_emailAscendingAndDescending_sortsAlphabeticallyIgnoringCase() {
        assertSortSuccess(EMAIL, ASCENDING, List.of(AMY, MIA, ZOE));
        assertSortSuccess(EMAIL, DESCENDING, List.of(ZOE, MIA, AMY));
    }

    // EP: sort by address in ascending order places empty address first
    @Test
    public void execute_addressAscending_placesEmptyAddressFirst() {
        assertSortSuccess(ADDRESS, ASCENDING, List.of(AMY, MIA, ZOE)); // Boundary value
    }

    // EP: sort by address in descending order places empty address last
    @Test
    public void execute_addressDescending_placesEmptyAddressLast() {
        assertSortSuccess(ADDRESS, DESCENDING, List.of(ZOE, MIA, AMY)); // Boundary value
    }

    // EP: sort on filtered list sorts matches and list restores roster order
    @Test
    public void execute_filteredList_sortsMatchesAndListRestoresRosterOrder() {
        model.updateFilteredPersonList(person -> !person.equals(MIA));
        new SortCommand(NAME, ASCENDING).execute(model);
        assertEquals(List.of(AMY, ZOE), model.getFilteredPersonList());

        new ListAthleteCommand().execute(model);
        assertEquals(List.of(ZOE, MIA, AMY), model.getFilteredPersonList());
    }

    // EP: sort on empty roster succeeds with empty display
    @Test
    public void execute_emptyRoster_succeedsWithEmptyDisplay() {
        model = new ModelManager();

        assertSortSuccess(NAME, ASCENDING, List.of()); // Boundary value
    }

    // EP: add command after sort places new athlete in correct sorted position
    @Test
    public void execute_sortThenAdd_newAthleteAppearsInSortedPosition() {
        Person ben = new PersonBuilder()
                .withName("Ben Tan")
                .withAgeCategory("Under 16")
                .withPhone("4000")
                .withEmail("ben@example.com")
                .build();
        new SortCommand(NAME, ASCENDING).execute(model);

        model.addPerson(ben);

        assertEquals(List.of(AMY, ben, MIA, ZOE), model.getFilteredPersonList());
    }

    // EP: delete command after sort targets 1-based index from sorted display
    @Test
    public void execute_sortThenDelete_usesDisplayedSortedIndex() throws Exception {
        new SortCommand(NAME, ASCENDING).execute(model);
        new DeleteCommand(Index.fromOneBased(1)).execute(model); // Boundary value

        assertFalse(model.hasPerson(AMY));
        assertEquals(List.of(MIA, ZOE), model.getFilteredPersonList());
    }

    @Test
    public void equals() {
        SortCommand command = new SortCommand(NAME, ASCENDING);

        // EP: reflexive equality (same object instance)
        assertTrue(command.equals(command));
        // EP: symmetric equality (different instance with same field and order)
        assertTrue(command.equals(new SortCommand(NAME, ASCENDING)));
        // EP: different sort order
        assertFalse(command.equals(new SortCommand(NAME, DESCENDING)));
        // EP: different sort field
        assertFalse(command.equals(new SortCommand(AGE, ASCENDING)));
        // EP: null comparison
        assertFalse(command.equals(null));
        // EP: different object type
        assertFalse(command.equals(new ClearCommand()));
    }

    // EP: string representation includes field and order
    @Test
    public void toStringMethod() {
        SortCommand command = new SortCommand(AGE, DESCENDING);
        String expected = SortCommand.class.getCanonicalName() + "{field=AGE, order=DESCENDING}";
        assertEquals(expected, command.toString());
    }

    private void assertSortSuccess(SortField field, SortOrder order, List<Person> expectedOrder) {
        List<Person> originalRosterOrder = List.copyOf(model.getRoster().getPersonList());
        SortCommand command = new SortCommand(field, order);
        CommandResult result = command.execute(model);
        String expectedMessage = String.format(SortCommand.MESSAGE_SUCCESS,
                field.name().toLowerCase(),
                order == ASCENDING ? "ascending" : "descending");
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedOrder, model.getFilteredPersonList());
        assertEquals(originalRosterOrder, model.getRoster().getPersonList());
    }

    private void useModelWithAllAgeCategories() {
        model = new ModelManager(new AthleteRosterBuilder()
                .withPerson(ZOE)
                .withPerson(MIA)
                .withPerson(AMY)
                .withPerson(UMA)
                .withPerson(IAN)
                .build(), new UserPrefs());
    }
}
