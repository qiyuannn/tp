// @@author qiyuannn
package trackflow.model.person;
// @@author

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
// @@author qiyuannn
import static trackflow.testutil.Assert.assertThrows;
// @@author

import org.junit.jupiter.api.Test;

public class AddressTest {

    // EP: null address
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Address(null));
    }

    // EP: invalid address (blank / whitespace-only string)
    @Test
    public void constructor_invalidAddress_throwsIllegalArgumentException() {
        String invalidAddress = " ";
        assertThrows(IllegalArgumentException.class, Address.MESSAGE_CONSTRAINTS, () -> new Address(invalidAddress));
    }

    @Test
    public void isValidAddress() {
        // EP: null address
        assertThrows(NullPointerException.class, () -> Address.isValidAddress(null));

        // EP: valid empty address (optional address for athlete)
        assertTrue(Address.isValidAddress("")); // Boundary value

        // EP: invalid blank address (whitespace only)
        assertFalse(Address.isValidAddress(" "));

        // EP: invalid address containing line breaks or line separators
        assertFalse(Address.isValidAddress("Line1 Line2"));
        assertFalse(Address.isValidAddress("Line1\nLine2"));
        assertTrue(Address.MESSAGE_CONSTRAINTS.contains("except line breaks"));

        // EP: valid typical non-empty address
        assertTrue(Address.isValidAddress("Blk 456, Den Road, #01-355"));

        // EP: valid single-character address (minimum non-empty length)
        assertTrue(Address.isValidAddress("-")); // Boundary value

        // EP: valid long address
        assertTrue(Address.isValidAddress("Leng Inc; 1234 Market St; San Francisco CA 2349879; USA"));
    }

    // EP: invalid address containing line break characters at various positions (start, middle, end, standalone)
    @Test
    public void isValidAddress_lineBreakAtAnyPosition_returnsFalse() {
        for (String lineBreak : new String[] {"\n", "\r", "\u000b", "\f", "\u0085", "\u2028", "\u2029"}) {
            assertFalse(Address.isValidAddress(lineBreak));
            assertFalse(Address.isValidAddress(lineBreak + "House"));
            assertFalse(Address.isValidAddress("Block" + lineBreak + "House"));
            assertFalse(Address.isValidAddress("House" + lineBreak));
        }
    }

    // EP: Unicode whitespace handling (invalid when blank or leading, valid when internal)
    @Test
    public void isValidAddress_unicodeSpaces_rejectsBlankButPreservesInternalSpacing() {
        for (String space : new String[] {"\u00a0", "\u2003", "\u2007", "\u202f", "\u3000"}) {
            assertFalse(Address.isValidAddress(space));
            assertFalse(Address.isValidAddress(space + "House"));
            assertTrue(Address.isValidAddress("Block" + space + "House"));
        }
    }

    @Test
    public void equals() {
        Address address = new Address("Valid Address");

        // EP: symmetric equality (different instance with same address value) & string representation
        assertTrue(address.equals(new Address("Valid Address")));
        assertEquals(address.hashCode(), new Address("Valid Address").hashCode());
        assertEquals("Valid Address", address.toString());

        // EP: reflexive equality (same object instance)
        assertTrue(address.equals(address));

        // EP: null comparison
        assertFalse(address.equals(null));

        // EP: different object type
        assertFalse(address.equals(5.0f));

        // EP: different field value
        assertFalse(address.equals(new Address("Other Valid Address")));
    }
}
