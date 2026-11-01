package trackflow.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class RemarkTest {

    // EP: null remark
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void equals() {
        Remark remark = new Remark("Hello");

        // EP: reflexive equality (same object instance)
        assertTrue(remark.equals(remark));

        // EP: symmetric equality (different instance with same remark text) & string representation
        Remark remarkCopy = new Remark(remark.value);
        assertTrue(remark.equals(remarkCopy));
        assertEquals(remark.hashCode(), remarkCopy.hashCode());
        assertEquals("Hello", remark.toString());

        // EP: different object type
        assertFalse(remark.equals(1));

        // EP: null comparison
        assertFalse(remark.equals(null));

        // EP: different remark value
        Remark differentRemark = new Remark("Bye");
        assertFalse(remark.equals(differentRemark));
    }
}
