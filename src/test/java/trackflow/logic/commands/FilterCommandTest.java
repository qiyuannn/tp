// @@author Austin-Chia
package trackflow.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import trackflow.logic.parser.TrackFlowParser;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.person.AgeCategory;
import trackflow.model.person.Person;
import trackflow.testutil.PersonBuilder;

/**
 * Exercises filtering and its interaction with existing commands through the parser and model.
 */
public class FilterCommandTest {
    private final Person open = new PersonBuilder().withName("Alex").withAgeCategory("Open").build();
    private final Person junior = new PersonBuilder().withName("Beth").withAgeCategory("Under 14").build();
    private final Person otherJunior = new PersonBuilder().withName("Cara").withAgeCategory("Under 14").build();
    private final Model model = new ModelManager();
    private final TrackFlowParser parser = new TrackFlowParser();

    private void populateRoster() {
        model.addPerson(open);
        model.addPerson(junior);
        model.addPerson(otherJunior);
    }

    // EP: multiple matching athletes in target category (preserves roster order)
    @Test
    public void execute_multipleMatches_preservesRosterAndOrder() throws Exception {
        populateRoster();
        assertEquals("Displaying 2 athletes in age category Under 14.",
                parser.parseCommand("FiLtEr a/under   14").execute(model).getFeedbackToUser());
        assertEquals(List.of(junior, otherJunior), model.getFilteredPersonList());
        assertEquals(List.of(open, junior, otherJunior), model.getRoster().getPersonList());
    }

    // EP: empty roster displaying zero matches
    @Test
    public void execute_emptyRoster_displaysNoMatches() throws Exception {
        assertEquals("No athletes found in age category Open (0 matches).",
                parser.parseCommand("filter a/Open").execute(model).getFeedbackToUser());
        assertTrue(model.getFilteredPersonList().isEmpty()); // Boundary value
    }

    // EP: non-empty roster with zero matching athletes in category
    @Test
    public void execute_noMatches_clearsDisplay() throws Exception {
        populateRoster();
        assertEquals("No athletes found in age category Under 20 (0 matches).",
                parser.parseCommand("filter a/Under 20").execute(model).getFeedbackToUser());
        assertTrue(model.getFilteredPersonList().isEmpty()); // Boundary value
        assertEquals(List.of(open, junior, otherJunior), model.getRoster().getPersonList());
    }

    // EP: consecutive filter commands replace previous filter with exactly 1 match
    @Test
    public void execute_previousCategoryFilter_replacesFilter() throws Exception {
        populateRoster();
        parser.parseCommand("filter a/Under 14").execute(model);

        assertEquals("Displaying 1 athlete in age category Open.",
                parser.parseCommand("filter a/Open").execute(model).getFeedbackToUser());
        assertEquals(List.of(open), model.getFilteredPersonList()); // Boundary value
    }

    @Test
    public void equals() {
        FilterCommand command = new FilterCommand(new AgeCategory("Under 14"));
        // EP: reflexive equality (same object instance)
        assertTrue(command.equals(command));
        // EP: symmetric equality (different instance with equivalent case-insensitive category)
        assertEquals(command, new FilterCommand(new AgeCategory("under 14")));
        // EP: different category value
        assertFalse(command.equals(new FilterCommand(new AgeCategory("Open"))));
        // EP: null comparison
        assertFalse(command.equals(null));
        // EP: different object type
        assertFalse(command.equals("Under 14"));
    }

    // EP: null age category in constructor
    @Test
    public void constructor_nullCategory_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FilterCommand(null));
    }

    // EP: null model during command execution
    @Test
    public void execute_nullModel_throwsNullPointerException() {
        FilterCommand command = new FilterCommand(new AgeCategory("Under 14"));
        assertThrows(NullPointerException.class, () -> command.execute(null));
    }
    // @@author

    // EP: string representation includes category field
    @Test
    public void toStringMethod() {
        AgeCategory ageCategory = new AgeCategory("Under 14");
        FilterCommand command = new FilterCommand(ageCategory);
        String expected = FilterCommand.class.getCanonicalName() + "{ageCategory=" + ageCategory + "}";
        assertEquals(expected, command.toString());
    }

    // EP: usage message contains all supported age category options
    @Test
    public void messageUsage_listsAllValidAgeCategories() {
        List<String> validCategories = List.of("Under 14", "Under 16", "Under 18", "Under 20", "Open");
        assertTrue(FilterCommand.MESSAGE_USAGE.contains(
                "a/AGE_CATEGORY (Under 14, Under 16, Under 18, Under 20, or Open)"));
        for (String category : validCategories) {
            assertTrue(FilterCommand.MESSAGE_USAGE.contains(category));
        }
    }
}
