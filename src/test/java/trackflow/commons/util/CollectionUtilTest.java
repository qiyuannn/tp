package trackflow.commons.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.commons.util.CollectionUtil.requireAllNonNull;
import static trackflow.testutil.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CollectionUtilTest {
    @Test
    public void requireAllNonNullVarargs() {
        // EP: empty varargs array (0 arguments)
        assertNullPointerExceptionNotThrown(); // Boundary value

        // EP: non-empty argument list with all non-null elements
        assertNullPointerExceptionNotThrown(new Object(), new Object());
        assertNullPointerExceptionNotThrown("test"); // Boundary value
        assertNullPointerExceptionNotThrown(""); // Boundary value

        // EP: argument list with null as the first element
        assertNullPointerExceptionThrown((Object) null); // Boundary value
        assertNullPointerExceptionThrown(null, "", new Object()); // Boundary value

        // EP: argument list with null in interior positions
        assertNullPointerExceptionThrown(new Object(), null, null, "test");

        // EP: argument list with null as the last element
        assertNullPointerExceptionThrown("", new Object(), null); // Boundary value

        // EP: null varargs array reference
        assertNullPointerExceptionThrown((Object[]) null);

        // EP: shallow non-null elements containing nested nulls
        List<Object> containingNull = Arrays.asList((Object) null);
        assertNullPointerExceptionNotThrown(containingNull, new Object());
    }

    @Test
    public void requireAllNonNullCollection() {
        // EP: collection with null as the first element
        assertNullPointerExceptionThrown(Arrays.asList((Object) null)); // Boundary value
        assertNullPointerExceptionThrown(Arrays.asList(null, new Object(), "")); // Boundary value

        // EP: collection with null in interior positions
        assertNullPointerExceptionThrown(Arrays.asList("spam", null, new Object()));

        // EP: collection with null as the last element
        assertNullPointerExceptionThrown(Arrays.asList("spam", new Object(), null)); // Boundary value

        // EP: null collection reference
        assertNullPointerExceptionThrown((Collection<Object>) null);

        // EP: empty collection (size 0)
        assertNullPointerExceptionNotThrown(Collections.emptyList()); // Boundary value

        // EP: non-empty collection with all non-null elements
        assertNullPointerExceptionNotThrown(Arrays.asList(new Object(), "ham", Integer.valueOf(1)));

        // EP: shallow non-null collection containing nested nulls
        List<Object> containingNull = Arrays.asList((Object) null);
        assertNullPointerExceptionNotThrown(Arrays.asList(containingNull, new Object()));
    }

    @Test
    public void isAnyNonNull() {
        // EP: empty varargs array (0 arguments)
        assertFalse(CollectionUtil.isAnyNonNull()); // Boundary value

        // EP: varargs array with only null elements
        assertFalse(CollectionUtil.isAnyNonNull((Object) null)); // Boundary value

        // EP: null varargs array reference
        assertFalse(CollectionUtil.isAnyNonNull((Object[]) null));

        // EP: varargs array with all non-null elements (singleton array)
        assertTrue(CollectionUtil.isAnyNonNull(new Object())); // Boundary value

        // EP: varargs array with mix of non-null and null elements
        assertTrue(CollectionUtil.isAnyNonNull(new Object(), null));
    }

    /**
     * Asserts that {@code CollectionUtil#requireAllNonNull(Object...)} throws {@code NullPointerException}
     * if {@code objects} or any element of {@code objects} is null.
     */
    private void assertNullPointerExceptionThrown(Object... objects) {
        assertThrows(NullPointerException.class, () -> requireAllNonNull(objects));
    }

    /**
     * Asserts that {@code CollectionUtil#requireAllNonNull(Collection<?>)} throws {@code NullPointerException}
     * if {@code collection} or any element of {@code collection} is null.
     */
    private void assertNullPointerExceptionThrown(Collection<?> collection) {
        assertThrows(NullPointerException.class, () -> requireAllNonNull(collection));
    }

    private void assertNullPointerExceptionNotThrown(Object... objects) {
        requireAllNonNull(objects);
    }

    private void assertNullPointerExceptionNotThrown(Collection<?> collection) {
        requireAllNonNull(collection);
    }
}
