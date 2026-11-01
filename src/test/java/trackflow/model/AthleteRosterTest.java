// @@author -
// Unassigned sections are adapted from SE-EDU AddressBook-Level3.
package trackflow.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static trackflow.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalPersons.ALICE;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import trackflow.model.person.Person;
import trackflow.model.person.exceptions.DuplicatePersonException;
import trackflow.testutil.PersonBuilder;

public class AthleteRosterTest {

    private final AthleteRoster roster = new AthleteRoster();

    // EP: default constructor initializing empty roster
    @Test
    public void constructor() {
        assertEquals(List.of(), roster.getPersonList()); // Boundary value
    }

    // EP: null roster data
    @Test
    public void resetData_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> roster.resetData(null)); // Boundary value
    }

    // EP: valid read-only roster replacing existing data
    @Test
    public void resetData_withValidReadOnlyRoster_replacesData() {
        AthleteRoster newData = getTypicalRoster();
        roster.resetData(newData);
        assertEquals(newData, roster);
    }

    // EP: roster data containing duplicate persons
    @Test
    public void resetData_withDuplicatePersons_throwsDuplicatePersonException() {
        // Two persons with the same identity fields
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        List<Person> newPersons = List.of(ALICE, editedAlice);
        AthleteRosterStub newData = new AthleteRosterStub(newPersons);

        assertThrows(DuplicatePersonException.class, () -> roster.resetData(newData));
    }

    // EP: null person
    @Test
    public void hasPerson_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> roster.hasPerson(null)); // Boundary value
    }

    // EP: person not in roster (empty roster)
    @Test
    public void hasPerson_personNotInAthleteRoster_returnsFalse() {
        assertFalse(roster.hasPerson(ALICE)); // Boundary value
    }

    // EP: person present in roster (singleton roster)
    @Test
    public void hasPerson_personInAthleteRoster_returnsTrue() {
        roster.addPerson(ALICE); // Boundary value
        assertTrue(roster.hasPerson(ALICE));
    }

    // EP: person with same identity fields but different non-identity attributes present in roster
    @Test
    public void hasPerson_personWithSameIdentityFieldsInAthleteRoster_returnsTrue() {
        roster.addPerson(ALICE);
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        assertTrue(roster.hasPerson(editedAlice));
    }

    // EP: unmodifiable person list view (direct modification rejected)
    @Test
    public void getPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> roster.getPersonList().remove(0)); // Boundary value
    }

    // EP: default AthleteRoster string representation
    @Test
    public void toStringMethod() {
        String expected = AthleteRoster.class.getCanonicalName() + "{persons=" + roster.getPersonList() + "}";
        assertEquals(expected, roster.toString());
    }

    /**
     * A stub ReadOnlyRoster whose persons list can violate interface constraints.
     */
    private static class AthleteRosterStub implements ReadOnlyRoster {
        private final ObservableList<Person> persons = FXCollections.observableArrayList();

        AthleteRosterStub(Collection<Person> persons) {
            this.persons.setAll(persons);
        }

        @Override
        public ObservableList<Person> getPersonList() {
            return persons;
        }
    }

}
// @@author
