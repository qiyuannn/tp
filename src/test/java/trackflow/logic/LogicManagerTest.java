// @@author qiyuannn
package trackflow.logic;
// @@author

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
// @@author qiyuannn
import static trackflow.logic.Messages.MESSAGE_EMPTY_COMMAND;
import static trackflow.logic.Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX;
import static trackflow.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static trackflow.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static trackflow.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static trackflow.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static trackflow.testutil.Assert.assertThrows;
// @@author

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.commons.core.GuiSettings;
// @@author qiyuannn
import trackflow.logic.commands.AddCommand;
import trackflow.logic.commands.ClearCommand;
import trackflow.logic.commands.CommandResult;
import trackflow.logic.commands.ListAthleteCommand;
import trackflow.logic.commands.exceptions.CommandException;
import trackflow.logic.parser.exceptions.ParseException;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.ReadOnlyRoster;
import trackflow.model.UserPrefs;
import trackflow.model.person.AgeCategory;
import trackflow.model.person.Email;
import trackflow.model.person.Person;
import trackflow.storage.JsonRosterStorage;
import trackflow.storage.JsonUserPrefsStorage;
import trackflow.storage.StorageManager;
// @@author

/**
 * Exercises command execution across model and storage boundaries, including persistence and failed-save isolation.
 * Temporary files verify reload behavior; failing storage doubles verify that unsuccessful saves are not published.
 */
public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        // @@author qiyuannn
        JsonRosterStorage rosterStorage =
                new JsonRosterStorage(temporaryFolder.resolve("roster.json"));
        // @@author
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        // @@author qiyuannn
        StorageManager storage = new StorageManager(rosterStorage, userPrefsStorage);
        // @@author
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        // EP: unrecognized command word
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, String.format(MESSAGE_UNKNOWN_COMMAND, invalidCommand));
    }

    @Test
    public void execute_emptyCommand_throwsParseException() {
        // EP: empty command string
        assertParseException("", MESSAGE_EMPTY_COMMAND); // Boundary value
    }

    @Test
    public void execute_pathologicalEmailInput_throwsParseExceptionQuickly() {
        // EP: pathological email username length
        Duration timeout = Duration.ofMillis(200);
        String longEmailCommand = "add n/Avery Tan a/Under 14 p/91234567 e/" + "a".repeat(5000)
                + " addr/123 Main Street"; // Boundary value
        assertTimeoutPreemptively(timeout, () ->
                assertParseException(longEmailCommand, Email.MESSAGE_CONSTRAINTS));

        // EP: pathological email domain length
        String longDomainCommand = "add n/Avery Tan a/Under 14 p/91234567 e/a@" + "b".repeat(5000)
                + " addr/123 Main Street"; // Boundary value
        assertTimeoutPreemptively(timeout, () ->
                assertParseException(longDomainCommand, Email.MESSAGE_CONSTRAINTS));

        // EP: pathological email in edit command
        String editLongEmailCommand = "edit 1 e/" + "a".repeat(5000);
        assertTimeoutPreemptively(timeout, () ->
                assertParseException(editLongEmailCommand, Email.MESSAGE_CONSTRAINTS));

        String editLongDomainCommand = "edit 1 e/a@" + "b".repeat(5000);
        assertTimeoutPreemptively(timeout, () ->
                assertParseException(editLongDomainCommand, Email.MESSAGE_CONSTRAINTS));

        // EP: pathological repeated domain labels risking stack overflow
        String editStackOverflowCommand = "edit 1 e/a@" + "a.".repeat(2500);
        assertTimeoutPreemptively(timeout, () ->
                assertParseException(editStackOverflowCommand, Email.MESSAGE_CONSTRAINTS));
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        // EP: out-of-bounds displayed index on empty list
        String deleteCommand = "delete 9"; // Boundary value
        assertCommandException(deleteCommand, MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        // EP: valid command execution on empty roster
        String listCommand = ListAthleteCommand.COMMAND_WORD; // Boundary value
        assertCommandSuccess(listCommand, ListAthleteCommand.MESSAGE_EMPTY_ROSTER, model);
    }

    @Test
    public void execute_clear_requiresConfirmationBeforeSaving() throws Exception {
        // EP: clear command triggers confirmation warning
        logic.execute("add n/Avery Tan a/Under 14 p/91234567 e/avery@example.com addr/Home");
        Path rosterPath = temporaryFolder.resolve("roster.json");
        String savedRoster = Files.readString(rosterPath);

        CommandResult warning = logic.execute("clear");
        assertEquals(ClearCommand.MESSAGE_CONFIRMATION, warning.getFeedbackToUser());
        assertEquals(false, warning.isDataSaved());
        assertEquals(1, model.getRoster().getPersonList().size());
        assertEquals(savedRoster, Files.readString(rosterPath));

        // EP: clear confirmation rejected with 'N'
        assertEquals(true, warning.isWarning());
        logic.execute("N");
        assertEquals(savedRoster, Files.readString(rosterPath));
        assertThrows(ParseException.class, () -> logic.execute("Y"));
        // EP: intervening command cancels pending clear confirmation
        logic.execute("clear");
        logic.execute("list");
        assertThrows(ParseException.class, () -> logic.execute("y"));
        // EP: clear confirmation accepted with 'Y'
        logic.execute("clear");
        CommandResult confirmed = logic.execute(" Y ");
        assertEquals(true, confirmed.isDataSaved());
        assertEquals(0, model.getRoster().getPersonList().size()); // Boundary value
        assertEquals(0, // Boundary value
                new JsonRosterStorage(rosterPath).readRoster().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_rosterChangingCommand_reportsDataSaved() throws Exception {
        // EP: roster-mutating command sets isDataSaved flag to true
        CommandResult result = logic.execute(
                "add n/Avery Tan a/Under 14 p/91234567 e/avery@example.com addr/123 Main Street");

        assertEquals(true, result.isDataSaved());
    }

    @Test
    public void execute_clearConfirmationWithUnicodeSpaces_usesCommandWhitespace() throws Exception {
        // EP: clear confirmation tokens surrounded by unicode whitespace separators
        for (String space : List.of("\u00a0", "\u2007", "\u202f", "\u2003")) {
            logic.execute("add n/Avery Tan a/Open p/91234567 e/avery@example.com addr/Home");
            Path rosterPath = temporaryFolder.resolve("roster.json");
            String savedRoster = Files.readString(rosterPath);

            logic.execute("clear");
            CommandResult cancelled = logic.execute(space + "N" + space);
            assertEquals("Clear cancelled. No athletes were deleted.", cancelled.getFeedbackToUser());
            assertEquals(savedRoster, Files.readString(rosterPath));
            assertThrows(ParseException.class, () -> logic.execute("y"));

            logic.execute("clear");
            assertEquals(true, logic.execute(space + "Y" + space).isDataSaved());
            assertEquals(0, model.getRoster().getPersonList().size()); // Boundary value
            assertEquals(0, new JsonRosterStorage(rosterPath).readRoster().orElseThrow().getPersonList().size());
        }
    }

    @Test
    public void execute_nullWhileClearPending_rejectsWithoutChangingData() throws Exception {
        // EP: null input during pending clear confirmation
        logic.execute("add n/Avery Tan a/Open p/91234567 e/avery@example.com addr/Home");
        Path rosterPath = temporaryFolder.resolve("roster.json");
        String savedRoster = Files.readString(rosterPath);
        logic.execute("clear");

        assertThrows(NullPointerException.class, () -> logic.execute(null)); // Boundary value
        assertEquals(savedRoster, Files.readString(rosterPath));
        assertEquals(1, model.getRoster().getPersonList().size());
        assertThrows(ParseException.class, () -> logic.execute("y"));
    }

    @Test
    public void execute_viewCommandsWithUnavailableStorage_doNotSave() throws Exception {
        // EP: non-mutating query/view commands do not trigger storage write
        JsonRosterStorage unavailableStorage = new JsonRosterStorage(temporaryFolder.resolve("unavailable.json")) {
            @Override
            public void saveRoster(ReadOnlyRoster roster) throws IOException {
                throw DUMMY_IO_EXCEPTION;
            }
        };
        Logic readOnlyLogic = new LogicManager(model, new StorageManager(unavailableStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("readOnlyPrefs.json"))));

        for (String command : List.of("list", "find amy", "filter a/Open", "sort name", "help", "exit")) {
            assertEquals(false, readOnlyLogic.execute(command).isDataSaved());
        }
    }

    @Test
    public void execute_mixedCaseListWithAthlete_returnsAthleteCount() throws Exception {
        // EP: mixed-case command word execution
        logic.execute("add n/Avery Tan a/Under 14 p/91234567 e/avery.tan@example.com addr/123 Main Street");
        // @@author qiyuannn
        Model expectedModel = new ModelManager(model.getRoster(), new UserPrefs());
        // @@author
        assertCommandSuccess("LiSt", ListAthleteCommand.MESSAGE_SUCCESS_SINGLE_ATHLETE, expectedModel);
    }

    // @@author qiyuannn
    @Test
    public void execute_editAthlete_persistsCategoryAndPreservesOtherDetails() throws Exception {
        // EP: edit and remark commands update target fields while preserving unmodified fields
        String command = "add n/Avery Tan a/Under 14 p/91234567 e/avery.tan@example.com addr/123 Main Street";
        assertEquals("New athlete added: Avery Tan; Age category: Under 14; Phone: 91234567; "
                + "Email: avery.tan@example.com", logic.execute(command).getFeedbackToUser());
        logic.execute("edit 1 a/Under 16 addr/Training centre p/92345678");
        logic.execute("remark 1 r/Sprints");
        Person athlete = model.getFilteredPersonList().get(0);
        assertEquals(new AgeCategory("Under 16"), athlete.getAgeCategory());
        assertEquals("Sprints", athlete.getRemark().value);
        assertEquals("Training centre", athlete.getAddress().value);
        assertEquals(0, athlete.getTags().size()); // Boundary value
        assertEquals("92345678", athlete.getPhone().value);
        JsonRosterStorage reloadedStorage =
                new JsonRosterStorage(temporaryFolder.resolve("roster.json"));
        Model reloaded = new ModelManager(reloadedStorage.readRoster().orElseThrow(), new UserPrefs());
        assertEquals(athlete, reloaded.getFilteredPersonList().get(0));
    }

    @Test
    // @@author
    public void execute_sameNameMatchingContact_rejectsDuplicates() throws Exception {
        // EP: duplicate add rejected on matching name and overlapping contacts
        logic.execute("add n/Avery Tan a/Under 14 p/91234567 e/avery.tan@example.com addr/123 Main Street");
        String[] variants = {"a/Under 16 p/91234567 e/avery.tan@example.com",
            "a/Under 14 p/92345678 e/avery.tan@example.com",
            "a/Under 14 p/91234567 e/other@example.com"};
        for (String variant : variants) {
            assertThrows(CommandException.class, () ->
                logic.execute("add n/Avery Tan " + variant + " addr/123 Main Street"));
        }
        assertEquals(1, model.getFilteredPersonList().size());
        assertThrows(CommandException.class,
                String.format(AddCommand.MESSAGE_DUPLICATE_PERSON, "Avery Tan"), () ->
            logic.execute("add n/AVERY   TAN a/under 14 p/91234567 e/AVERY.TAN@example.com addr/123 Main Street"));
        assertEquals(1, model.getFilteredPersonList().size());
        // @@author qiyuannn
    }

    @Test
    public void execute_addAthleteWithRequiredAddress_survivesReload() throws Exception {
        // EP: valid add command with all required fields persists across reloads
        logic.execute("add n/Avery Tan a/under   16 p/91234567 e/Avery.Tan@example.com addr/123 Main Street");
        JsonRosterStorage storage = new JsonRosterStorage(temporaryFolder.resolve("roster.json"));
        Model reloaded = new ModelManager(storage.readRoster().orElseThrow(), new UserPrefs());
        Person athlete = reloaded.getFilteredPersonList().get(0);
        assertEquals(1, reloaded.getFilteredPersonList().size());
        assertEquals("Avery Tan", athlete.getName().fullName);
        assertEquals("Under 16", athlete.getAgeCategory().value);
        assertEquals("91234567", athlete.getPhone().value);
        assertEquals("Avery.Tan@example.com", athlete.getEmail().value);
        assertEquals("123 Main Street", athlete.getAddress().value);
        assertEquals("", athlete.getRemark().value); // Boundary value
        assertEquals(0, athlete.getTags().size()); // Boundary value
    }

    @Test
    public void execute_invalidAdd_preservesRosterAndSavedFile() throws Exception {
        // EP: invalid add command syntax or missing mandatory fields leaves roster and file unmodified
        String validCommand = "add n/Avery Tan a/Under 14 p/91234567 e/avery.tan@example.com addr/123 Main Street";
        logic.execute(validCommand);
        Path savedPath = temporaryFolder.resolve("roster.json");
        String savedData = Files.readString(savedPath);
        Model expectedModel = new ModelManager(model.getRoster(), new UserPrefs());
        // @@author
        String[] invalidCommands = {validCommand.replace(" a/Under 14", ""),
            validCommand.replace(" addr/123 Main Street", ""),
            validCommand.replace("addr/123 Main Street", "addr/"),
            validCommand.replace("Under 14", "Under 15"),
            validCommand.replace("91234567", "911a"),
            validCommand.replace("avery.tan@example.com", "invalid"),
            validCommand + " n/Other Athlete", validCommand + " t/invalid-tag", validCommand + " r/note r/other",
            validCommand + " addr/"};
        for (String invalidCommand : invalidCommands) {
            assertThrows(ParseException.class, () -> logic.execute(invalidCommand));
            assertEquals(expectedModel, model);
            assertEquals(savedData, Files.readString(savedPath));
        }
    }

    @Test
    public void execute_sort_changesDisplayButPreservesSavedRosterOrder() throws Exception {
        // EP: sorting changes displayed order while preserving physical storage order
        logic.execute("add n/Zoe Tan a/Under 18 p/999 e/zoe@example.com addr/123 Main Street");
        logic.execute("add n/Amy Lim a/Under 14 p/1000 e/amy@example.com addr/123 Main Street");

        assertEquals("Sorted the displayed athlete list by name in ascending order.",
                logic.execute("sort name").getFeedbackToUser());
        assertEquals(List.of("Amy Lim", "Zoe Tan"), getDisplayedNames(model));
        // @@author qiyuannn
        assertEquals(List.of("Zoe Tan", "Amy Lim"), getStoredNames(model.getRoster()));
        // @@author

        // @@author qiyuannn
        JsonRosterStorage storage =
                new JsonRosterStorage(temporaryFolder.resolve("roster.json"));
        ReadOnlyRoster reloadedRoster = storage.readRoster().orElseThrow();
        assertEquals(List.of("Zoe Tan", "Amy Lim"), getStoredNames(reloadedRoster));
        // @@author

        CommandResult listResult = logic.execute("list");
        assertEquals("Displaying 2 athletes.", listResult.getFeedbackToUser());
        assertEquals(false, listResult.isDataSaved());
        assertEquals(List.of("Zoe Tan", "Amy Lim"), getDisplayedNames(model));
    }

    @Test
    public void execute_nameSortWithRepeatedSpaces_preservesTextAcrossUpdatesAndReload() throws Exception {
        // EP: name sorting with multi-space internal formatting across edits and reload
        logic.execute("add n/Lee  Bob a/Open p/911 e/bob@example.com addr/Home");
        logic.execute("add n/Lee Ann a/Open p/922 e/ann@example.com addr/Home");
        logic.execute("sort name");
        assertEquals(List.of("Lee Ann", "Lee  Bob"), getDisplayedNames(model));
        logic.execute("sort name desc");
        assertEquals(List.of("Lee  Bob", "Lee Ann"), getDisplayedNames(model));

        logic.execute("filter a/Open");
        logic.execute("add n/Lee   Amy a/Under 14 p/933 e/amy@example.com addr/Home");
        assertEquals(List.of("Lee  Bob", "Lee Ann", "Lee   Amy"), getDisplayedNames(model));
        logic.execute("edit 1 n/Lee    Aaron");
        assertEquals(List.of("Lee Ann", "Lee   Amy", "Lee    Aaron"), getDisplayedNames(model));

        JsonRosterStorage storage = new JsonRosterStorage(temporaryFolder.resolve("roster.json"));
        ReadOnlyRoster reloaded = storage.readRoster().orElseThrow();
        assertEquals(List.of("Lee    Aaron", "Lee Ann", "Lee   Amy"), getStoredNames(reloaded));
    }

    @Test
    public void execute_phoneNotesSortThenUpdates_preservesTextAcrossReload() throws Exception {
        // EP: phone number sort with international prefixes and parenthesized notes
        logic.execute("add n/Aaa Tan a/Open p/200 (5) e/aaa@example.com addr/Home");
        logic.execute("add n/Bbb Tan a/Open p/300 e/bbb@example.com addr/Home");
        logic.execute("add n/Ccc Tan a/Open p/(+65) 200 e/ccc@example.com addr/Home");
        logic.execute("sort phone");
        assertEquals(List.of("Aaa Tan", "Bbb Tan", "Ccc Tan"), getDisplayedNames(model));
        logic.execute("sort phone desc");
        assertEquals(List.of("Ccc Tan", "Bbb Tan", "Aaa Tan"), getDisplayedNames(model));

        logic.execute("edit 1 p/100 (Office 9)");
        assertEquals(List.of("Bbb Tan", "Aaa Tan", "Ccc Tan"), getDisplayedNames(model));
        logic.execute("add n/Ddd Tan a/Open p/2 50 (Office 8) e/ddd@example.com addr/Home");
        assertEquals(List.of("Bbb Tan", "Ddd Tan", "Aaa Tan", "Ccc Tan"), getDisplayedNames(model));

        JsonRosterStorage storage = new JsonRosterStorage(temporaryFolder.resolve("roster.json"));
        ReadOnlyRoster reloaded = storage.readRoster().orElseThrow();
        assertEquals(List.of("Aaa Tan", "Bbb Tan", "Ccc Tan", "Ddd Tan"), getStoredNames(reloaded));
        assertEquals(List.of("200 (5)", "300", "100 (Office 9)", "2 50 (Office 8)"),
                reloaded.getPersonList().stream().map(person -> person.getPhone().value).toList());
        Model reloadedModel = new ModelManager(reloaded, new UserPrefs());
        Logic reloadedLogic = new LogicManager(reloadedModel, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("reloadedPrefs.json"))));
        reloadedLogic.execute("sort phone");
        assertEquals(List.of("Ccc Tan", "Aaa Tan", "Ddd Tan", "Bbb Tan"), getDisplayedNames(reloadedModel));
    }

    @Test
    public void execute_deleteAfterSort_usesSortedDisplayIndex() throws Exception {
        // EP: index-based deletion following custom sort targets displayed position
        logic.execute("add n/Amy Lim a/Under 14 p/1000 e/amy@example.com addr/123 Main Street");
        logic.execute("add n/Zoe Tan a/Under 18 p/999 e/zoe@example.com addr/123 Main Street");
        logic.execute("sort name desc");
        assertEquals(List.of("Zoe Tan", "Amy Lim"), getDisplayedNames(model));

        logic.execute("delete 1"); // Boundary value
        assertEquals(List.of("Amy Lim"), getDisplayedNames(model));
        assertEquals(List.of("Amy Lim"), getStoredNames(model.getRoster()));
    }

    @Test
    public void execute_phoneSortThenFormattedUpdates_preservesOrderAndReloads() throws Exception {
        // EP: numeric sorting of formatted phone numbers with spaces, symbols, and labels
        logic.execute("add n/Zoe Tan a/Open p/999 e/zoe@example.com addr/Home");
        logic.execute("add n/Amy Lim a/Open p/1000 e/amy@example.com addr/Home");
        logic.execute("sort phone");

        logic.execute("edit 1 p/2 000 (HP)");
        assertEquals(List.of("Amy Lim", "Zoe Tan"), getDisplayedNames(model));
        logic.execute("add n/Ben Lee a/Open p/(65) 9123-4567 e/ben@example.com addr/Home");
        assertEquals(List.of("Amy Lim", "Zoe Tan", "Ben Lee"), getDisplayedNames(model));
        logic.execute("sort phone desc");
        assertEquals(List.of("Ben Lee", "Zoe Tan", "Amy Lim"), getDisplayedNames(model));
        logic.execute("edit 1 p/+1 500");
        assertEquals(List.of("Zoe Tan", "Ben Lee", "Amy Lim"), getDisplayedNames(model));

        JsonRosterStorage rosterStorage = new JsonRosterStorage(temporaryFolder.resolve("roster.json"));
        Model reloaded = new ModelManager(rosterStorage.readRoster().orElseThrow(), new UserPrefs());
        Logic reloadedLogic = new LogicManager(reloaded, new StorageManager(rosterStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("reloadedPrefs.json"))));
        reloadedLogic.execute("sort phone");
        assertEquals(List.of("Amy Lim", "Ben Lee", "Zoe Tan"), getDisplayedNames(reloaded));
        assertEquals(List.of("1000", "+1 500", "2 000 (HP)"), reloaded.getFilteredPersonList().stream()
                .map(person -> person.getPhone().value).toList());
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        // EP: storage layer generic IOException handling
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, String.format(
                LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage()));
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        // EP: storage layer AccessDeniedException handling
        // The permission error names the roster file, not the internal temporary file.
        Path rosterPath = temporaryFolder.resolve("ExceptionUserPrefs.json");
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, String.format(
                LogicManager.FILE_OPS_PERMISSION_ERROR_FORMAT, rosterPath));
    }

    @Test
    public void execute_failedRosterChanges_preserveModelDisplayAndSavedFile() throws Exception {
        // EP: atomic rollback when storage save fails
        logic.execute("add n/Zoe Tan a/Under 14 p/999 e/zoe@example.com addr/Home");
        logic.execute("add n/Amy Lim a/Open p/1000 e/amy@example.com addr/Home");
        logic.execute("sort name desc");
        logic.execute("find n/zoe");

        ReadOnlyRoster rosterBeforeFailure = new ModelManager(model.getRoster(), model.getUserPrefs()).getRoster();
        List<Person> displayBeforeFailure = List.copyOf(model.getFilteredPersonList());
        var predicateBeforeFailure = model.getPersonListPredicate();
        var comparatorBeforeFailure = model.getPersonListComparator();
        Path savedPath = temporaryFolder.resolve("roster.json");
        String savedDataBeforeFailure = Files.readString(savedPath);
        int[] publishedChanges = {0};
        model.getRoster().getPersonList().addListener(
                (javafx.collections.ListChangeListener<Person>) change -> publishedChanges[0]++);

        JsonRosterStorage failingStorage = new JsonRosterStorage(savedPath) {
            @Override
            public void saveRoster(ReadOnlyRoster roster) throws IOException {
                throw DUMMY_IO_EXCEPTION;
            }
        };
        Logic failingLogic = new LogicManager(model, new StorageManager(failingStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("failingPrefs.json"))));

        for (String command : List.of("add n/Ben Lee a/Open p/222 e/ben@example.com addr/Home",
                "edit 1 n/Zed Tan", "remark 1 r/Injured", "delete 1", "y")) {
            if (command.equals("y")) {
                failingLogic.execute("clear");
            }
            assertThrows(CommandException.class, () -> failingLogic.execute(command));
            assertEquals(rosterBeforeFailure, model.getRoster());
            assertEquals(displayBeforeFailure, model.getFilteredPersonList());
            assertSame(predicateBeforeFailure, model.getPersonListPredicate());
            assertSame(comparatorBeforeFailure, model.getPersonListComparator());
            assertEquals(savedDataBeforeFailure, Files.readString(savedPath));
            assertEquals(0, publishedChanges[0]); // Boundary value
        }
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        // EP: immutability of filtered person list view
        assertThrows(UnsupportedOperationException.class, () ->
                logic.getFilteredPersonList().remove(0)); // Boundary value
    }

    @Test
    public void guiSettings_getAndSet_delegatesToModel() {
        // EP: get and set GuiSettings delegation to model
        GuiSettings customSettings = new GuiSettings(1280, 720, 100, 200);
        logic.setGuiSettings(customSettings);
        assertEquals(customSettings, logic.getGuiSettings());
        assertEquals(customSettings, model.getGuiSettings());
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        // @@author qiyuannn
        Model expectedModel = new ModelManager(model.getRoster(), new UserPrefs());
        // @@author
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // @@author qiyuannn
        // Inject LogicManager with a JsonRosterStorage that throws the IOException e when saving
        JsonRosterStorage rosterStorage = new JsonRosterStorage(prefPath) {
            // @@author
            @Override
            // @@author qiyuannn
            public void saveRoster(ReadOnlyRoster roster) throws IOException {
                // @@author
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        // @@author qiyuannn
        StorageManager storage = new StorageManager(rosterStorage, userPrefsStorage);
        // @@author

        logic = new LogicManager(model, storage);

        // @@author qiyuannn
        // Triggers the saveRoster method by executing an add command
        // @@author
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY
                + EMAIL_DESC_AMY + " a/Open addr/123 Main Street";
        ModelManager expectedModel = new ModelManager();
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, expectedModel);
    }

    private static List<String> getDisplayedNames(Model model) {
        return model.getFilteredPersonList().stream()
                .map(person -> person.getName().fullName)
                .toList();
    }

    // @@author qiyuannn
    private static List<String> getStoredNames(ReadOnlyRoster roster) {
        return roster.getPersonList().stream()
                // @@author
                .map(person -> person.getName().fullName)
                .toList();
    }
}
