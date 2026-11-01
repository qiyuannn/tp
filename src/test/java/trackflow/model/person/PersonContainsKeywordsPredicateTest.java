package trackflow.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import trackflow.model.person.PersonContainsKeywordsPredicate.SearchField;
import trackflow.testutil.PersonBuilder;

/**
 * Distinguishes broad-keyword OR matching from AND across scoped fields using a fixed athlete fixture.
 * Each field-specific case also guards against a match leaking in from another searchable field.
 */
public class PersonContainsKeywordsPredicateTest {

    private static final Person ATHLETE = new PersonBuilder()
            .withName("Avery Tan")
            .withAgeCategory("Under 16")
            .withPhone("91234567")
            .withEmail("avery.tan@example.com")
            .withAddress("National Stadium")
            .withRemark("100m personal best")
            .withTags("Sprints", "Relay")
            .build();

    @Test
    public void constructor_null_throwsNullPointerException() {
        // EP: null broad keywords list
        assertThrows(NullPointerException.class, () -> new PersonContainsKeywordsPredicate(null));

        // EP: null field keywords map
        assertThrows(NullPointerException.class, () -> new PersonContainsKeywordsPredicate(List.of(), null));
    }

    @Test
    public void equals() {
        List<String> firstKeywordList = List.of("first");
        List<String> secondKeywordList = List.of("first", "second");

        PersonContainsKeywordsPredicate firstPredicate = new PersonContainsKeywordsPredicate(firstKeywordList);
        PersonContainsKeywordsPredicate secondPredicate = new PersonContainsKeywordsPredicate(secondKeywordList);

        // EP: reflexive equality (same object instance)
        assertTrue(firstPredicate.equals(firstPredicate));

        // EP: symmetric equality (different instance with same keyword list)
        assertTrue(firstPredicate.equals(new PersonContainsKeywordsPredicate(firstKeywordList)));

        // EP: different broad keyword list
        assertFalse(firstPredicate.equals(secondPredicate));

        // EP: different field-specific keywords map
        assertFalse(firstPredicate.equals(new PersonContainsKeywordsPredicate(firstKeywordList,
                Map.of(SearchField.NAME, List.of("first")))));

        // EP: different object type
        assertFalse(firstPredicate.equals(1));

        // EP: null comparison
        assertFalse(firstPredicate.equals(null));
    }

    // EP: partial keyword matching across each searchable field (substring match)
    @Test
    public void test_partialKeywordMatchesEachSearchableField_returnsTrue() {
        List<String> partialMatches = List.of(
                "ver", // name
                "nder", // age category
                "2345", // phone
                "tan@exam", // email
                "stad", // address
                "personal", // remark
                "rela"); // event

        for (String keyword : partialMatches) {
            PersonContainsKeywordsPredicate predicate = new PersonContainsKeywordsPredicate(List.of(keyword));
            assertTrue(predicate.test(ATHLETE), "Expected partial match for keyword: " + keyword);
        }
    }

    // EP: case-insensitive keyword matching (uppercase, mixed case)
    @Test
    public void test_keywordUsesDifferentCase_returnsTrue() {
        assertTrue(new PersonContainsKeywordsPredicate(List.of("AVERY")).test(ATHLETE));
        assertTrue(new PersonContainsKeywordsPredicate(List.of("PeRsOnAl")).test(ATHLETE));
    }

    // EP: multiple broad keywords with disjunctive OR logic (one matches, one does not)
    @Test
    public void test_anyKeywordMatches_returnsTrue() {
        PersonContainsKeywordsPredicate predicate =
                new PersonContainsKeywordsPredicate(List.of("missing", "1234"));
        assertTrue(predicate.test(ATHLETE));
    }

    // EP: field-scoped keyword matches only specified field without leaking to other fields
    @Test
    public void test_nameScopedKeyword_doesNotMatchAgeCategory() {
        PersonContainsKeywordsPredicate predicate = new PersonContainsKeywordsPredicate(List.of(),
                Map.of(SearchField.NAME, List.of("under")));
        Person namedUnderwood = new PersonBuilder(ATHLETE)
                .withName("Underwood Tan")
                .withAgeCategory("Open")
                .build();

        assertFalse(predicate.test(ATHLETE));
        assertTrue(predicate.test(namedUnderwood));
    }

    // EP: single field-scoped keywords matching each respective field
    @Test
    public void test_scopedKeywordsMatchOnlyTheirFields() {
        Map<SearchField, String> matchingValues = Map.of(
                SearchField.NAME, "avery",
                SearchField.AGE_CATEGORY, "under",
                SearchField.PHONE, "2345",
                SearchField.EMAIL, "tan@exam",
                SearchField.ADDRESS, "stad",
                SearchField.REMARK, "personal",
                SearchField.EVENT, "sprint");

        matchingValues.forEach((field, keyword) -> assertTrue(
                new PersonContainsKeywordsPredicate(List.of(), Map.of(field, List.of(keyword))).test(ATHLETE),
                "Expected scoped match for " + field));
    }

    @Test
    public void test_multipleFields_requiresAllToMatch() {
        // EP: multiple scoped fields matching conjunctively (all fields match -> returns true)
        PersonContainsKeywordsPredicate predicate = new PersonContainsKeywordsPredicate(List.of(), Map.of(
                SearchField.NAME, List.of("avery"),
                SearchField.AGE_CATEGORY, List.of("Under 16")));
        assertTrue(predicate.test(ATHLETE));

        // EP: multiple scoped fields with partial match (one field fails -> returns false)
        Person openAvery = new PersonBuilder(ATHLETE).withAgeCategory("Open").build();
        assertFalse(predicate.test(openAvery));
    }

    @Test
    public void test_broadKeywordOrField_matchesEither() {
        // EP: broad keyword matches while scoped field does not (disjunctive match)
        PersonContainsKeywordsPredicate broadMatches = new PersonContainsKeywordsPredicate(List.of("avery"),
                Map.of(SearchField.EMAIL, List.of("other.com")));
        assertTrue(broadMatches.test(ATHLETE));

        // EP: scoped field matches while broad keyword does not (disjunctive match)
        PersonContainsKeywordsPredicate fieldMatches = new PersonContainsKeywordsPredicate(List.of("zzznomatch"),
                Map.of(SearchField.EMAIL, List.of("example.com")));
        assertTrue(fieldMatches.test(ATHLETE));

        // EP: neither broad keyword nor scoped field matches
        PersonContainsKeywordsPredicate neither = new PersonContainsKeywordsPredicate(List.of("zzznomatch"),
                Map.of(SearchField.EMAIL, List.of("other.com")));
        assertFalse(neither.test(ATHLETE));
    }

    // EP: scoped keyword values applied to non-matching field types
    @Test
    public void test_scopedKeywordInWrongField_returnsFalse() {
        assertFalse(new PersonContainsKeywordsPredicate(List.of(),
                Map.of(SearchField.NAME, List.of("9123"))).test(ATHLETE));
        assertFalse(new PersonContainsKeywordsPredicate(List.of(),
                Map.of(SearchField.PHONE, List.of("avery"))).test(ATHLETE));
    }

    @Test
    public void test_noKeywordMatches_returnsFalse() {
        // EP: empty keyword list (boundary: zero keywords)
        assertFalse(new PersonContainsKeywordsPredicate(List.of()).test(ATHLETE)); // Boundary value

        // EP: whitespace-only keyword
        assertFalse(new PersonContainsKeywordsPredicate(List.of("   ")).test(ATHLETE));

        // EP: non-matching keywords
        assertFalse(new PersonContainsKeywordsPredicate(List.of("swimming", "9999")).test(ATHLETE));
    }

    // EP: event keyword spanning across separate tags without individual match
    @Test
    public void test_eventKeywordAcrossTwoEvents_returnsFalse() {
        // events are matched one at a time, so a keyword spanning two events must not match
        assertFalse(new PersonContainsKeywordsPredicate(List.of(),
                Map.of(SearchField.EVENT, List.of("sprintsrelay"))).test(ATHLETE));
        assertFalse(new PersonContainsKeywordsPredicate(List.of(),
                Map.of(SearchField.EVENT, List.of("personal"))).test(ATHLETE));
    }

    @Test
    public void test_combinedBroadAndScopedKeywords_matchesIfEitherMatches() {
        // EP: broad match with non-matching scoped field (disjunctive success)
        PersonContainsKeywordsPredicate broadMatchPredicate = new PersonContainsKeywordsPredicate(
                List.of("avery"), Map.of(SearchField.PHONE, List.of("0000")));
        assertTrue(broadMatchPredicate.test(ATHLETE));

        // EP: scoped match with non-matching broad keyword (disjunctive success)
        PersonContainsKeywordsPredicate scopedMatchPredicate = new PersonContainsKeywordsPredicate(
                List.of("nonexistent"), Map.of(SearchField.PHONE, List.of("2345")));
        assertTrue(scopedMatchPredicate.test(ATHLETE));

        // EP: neither broad keyword nor scoped field matches (disjunctive failure)
        PersonContainsKeywordsPredicate noMatchPredicate = new PersonContainsKeywordsPredicate(
                List.of("nonexistent"), Map.of(SearchField.PHONE, List.of("0000")));
        assertFalse(noMatchPredicate.test(ATHLETE));
    }

    // EP: default PersonContainsKeywordsPredicate string representation
    @Test
    public void toStringMethod() {
        List<String> keywords = List.of("keyword1", "keyword2");
        PersonContainsKeywordsPredicate predicate = new PersonContainsKeywordsPredicate(keywords);

        String expected = PersonContainsKeywordsPredicate.class.getCanonicalName()
                + "{keywords=" + keywords + ", fieldKeywords={}}";
        assertEquals(expected, predicate.toString());
    }
}
