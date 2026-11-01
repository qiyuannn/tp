// @@author Austin-Chia
package trackflow.model.person;
// @@author

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
// @@author Austin-Chia
import static trackflow.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static trackflow.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalPersons.ALICE;
import static trackflow.testutil.TypicalPersons.BOB;
// @@author

import java.util.List;

import org.junit.jupiter.api.Test;

// @@author Austin-Chia
import trackflow.model.person.exceptions.DuplicatePersonException;
import trackflow.model.person.exceptions.PersonNotFoundException;
import trackflow.testutil.PersonBuilder;
// @@author

public class UniquePersonListTest {

    private final UniquePersonList uniquePersonList = new UniquePersonList();

    // EP: null person argument
    @Test
    public void contains_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.contains(null)); // Boundary value
    }

    // EP: person not present in list (tested against empty list)
    @Test
    public void contains_personNotInList_returnsFalse() {
        assertFalse(uniquePersonList.contains(ALICE)); // Boundary value
    }

    // EP: person present in list (exact same instance)
    @Test
    public void contains_personInList_returnsTrue() {
        uniquePersonList.add(ALICE);
        assertTrue(uniquePersonList.contains(ALICE));
    }

    // EP: person with same identity fields (isSamePerson) present in list
    @Test
    public void contains_personWithSameIdentityFieldsInList_returnsTrue() {
        uniquePersonList.add(ALICE);
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        assertTrue(uniquePersonList.contains(editedAlice));
    }

    // EP: null person argument
    @Test
    public void add_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.add(null)); // Boundary value
    }

    // EP: duplicate person already present in list
    @Test
    public void add_duplicatePerson_throwsDuplicatePersonException() {
        uniquePersonList.add(ALICE);
        assertThrows(DuplicatePersonException.class, () -> uniquePersonList.add(ALICE));
    }

    // EP: null target person argument
    @Test
    public void setPerson_nullTargetPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.setPerson(null, ALICE)); // Boundary value
    }

    // EP: null edited person argument
    @Test
    public void setPerson_nullEditedPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.setPerson(ALICE, null)); // Boundary value
    }

    // EP: target person not present in list
    @Test
    public void setPerson_targetPersonNotInList_throwsPersonNotFoundException() {
        assertThrows(PersonNotFoundException.class, () -> uniquePersonList.setPerson(ALICE, ALICE)); // Boundary value
    }

    // EP: edited person is the exact same instance as target person
    @Test
    public void setPerson_editedPersonIsSamePerson_success() {
        uniquePersonList.add(ALICE);
        uniquePersonList.setPerson(ALICE, ALICE);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        expectedUniquePersonList.add(ALICE);
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    // EP: edited person has same identity as target person (updating non-identity fields)
    @Test
    public void setPerson_editedPersonHasSameIdentity_success() {
        uniquePersonList.add(ALICE);
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        uniquePersonList.setPerson(ALICE, editedAlice);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        expectedUniquePersonList.add(editedAlice);
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    // EP: edited person has different identity from target person and does not conflict with existing persons
    @Test
    public void setPerson_editedPersonHasDifferentIdentity_success() {
        uniquePersonList.add(ALICE);
        uniquePersonList.setPerson(ALICE, BOB);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        expectedUniquePersonList.add(BOB);
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    // EP: edited person has same identity as another existing person in list
    @Test
    public void setPerson_editedPersonHasNonUniqueIdentity_throwsDuplicatePersonException() {
        uniquePersonList.add(ALICE);
        uniquePersonList.add(BOB);
        assertThrows(DuplicatePersonException.class, () -> uniquePersonList.setPerson(ALICE, BOB));
    }

    // EP: null person argument
    @Test
    public void remove_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.remove(null)); // Boundary value
    }

    // EP: person to remove does not exist in list
    @Test
    public void remove_personDoesNotExist_throwsPersonNotFoundException() {
        assertThrows(PersonNotFoundException.class, () -> uniquePersonList.remove(ALICE)); // Boundary value
    }

    // EP: existing person removed resulting in empty list
    @Test
    public void remove_existingPerson_removesPerson() {
        uniquePersonList.add(ALICE);
        uniquePersonList.remove(ALICE);
        UniquePersonList expectedUniquePersonList = new UniquePersonList(); // Boundary value
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    // EP: null UniquePersonList replacement argument
    @Test
    public void setPersons_nullUniquePersonList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.setPersons((UniquePersonList) null));
    }

    // EP: valid UniquePersonList replacement containing unique persons
    @Test
    public void setPersons_uniquePersonList_replacesOwnListWithProvidedUniquePersonList() {
        uniquePersonList.add(ALICE);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        expectedUniquePersonList.add(BOB);
        uniquePersonList.setPersons(expectedUniquePersonList);
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    // EP: null List<Person> replacement argument
    @Test
    public void setPersons_nullList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.setPersons((List<Person>) null));
    }

    // EP: valid List<Person> replacement containing unique persons
    @Test
    public void setPersons_list_replacesOwnListWithProvidedList() {
        uniquePersonList.add(ALICE);
        List<Person> personList = List.of(BOB);
        uniquePersonList.setPersons(personList);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        expectedUniquePersonList.add(BOB);
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    // EP: list containing identical duplicate person instances
    @Test
    public void setPersons_listWithDuplicatePersons_throwsDuplicatePersonException() {
        List<Person> listWithDuplicatePersons = List.of(ALICE, ALICE);
        assertThrows(DuplicatePersonException.class, () -> uniquePersonList.setPersons(listWithDuplicatePersons));
    }

    // EP: list containing persons with normalized same name and shared contact (duplicate identity)
    @Test
    public void setPersons_sameNameDifferentCaseAndSpacingSharedContact_throwsDuplicatePersonException() {
        // A name that differs only in case and spacing, sharing a contact detail, is still a duplicate.
        Person variant = new PersonBuilder(ALICE)
                .withName(ALICE.getName().fullName.toUpperCase().replace(" ", "  ")).build();
        assertThrows(DuplicatePersonException.class, () -> uniquePersonList.setPersons(List.of(ALICE, variant)));
    }

    // EP: list containing persons with same name but completely different contact details (unique identities)
    @Test
    public void setPersons_sameNameDifferentContacts_allowed() {
        // Two athletes with the same name but different phone and email are not duplicates.
        Person sameName = new PersonBuilder(ALICE).withPhone("80008000").withEmail("different@example.com").build();
        uniquePersonList.setPersons(List.of(ALICE, sameName));
        assertEquals(List.of(ALICE, sameName), uniquePersonList.asUnmodifiableObservableList());
    }

    // EP: modification of unmodifiable observable list (throws UnsupportedOperationException)
    @Test
    public void asUnmodifiableObservableList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, ()
            -> uniquePersonList.asUnmodifiableObservableList().remove(0)); // Boundary value
    }

    // EP: valid string representation of UniquePersonList
    @Test
    public void toStringMethod() {
        assertEquals(uniquePersonList.asUnmodifiableObservableList().toString(), uniquePersonList.toString());
    }
}
