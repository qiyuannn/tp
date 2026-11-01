package trackflow.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.logic.commands.AddCommand;
import trackflow.logic.commands.exceptions.CommandException;
import trackflow.logic.parser.exceptions.ParseException;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.UserPrefs;
import trackflow.model.util.SampleDataUtil;
import trackflow.storage.JsonRosterStorage;
import trackflow.storage.JsonUserPrefsStorage;
import trackflow.storage.StorageManager;

/**
 * Exercises help examples and actionable parser feedback through real command execution and saved data.
 */
public class CommandFeedbackRegressionTest {

    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonRosterStorage storage;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager(SampleDataUtil.getSampleRoster(), new UserPrefs());
        storage = new JsonRosterStorage(temporaryFolder.resolve("roster.json"));
        storage.saveRoster(model.getRoster());
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }

    @Test
    public void execute_helpAddExampleOnSampleRoster_addsNewAthlete() throws Exception {
        // EP: valid add command derived from help usage example
        String command = AddCommand.MESSAGE_USAGE.split("Example: ", 2)[1];
        int originalCount = model.getRoster().getPersonList().size();

        assertTrue(logic.execute(command).getFeedbackToUser().startsWith("New athlete added: Morgan Vale;"));
        assertEquals(originalCount + 1, // Boundary value
                storage.readRoster().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_duplicateWithDifferentCaseAndSpacing_namesStoredAthlete() throws Exception {
        // EP: duplicate athlete differing in letter case and whitespace
        String before = Files.readString(storage.getRosterFilePath());
        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute("add n/avery   tan a/Open p/91234567 e/other@example.com addr/Home"));
        assertEquals(String.format(AddCommand.MESSAGE_DUPLICATE_PERSON, "Avery Tan"), error.getMessage());
        assertEquals(before, Files.readString(storage.getRosterFilePath()));
    }

    @Test
    public void execute_searchSortAndDuplicateChecks_doNotRenameStoredAthlete() throws Exception {
        // EP: preserving athlete original name casing and internal spacing across search, sort, and duplicate rejection
        logic.execute("add n/LEE   Ann a/Open p/84446666 e/lee.ann@example.com addr/Home");
        String before = Files.readString(storage.getRosterFilePath());
        for (String command : List.of("find n/lee ann", "sort name", "find n/LEE   Ann", "sort age")) {
            logic.execute(command);
            assertEquals("LEE   Ann", model.getFilteredPersonList().getFirst().getName().fullName);
            assertEquals(before, Files.readString(storage.getRosterFilePath()));
        }
        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute("add n/lee ann a/Open p/84446666 e/other@example.com addr/Home"));
        assertEquals(String.format(AddCommand.MESSAGE_DUPLICATE_PERSON, "LEE   Ann"), error.getMessage());
        assertEquals(before, Files.readString(storage.getRosterFilePath()));

        logic.execute("remark 1 r/Checked");
        assertEquals("LEE   Ann", storage.readRoster().orElseThrow().getPersonList().getLast().getName().fullName);
    }

    @Test
    public void execute_unknownPrefixInFreeText_explainsEscapingAndPreservesData() throws Exception {
        // EP: unescaped prefix syntax in free text requiring escaping
        String before = Files.readString(storage.getRosterFilePath());
        for (String command : List.of("remark 1 r/Practise w/coach", "edit 1 addr/Block s/West")) {
            ParseException error = assertThrows(ParseException.class, () -> logic.execute(command));
            assertTrue(error.getMessage().contains("is not valid for this command"));
            assertTrue(error.getMessage().contains("w//coach"));
            assertEquals(before, Files.readString(storage.getRosterFilePath()));
        }
        // EP: valid escaped slash prefix in free text
        logic.execute("remark 1 r/Practise w//coach");
        assertEquals("Practise w/coach", storage.readRoster().orElseThrow()
                .getPersonList().get(0).getRemark().value);
    }

    @Test
    public void execute_escapedAddressPrefix_explainsLeadingSlashAndAcceptsCorrection() throws Exception {
        // EP: leading slash-sequence without whitespace separation
        String before = Files.readString(storage.getRosterFilePath());
        ParseException error = assertThrows(ParseException.class, () ->
                logic.execute("edit 1 addr///server/share"));
        assertTrue(error.getMessage().contains("addr/ //server/share"));
        assertEquals(before, Files.readString(storage.getRosterFilePath()));

        // EP: leading slash-sequence with whitespace separation
        logic.execute("edit 1 addr/ //server/share");
        assertEquals("//server/share", storage.readRoster().orElseThrow().getPersonList().get(0).getAddress().value);
    }

    @Test
    public void execute_addWithEventsOrRemarks_identifiesUnsupportedPrefix() throws Exception {
        // EP: unsupported prefixes in add command
        String before = Files.readString(storage.getRosterFilePath());
        for (String prefix : List.of("t/", "r/")) {
            ParseException error = assertThrows(ParseException.class, () -> logic.execute(
                    "add n/Unique Athlete a/Open p/555 e/unique@example.com addr/Home " + prefix + "Note"));
            assertTrue(error.getMessage().startsWith(prefix + " is not valid for this command."));
            assertEquals(before, Files.readString(storage.getRosterFilePath()));
        }
    }

    @Test
    public void execute_overlongEmail_explainsLimitAndPreservesSavedEmail() throws Exception {
        // EP: maximum allowed email length (254 characters)
        String validEmail = "a".repeat(251) + "@ab"; // Boundary value
        logic.execute("edit 1 e/" + validEmail);
        String before = Files.readString(storage.getRosterFilePath());
        // EP: email length exceeding maximum limit (255 characters)
        ParseException error = assertThrows(ParseException.class, () ->
                logic.execute("edit 1 e/" + "a".repeat(252) + "@ab")); // Boundary value
        assertTrue(error.getMessage().contains("at most 254 characters"));
        assertEquals(before, Files.readString(storage.getRosterFilePath()));
        assertEquals(validEmail, storage.readRoster().orElseThrow().getPersonList().get(0).getEmail().value);
    }

    @Test
    public void execute_addressContainingLineSeparator_explainsSingleLineConstraint() throws Exception {
        // EP: address containing multi-line unicode line separator
        String before = Files.readString(storage.getRosterFilePath());
        ParseException error = assertThrows(ParseException.class, () ->
                logic.execute("edit 1 addr/Home\u2028Annex"));
        assertTrue(error.getMessage().contains("single-line"));
        assertEquals(before, Files.readString(storage.getRosterFilePath()));
    }

    @Test
    public void execute_unicodeEventSearchAndRemoval_useSameRulesAfterReload() throws Exception {
        // EP: unicode case and diacritics folding in event tags across session reloads
        logic.execute("tag 1 t/Σ t/Saut-E\u0301lan t/Straße");
        Model restarted = new ModelManager(storage.readRoster().orElseThrow(), new UserPrefs());
        Logic restartedLogic = new LogicManager(restarted, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("restarted-prefs.json"))));

        for (String command : List.of("find t/ς", "find ς", "find t/Saut-E\u0301lan", "find Saut-E\u0301lan",
                "find t/STRASSE")) {
            restartedLogic.execute(command);
            assertEquals(List.of("Avery Tan"), restarted.getFilteredPersonList().stream()
                    .map(person -> person.getName().fullName).toList(), command);
        }
        // EP: tag removal matching canonical unicode forms
        restartedLogic.execute("untag 1 t/ς");
        restartedLogic.execute("find t/ς");
        assertEquals(0, restarted.getFilteredPersonList().size()); // Boundary value
        assertEquals(restarted.getRoster(), storage.readRoster().orElseThrow());
    }
}
