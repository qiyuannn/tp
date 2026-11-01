package trackflow.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.geometry.Rectangle2D;
import trackflow.commons.core.GuiSettings;

public class WindowPlacementTest {

    private static final Rectangle2D PRIMARY = new Rectangle2D(0, 0, 1440, 900);
    private static final Rectangle2D SECOND = new Rectangle2D(-1920, 0, 1920, 1080);
    private static final List<Rectangle2D> PRIMARY_ONLY = List.of(PRIMARY);
    private static final List<Rectangle2D> BOTH = List.of(PRIMARY, SECOND);

    private static final double WIDTH = 740;
    private static final double HEIGHT = 600;

    // EP: window coordinates fully interior to primary screen
    @Test
    public void isReachable_onPrimaryScreen_true() {
        assertTrue(WindowPlacement.isReachable(100, 100, WIDTH, HEIGHT, PRIMARY_ONLY));
    }

    // EP: coordinates located on disconnected secondary display
    @Test
    public void isReachable_onSecondScreenButOnlyPrimaryConnected_false() {
        assertFalse(WindowPlacement.isReachable(-1920, 100, WIDTH, HEIGHT, PRIMARY_ONLY));
    }

    // EP: coordinates located on connected secondary display
    @Test
    public void isReachable_onSecondScreenWithBothConnected_true() {
        assertTrue(WindowPlacement.isReachable(-1920, 100, WIDTH, HEIGHT, BOTH));
    }

    @Test
    public void isReachable_topToleranceBoundary() {
        // EP: top edge within tolerance bound (-10 px)
        assertTrue(WindowPlacement.isReachable(100, -10, WIDTH, HEIGHT, PRIMARY_ONLY)); // Boundary value

        // EP: top edge exceeding tolerance bound (-11 px)
        assertFalse(WindowPlacement.isReachable(100, -11, WIDTH, HEIGHT, PRIMARY_ONLY)); // Boundary value
    }

    @Test
    public void isReachable_minimumVisibleHeightBoundary() {
        // EP: exactly minimum visible height remaining on screen (50 px)
        assertTrue(WindowPlacement.isReachable(100, 850, WIDTH, HEIGHT, PRIMARY_ONLY)); // Boundary value

        // EP: strictly less than minimum visible height remaining on screen
        assertFalse(WindowPlacement.isReachable(100, 851, WIDTH, HEIGHT, PRIMARY_ONLY)); // Boundary value
    }

    // EP: coordinates far above top display boundary
    @Test
    public void isReachable_aboveTopEdge_false() {
        assertFalse(WindowPlacement.isReachable(100, -500, WIDTH, HEIGHT, PRIMARY_ONLY));
    }

    // EP: coordinates at bottom screen edge (zero visible height)
    @Test
    public void isReachable_belowBottomEdge_false() {
        assertFalse(WindowPlacement.isReachable(100, 900, WIDTH, HEIGHT, PRIMARY_ONLY)); // Boundary value
    }

    // EP: exactly minimum visible width remaining on screen (100 px)
    @Test
    public void isReachable_exactlyMinimumVisibleWidth_true() {
        assertTrue(WindowPlacement.isReachable(1340, 100, WIDTH, HEIGHT, PRIMARY_ONLY)); // Boundary value
    }

    // EP: strictly less than minimum visible width remaining on screen
    @Test
    public void isReachable_lessThanMinimumVisibleWidth_false() {
        assertFalse(WindowPlacement.isReachable(1400, 100, WIDTH, HEIGHT, PRIMARY_ONLY));
    }

    // EP: empty display list (zero connected screens)
    @Test
    public void isReachable_noScreens_false() {
        assertFalse(WindowPlacement.isReachable(100, 100, WIDTH, HEIGHT, List.of())); // Boundary value
    }

    // EP: window width narrower than minimum visible width threshold
    @Test
    public void isReachable_windowNarrowerThanMinimumVisibleWidth_true() {
        assertTrue(WindowPlacement.isReachable(0, 0, 80, HEIGHT, PRIMARY_ONLY));
    }

    // EP: non-numeric / NaN coordinate value
    @Test
    public void isReachable_nanCoordinate_false() {
        assertFalse(WindowPlacement.isReachable(Double.NaN, 100, WIDTH, HEIGHT, PRIMARY_ONLY)); // Boundary value
    }

    // EP: valid saved bounds fully within connected display geometry
    @Test
    public void fit_validSavedBounds_preservesSizeAndPosition() {
        assertEquals(new Rectangle2D(100, 100, WIDTH, HEIGHT),
                WindowPlacement.fit(new GuiSettings(WIDTH, HEIGHT, 100, 100), BOTH, PRIMARY));
    }

    // EP: oversized window dimensions exceeding display bounds clamped to display size
    @Test
    public void fit_oversizedReachableWindow_fitsEntirePrimaryScreen() {
        assertEquals(PRIMARY, WindowPlacement.fit(new GuiSettings(5000, 4000, 100, 100), BOTH, PRIMARY));
    }

    // EP: display usable height smaller than default 600 px
    @Test
    public void fit_smallUsableScreen_doesNotForceSixHundredPixelHeight() {
        Rectangle2D usable = new Rectangle2D(0, 25, 853, 440);
        assertEquals(usable, WindowPlacement.fit(new GuiSettings(900, 600, 0, 25), List.of(usable), usable));
    }

    // EP: window height at standard minimum allowable height (400 px)
    @Test
    public void fit_shortSavedWindow_keepsFourHundredPixelHeight() {
        assertEquals(new Rectangle2D(100, 100, WIDTH, 400),
                WindowPlacement.fit(new GuiSettings(WIDTH, 400, 100, 100), BOTH, PRIMARY)); // Boundary value
    }

    // EP: window dimensions strictly below minimum allowable dimensions clamped up
    @Test
    public void fit_windowBelowNormalMinimum_usesMinimumWhereSpaceAllows() {
        assertEquals(new Rectangle2D(100, 100, 450, 400),
                WindowPlacement.fit(new GuiSettings(100, 100, 100, 100), BOTH, PRIMARY));
    }

    // EP: display size smaller than minimum window size prioritizing screen bounds
    @Test
    public void fit_screenSmallerThanMinimum_prioritizesScreenBounds() {
        Rectangle2D tiny = new Rectangle2D(0, 25, 400, 350);
        assertEquals(tiny, WindowPlacement.fit(new GuiSettings(), List.of(tiny), tiny));
    }

    // EP: unreachable saved coordinates on disconnected monitor centered on primary display
    @Test
    public void fit_removedMonitor_centersOnPrimary() {
        assertEquals(new Rectangle2D(350, 150, WIDTH, HEIGHT),
                WindowPlacement.fit(new GuiSettings(WIDTH, HEIGHT, -1920, 100), PRIMARY_ONLY, PRIMARY));
    }

    // EP: oversized window on secondary display with negative coordinate space
    @Test
    public void fit_oversizedWindowOnSecondary_fitsSecondaryWithNegativeCoordinates() {
        assertEquals(SECOND, WindowPlacement.fit(new GuiSettings(3000, 2000, -1920, 100), BOTH, PRIMARY));
    }

    // EP: window extending beyond display boundaries shifted completely into view
    @Test
    public void fit_windowPartlyPastScreenEdges_movesWholeWindowInside() {
        assertEquals(new Rectangle2D(700, 300, WIDTH, HEIGHT),
                WindowPlacement.fit(new GuiSettings(WIDTH, HEIGHT, 1340, 850), PRIMARY_ONLY, PRIMARY));
    }

    // EP: window spanning across multiple displays placed on display with maximum overlap
    @Test
    public void fit_windowSpanningScreens_selectsScreenWithMostOverlap() {
        assertEquals(new Rectangle2D(-740, 100, WIDTH, HEIGHT),
                WindowPlacement.fit(new GuiSettings(WIDTH, HEIGHT, -500, 100), BOTH, PRIMARY));
    }

    @Test
    public void fit_helpWindow_usesItsOwnMinimumDimensions() {
        // EP: custom window minimum dimensions (480x320) on ample display
        assertEquals(new Rectangle2D(100, 100, 480, 320), WindowPlacement.fit(
                new GuiSettings(480, 320, 100, 100), BOTH, PRIMARY, 480, 320)); // Boundary value

        // EP: custom window minimum dimensions with height exceeding small display clamped
        Rectangle2D usable = new Rectangle2D(0, 25, 853, 440);
        assertEquals(new Rectangle2D(0, 25, 760, 440), WindowPlacement.fit(
                new GuiSettings(760, 680, 0, 25), List.of(usable), usable, 480, 320));
    }
}
