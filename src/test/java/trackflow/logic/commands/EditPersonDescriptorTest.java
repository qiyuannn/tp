// @@author qiyuannn
package trackflow.logic.commands;
// @@author

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
// @@author qiyuannn
import static trackflow.logic.commands.CommandTestUtil.DESC_AMY;
import static trackflow.logic.commands.CommandTestUtil.DESC_BOB;
import static trackflow.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static trackflow.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static trackflow.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
// @@author

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

// @@author qiyuannn
import trackflow.logic.commands.EditCommand.EditPersonDescriptor;
import trackflow.model.person.Address;
import trackflow.model.person.AgeCategory;
import trackflow.model.person.Email;
import trackflow.model.person.Name;
import trackflow.model.person.Phone;
import trackflow.model.tag.Tag;
import trackflow.testutil.EditPersonDescriptorBuilder;
// @@author

public class EditPersonDescriptorTest {

    @Test
    public void isAnyFieldEdited_individualFields_returnsTrue() {
        // EP: only name field edited
        EditPersonDescriptor nameOnly = new EditPersonDescriptor();
        nameOnly.setName(new Name(VALID_NAME_BOB));
        assertTrue(nameOnly.isAnyFieldEdited());

        // EP: only phone field edited
        EditPersonDescriptor phoneOnly = new EditPersonDescriptor();
        phoneOnly.setPhone(new Phone(VALID_PHONE_BOB));
        assertTrue(phoneOnly.isAnyFieldEdited());

        // EP: only email field edited
        EditPersonDescriptor emailOnly = new EditPersonDescriptor();
        emailOnly.setEmail(new Email(VALID_EMAIL_BOB));
        assertTrue(emailOnly.isAnyFieldEdited());
    }

    @Test
    public void equals() {
        // EP: symmetric equality (different instance with same field values)
        EditPersonDescriptor descriptorWithSameValues = new EditPersonDescriptor(DESC_AMY);
        assertTrue(DESC_AMY.equals(descriptorWithSameValues));

        // EP: reflexive equality (same object instance)
        assertTrue(DESC_AMY.equals(DESC_AMY));

        // EP: null comparison
        assertFalse(DESC_AMY.equals(null)); // Boundary value

        // EP: different object type
        assertFalse(DESC_AMY.equals(5));

        // EP: different field values
        assertFalse(DESC_AMY.equals(DESC_BOB));

        // EP: different name field
        EditPersonDescriptor editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).withName(VALID_NAME_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // EP: different phone field
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).withPhone(VALID_PHONE_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // EP: different email field
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // EP: different age category field
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).withAgeCategory("Under 16").build();
        assertFalse(DESC_AMY.equals(editedAmy));
    }

    // EP: only age category field edited and copy constructor creates independent copy
    @Test
    public void ageCategoryOnly_countsAsEditAndIsCopied() {
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        assertFalse(descriptor.isAnyFieldEdited()); // Boundary value
        descriptor.setAgeCategory(new AgeCategory("Under 14"));
        assertTrue(descriptor.isAnyFieldEdited());
        EditPersonDescriptor copy = new EditPersonDescriptor(descriptor);
        descriptor.setAgeCategory(new AgeCategory("Open"));
        assertEquals(new AgeCategory("Under 14"), copy.getAgeCategory().orElseThrow());
    }

    // EP: only address field edited and copy constructor creates independent copy
    @Test
    public void addressOnly_countsAsEditAndIsCopied() {
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.setAddress(new Address("123 Main Street"));
        assertTrue(descriptor.isAnyFieldEdited());
        EditPersonDescriptor copy = new EditPersonDescriptor(descriptor);
        descriptor.setAddress(new Address("456 Main Street"));
        assertEquals(new Address("123 Main Street"), copy.getAddress().orElseThrow());
        assertFalse(descriptor.equals(copy));
    }

    @Test
    public void tags_defensiveCopyAndEmptyReplacement() {
        // EP: non-empty tag set defensive copying and immutability
        Set<Tag> tags = new HashSet<>(Set.of(new Tag("sprinter"))); // Boundary value
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.setTags(tags);
        EditPersonDescriptor copy = new EditPersonDescriptor(descriptor);
        tags.clear();
        assertEquals(Set.of(new Tag("sprinter")), descriptor.getTags().orElseThrow());
        assertThrows(UnsupportedOperationException.class, descriptor.getTags().orElseThrow()::clear);

        // EP: empty tag set replacement
        descriptor.setTags(Set.of()); // Boundary value
        assertTrue(descriptor.isAnyFieldEdited());
        assertEquals(Set.of(), descriptor.getTags().orElseThrow()); // Boundary value
        assertEquals(Set.of(new Tag("sprinter")), copy.getTags().orElseThrow());
        assertFalse(descriptor.equals(copy));
    }

    // EP: string representation of edit person descriptor
    @Test
    public void toStringMethod() {
        EditPersonDescriptor editPersonDescriptor = new EditPersonDescriptor();
        String expected = EditPersonDescriptor.class.getCanonicalName() + "{name="
                + editPersonDescriptor.getName().orElse(null) + ", phone="
                + editPersonDescriptor.getPhone().orElse(null) + ", email="
                + editPersonDescriptor.getEmail().orElse(null) + ", ageCategory="
                + editPersonDescriptor.getAgeCategory().orElse(null) + ", address="
                + editPersonDescriptor.getAddress().orElse(null) + ", tags="
                + editPersonDescriptor.getTags().orElse(null) + "}";
        assertEquals(expected, editPersonDescriptor.toString());
    }
}
