package trackflow.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import trackflow.commons.core.GuiSettings;

public class UserPrefsTest {

    // EP: null user preferences
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new UserPrefs(null)); // Boundary value
    }

    // EP: null GuiSettings
    @Test
    public void setGuiSettings_nullGuiSettings_throwsNullPointerException() {
        UserPrefs userPref = new UserPrefs();
        assertThrows(NullPointerException.class, () -> userPref.setGuiSettings(null)); // Boundary value
    }

    @Test
    public void equals() {
        UserPrefs userPrefs = new UserPrefs();
        UserPrefs copy = new UserPrefs(userPrefs);

        // EP: reflexive equality (same object instance)
        assertTrue(userPrefs.equals(userPrefs));

        // EP: symmetric equality (different instance with same preferences) & consistent hash code
        assertTrue(userPrefs.equals(copy));
        assertEquals(userPrefs.hashCode(), copy.hashCode());

        // EP: default UserPrefs string representation
        assertEquals("Gui Settings : " + userPrefs.getGuiSettings(), userPrefs.toString());

        // EP: null comparison
        assertFalse(userPrefs.equals(null));

        // EP: different object type
        assertFalse(userPrefs.equals("not a UserPrefs"));

        // EP: different GuiSettings values
        UserPrefs different = new UserPrefs();
        different.setGuiSettings(new GuiSettings(800, 600, 0, 0)); // Boundary value
        assertFalse(userPrefs.equals(different));
    }

}
