package trackflow.logic.commands;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalPersons.ALICE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import javafx.collections.ObservableList;
import trackflow.commons.core.GuiSettings;
import trackflow.logic.Messages;
import trackflow.logic.commands.exceptions.CommandException;
import trackflow.model.AthleteRoster;
import trackflow.model.Model;
import trackflow.model.ReadOnlyRoster;
import trackflow.model.ReadOnlyUserPrefs;
import trackflow.model.person.Person;
import trackflow.testutil.PersonBuilder;

public class AddCommandTest {

    // EP: null person to add
    @Test
    public void constructor_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new AddCommand(null)); // Boundary value
    }

    // EP: valid person successfully added to model
    @Test
    public void execute_personAcceptedByModel_addSuccessful() throws Exception {
        ModelStubAcceptingPersonAdded modelStub = new ModelStubAcceptingPersonAdded();
        Person validPerson = new PersonBuilder().build();

        CommandResult commandResult = new AddCommand(validPerson).execute(modelStub);

        assertEquals(String.format(AddCommand.MESSAGE_SUCCESS, Messages.formatAthlete(validPerson)),
                commandResult.getFeedbackToUser());
        assertEquals(List.of(validPerson), modelStub.personsAdded); // Boundary value
    }

    // EP: duplicate person rejected by model
    @Test
    public void execute_duplicatePerson_throwsCommandException() {
        Person validPerson = new PersonBuilder().build();
        AddCommand addCommand = new AddCommand(validPerson);
        ModelStub modelStub = new ModelStubWithPerson(validPerson);

        assertThrows(CommandException.class, String.format(AddCommand.MESSAGE_DUPLICATE_PERSON,
                validPerson.getName()), () -> addCommand.execute(modelStub));
    }

    // EP: person with shared contact details added with duplicate warning
    @Test
    public void execute_sharedContactWithDifferentNames_addsWithWarning() throws Exception {
        Person firstMatch = new PersonBuilder().withName("First Match")
                .withPhone("91234567").withEmail("first@example.com").build();
        Person secondMatch = new PersonBuilder().withName("Second Match")
                .withPhone("81112222").withEmail("SECOND@example.com").build();
        ModelStubAcceptingPersonAdded modelStub = new ModelStubAcceptingPersonAdded();
        modelStub.addPerson(firstMatch);
        modelStub.addPerson(secondMatch);

        Person added = new PersonBuilder().withName("New Athlete")
                .withPhone("91234567").withEmail("second@example.com").build();
        CommandResult result = new AddCommand(added).execute(modelStub);

        assertEquals(String.format(AddCommand.MESSAGE_SUCCESS, Messages.formatAthlete(added))
                + String.format(Messages.MESSAGE_POSSIBLE_DUPLICATE,
                        "- First Match; Age category: Open; Phone: 91234567; Email: first@example.com; Matches: phone\n"
                        + "- Second Match; Age category: Open; Phone: 81112222; "
                        + "Email: SECOND@example.com; Matches: email"),
                result.getFeedbackToUser());
    }

    @Test
    public void equals() {
        Person alice = new PersonBuilder().withName("Alice").build();
        Person bob = new PersonBuilder().withName("Bob").build();
        AddCommand addAliceCommand = new AddCommand(alice);
        AddCommand addBobCommand = new AddCommand(bob);

        // EP: reflexive equality (same object instance)
        assertTrue(addAliceCommand.equals(addAliceCommand));

        // EP: symmetric equality (different instance with same person)
        AddCommand addAliceCommandCopy = new AddCommand(alice);
        assertTrue(addAliceCommand.equals(addAliceCommandCopy));

        // EP: different object type
        assertFalse(addAliceCommand.equals(1));

        // EP: null comparison
        assertFalse(addAliceCommand.equals(null)); // Boundary value

        // EP: different person to add
        assertFalse(addAliceCommand.equals(addBobCommand));
    }

    // EP: string representation of add command
    @Test
    public void toStringMethod() {
        AddCommand addCommand = new AddCommand(ALICE);
        String expected = AddCommand.class.getCanonicalName() + "{toAdd=" + ALICE + "}";
        assertEquals(expected, addCommand.toString());
    }

    // EP: message usage contains all enumerated valid age categories
    @Test
    public void messageUsage_listsAllValidAgeCategories() {
        List<String> validCategories = List.of("Under 14", "Under 16", "Under 18", "Under 20", "Open");
        assertTrue(AddCommand.MESSAGE_USAGE.contains(
                "a/AGE_CATEGORY (Under 14, Under 16, Under 18, Under 20, or Open)"));
        for (String category : validCategories) {
            assertTrue(AddCommand.MESSAGE_USAGE.contains(category));
        }
    }

    /**
     * A default model stub that has all of the methods failing.
     */
    private class ModelStub implements Model {
        @Override
        public ReadOnlyUserPrefs getUserPrefs() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public GuiSettings getGuiSettings() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setGuiSettings(GuiSettings guiSettings) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void addPerson(Person person) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setRoster(ReadOnlyRoster newData) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ReadOnlyRoster getRoster() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public boolean hasPerson(Person person) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void deletePerson(Person target) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void setPerson(Person target, Person editedPerson) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public ObservableList<Person> getFilteredPersonList() {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void updateFilteredPersonList(Predicate<Person> predicate) {
            throw new AssertionError("This method should not be called.");
        }

        @Override
        public void updateSortedPersonList(Comparator<Person> comparator) {
            throw new AssertionError("This method should not be called.");
        }
    }

    /**
     * A Model stub that contains a single person.
     */
    private class ModelStubWithPerson extends ModelStub {
        private final Person person;

        ModelStubWithPerson(Person person) {
            requireNonNull(person);
            this.person = person;
        }

        @Override
        public boolean hasPerson(Person person) {
            requireNonNull(person);
            return this.person.isSamePerson(person);
        }

        @Override
        public ReadOnlyRoster getRoster() {
            AthleteRoster roster = new AthleteRoster();
            roster.addPerson(person);
            return roster;
        }
    }

    /**
     * A Model stub that always accepts the person being added.
     */
    private class ModelStubAcceptingPersonAdded extends ModelStub {
        final ArrayList<Person> personsAdded = new ArrayList<>();

        @Override
        public boolean hasPerson(Person person) {
            requireNonNull(person);
            return personsAdded.stream().anyMatch(person::isSamePerson);
        }

        @Override
        public void addPerson(Person person) {
            requireNonNull(person);
            personsAdded.add(person);
        }

        @Override
        public ReadOnlyRoster getRoster() {
            AthleteRoster roster = new AthleteRoster();
            for (Person person : personsAdded) {
                roster.addPerson(person);
            }
            return roster;
        }
    }

}
