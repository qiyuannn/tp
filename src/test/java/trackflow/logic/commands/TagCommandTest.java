package trackflow.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.logic.commands.CommandTestUtil.assertCommandFailure;
import static trackflow.logic.commands.CommandTestUtil.assertCommandSuccess;
import static trackflow.logic.commands.CommandTestUtil.showPersonAtIndex;
import static trackflow.model.util.SampleDataUtil.getTagSet;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static trackflow.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;

import org.junit.jupiter.api.Test;

import trackflow.commons.core.index.Index;
import trackflow.logic.Messages;
import trackflow.model.AthleteRoster;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.UserPrefs;
import trackflow.model.person.Person;
import trackflow.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for {@code TagCommand}.
 */
public class TagCommandTest {

    private final Model model = new ModelManager(getTypicalRoster(), new UserPrefs());

    @Test
    public void constructor_null_throwsNullPointerException() {
        // EP: null target index in constructor
        assertThrows(NullPointerException.class, () -> new TagCommand(null, getTagSet("100m")));
        // EP: null tag set in constructor
        assertThrows(NullPointerException.class, () -> new TagCommand(INDEX_FIRST_PERSON, null));
    }

    // EP: null model during command execution
    @Test
    public void execute_nullModel_throwsNullPointerException() {
        TagCommand command = new TagCommand(INDEX_FIRST_PERSON, getTagSet("100m"));
        assertThrows(NullPointerException.class, () -> command.execute(null));
    }

    // EP: add multiple new distinct event tags to athlete (keeps existing tags)
    @Test
    public void execute_newEvents_addsEventsAndKeepsExisting() {
        Person athlete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        TagCommand command = new TagCommand(INDEX_FIRST_PERSON, getTagSet("Long Jump", "100m")); // Boundary value
        Person taggedAthlete = new PersonBuilder(athlete).withTags("friends", "100m", "Long Jump").build();

        String expectedMessage = String.format(TagCommand.MESSAGE_SUCCESS, "100m, Long Jump",
                athlete.getName());
        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        expectedModel.setPerson(athlete, taggedAthlete);

        assertCommandSuccess(command, model, expectedMessage, expectedModel);
    }

    // EP: add mix of existing (case-insensitive) and new event tags (adds only new)
    @Test
    public void execute_someEventsAlreadyPresent_addsOnlyNewEvents() {
        // ALICE already has 'friends'; matching ignores case and the stored name is reported
        Person athlete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        TagCommand command = new TagCommand(INDEX_FIRST_PERSON, getTagSet("FRIENDS", "200m"));
        Person taggedAthlete = new PersonBuilder(athlete).withTags("friends", "200m").build();

        String expectedMessage = String.format(TagCommand.MESSAGE_SUCCESS, "200m", athlete.getName())
                + String.format(TagCommand.MESSAGE_SKIPPED_EVENTS, "friends");
        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        expectedModel.setPerson(athlete, taggedAthlete);

        assertCommandSuccess(command, model, expectedMessage, expectedModel);
    }

    // EP: all event tags already present on athlete (failure)
    @Test
    public void execute_allEventsAlreadyPresent_failure() {
        Person athlete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        TagCommand command = new TagCommand(INDEX_FIRST_PERSON, getTagSet("Friends"));

        assertCommandFailure(command, model,
                String.format(TagCommand.MESSAGE_ALREADY_TAGGED, athlete.getName(), "friends"));
    }

    // EP: valid index in filtered list tags correct displayed athlete
    @Test
    public void execute_filteredList_tagsDisplayedAthlete() {
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        Person athlete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        TagCommand command = new TagCommand(INDEX_FIRST_PERSON, getTagSet("Javelin")); // Boundary value
        Person taggedAthlete = new PersonBuilder(athlete).withTags("owesMoney", "friends", "Javelin").build();

        String expectedMessage = String.format(TagCommand.MESSAGE_SUCCESS, "Javelin", athlete.getName());
        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        expectedModel.setPerson(athlete, taggedAthlete);

        assertCommandSuccess(command, model, expectedMessage, expectedModel);
    }

    // EP: invalid index exceeding unfiltered list size (failure)
    @Test
    public void execute_invalidIndexUnfilteredList_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1); // Boundary value
        TagCommand command = new TagCommand(outOfBoundIndex, getTagSet("100m"));

        assertCommandFailure(command, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    // EP: invalid index out of bounds of filtered list (failure)
    @Test
    public void execute_invalidIndexFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        TagCommand command = new TagCommand(INDEX_SECOND_PERSON, getTagSet("100m")); // Boundary value

        assertCommandFailure(command, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    // EP: changesRoster returns true for tag modification command
    @Test
    public void changesRoster_returnsTrue() {
        assertTrue(new TagCommand(INDEX_FIRST_PERSON, getTagSet("100m")).changesRoster());
    }

    @Test
    public void equals() {
        TagCommand standardCommand = new TagCommand(INDEX_FIRST_PERSON, getTagSet("100m"));

        // EP: symmetric equality (different instance with same index and event tags)
        assertTrue(standardCommand.equals(new TagCommand(INDEX_FIRST_PERSON, getTagSet("100m"))));

        // EP: case-insensitive event tags yield equal command and hash code
        TagCommand caseInsensitiveCopy = new TagCommand(INDEX_FIRST_PERSON, getTagSet("100M"));
        assertTrue(standardCommand.equals(caseInsensitiveCopy));
        assertEquals(standardCommand.hashCode(), caseInsensitiveCopy.hashCode());

        // EP: reflexive equality (same object instance)
        assertTrue(standardCommand.equals(standardCommand));

        // EP: null comparison
        assertFalse(standardCommand.equals(null));

        // EP: different object type
        assertFalse(standardCommand.equals(new ClearCommand()));

        // EP: different command class with same arguments
        assertFalse(standardCommand.equals(new UntagCommand(INDEX_FIRST_PERSON, getTagSet("100m"))));

        // EP: different target index
        assertFalse(standardCommand.equals(new TagCommand(INDEX_SECOND_PERSON, getTagSet("100m"))));

        // EP: different events set
        assertFalse(standardCommand.equals(new TagCommand(INDEX_FIRST_PERSON, getTagSet("200m"))));

        // EP: string representation includes target index and event tags
        String expected = TagCommand.class.getCanonicalName()
                + "{index=" + INDEX_FIRST_PERSON + ", events=" + getTagSet("100m") + "}";
        assertEquals(expected, standardCommand.toString());
    }
}
