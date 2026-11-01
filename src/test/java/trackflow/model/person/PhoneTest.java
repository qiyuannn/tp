package trackflow.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class PhoneTest {

    // EP: null phone
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Phone(null));
    }

    // EP: invalid phone (empty string)
    @Test
    public void constructor_invalidPhone_throwsIllegalArgumentException() {
        String invalidPhone = ""; // Boundary value
        assertThrows(IllegalArgumentException.class, Phone.MESSAGE_CONSTRAINTS, () -> new Phone(invalidPhone));
    }

    @Test
    public void isValidPhone() {
        // EP: null phone number
        assertThrows(NullPointerException.class, () -> Phone.isValidPhone(null));

        // EP: empty or blank strings
        assertFalse(Phone.isValidPhone("")); // Boundary value
        assertFalse(Phone.isValidPhone(" "));

        // EP: less than 3 digits (insufficient significant digits)
        assertFalse(Phone.isValidPhone("91")); // Boundary value

        // EP: non-numeric characters and misplaced letters
        assertFalse(Phone.isValidPhone("phone"));
        assertFalse(Phone.isValidPhone("9011p041"));

        // EP: digits inside note brackets without separate significant digits
        assertFalse(Phone.isValidPhone("(HP)"));
        assertFalse(Phone.isValidPhone("(91234567)"));

        // EP: malformed brackets (dangling, empty, nested/unbalanced)
        assertFalse(Phone.isValidPhone("91234567 ("));
        assertFalse(Phone.isValidPhone("123 ()"));
        assertFalse(Phone.isValidPhone("(a(b)) 123"));

        // EP: invalid hyphen placement and misplaced plus signs
        assertFalse(Phone.isValidPhone("---123---"));
        assertFalse(Phone.isValidPhone("91+23+45"));

        // EP: valid minimum length phone number (exactly 3 digits)
        assertTrue(Phone.isValidPhone("911")); // Boundary value

        // EP: valid typical phone numbers and digit formats (spaces, long digit strings)
        assertTrue(Phone.isValidPhone("93121534"));
        assertTrue(Phone.isValidPhone("124293842033123"));
        assertTrue(Phone.isValidPhone("9312 1534"));

        // EP: valid phone numbers with country codes (+65, (+65))
        assertTrue(Phone.isValidPhone("+65 9123 4567"));
        assertTrue(Phone.isValidPhone("(+65) 9123 4567"));

        // EP: valid phone numbers with notes and formatting (brackets, hyphens)
        assertTrue(Phone.isValidPhone("(65) 9123-4567"));
        assertTrue(Phone.isValidPhone("9123 4567 (HP)"));
        assertTrue(Phone.isValidPhone("200 (5)"));
        assertTrue(Phone.isValidPhone("(+65)91234567"));
        assertTrue(Phone.isValidPhone("91234567(HP)"));
        assertTrue(Phone.isValidPhone("9123-4567"));
    }

    // EP: long digit sequences and repeated hyphen groups under timeout (performance & robustness)
    @Test
    public void isValidPhone_longDigitSequences_doesNotOverflowOrStall() {
        String manyGroups = "123 ".repeat(3000).trim();
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertTrue(Phone.isValidPhone(manyGroups));
            assertTrue(Phone.isValidPhone("123-".repeat(3000) + "456"));
            assertFalse(Phone.isValidPhone(manyGroups + "x"));
            assertFalse(Phone.isValidPhone("1".repeat(30000) + "x"));
        });
    }

    @Test
    public void getSignificantDigits() {
        // EP: standard phone number (spaces ignored)
        assertEquals("91234567", new Phone("9123 4567").getSignificantDigits());

        // EP: textual note ignored
        assertEquals("91234567", new Phone("9123 4567 (HP)").getSignificantDigits());

        // EP: numeric note ignored with minimum 3 significant digits
        assertEquals("200", new Phone("200 (5)").getSignificantDigits()); // Boundary value

        // EP: country code counted in significant digits
        assertEquals("6591234567", new Phone("(+65) 9123 4567").getSignificantDigits());
    }

    // EP: phone constructor collapses multiple and leading/trailing whitespace
    @Test
    public void constructor_collapsesWhitespace() {
        assertTrue(new Phone("9123  4567").value.equals("9123 4567"));
        assertTrue(new Phone("  9123 4567 (HP) ").value.equals("9123 4567 (HP)"));
    }

    @Test
    public void equals() {
        Phone phone = new Phone("999");

        // EP: symmetric equality (different instance with same phone digits) & string representation
        assertTrue(phone.equals(new Phone("999")));
        assertEquals(phone.hashCode(), new Phone("999").hashCode());
        assertEquals("999", phone.toString());

        // EP: reflexive equality (same object instance)
        assertTrue(phone.equals(phone));

        // EP: null comparison
        assertFalse(phone.equals(null));

        // EP: different object type
        assertFalse(phone.equals(5.0f));

        // EP: different phone digits
        assertFalse(phone.equals(new Phone("995")));

        // EP: same significant digits with different whitespace formatting or notes (treated as equal)
        assertTrue(new Phone("9123 4567 (HP)").equals(new Phone("91234567")));
        assertTrue(new Phone("9123 4567").hashCode() == new Phone("91234567").hashCode());

        // EP: numeric note digits ignored for identity comparison
        assertTrue(new Phone("200 (5)").equals(new Phone("200")));
        assertFalse(new Phone("200 (5)").equals(new Phone("2005")));

        // EP: bracketed country code included in significant digits comparison
        assertTrue(new Phone("(+65) 9123 4567").equals(new Phone("6591234567")));

        // EP: different digits once country code is included
        assertFalse(new Phone("+65 9123 4567").equals(new Phone("9123 4567")));
    }
}
