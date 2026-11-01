package trackflow.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class CommandResultTest {
    // EP: null feedback to user
    @Test
    public void constructor_nullFeedbackToUser_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new CommandResult(null));
    }

    @Test
    public void equals() {
        CommandResult commandResult = new CommandResult("feedback");

        // EP: symmetric equality (different instance with same feedback and default boolean flags)
        assertTrue(commandResult.equals(new CommandResult("feedback")));
        // EP: symmetric equality (different instance with explicit matching boolean flags)
        assertTrue(commandResult.equals(new CommandResult("feedback", false, false)));

        // EP: reflexive equality (same object instance)
        assertTrue(commandResult.equals(commandResult));

        // EP: null comparison
        assertFalse(commandResult.equals(null));

        // EP: different object type
        assertFalse(commandResult.equals(0.5f));

        // EP: different feedbackToUser value
        assertFalse(commandResult.equals(new CommandResult("different")));

        // EP: different showHelp flag
        assertFalse(commandResult.equals(new CommandResult("feedback", true, false)));

        // EP: different exit flag
        assertFalse(commandResult.equals(new CommandResult("feedback", false, true)));

        // EP: different dataSaved flag
        assertFalse(commandResult.equals(commandResult.withDataSaved()));
    }

    @Test
    public void hashcode() {
        CommandResult commandResult = new CommandResult("feedback");

        // EP: same field values yield equal hash code
        assertEquals(commandResult.hashCode(), new CommandResult("feedback").hashCode());

        // EP: different feedbackToUser yields different hash code
        assertNotEquals(commandResult.hashCode(), new CommandResult("different").hashCode());

        // EP: different showHelp flag yields different hash code
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", true, false).hashCode());

        // EP: different exit flag yields different hash code
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", false, true).hashCode());

        // EP: different dataSaved flag yields different hash code
        assertNotEquals(commandResult.hashCode(), commandResult.withDataSaved().hashCode());
    }

    // EP: string representation includes all state fields
    @Test
    public void toStringMethod() {
        CommandResult commandResult = new CommandResult("feedback");
        String expected = CommandResult.class.getCanonicalName() + "{feedbackToUser="
                + commandResult.getFeedbackToUser() + ", showHelp=" + commandResult.isShowHelp()
                + ", exit=" + commandResult.isExit() + ", dataSaved=" + commandResult.isDataSaved()
                + ", warning=" + commandResult.isWarning() + "}";
        assertEquals(expected, commandResult.toString());
    }

    // EP: warning factory creates distinct warning state preserving warning on mutation
    @Test
    public void warning_hasDistinctState() {
        CommandResult warning = CommandResult.warning("Confirm?");
        assertTrue(warning.isWarning());
        assertFalse(warning.isDataSaved());
        assertNotEquals(new CommandResult("Confirm?"), warning);
        assertTrue(warning.withDataSaved().isWarning());
    }

    // EP: withDataSaved creates immutable modified copy preserving original state
    @Test
    public void withDataSaved_returnsSavedCopyWithoutChangingOriginal() {
        CommandResult original = new CommandResult("feedback", true, false);

        CommandResult saved = original.withDataSaved();

        assertFalse(original.isDataSaved());
        assertTrue(saved.isDataSaved());
        assertEquals(original.getFeedbackToUser(), saved.getFeedbackToUser());
        assertEquals(original.isShowHelp(), saved.isShowHelp());
        assertEquals(original.isExit(), saved.isExit());
    }
}
