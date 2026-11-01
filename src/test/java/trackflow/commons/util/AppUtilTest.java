package trackflow.commons.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static trackflow.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class AppUtilTest {

    // EP: valid existing image resource path
    @Test
    public void getImage_exitingImage() {
        assertNotNull(AppUtil.getImage("/images/trackflow_32.png"));
    }

    // EP: null image path
    @Test
    public void getImage_nullGiven_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> AppUtil.getImage(null));
    }

    @Test
    public void checkArgument_true_nothingHappens() {
        // EP: condition is true without error message
        AppUtil.checkArgument(true);

        // EP: condition is true with empty error message
        AppUtil.checkArgument(true, ""); // Boundary value
    }

    // EP: condition is false without error message
    @Test
    public void checkArgument_falseWithoutErrorMessage_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> AppUtil.checkArgument(false));
    }

    // EP: condition is false with non-empty error message
    @Test
    public void checkArgument_falseWithErrorMessage_throwsIllegalArgumentException() {
        String errorMessage = "error message";
        assertThrows(IllegalArgumentException.class, errorMessage, () -> AppUtil.checkArgument(false, errorMessage));
    }
}
