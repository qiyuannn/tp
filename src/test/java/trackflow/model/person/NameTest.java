package trackflow.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class NameTest {

    // EP: null name
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Name(null));
    }

    // EP: invalid name (empty string)
    @Test
    public void constructor_invalidName_throwsIllegalArgumentException() {
        String invalidName = ""; // Boundary value
        assertThrows(IllegalArgumentException.class, Name.MESSAGE_CONSTRAINTS, () -> new Name(invalidName));
    }

    @Test
    public void isValidName() {
        // EP: null name
        assertThrows(NullPointerException.class, () -> Name.isValidName(null));

        // EP: empty or blank strings
        assertFalse(Name.isValidName("")); // Boundary value
        assertFalse(Name.isValidName(" "));

        // EP: non-alphanumeric characters or symbols
        assertFalse(Name.isValidName("^"));
        assertFalse(Name.isValidName("peter*"));

        // EP: invalid punctuation placement (leading or trailing hyphen / apostrophe)
        assertFalse(Name.isValidName("-Jane")); // Boundary value
        assertFalse(Name.isValidName("'Brien")); // Boundary value
        assertFalse(Name.isValidName("Jane-")); // Boundary value
        assertFalse(Name.isValidName("O'")); // Boundary value

        // EP: invalid consecutive separators or spaces around punctuation
        assertFalse(Name.isValidName("Mary--Jane"));
        assertFalse(Name.isValidName("A--''"));
        assertFalse(Name.isValidName("Mary - Jane"));
        assertFalse(Name.isValidName("Jane- ")); // Boundary value

        // EP: valid alphabetic and alphanumeric names
        assertTrue(Name.isValidName("peter jack"));
        assertTrue(Name.isValidName("12345"));
        assertTrue(Name.isValidName("peter the 2nd"));
        assertTrue(Name.isValidName("Capital Tan"));
        assertTrue(Name.isValidName("David Roger Jackson Ray Jr 2nd"));
        assertTrue(Name.isValidName("Mary-Jane"));
        assertTrue(Name.isValidName("O'Brien"));
        assertTrue(Name.isValidName("Jean-Luc O'Neil"));
        assertTrue(Name.isValidName("Lee  Ann"));

        // EP: valid single-character name (minimum length)
        assertTrue(Name.isValidName("A")); // Boundary value
    }

    // EP: long word sequences and boundary punctuation under timeout (performance & robustness)
    @Test
    public void isValidName_longWordSequences_doesNotOverflowOrStall() {
        String manyWords = "Alex ".repeat(3000).trim();
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertTrue(Name.isValidName(manyWords));
            assertTrue(Name.isValidName(manyWords + "   "));
            assertTrue(Name.isValidName("Alex-".repeat(3000) + "Tan"));
            assertFalse(Name.isValidName(manyWords + "!"));
            assertFalse(Name.isValidName(manyWords + "-"));
        });
    }

    @Test
    public void equals() {
        Name name = new Name("Valid Name");

        // EP: symmetric equality (different instance with same name value) & string representation
        assertTrue(name.equals(new Name("Valid Name")));
        assertEquals(name.hashCode(), new Name("Valid Name").hashCode());
        assertEquals("Valid Name", name.toString());

        // EP: reflexive equality (same object instance)
        assertTrue(name.equals(name));

        // EP: null comparison
        assertFalse(name.equals(null));

        // EP: different object type
        assertFalse(name.equals(5.0f));

        // EP: different field value
        assertFalse(name.equals(new Name("Other Valid Name")));
    }
}
