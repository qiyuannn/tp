package trackflow.commons.core.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class IndexTest {

    @Test
    public void createOneBasedIndex() {
        // EP: invalid non-positive 1-based index (<= 0)
        assertThrows(IndexOutOfBoundsException.class, () -> Index.fromOneBased(0)); // Boundary value

        // EP: valid 1-based index (same base retrieval)
        assertEquals(1, Index.fromOneBased(1).getOneBased()); // Boundary value
        assertEquals(5, Index.fromOneBased(5).getOneBased());

        // EP: valid 1-based index (conversion to 0-based index)
        assertEquals(0, Index.fromOneBased(1).getZeroBased()); // Boundary value
        assertEquals(4, Index.fromOneBased(5).getZeroBased());
    }

    @Test
    public void createZeroBasedIndex() {
        // EP: invalid negative 0-based index (< 0)
        assertThrows(IndexOutOfBoundsException.class, () -> Index.fromZeroBased(-1)); // Boundary value

        // EP: valid 0-based index (same base retrieval)
        assertEquals(0, Index.fromZeroBased(0).getZeroBased()); // Boundary value
        assertEquals(5, Index.fromZeroBased(5).getZeroBased());

        // EP: valid 0-based index (conversion to 1-based index)
        assertEquals(1, Index.fromZeroBased(0).getOneBased()); // Boundary value
        assertEquals(6, Index.fromZeroBased(5).getOneBased());
    }

    @Test
    public void equals() {
        final Index fifthPersonIndex = Index.fromOneBased(5);

        // EP: symmetric equality (different instance with same index value)
        assertTrue(fifthPersonIndex.equals(Index.fromOneBased(5)));
        assertTrue(fifthPersonIndex.equals(Index.fromZeroBased(4)));

        // EP: reflexive equality (same object instance)
        assertTrue(fifthPersonIndex.equals(fifthPersonIndex));

        // EP: null comparison
        assertFalse(fifthPersonIndex.equals(null));

        // EP: different object type
        assertFalse(fifthPersonIndex.equals(5.0f));

        // EP: different index value
        assertFalse(fifthPersonIndex.equals(Index.fromOneBased(1))); // Boundary value
    }

    // EP: valid Index string representation
    @Test
    public void toStringMethod() {
        Index index = Index.fromZeroBased(0); // Boundary value
        String expected = Index.class.getCanonicalName() + "{zeroBasedIndex=" + index.getZeroBased() + "}";
        assertEquals(expected, index.toString());
    }
}
