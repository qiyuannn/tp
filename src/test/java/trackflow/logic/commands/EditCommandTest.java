// @@author qiyuannn
package trackflow.logic.commands;
// @@author

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
// @@author qiyuannn
import static trackflow.logic.commands.CommandTestUtil.DESC_AMY;
import static trackflow.logic.commands.CommandTestUtil.DESC_BOB;
// @@author
import static trackflow.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
// @@author qiyuannn
import static trackflow.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static trackflow.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
// @@author
import static trackflow.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
// @@author qiyuannn
import static trackflow.logic.commands.CommandTestUtil.assertCommandFailure;
import static trackflow.logic.commands.CommandTestUtil.assertCommandSuccess;
import static trackflow.logic.commands.CommandTestUtil.showPersonAtIndex;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static trackflow.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static trackflow.testutil.TypicalPersons.getTypicalRoster;
// @@author

import java.util.List;

import org.junit.jupiter.api.Test;

// @@author qiyuannn
import trackflow.commons.core.index.Index;
import trackflow.logic.Messages;
import trackflow.logic.commands.EditCommand.EditPersonDescriptor;
import trackflow.model.AthleteRoster;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.UserPrefs;
import trackflow.model.person.AgeCategory;
import trackflow.model.person.AgeCategoryPredicate;
import trackflow.model.person.Person;
import trackflow.testutil.EditPersonDescriptorBuilder;
import trackflow.testutil.PersonBuilder;
// @@author

/**
 * Contains integration tests (interaction with the Model) and unit tests for EditCommand.
 */
public class EditCommandTest {

    // @@author qiyuannn
    private Model model = new ModelManager(getTypicalRoster(), new UserPrefs());
    // @@author

    // @@author Austin-Chia
    // EP: edit person creating shared contact with existing person (warning emitted)
    @Test
    public void execute_sharedContactWithDifferentName_successWithWarning() {
        Person original = new PersonBuilder().withName("Avery Tan").build();
        Person other = new PersonBuilder().withName("Jordan Lee").withPhone("81112222")
                .withEmail("other@example.com").build();
        model = new ModelManager();
        model.addPerson(original);
        model.addPerson(other);
        model.updateFilteredPersonList(person -> person == original);
        Person edited = new PersonBuilder(original).withEmail("OTHER@example.com").build();
        Model expectedModel = new ModelManager(model.getRoster(), new UserPrefs());
        expectedModel.setPerson(original, edited);
        EditCommand command = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withEmail("OTHER@example.com").build()); // Boundary value
        String expected = String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.formatAthlete(edited))
                + String.format(Messages.MESSAGE_POSSIBLE_DUPLICATE,
                        "- Jordan Lee; Age category: Open; Phone: 81112222; Email: other@example.com; Matches: email");
        assertCommandSuccess(command, model, expected, expectedModel);
    }

    // @@author
    // EP: edit all fields of person at valid first index in unfiltered list
    @Test
    public void execute_allFieldsSpecifiedUnfilteredList_success() {
        Person editedPerson = new PersonBuilder(model.getFilteredPersonList().get(0))
                .withName(VALID_NAME_BOB).withAgeCategory("Under 16")
                .withPhone(VALID_PHONE_BOB).withEmail("bob@example.com")
                .withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND).build();
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(editedPerson).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON, descriptor); // Boundary value

        String expectedMessage = String.format(
                EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.formatAthlete(editedPerson));

        // @@author qiyuannn
        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        // @@author
        expectedModel.setPerson(model.getFilteredPersonList().get(0), editedPerson);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    // EP: edit subset of fields of person at valid maximum boundary index in unfiltered list
    @Test
    public void execute_someFieldsSpecifiedUnfilteredList_success() {
        Index indexLastPerson = Index.fromOneBased(model.getFilteredPersonList().size()); // Boundary value
        Person lastPerson = model.getFilteredPersonList().get(indexLastPerson.getZeroBased());

        PersonBuilder personInList = new PersonBuilder(lastPerson);
        Person editedPerson = personInList.withName(VALID_NAME_BOB).withPhone(VALID_PHONE_BOB).build();

        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB)
                .withPhone(VALID_PHONE_BOB).build();
        EditCommand editCommand = new EditCommand(indexLastPerson, descriptor);

        String expectedMessage = String.format(
                EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.formatAthlete(editedPerson));

        // @@author qiyuannn
        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        // @@author
        expectedModel.setPerson(lastPerson, editedPerson);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    // EP: edit command with empty descriptor (no fields specified)
    @Test
    public void execute_noFieldSpecifiedUnfilteredList_failure() {
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON, new EditPersonDescriptor()); // Boundary value
        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_NO_CHANGE);
    }

    // EP: edit person with identical fields (no effective change)
    @Test
    public void execute_identicalFieldsUnfilteredList_failure() {
        Person firstPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(firstPerson).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON, descriptor);
        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_NO_CHANGE);
    }

    // EP: edit person with only same name re-specified (no change)
    @Test
    public void execute_sameNameUnfilteredList_failure() {
        Person firstPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder()
                .withName(firstPerson.getName().fullName).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON, descriptor);
        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_NO_CHANGE);
    }

    // EP: edit person in filtered list at valid index
    @Test
    public void execute_filteredList_success() {
        showPersonAtIndex(model, INDEX_SECOND_PERSON);

        Person personInFilteredList = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person editedPerson = new PersonBuilder(personInFilteredList).withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build()); // Boundary value

        String expectedMessage = String.format(
                EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, Messages.formatAthlete(editedPerson));

        // @@author qiyuannn
        Model expectedModel = new ModelManager(new AthleteRoster(model.getRoster()), new UserPrefs());
        // @@author
        expectedModel.setPerson(model.getFilteredPersonList().get(0), editedPerson);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    // EP: edit person to duplicate another existing person in unfiltered list
    @Test
    public void execute_duplicatePersonUnfilteredList_failure() {
        Person firstPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(firstPerson).build();
        EditCommand editCommand = new EditCommand(INDEX_SECOND_PERSON, descriptor);

        assertCommandFailure(editCommand, model,
                String.format(EditCommand.MESSAGE_DUPLICATE_PERSON, firstPerson.getName()));
    }

    // EP: edit person in filtered list to duplicate another person in athlete roster
    @Test
    public void execute_duplicatePersonFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        // edit person in filtered list into a duplicate in athlete roster
        // @@author qiyuannn
        Person personInList = model.getRoster().getPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        // @@author
        EditCommand editCommand = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder(personInList).build());

        assertCommandFailure(editCommand, model,
                String.format(EditCommand.MESSAGE_DUPLICATE_PERSON, personInList.getName()));
    }

    // EP: invalid index exceeding unfiltered list size
    @Test
    public void execute_invalidPersonIndexUnfilteredList_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1); // Boundary value
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(outOfBoundIndex, descriptor);

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    /**
     * Edit filtered list where index is larger than size of filtered list,
     * but smaller than size of athlete roster
     */
    // EP: invalid index exceeding filtered list size but within athlete roster size
    @Test
    public void execute_invalidPersonIndexFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        Index outOfBoundIndex = INDEX_SECOND_PERSON; // Boundary value
        // ensures that outOfBoundIndex is still in bounds of athlete roster list
        // @@author qiyuannn
        assertTrue(outOfBoundIndex.getZeroBased() < model.getRoster().getPersonList().size());
        // @@author

        EditCommand editCommand = new EditCommand(outOfBoundIndex,
                new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build());

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_tagsOnly_replacesAndClearsWhilePreservingOtherDetails() throws Exception {
        // EP: replace tags with multiple tags while preserving other details
        Person original = model.getFilteredPersonList().get(0);
        new EditCommand(INDEX_FIRST_PERSON, new EditPersonDescriptorBuilder()
                .withTags("sprinter", "relay").build()).execute(model);
        Person edited = model.getFilteredPersonList().get(0);
        assertEquals(new PersonBuilder(original).withTags("sprinter", "relay").build(), edited);

        // EP: clear tags with empty tag set while preserving other details
        new EditCommand(INDEX_FIRST_PERSON, new EditPersonDescriptorBuilder().withTags().build()).execute(model);
        Person cleared = model.getFilteredPersonList().get(0);
        assertEquals(new PersonBuilder(original).withTags().build(), cleared); // Boundary value
    }

    // EP: contact change creating duplicate with case/whitespace normalized match
    @Test
    public void execute_contactChangeCreatesNormalizedDuplicate_failure() {
        Person first = new PersonBuilder().withAgeCategory("Under 14").withTags("sprinter").build();
        Person second = new PersonBuilder(first).withName("AMY   BEE")
                .withPhone("81112222").withEmail("other@example.com")
                .withAgeCategory("Under 16").withRemark("Keep me").build();
        model.addPerson(first);
        model.addPerson(second);
        model.updateFilteredPersonList(new AgeCategoryPredicate(new AgeCategory("Under 16")));
        EditCommand command = new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withEmail(first.getEmail().value.toUpperCase()).build());
        assertCommandFailure(command, model,
                String.format(EditCommand.MESSAGE_DUPLICATE_PERSON, second.getName()));
        assertEquals(second.getRemark(), model.getFilteredPersonList().get(0).getRemark());
    }

    @Test
    public void equals() {
        final EditCommand standardCommand = new EditCommand(INDEX_FIRST_PERSON, DESC_AMY);

        // EP: symmetric equality (different instance with same index and descriptor)
        EditPersonDescriptor copyDescriptor = new EditPersonDescriptor(DESC_AMY);
        EditCommand commandWithSameValues = new EditCommand(INDEX_FIRST_PERSON, copyDescriptor);
        assertTrue(standardCommand.equals(commandWithSameValues));

        // EP: reflexive equality (same object instance)
        assertTrue(standardCommand.equals(standardCommand));

        // EP: null comparison
        assertFalse(standardCommand.equals(null)); // Boundary value

        // EP: different object type
        assertFalse(standardCommand.equals(new ClearCommand()));

        // EP: different index
        assertFalse(standardCommand.equals(new EditCommand(INDEX_SECOND_PERSON, DESC_AMY)));

        // EP: different descriptor
        assertFalse(standardCommand.equals(new EditCommand(INDEX_FIRST_PERSON, DESC_BOB)));
    }

    // EP: string representation of edit command
    @Test
    public void toStringMethod() {
        Index index = Index.fromOneBased(1); // Boundary value
        EditPersonDescriptor editPersonDescriptor = new EditPersonDescriptor();
        EditCommand editCommand = new EditCommand(index, editPersonDescriptor);
        String expected = EditCommand.class.getCanonicalName() + "{index=" + index + ", editPersonDescriptor="
                + editPersonDescriptor + "}";
        assertEquals(expected, editCommand.toString());
    }

    // EP: message usage contains all enumerated valid age categories
    @Test
    public void messageUsage_listsAllValidAgeCategories() {
        List<String> validCategories = List.of("Under 14", "Under 16", "Under 18", "Under 20", "Open");
        assertTrue(EditCommand.MESSAGE_USAGE.contains(
                "[a/AGE_CATEGORY (Under 14, Under 16, Under 18, Under 20, or Open)]"));
        for (String category : validCategories) {
            assertTrue(EditCommand.MESSAGE_USAGE.contains(category));
        }
    }
}
