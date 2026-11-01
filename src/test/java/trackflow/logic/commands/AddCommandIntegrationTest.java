// @@author Austin-Chia
package trackflow.logic.commands;
// @@author

// @@author Austin-Chia
import static trackflow.logic.commands.CommandTestUtil.assertCommandFailure;
import static trackflow.logic.commands.CommandTestUtil.assertCommandSuccess;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;
// @@author

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// @@author Austin-Chia
import trackflow.logic.Messages;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.UserPrefs;
import trackflow.model.person.Person;
import trackflow.testutil.PersonBuilder;
// @@author

/**
 * Contains integration tests (interaction with the Model) for {@code AddCommand}.
 */
public class AddCommandIntegrationTest {

    private Model model;

    @BeforeEach
    public void setUp() {
        // @@author Austin-Chia
        model = new ModelManager(getTypicalRoster(), new UserPrefs());
        // @@author
    }

    // @@author Austin-Chia
    // EP: shared contact with person outside currently filtered (empty) list
    @Test
    public void execute_sharedContactOutsideFilteredList_succeedsWithWarning() {
        Person existing = model.getRoster().getPersonList().get(0); // Boundary value
        model.updateFilteredPersonList(person -> false); // Boundary value
        Person candidate = new PersonBuilder(existing).withName("Different Athlete")
                .withEmail("new@example.com").build();
        Model expectedModel = new ModelManager(model.getRoster(), new UserPrefs());
        expectedModel.updateFilteredPersonList(person -> false);
        expectedModel.addPerson(candidate);
        String expected = String.format(AddCommand.MESSAGE_SUCCESS, Messages.formatAthlete(candidate))
                + String.format(Messages.MESSAGE_POSSIBLE_DUPLICATE,
                        "- " + Messages.formatAthlete(existing) + "; Matches: phone");
        assertCommandSuccess(new AddCommand(candidate), model, expected, expectedModel);
    }

    // EP: person with same name but distinct phone and email contacts
    @Test
    public void execute_sameNameDifferentContacts_succeedsWithoutWarning() {
        Person existing = model.getRoster().getPersonList().get(0); // Boundary value
        Person candidate = new PersonBuilder(existing).withPhone("81112222").withEmail("new@example.com").build();
        Model expectedModel = new ModelManager(model.getRoster(), new UserPrefs());
        // @@author
        expectedModel.addPerson(candidate);
        assertCommandSuccess(new AddCommand(candidate), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.formatAthlete(candidate)), expectedModel);
    }

    // EP: valid new person added to populated roster
    @Test
    public void execute_newPerson_success() {
        Person validPerson = new PersonBuilder().build();

        // @@author Austin-Chia
        Model expectedModel = new ModelManager(model.getRoster(), new UserPrefs());
        // @@author
        expectedModel.addPerson(validPerson);

        assertCommandSuccess(new AddCommand(validPerson), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.formatAthlete(validPerson)),
                expectedModel);
    }

    // EP: duplicate person already present in roster
    @Test
    public void execute_duplicatePerson_throwsCommandException() {
        // @@author Austin-Chia
        Person personInList = model.getRoster().getPersonList().get(0); // Boundary value
        // @@author
        assertCommandFailure(new AddCommand(personInList), model,
                String.format(AddCommand.MESSAGE_DUPLICATE_PERSON, personInList.getName()));
    }

}
