// @@author -
// Unassigned sections are adapted from SE-EDU AddressBook-Level3.
package trackflow.logic.commands;

import static trackflow.logic.commands.CommandTestUtil.assertCommandSuccess;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;

import org.junit.jupiter.api.Test;

import trackflow.model.AthleteRoster;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.UserPrefs;

public class ClearCommandTest {

    // EP: unconfirmed clear command (requires confirmation, roster preserved)
    @Test
    public void execute_unconfirmed_keepsRoster() {
        Model model = new ModelManager(getTypicalRoster(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalRoster(), new UserPrefs());

        assertCommandSuccess(new ClearCommand(), model,
                CommandResult.warning(ClearCommand.MESSAGE_CONFIRMATION), expectedModel);
    }

    // EP: confirmed clear on empty athlete roster
    @Test
    public void execute_emptyAthleteRoster_success() {
        Model model = new ModelManager(); // Boundary value
        Model expectedModel = new ModelManager();

        assertCommandSuccess(new ClearCommand(true), model, ClearCommand.MESSAGE_SUCCESS, expectedModel);
    }

    // EP: confirmed clear on non-empty athlete roster
    @Test
    public void execute_nonEmptyAthleteRoster_success() {
        Model model = new ModelManager(getTypicalRoster(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalRoster(), new UserPrefs());
        expectedModel.setRoster(new AthleteRoster());

        assertCommandSuccess(new ClearCommand(true), model, ClearCommand.MESSAGE_SUCCESS, expectedModel);
    }

}
// @@author
