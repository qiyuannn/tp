package trackflow.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.logic.commands.exceptions.CommandException;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.ReadOnlyRoster;
import trackflow.model.person.Person;
import trackflow.storage.JsonRosterStorage;
import trackflow.storage.JsonUserPrefsStorage;
import trackflow.storage.StorageManager;
import trackflow.testutil.PersonBuilder;

/**
 * Checks indexed mutations when a later sort leaves ties in the previous displayed order.
 * Real storage reads distinguish changing the displayed athlete from merely reporting the right name.
 */
public class DisplayedIndexPersistenceTest {

    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_deleteAfterTiedSort_deletesDisplayedAthlete() throws Exception {
        // EP: delete command using displayed index 1 after tied sort
        assertTargetsDisplayedBob("delete 1", person -> null); // Boundary value
    }

    @Test
    public void execute_editAfterTiedSort_editsDisplayedAthlete() throws Exception {
        // EP: edit command using displayed index 1 after tied sort
        assertTargetsDisplayedBob("edit 1 addr/Changed", person ->
                new PersonBuilder(person).withAddress("Changed").build()); // Boundary value
    }

    @Test
    public void execute_remarkAfterTiedSort_updatesDisplayedAthlete() throws Exception {
        // EP: remark command using displayed index 1 after tied sort
        assertTargetsDisplayedBob("remark 1 r/Changed", person ->
                new PersonBuilder(person).withRemark("Changed").build()); // Boundary value
    }

    @Test
    public void execute_tagAfterTiedSort_tagsDisplayedAthlete() throws Exception {
        // EP: tag command using displayed index 1 after tied sort
        assertTargetsDisplayedBob("tag 1 t/Changed", person ->
                new PersonBuilder(person).withTags("Original", "Changed").build()); // Boundary value
    }

    @Test
    public void execute_untagAfterTiedSort_untagsDisplayedAthlete() throws Exception {
        // EP: untag command using displayed index 1 after tied sort
        assertTargetsDisplayedBob("untag 1 t/Original", person ->
                new PersonBuilder(person).withTags().build()); // Boundary value
    }

    private void assertTargetsDisplayedBob(String command, UnaryOperator<Person> change) throws Exception {
        // EP: verifying mutation persistence across unfiltered (false) and filtered (true) views
        for (boolean filtered : List.of(false, true)) {
            Fixture fixture = createFixture(filtered);
            Person alice = fixture.model.getRoster().getPersonList().get(0);
            Person bob = fixture.model.getRoster().getPersonList().get(1);
            var comparator = fixture.model.getPersonListComparator();
            List<Person> expectedStored = new ArrayList<>(fixture.model.getRoster().getPersonList());
            Person editedBob = change.apply(bob);
            if (editedBob == null) {
                expectedStored.remove(1);
            } else {
                expectedStored.set(1, editedBob);
            }

            String feedback = fixture.logic.execute(command).getFeedbackToUser();

            assertTrue(feedback.contains("Bob"), feedback);
            assertEquals(expectedStored, fixture.model.getRoster().getPersonList(), command);
            assertEquals(expectedStored, fixture.storage.readRoster().orElseThrow().getPersonList(), command);
            assertEquals(alice, fixture.model.getRoster().getPersonList().get(0));
            // Staging's tie breaker must not replace the live session's selected comparator.
            assertSame(comparator, fixture.model.getPersonListComparator());
        }
    }

    @Test
    public void execute_failedSaveAfterTiedSort_preservesRosterDisplayAndFile() throws Exception {
        // EP: failing storage save rolls back model and preserves file across unfiltered and filtered views
        for (boolean filtered : List.of(false, true)) {
            Fixture fixture = createFixture(filtered);
            List<Person> stored = List.copyOf(fixture.model.getRoster().getPersonList());
            List<Person> displayed = List.copyOf(fixture.model.getFilteredPersonList());
            var predicate = fixture.model.getPersonListPredicate();
            var comparator = fixture.model.getPersonListComparator();
            Path savedPath = fixture.storage.getRosterFilePath();
            String savedFile = Files.readString(savedPath);
            JsonRosterStorage failingStorage = new JsonRosterStorage(savedPath) {
                @Override
                public void saveRoster(ReadOnlyRoster roster) throws IOException {
                    throw new IOException("Simulated save failure");
                }
            };
            Logic failingLogic = new LogicManager(fixture.model, new StorageManager(failingStorage,
                    new JsonUserPrefsStorage(savedPath.resolveSibling("failing-prefs.json"))));

            for (String command : List.of("delete 1", "edit 1 addr/Changed", "remark 1 r/Changed",
                    "tag 1 t/Changed", "untag 1 t/Original")) {
                assertThrows(CommandException.class, () -> failingLogic.execute(command));
                assertEquals(stored, fixture.model.getRoster().getPersonList());
                assertEquals(displayed, fixture.model.getFilteredPersonList());
                assertSame(predicate, fixture.model.getPersonListPredicate());
                assertSame(comparator, fixture.model.getPersonListComparator());
                assertEquals(savedFile, Files.readString(savedPath));
            }
        }
    }

    private Fixture createFixture(boolean filtered) throws Exception {
        Path folder = Files.createTempDirectory(temporaryFolder, "tied-sort-");
        JsonRosterStorage storage = new JsonRosterStorage(folder.resolve("roster.json"));
        Model model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Alice").withPhone("111")
                .withEmail("alice@example.com").withTags("Original").build());
        model.addPerson(new PersonBuilder().withName("Bob").withPhone("222")
                .withEmail("bob@example.com").withTags("Original").build());
        if (filtered) {
            model.addPerson(new PersonBuilder().withName("Hidden").withAgeCategory("Under 14")
                    .withPhone("333").withEmail("hidden@example.com").build());
        }
        storage.saveRoster(model.getRoster());
        Logic logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(folder.resolve("prefs.json"))));
        if (filtered) {
            logic.execute("filter a/Open");
        }
        logic.execute("sort name desc");
        logic.execute("sort age");
        assertEquals(List.of("Bob", "Alice"), model.getFilteredPersonList().stream()
                .map(person -> person.getName().fullName).toList());
        return new Fixture(model, logic, storage);
    }

    private record Fixture(Model model, Logic logic, JsonRosterStorage storage) {}
}
