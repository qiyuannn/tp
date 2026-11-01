package trackflow.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.logic.commands.CommandTestUtil.assertCommandFailure;
import static trackflow.logic.commands.CommandTestUtil.assertCommandSuccess;
import static trackflow.logic.commands.CommandTestUtil.showPersonAtIndex;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static trackflow.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;

import org.junit.jupiter.api.Test;

import trackflow.commons.core.index.Index;
import trackflow.logic.Messages;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.UserPrefs;
import trackflow.model.person.Person;

/**
 * Contains integration tests (interaction with the Model) and unit tests for
 * {@code DeleteCommand}.
 */
public class DeleteCommandTest {

    private Model model = new ModelManager(getTypicalRoster(), new UserPrefs());

    // EP: valid 1-based index within unfiltered list range
    @Test
    public void execute_validIndexUnfilteredList_success() {
        Person personToDelete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_PERSON); // Boundary value

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_ATHLETE_SUCCESS,
                Messages.formatAthlete(personToDelete));

        ModelManager expectedModel = new ModelManager(model.getRoster(), new UserPrefs());
        expectedModel.deletePerson(personToDelete);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    // EP: invalid index exceeding unfiltered list size
    @Test
    public void execute_invalidIndexUnfilteredList_throwsCommandException() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1); // Boundary value
        DeleteCommand deleteCommand = new DeleteCommand(outOfBoundIndex);

        assertCommandFailure(deleteCommand, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    // EP: null model reference
    @Test
    public void execute_nullModel_throwsNullPointerException() {
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_PERSON); // Boundary value
        assertThrows(NullPointerException.class, () -> deleteCommand.execute(null)); // Boundary value
    }

    // EP: valid 1-based index within filtered list range
    @Test
    public void execute_validIndexFilteredList_success() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Person personToDelete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_PERSON); // Boundary value

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_ATHLETE_SUCCESS,
                Messages.formatAthlete(personToDelete));

        Model expectedModel = new ModelManager(model.getRoster(), new UserPrefs());
        expectedModel.deletePerson(personToDelete);
        showNoPerson(expectedModel);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    // EP: invalid index exceeding filtered list size but within athlete roster size
    @Test
    public void execute_invalidIndexFilteredList_throwsCommandException() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Index outOfBoundIndex = INDEX_SECOND_PERSON; // Boundary value
        // ensures that outOfBoundIndex is still in bounds of athlete roster list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getRoster().getPersonList().size());

        DeleteCommand deleteCommand = new DeleteCommand(outOfBoundIndex);

        assertCommandFailure(deleteCommand, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        DeleteCommand deleteFirstCommand = new DeleteCommand(INDEX_FIRST_PERSON);
        DeleteCommand deleteSecondCommand = new DeleteCommand(INDEX_SECOND_PERSON);

        // EP: reflexive equality (same object instance)
        assertTrue(deleteFirstCommand.equals(deleteFirstCommand));

        // EP: symmetric equality (different instance with same index)
        DeleteCommand deleteFirstCommandCopy = new DeleteCommand(INDEX_FIRST_PERSON);
        assertTrue(deleteFirstCommand.equals(deleteFirstCommandCopy));

        // EP: different object type
        assertFalse(deleteFirstCommand.equals(1));

        // EP: null comparison
        assertFalse(deleteFirstCommand.equals(null)); // Boundary value

        // EP: different target index
        assertFalse(deleteFirstCommand.equals(deleteSecondCommand));
    }

    // EP: string representation of delete command
    @Test
    public void toStringMethod() {
        Index targetIndex = Index.fromOneBased(1); // Boundary value
        DeleteCommand deleteCommand = new DeleteCommand(targetIndex);
        String expected = DeleteCommand.class.getCanonicalName() + "{targetIndex=" + targetIndex + "}";
        assertEquals(expected, deleteCommand.toString());
    }

    /**
     * Updates {@code model}'s filtered list to show no one.
     */
    private void showNoPerson(Model model) {
        model.updateFilteredPersonList(p -> false);

        assertTrue(model.getFilteredPersonList().isEmpty());
    }
}
