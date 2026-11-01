package trackflow.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class TagTest {

    // EP: null tag name
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Tag(null));
    }

    // EP: invalid tag name (empty string)
    @Test
    public void constructor_invalidTagName_throwsIllegalArgumentException() {
        String invalidTagName = ""; // Boundary value
        assertThrows(IllegalArgumentException.class, () -> new Tag(invalidTagName));
    }

    @Test
    public void isValidTagName() {
        // EP: null tag name
        assertThrows(NullPointerException.class, () -> Tag.isValidTagName(null));

        // EP: empty or blank strings
        assertFalse(Tag.isValidTagName("")); // Boundary value
        assertFalse(Tag.isValidTagName(" ")); // spaces only

        // EP: invalid whitespace placement (leading, trailing, or consecutive spaces)
        assertFalse(Tag.isValidTagName(" 100m")); // Boundary value
        assertFalse(Tag.isValidTagName("100m ")); // Boundary value
        assertFalse(Tag.isValidTagName("Long  Jump")); // repeated space

        // EP: invalid hyphen placement (leading, trailing, spaced, or consecutive hyphens)
        assertFalse(Tag.isValidTagName("-100m")); // Boundary value
        assertFalse(Tag.isValidTagName("100m-")); // Boundary value
        assertFalse(Tag.isValidTagName("Long - Jump")); // spaces around a hyphen
        assertFalse(Tag.isValidTagName("Long -Jump")); // space before a hyphen
        assertFalse(Tag.isValidTagName("Long- Jump")); // space after a hyphen
        assertFalse(Tag.isValidTagName("Long--Jump")); // repeated hyphen

        // EP: disallowed symbols and punctuation
        assertFalse(Tag.isValidTagName("4x100m/relay")); // slash
        assertFalse(Tag.isValidTagName("100m*")); // other symbol

        // EP: tag name exceeding maximum allowed length
        assertFalse(Tag.isValidTagName("a".repeat(Tag.MAX_LENGTH + 1))); // Boundary value

        // EP: valid tag names at length boundaries (minimum and maximum allowed length)
        assertTrue(Tag.isValidTagName("a")); // Boundary value
        assertTrue(Tag.isValidTagName("a".repeat(Tag.MAX_LENGTH))); // Boundary value

        // EP: valid alphanumeric tag names with allowed single space or hyphen
        assertTrue(Tag.isValidTagName("100m")); // letters and digits
        assertTrue(Tag.isValidTagName("4x100m"));
        assertTrue(Tag.isValidTagName("Long Jump")); // single space
        assertTrue(Tag.isValidTagName("110m-Hurdles")); // single hyphen
        assertTrue(Tag.isValidTagName("Saut-Élan")); // non-English letter
        assertTrue(Tag.isValidTagName("Ünder18 Sprint")); // non-English letter with a digit

        // EP: valid words containing apostrophes or decimal points between digits
        assertTrue(Tag.isValidTagName("Women's 100m"));
        assertTrue(Tag.isValidTagName("Hammer 7.26kg"));
        assertTrue(Tag.isValidTagName("0.84m Hurdles"));
        assertTrue(Tag.isValidTagName("Men's 4x400m-Relay"));

        // EP: invalid apostrophe placement (leading, trailing, consecutive, or spaced)
        assertFalse(Tag.isValidTagName("'100m")); // Boundary value
        assertFalse(Tag.isValidTagName("100m'")); // Boundary value
        assertFalse(Tag.isValidTagName("Women''s")); // repeated apostrophe

        // EP: invalid decimal point placement (non-numeric adjacent, consecutive, or between letters)
        assertFalse(Tag.isValidTagName("7.kg")); // Boundary value
        assertFalse(Tag.isValidTagName(".5kg")); // Boundary value
        assertFalse(Tag.isValidTagName("Shot.Put")); // decimal point between letters
        assertFalse(Tag.isValidTagName("7..26kg")); // repeated decimal point
        assertFalse(Tag.isValidTagName("Women 's")); // space before an apostrophe

        // EP: disallowed non-alphanumeric symbols and non-ASCII punctuation
        assertFalse(Tag.isValidTagName("100m€"));
        assertFalse(Tag.isValidTagName("Élan!"));
    }

    // EP: valid legacy alphanumeric tag names exceeding standard maximum length
    @Test
    public void fromStoredName_legacyAlphanumeric_preservesNameWithoutAllowingNewLongTags() {
        for (int length : new int[] {Tag.MAX_LENGTH + 1, 1000}) { // Boundary value
            String legacyName = "A".repeat(length);
            Tag restored = Tag.fromStoredName(legacyName);
            assertEquals(legacyName, restored.tagName);
            assertEquals(restored, Tag.fromStoredName(legacyName.toLowerCase(Locale.ROOT)));
            assertEquals(restored.hashCode(),
                    Tag.fromStoredName(legacyName.toLowerCase(Locale.ROOT)).hashCode());
            assertThrows(IllegalArgumentException.class, () -> new Tag(legacyName));
        }
        assertEquals(new Tag("Long Jump"), Tag.fromStoredName("Long Jump"));
    }

    @Test
    public void fromStoredName_invalidName_rejectsWithoutTrimmingOrRelaxingLegacyFormat() {
        // EP: null stored name
        assertThrows(NullPointerException.class, () -> Tag.fromStoredName(null));

        // EP: invalid stored tag names (empty, malformed spacing, or symbols)
        for (String name : new String[] {"", " ", " 100m", "100m ", "100m*", "Long  Jump",
            "A".repeat(31) + " B", "A".repeat(31) + "-B"}) {
            assertThrows(IllegalArgumentException.class, () -> Tag.fromStoredName(name));
        }
    }

    // EP: validation constraint message content (separator requirements)
    @Test
    public void messageConstraints_describesSeparatorRule() {
        // the message must state the rule that rejects spaced or repeated hyphens
        assertTrue(Tag.MESSAGE_CONSTRAINTS.contains("single space or a single hyphen"));
        assertTrue(Tag.MESSAGE_CONSTRAINTS.contains("not Long - Jump"));
    }

    @Test
    public void equals() {
        Tag tag = new Tag("Long Jump");

        // EP: symmetric equality (different instance with identical tag name)
        assertTrue(tag.equals(new Tag("Long Jump")));

        // EP: case-insensitive equality and hash code consistency
        assertTrue(tag.equals(new Tag("long jump")));
        assertEquals(tag.hashCode(), new Tag("LONG JUMP").hashCode());

        // EP: reflexive equality (same object instance)
        assertTrue(tag.equals(tag));

        // EP: null comparison
        assertFalse(tag.equals(null)); // Boundary value

        // EP: different object type
        assertFalse(tag.equals("Long Jump"));

        // EP: different tag name value
        assertNotEquals(tag, new Tag("High Jump"));
    }

    // EP: Unicode normalization (decomposed combining accent marks composed into single characters)
    @Test
    public void constructor_decomposedAccent_storedAsSingleLetter() {
        String decomposedName = "Élan"; // E followed by a combining acute accent
        assertTrue(Tag.isValidTagName(decomposedName));
        assertEquals("Élan", new Tag(decomposedName).tagName);
        assertEquals("Élan", Tag.fromStoredName(decomposedName).tagName);
        assertEquals(new Tag("Élan"), new Tag(decomposedName));
    }

    // EP: Unicode case folding (Greek capital sigma matching both standard and final lowercase forms)
    @Test
    public void equals_lettersWithSeveralLowerCaseForms_returnsTrue() {
        // the Greek capital sigma has two lower-case forms, σ and the word-final ς
        Tag capitalSigma = new Tag("Σ");
        for (String name : new String[] {"σ", "ς"}) {
            assertEquals(capitalSigma, new Tag(name));
            assertEquals(capitalSigma.hashCode(), new Tag(name).hashCode());
        }
    }

    // EP: string representation preserving original casing
    @Test
    public void toString_keepsOriginalCase() {
        assertEquals("[Long Jump]", new Tag("Long Jump").toString());
    }

}
