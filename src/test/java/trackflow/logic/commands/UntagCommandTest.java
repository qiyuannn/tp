package trackflow.logic.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.logic.commands.CommandTestUtil.assertCommandFailure;
import static trackflow.logic.commands.CommandTestUtil.assertCommandSuccess;
import static trackflow.logic.commands.CommandTestUtil.showPersonAtIndex;
import static trackflow.model.util.SampleDataUtil.getTagSet;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static trackflow.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;

import java.util.Set;

import org.junit.jupiter.api.Test;

import trackflow.commons.core.index.Index;
import trackflow.logic.Messages;
import trackflow.model.AthleteRoster;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.UserPrefs;
import trackflow.model.person.Person;
import trackflow.model.tag.Tag;
import trackflow.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for {@code UntagCommand}.
 */
public class UntagCommandTest {

    private final Model model = new ModelManager(getTypicalRoster(), new UserPrefs());

    // EP: remove single existing tag/event with case-insensitive matching
    @Test
    public void execute_existingEvent_removesEvent() {
        // BENSON has 'owesMoney' and 'friends'; matching ignores case and reports the stored name
        Person athlete = model.getFilteredPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        UntagCommand command = new UntagCommand(INDEX_SECOND_PERSON, getTagSet("OWESMONEY"));
        Person untaggedAthlete = new PersonBuilder(athlete).withTags("friends").build();

        String expectedMessage = String.format(UntagCommand.MESSAGE_SUCCESS, "owesMoney", athlete.getName());
        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        expectedModel.setPerson(athlete, untaggedAthlete);

        assertCommandSuccess(command, model, expectedMessage, expectedModel);
    }

    // EP: remove all existing tags/events from person resulting in empty tag set
    @Test
    public void execute_allEvents_removesAllEvents() {
        Person athlete = model.getFilteredPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        UntagCommand command = new UntagCommand(INDEX_SECOND_PERSON, getTagSet("owesMoney", "friends"));
        Person untaggedAthlete = new PersonBuilder(athlete).withTags().build(); // Boundary value

        String expectedMessage = String.format(UntagCommand.MESSAGE_SUCCESS, "friends, owesMoney",
                athlete.getName());
        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        expectedModel.setPerson(athlete, untaggedAthlete);

        assertCommandSuccess(command, model, expectedMessage, expectedModel);
    }

    // EP: remove legacy tag/event exceeding max length limit
    @Test
    public void execute_legacyEventLongerThanLimit_removesEvent() {
        Person athlete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Tag legacyEvent = Tag.fromStoredName("A".repeat(Tag.MAX_LENGTH + 1)); // Boundary value
        Person athleteWithLegacyEvent = new Person(athlete.getName(), athlete.getAgeCategory(), athlete.getPhone(),
                athlete.getEmail(), athlete.getAddress(), athlete.getRemark(), Set.of(legacyEvent));
        model.setPerson(athlete, athleteWithLegacyEvent);

        UntagCommand command = new UntagCommand(INDEX_FIRST_PERSON,
                Set.of(Tag.fromStoredName("a".repeat(Tag.MAX_LENGTH + 1)))); // Boundary value
        Person untaggedAthlete = new PersonBuilder(athleteWithLegacyEvent).withTags().build(); // Boundary value

        String expectedMessage = String.format(UntagCommand.MESSAGE_SUCCESS, legacyEvent.tagName,
                athlete.getName());
        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        expectedModel.setPerson(athleteWithLegacyEvent, untaggedAthlete);

        assertCommandSuccess(command, model, expectedMessage, expectedModel);
    }

    // EP: untag non-existent tags/events (throws failure and makes no change)
    @Test
    public void execute_eventNotPresent_failureAndNothingRemoved() {
        Person athlete = model.getFilteredPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        UntagCommand command = new UntagCommand(INDEX_SECOND_PERSON, getTagSet("friends", "100m", "200m"));

        assertCommandFailure(command, model,
                String.format(UntagCommand.MESSAGE_EVENT_NOT_FOUND, athlete.getName(), "100m, 200m"));
    }

    // EP: remove tag from displayed person in filtered list at valid first index
    @Test
    public void execute_filteredList_untagsDisplayedAthlete() {
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        Person athlete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        UntagCommand command = new UntagCommand(INDEX_FIRST_PERSON, getTagSet("owesMoney")); // Boundary value
        Person untaggedAthlete = new PersonBuilder(athlete).withTags("friends").build();

        String expectedMessage = String.format(UntagCommand.MESSAGE_SUCCESS, "owesMoney", athlete.getName());
        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        expectedModel.setPerson(athlete, untaggedAthlete);

        assertCommandSuccess(command, model, expectedMessage, expectedModel);
    }

    // EP: invalid index exceeding unfiltered list size
    @Test
    public void execute_invalidIndexUnfilteredList_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1); // Boundary value
        UntagCommand command = new UntagCommand(outOfBoundIndex, getTagSet("friends"));

        assertCommandFailure(command, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    // EP: invalid index exceeding filtered list size but within athlete roster size
    @Test
    public void execute_invalidIndexFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        UntagCommand command = new UntagCommand(INDEX_SECOND_PERSON, getTagSet("friends")); // Boundary value

        assertCommandFailure(command, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        UntagCommand standardCommand = new UntagCommand(INDEX_FIRST_PERSON, getTagSet("100m"));

        // EP: symmetric equality (different instance with same index and tag set)
        assertTrue(standardCommand.equals(new UntagCommand(INDEX_FIRST_PERSON, getTagSet("100m"))));

        // EP: case-insensitive event match equality
        assertTrue(standardCommand.equals(new UntagCommand(INDEX_FIRST_PERSON, getTagSet("100M"))));

        // EP: reflexive equality (same object instance)
        assertTrue(standardCommand.equals(standardCommand));

        // EP: null comparison
        assertFalse(standardCommand.equals(null)); // Boundary value

        // EP: different command type
        assertFalse(standardCommand.equals(new TagCommand(INDEX_FIRST_PERSON, getTagSet("100m"))));

        // EP: different index value
        assertFalse(standardCommand.equals(new UntagCommand(INDEX_SECOND_PERSON, getTagSet("100m"))));

        // EP: different tag set
        assertFalse(standardCommand.equals(new UntagCommand(INDEX_FIRST_PERSON, getTagSet("200m"))));
    }
}
