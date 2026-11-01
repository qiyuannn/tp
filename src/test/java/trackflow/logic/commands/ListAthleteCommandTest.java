package trackflow.logic.commands;

import static trackflow.logic.commands.CommandTestUtil.assertCommandSuccess;
import static trackflow.logic.commands.CommandTestUtil.showPersonAtIndex;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.UserPrefs;
import trackflow.testutil.AthleteRosterBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for ListAthleteCommand.
 */
public class ListAthleteCommandTest {

    private Model model;
    private Model expectedModel;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalRoster(), new UserPrefs());
        expectedModel = new ModelManager(model.getRoster(), new UserPrefs());
    }

    // EP: unfiltered roster list displays all athletes
    @Test
    public void execute_listIsNotFiltered_showsSameList() {
        String expectedMessage = String.format(ListAthleteCommand.MESSAGE_SUCCESS,
                model.getFilteredPersonList().size());
        assertCommandSuccess(new ListAthleteCommand(), model, expectedMessage, expectedModel);
    }

    // EP: filtered roster list resets to show all athletes
    @Test
    public void execute_listIsFiltered_showsEverything() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        String expectedMessage = String.format(ListAthleteCommand.MESSAGE_SUCCESS,
                expectedModel.getFilteredPersonList().size());
        assertCommandSuccess(new ListAthleteCommand(), model, expectedMessage, expectedModel);
    }

    // EP: empty roster displays empty roster message
    @Test
    public void execute_emptyRoster_showsEmptyRosterMessage() {
        Model emptyModel = new ModelManager(); // Boundary value
        Model expectedEmptyModel = new ModelManager();
        assertCommandSuccess(new ListAthleteCommand(), emptyModel, ListAthleteCommand.MESSAGE_EMPTY_ROSTER,
                expectedEmptyModel);
    }

    // EP: single athlete roster displays singular athlete message
    @Test
    public void execute_oneAthlete_showsSingularMessage() {
        Model singleAthleteModel = new ModelManager(
                new AthleteRosterBuilder().withPerson(getTypicalRoster().getPersonList().getFirst()).build(),
                new UserPrefs()); // Boundary value
        Model expectedSingleAthleteModel = new ModelManager(singleAthleteModel.getRoster(), new UserPrefs());
        assertCommandSuccess(new ListAthleteCommand(), singleAthleteModel,
                ListAthleteCommand.MESSAGE_SUCCESS_SINGLE_ATHLETE, expectedSingleAthleteModel);
    }
}
