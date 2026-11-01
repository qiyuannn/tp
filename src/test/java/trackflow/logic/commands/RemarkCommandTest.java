package trackflow.logic.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.logic.commands.CommandTestUtil.VALID_REMARK_AMY;
import static trackflow.logic.commands.CommandTestUtil.VALID_REMARK_BOB;
import static trackflow.logic.commands.CommandTestUtil.assertCommandFailure;
import static trackflow.logic.commands.CommandTestUtil.assertCommandSuccess;
import static trackflow.logic.commands.CommandTestUtil.showPersonAtIndex;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static trackflow.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;

import org.junit.jupiter.api.Test;

import trackflow.commons.core.index.Index;
import trackflow.logic.Messages;
import trackflow.model.AthleteRoster;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.UserPrefs;
import trackflow.model.person.Person;
import trackflow.model.person.Remark;
import trackflow.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for {@code RemarkCommand}.
 */
public class RemarkCommandTest {

    private Model model = new ModelManager(getTypicalRoster(), new UserPrefs());

    // EP: null target index or null remark in constructor
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new RemarkCommand(null, // Boundary value
                new Remark(VALID_REMARK_AMY)));
        assertThrows(NullPointerException.class, () -> new RemarkCommand(INDEX_FIRST_PERSON, null)); // Boundary value
    }

    // EP: null model reference
    @Test
    public void execute_nullModel_throwsNullPointerException() {
        RemarkCommand remarkCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(VALID_REMARK_AMY));
        assertThrows(NullPointerException.class, () -> remarkCommand.execute(null)); // Boundary value
    }

    // EP: add non-empty remark to person at valid first index in unfiltered list
    @Test
    public void execute_addRemarkUnfilteredList_success() {
        Person personToEdit = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Remark remark = new Remark(VALID_REMARK_AMY);
        RemarkCommand remarkCommand = new RemarkCommand(INDEX_FIRST_PERSON, remark); // Boundary value
        Person editedPerson = new PersonBuilder(personToEdit).withRemark(VALID_REMARK_AMY).build();

        String expectedMessage = String.format(RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS,
                Messages.formatAthlete(editedPerson));

        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        expectedModel.setPerson(personToEdit, editedPerson);

        assertCommandSuccess(remarkCommand, model, expectedMessage, expectedModel);
    }

    // EP: delete remark by setting empty remark string in unfiltered list
    @Test
    public void execute_deleteRemarkUnfilteredList_success() {
        Person personToEdit = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        RemarkCommand remarkCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark("")); // Boundary value
        Person editedPerson = new PersonBuilder(personToEdit).withRemark("").build(); // Boundary value

        String expectedMessage = String.format(RemarkCommand.MESSAGE_DELETE_REMARK_SUCCESS,
                Messages.formatAthlete(editedPerson));

        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        expectedModel.setPerson(personToEdit, editedPerson);

        assertCommandSuccess(remarkCommand, model, expectedMessage, expectedModel);
    }

    // EP: delete remark from person with no existing remark (empty remark string)
    @Test
    public void execute_deleteMissingRemark_failureAndPreservesFilteredView() {
        Person person = model.getFilteredPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        Person withoutRemark = new PersonBuilder(person).withRemark("").build(); // Boundary value
        model.setPerson(person, withoutRemark);
        showPersonAtIndex(model, INDEX_SECOND_PERSON);

        assertCommandFailure(new RemarkCommand(INDEX_FIRST_PERSON, new Remark("")), model, // Boundary value
                String.format(RemarkCommand.MESSAGE_NO_REMARK, Messages.formatAthlete(withoutRemark)));
    }

    // EP: add non-empty remark to person in filtered list at valid first index
    @Test
    public void execute_filteredList_success() {
        showPersonAtIndex(model, INDEX_SECOND_PERSON);

        Person personToEdit = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Remark remark = new Remark(VALID_REMARK_AMY);
        RemarkCommand remarkCommand = new RemarkCommand(INDEX_FIRST_PERSON, remark); // Boundary value
        Person editedPerson = new PersonBuilder(personToEdit).withRemark(VALID_REMARK_AMY).build();

        String expectedMessage = String.format(RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS,
                Messages.formatAthlete(editedPerson));

        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        expectedModel.setPerson(personToEdit, editedPerson);

        assertCommandSuccess(remarkCommand, model, expectedMessage, expectedModel);
    }

    // EP: invalid index exceeding unfiltered list size
    @Test
    public void execute_invalidPersonIndexUnfilteredList_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1); // Boundary value
        RemarkCommand remarkCommand = new RemarkCommand(outOfBoundIndex, new Remark(VALID_REMARK_AMY));

        assertCommandFailure(remarkCommand, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    // EP: invalid index exceeding filtered list size but within athlete roster size
    @Test
    public void execute_invalidPersonIndexFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        Index outOfBoundIndex = INDEX_SECOND_PERSON; // Boundary value
        assertTrue(outOfBoundIndex.getZeroBased() < model.getRoster().getPersonList().size());

        RemarkCommand remarkCommand = new RemarkCommand(outOfBoundIndex, new Remark(VALID_REMARK_AMY));

        assertCommandFailure(remarkCommand, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    // EP: command mutates roster state (changesRoster returns true)
    @Test
    public void changesRoster_returnsTrue() {
        RemarkCommand remarkCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(VALID_REMARK_AMY));
        assertTrue(remarkCommand.changesRoster());
    }

    @Test
    public void equals() {
        final RemarkCommand standardCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(VALID_REMARK_AMY));

        // EP: symmetric equality (different instance with same index and remark)
        RemarkCommand commandWithSameValues = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(VALID_REMARK_AMY));
        assertTrue(standardCommand.equals(commandWithSameValues));

        // EP: reflexive equality (same object instance)
        assertTrue(standardCommand.equals(standardCommand));

        // EP: null comparison
        assertFalse(standardCommand.equals(null)); // Boundary value

        // EP: different object type
        assertFalse(standardCommand.equals(new ClearCommand()));

        // EP: different index value
        assertFalse(standardCommand.equals(new RemarkCommand(INDEX_SECOND_PERSON, new Remark(VALID_REMARK_AMY))));

        // EP: different remark value
        assertFalse(standardCommand.equals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark(VALID_REMARK_BOB))));
    }
}
