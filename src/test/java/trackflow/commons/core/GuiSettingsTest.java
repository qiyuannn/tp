package trackflow.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.awt.Point;

import org.junit.jupiter.api.Test;

public class GuiSettingsTest {
    // EP: invalid dimensions (nonpositive or non-finite double values)
    @Test
    public void constructor_invalidDimensions_throwsIllegalArgumentException() {
        for (double dimension : new double[] {
            0, // Boundary value
            -1, // Boundary value
            Double.NaN,
            Double.POSITIVE_INFINITY, // Boundary value
            Double.NEGATIVE_INFINITY // Boundary value
        }) {
            assertThrows(IllegalArgumentException.class, () -> new GuiSettings(dimension, 600, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> new GuiSettings(740, dimension, 0, 0));
        }
    }

    // EP: defensive copying of mutable coordinates (mutating returned Point does not mutate internal state)
    @Test
    public void getWindowCoordinates_defensiveCopy_doesNotMutateInternalState() {
        GuiSettings guiSettings = new GuiSettings(800, 600, 10, 20);
        Point coordinates = guiSettings.getWindowCoordinates();
        coordinates.setLocation(999, 999);
        assertEquals(new Point(10, 20), guiSettings.getWindowCoordinates());
    }

    @Test
    public void equals() {
        GuiSettings defaultSettings = new GuiSettings();
        GuiSettings explicitSettings = new GuiSettings(800, 600, 10, 20);
        GuiSettings explicitSettingsCopy = new GuiSettings(800, 600, 10, 20);

        // EP: reflexive equality (same object instance)
        assertTrue(defaultSettings.equals(defaultSettings));

        // EP: symmetric equality (different instance with same field values) & consistent hash code
        assertTrue(explicitSettings.equals(explicitSettingsCopy));
        assertEquals(explicitSettings.hashCode(), explicitSettingsCopy.hashCode());

        // EP: null comparison
        assertFalse(defaultSettings.equals(null));

        // EP: different object type
        assertFalse(defaultSettings.equals("not a GuiSettings"));

        // EP: different field values (width, height, or coordinates)
        assertFalse(defaultSettings.equals(explicitSettings));
        assertFalse(explicitSettings.equals(new GuiSettings(900, 600, 10, 20)));
        assertFalse(explicitSettings.equals(new GuiSettings(800, 700, 10, 20)));
        assertFalse(explicitSettings.equals(new GuiSettings(800, 600, 30, 20)));
    }

    // EP: default GuiSettings string representation
    @Test
    public void toStringMethod() {
        GuiSettings guiSettings = new GuiSettings();
        String expected = GuiSettings.class.getCanonicalName() + "{windowWidth=" + guiSettings.getWindowWidth()
                + ", windowHeight=" + guiSettings.getWindowHeight() + ", windowCoordinates="
                + guiSettings.getWindowCoordinates() + "}";
        assertEquals(expected, guiSettings.toString());
    }
}
