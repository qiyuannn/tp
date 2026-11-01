// @@author qiyuannn
package trackflow.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class AgeCategoryTest {
    @Test
    public void constructor_validCategories_normalizesDisplay() {
        // EP: valid age categories (Under 14, 16, 18, 20, Open) with case insensitivity
        for (String category : new String[] {"Under 14", "Under 16", "Under 18", "Under 20", "Open"}) {
            assertTrue(AgeCategory.isValidAgeCategory(category));
            assertEquals(category, new AgeCategory(category.toUpperCase()).value);
        }

        // EP: valid age category with irregular leading, trailing, and internal whitespace normalized
        assertEquals("Under 14", new AgeCategory("  under \t  14  ").value);
        assertEquals("Under 14", new AgeCategory("under 14").toString());
    }

    @Test
    public void constructor_invalidCategory_throws() {
        // EP: null age category
        assertThrows(NullPointerException.class, () -> new AgeCategory(null));
        assertThrows(NullPointerException.class, () -> AgeCategory.isValidAgeCategory(null));

        // EP: invalid age categories (empty, blank, missing prefix, abbreviated, unsupported age)
        for (String category : new String[] {
            "", // Boundary value
            " ",
            "14",
            "U14",
            "Under 15"
        }) {
            assertFalse(AgeCategory.isValidAgeCategory(category));
            assertThrows(IllegalArgumentException.class, AgeCategory.MESSAGE_CONSTRAINTS, () ->
                new AgeCategory(category));
        }
    }

    @Test
    public void equals_normalizedCategories_match() {
        AgeCategory category = new AgeCategory("Under 14");

        // EP: symmetric equality (normalized whitespace & case) and consistent hash code
        assertEquals(category, new AgeCategory("under   14"));
        assertEquals(category.hashCode(), new AgeCategory("UNDER 14").hashCode());

        // EP: different category value
        assertFalse(category.equals(new AgeCategory("Open")));

        // EP: null comparison
        assertFalse(category.equals(null));

        // EP: different object type
        assertFalse(category.equals("Under 14"));
    }
}
// @@author
