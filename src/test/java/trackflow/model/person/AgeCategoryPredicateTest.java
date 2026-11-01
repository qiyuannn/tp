// @@author Austin-Chia
package trackflow.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import trackflow.testutil.PersonBuilder;

public class AgeCategoryPredicateTest {
    @Test
    public void test_sameAndDifferentCategories_matchesOnlyRecordedCategory() {
        AgeCategoryPredicate predicate = new AgeCategoryPredicate(new AgeCategory("under 14"));

        // EP: exact matching category matches athlete
        assertTrue(predicate.test(new PersonBuilder().withAgeCategory("Under 14").build()));

        // EP: non-matching age categories do not match athlete (even if name contains category text)
        for (String category : new String[] {"Under 16", "Under 18", "Under 20", "Open"}) {
            assertFalse(predicate.test(new PersonBuilder().withName("Under 14").withAgeCategory(category).build()));
        }
    }

    @Test
    public void equals() {
        AgeCategoryPredicate predicate = new AgeCategoryPredicate(new AgeCategory("Open"));

        // EP: reflexive equality (same object instance)
        assertTrue(predicate.equals(predicate));

        // EP: symmetric equality (different instance with case-insensitive equal category)
        assertEquals(predicate, new AgeCategoryPredicate(new AgeCategory("OPEN")));

        // EP: different category value
        assertFalse(predicate.equals(new AgeCategoryPredicate(new AgeCategory("Under 14"))));

        // EP: null comparison
        assertFalse(predicate.equals(null));

        // EP: different object type
        assertFalse(predicate.equals("Open"));
    }

    // EP: null age category
    @Test
    public void constructor_nullCategory_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new AgeCategoryPredicate(null));
    }
    // @@author

    // EP: default AgeCategoryPredicate string representation
    @Test
    public void toStringMethod() {
        AgeCategoryPredicate predicate = new AgeCategoryPredicate(new AgeCategory("Open"));
        String expected = AgeCategoryPredicate.class.getCanonicalName() + "{ageCategory=Open}";
        assertEquals(expected, predicate.toString());
    }
}
