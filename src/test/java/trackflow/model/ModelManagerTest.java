package trackflow.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.model.Model.PREDICATE_SHOW_ALL_PERSONS;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalPersons.ALICE;
import static trackflow.testutil.TypicalPersons.BENSON;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import trackflow.commons.core.GuiSettings;
import trackflow.model.person.PersonContainsKeywordsPredicate;
import trackflow.testutil.AthleteRosterBuilder;

public class ModelManagerTest {

    private ModelManager modelManager = new ModelManager();

    // EP: default constructor initializing empty roster and default preferences
    @Test
    public void constructor() {
        assertEquals(new UserPrefs(), modelManager.getUserPrefs());
        assertEquals(new GuiSettings(), modelManager.getGuiSettings());
        assertEquals(new AthleteRoster(), new AthleteRoster(modelManager.getRoster())); // Boundary value
    }

    @Test
    public void constructor_validUserPrefs_copiesUserPrefs() {
        // EP: constructor with custom user preferences (defensive copy initialization)
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        modelManager = new ModelManager(new AthleteRoster(), userPrefs); // Boundary value
        assertEquals(userPrefs, modelManager.getUserPrefs());

        // EP: defensive copying of user preferences (mutating original does not mutate model manager state)
        UserPrefs oldUserPrefs = new UserPrefs(userPrefs);
        userPrefs.setGuiSettings(new GuiSettings(5, 6, 7, 8));
        assertEquals(oldUserPrefs, modelManager.getUserPrefs());
    }

    // EP: null GuiSettings
    @Test
    public void setGuiSettings_nullGuiSettings_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.setGuiSettings(null)); // Boundary value
    }

    // EP: valid GuiSettings instance
    @Test
    public void setGuiSettings_validGuiSettings_setsGuiSettings() {
        GuiSettings guiSettings = new GuiSettings(1, 2, 3, 4); // Boundary value
        modelManager.setGuiSettings(guiSettings);
        assertEquals(guiSettings, modelManager.getGuiSettings());
    }

    // EP: null person
    @Test
    public void hasPerson_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.hasPerson(null)); // Boundary value
    }

    // EP: person not in roster (empty roster)
    @Test
    public void hasPerson_personNotInAthleteRoster_returnsFalse() {
        assertFalse(modelManager.hasPerson(ALICE)); // Boundary value
    }

    // EP: person present in roster (singleton roster)
    @Test
    public void hasPerson_personInAthleteRoster_returnsTrue() {
        modelManager.addPerson(ALICE); // Boundary value
        assertTrue(modelManager.hasPerson(ALICE));
    }

    // EP: unmodifiable filtered person list view (direct modification rejected)
    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () ->
                modelManager.getFilteredPersonList().remove(0)); // Boundary value
    }

    // EP: null comparator restores original roster order
    @Test
    public void updateSortedPersonList_nullComparator_restoresRosterOrder() {
        modelManager = new ModelManager(
                new AthleteRosterBuilder().withPerson(BENSON).withPerson(ALICE).build(), new UserPrefs());
        modelManager.updateSortedPersonList(
                Comparator.comparing(person -> person.getName().fullName, String.CASE_INSENSITIVE_ORDER));

        modelManager.updateSortedPersonList(null); // Boundary value

        assertNull(modelManager.getPersonListComparator());
        assertEquals(List.of(BENSON, ALICE), modelManager.getFilteredPersonList());
    }

    // EP: valid comparator sorts displayed person list
    @Test
    public void updateSortedPersonList_validComparator_sortsDisplayedList() {
        modelManager = new ModelManager(
                new AthleteRosterBuilder().withPerson(BENSON).withPerson(ALICE).build(), new UserPrefs());
        modelManager.updateSortedPersonList(
                Comparator.comparing(person -> person.getName().fullName, String.CASE_INSENSITIVE_ORDER));
        assertEquals(List.of(ALICE, BENSON), modelManager.getFilteredPersonList());
    }

    @Test
    public void equals() {
        AthleteRoster roster = new AthleteRosterBuilder().withPerson(ALICE).withPerson(BENSON).build();
        AthleteRoster differentRoster = new AthleteRoster();
        UserPrefs userPrefs = new UserPrefs();

        // EP: symmetric equality (different instance with same roster and preferences)
        modelManager = new ModelManager(roster, userPrefs);
        ModelManager modelManagerCopy = new ModelManager(roster, userPrefs);
        assertTrue(modelManager.equals(modelManagerCopy));

        // EP: reflexive equality (same object instance)
        assertTrue(modelManager.equals(modelManager));

        // EP: null comparison
        assertFalse(modelManager.equals(null));

        // EP: different object type
        assertFalse(modelManager.equals(5));

        // EP: different roster
        assertFalse(modelManager.equals(new ModelManager(differentRoster, userPrefs))); // Boundary value

        // EP: different filtered person list (filtered vs unfiltered)
        String[] keywords = ALICE.getName().fullName.split("\\s+");
        modelManager.updateFilteredPersonList(new PersonContainsKeywordsPredicate(List.of(keywords)));
        assertFalse(modelManager.equals(new ModelManager(roster, userPrefs)));

        // resets modelManager to initial state for upcoming tests
        modelManager.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);

        // EP: different user preferences
        UserPrefs differentUserPrefs = new UserPrefs();
        differentUserPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4)); // Boundary value
        assertFalse(modelManager.equals(new ModelManager(roster, differentUserPrefs)));
    }
}
