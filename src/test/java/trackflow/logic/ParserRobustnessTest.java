package trackflow.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static trackflow.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.logic.commands.exceptions.CommandException;
import trackflow.logic.parser.Prefix;
import trackflow.logic.parser.TrackFlowParser;
import trackflow.logic.parser.exceptions.ParseException;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.ReadOnlyRoster;
import trackflow.model.person.Person;
import trackflow.storage.JsonRosterStorage;
import trackflow.storage.JsonUserPrefsStorage;
import trackflow.storage.StorageManager;

/**
 * Checks parser-to-storage boundaries with a real JSON file in a disposable directory.
 * Direct logic calls intentionally bypass the JavaFX text field, which removes pasted tabs and line breaks.
 * These tests therefore complement, rather than replace, keyboard checks on the packaged application.
 */
public class ParserRobustnessTest {

    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonRosterStorage rosterStorage;
    private Path savedPath;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        savedPath = temporaryFolder.resolve("roster.json");
        rosterStorage = new JsonRosterStorage(savedPath);
        logic = new LogicManager(model, new StorageManager(rosterStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
        logic.execute("add n/Avery Tan a/Under 14 p/91234567 e/avery@example.com addr/Home");
    }

    @Test
    public void execute_unicodeBoundaries_preserveContentAndValidatePrefixes() throws Exception {
        // EP: unicode whitespace used as valid syntax separators and preserved in values
        for (String separator : List.of("\u00a0", "\u2003", "\u2007", "\u202f", "\u3000")) {
            // Only syntax boundaries are interchangeable; whitespace inside free text must survive saving.
            String address = "Block" + separator + "Annex  A/B";
            String remark = "Practise" + separator + "x/y  twice";
            logic.execute(separator + "edit" + separator + "1" + separator + "addr/" + separator
                    + "Block" + separator + "Annex  A//B" + separator);
            logic.execute("remark" + separator + "1" + separator + "r/Practise" + separator + "x//y  twice");
            Person athlete = model.getFilteredPersonList().getFirst();
            assertEquals(address, athlete.getAddress().value);
            assertEquals(remark, athlete.getRemark().value);
            assertEquals(athlete, rosterStorage.readRoster().orElseThrow().getPersonList().getFirst());

            logic.execute("find" + separator + "Missing" + separator + "Avery");
            assertEquals(List.of(athlete), model.getFilteredPersonList());
            // EP: invalid commands with unicode whitespace rejecting cleanly
            assertRejectedWithoutChanges(ParseException.class, List.of(
                    "edit" + separator + "1" + separator + "addr/Home" + separator + "x/unknown",
                    "remark 1 r/Note" + separator + "x/unknown",
                    "edit 1 addr/Home" + separator + "ADDR/Other",
                    "edit 1 addr/" + separator,
                    "find n/" + separator));
        }
    }

    @Test
    public void execute_addressLineBreaks_rejectWithoutChangingRosterViewOrFile() throws Exception {
        // EP: address containing standard and unicode line breaks
        logic.execute("sort name desc");
        logic.execute("find n/Avery");
        List<String> commands = new ArrayList<>();
        for (String lineBreak : List.of("\n", "\r", "\u000b", "\f", "\u0085", "\u2028", "\u2029")) {
            String address = "Block" + lineBreak + "House";
            commands.add("edit 1 addr/" + address);
            commands.add("add n/Ben a/Open p/92345678 e/ben@example.com addr/" + address);
        }
        // NEL is not command-boundary whitespace, so it also reaches validation at the start of a value.
        commands.add("edit 1 addr/\u0085House");
        commands.add("edit 1 addr/\u0085");
        commands.add("add n/Ben a/Open p/92345678 e/ben@example.com addr/\u0085House");
        assertRejectedWithoutChanges(ParseException.class, commands);
    }

    @Test
    public void execute_escapedSlashText_persistsAndCanBeFound() throws Exception {
        // EP: escaped slash sequences inside free-text fields
        logic.execute("edit 1 addr/Block A//B addr//Annex");
        logic.execute("remark 1 r/Practise r//relay x//y Mon/Tue");

        Person athlete = model.getFilteredPersonList().getFirst();
        assertEquals("Block A/B addr/Annex", athlete.getAddress().value);
        assertEquals("Practise r/relay x/y Mon/Tue", athlete.getRemark().value);
        assertEquals(athlete, rosterStorage.readRoster().orElseThrow().getPersonList().getFirst());

        // EP: searching escaped slash sequences via find command
        for (String command : List.of("find A//B", "find addr/Block A//B", "find r/r//relay")) {
            logic.execute(command);
            assertEquals(List.of(athlete), model.getFilteredPersonList());
        }
    }

    @Test
    public void execute_addWithOrdinaryAddress_preservesSlashesAndBackslashes() throws Exception {
        // EP: single slashes and backslashes in addresses preserved without escaping
        logic.execute("add n/Ben Lim a/Open p/92345678 e/ben@example.com"
                + " addr/23 Bishan #05-110 C:\\Training Mon/Tue");

        Person athlete = model.getFilteredPersonList().getLast();
        assertEquals("23 Bishan #05-110 C:\\Training Mon/Tue", athlete.getAddress().value);
        assertEquals(athlete, rosterStorage.readRoster().orElseThrow().getPersonList().getLast());
    }

    @Test
    public void execute_literalBackslashBeforePrefix_roundTripsThroughSearchAndStorage() throws Exception {
        // EP: literal backslash preceding prefix pattern preserved across search and storage
        logic.execute("edit 1 addr/Block \\A/B");
        logic.execute("remark 1 r/Use \\r/relay");
        Person athlete = model.getFilteredPersonList().getFirst();

        assertEquals("Block \\A/B", athlete.getAddress().value);
        assertEquals("Use \\r/relay", athlete.getRemark().value);
        logic.execute("find addr/\\A/B");
        assertEquals(List.of(athlete), model.getFilteredPersonList());
        assertEquals(athlete, rosterStorage.readRoster().orElseThrow().getPersonList().getFirst());
    }

    @Test
    public void execute_escapedTokens_doNotSatisfyRequiredPrefixesOrBypassValidation() throws Exception {
        // EP: escaped prefix tokens not recognized as command argument prefixes
        String savedBefore = Files.readString(savedPath);
        Person athlete = model.getFilteredPersonList().getFirst();
        for (String command : List.of(
                "add n/Ben a/Open p/92345678 e/ben@example.com addr//Home",
                "edit 1 addr//home", "remark 1 r//Note", "edit 1 p//999", "edit 1 n/x//value",
                "edit 1 p/x//999", "edit 1 e/x//example.com", "edit 1 a/a//Open", "filter a//Open",
                "remark 1 r/first r//literal R/second",
                "edit 1 addr/Home x//literal x/unknown")) {
            assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(List.of(athlete), model.getFilteredPersonList(), command);
            assertEquals(savedBefore, Files.readString(savedPath), command);
        }
    }

    @Test
    public void execute_escapedValuesInSortedFilteredViews_updatesDisplayedAthletes() throws Exception {
        // EP: index with leading zeros in sorted and filtered view
        logic.execute("add n/Zoe Tan a/Under 16 p/92345678 e/zoe@example.com addr/Home");
        logic.execute("add n/Ben Lim a/Open p/93456789 e/ben@example.com addr/Home");
        logic.execute("sort name desc");
        logic.execute("filter a/Under 16");
        assertEquals("Zoe Tan", model.getFilteredPersonList().getFirst().getName().fullName);

        logic.execute("edit 0001 addr/Block A//B"); // Boundary value
        logic.execute("find n/Ben");
        logic.execute("remark 0001 r/Practise r//relay");
        logic.execute("find addr/Block A//B");
        assertEquals(1, model.getFilteredPersonList().size());
        assertEquals("Zoe Tan", model.getFilteredPersonList().getFirst().getName().fullName);
        logic.execute("delete 0001");
        assertEquals(List.of(), model.getFilteredPersonList()); // Boundary value

        List<Person> reloaded = rosterStorage.readRoster().orElseThrow().getPersonList();
        assertEquals(List.of("Avery Tan", "Ben Lim"),
                reloaded.stream().map(person -> person.getName().fullName).toList());
        assertEquals("Practise r/relay", reloaded.getLast().getRemark().value);
        assertEquals("", reloaded.getFirst().getRemark().value); // Boundary value
    }

    @Test
    public void execute_quotesAndBackslashes_remainOrdinaryText() throws Exception {
        // EP: double quotes and backslashes treated as ordinary text
        logic.execute("remark 1 r/Coach said \"go\" in r//relay");
        assertEquals("Coach said \"go\" in r/relay", model.getFilteredPersonList().getFirst().getRemark().value);
        logic.execute("remark 1 r/\"Quoted\"");
        assertEquals("\"Quoted\"", model.getFilteredPersonList().getFirst().getRemark().value);
        logic.execute("remark 1 r/C:\\Training \"unfinished");
        assertEquals("C:\\Training \"unfinished", model.getFilteredPersonList().getFirst().getRemark().value);
        // EP: empty remark prefix value clearing existing remark
        logic.execute("remark 1 r/"); // Boundary value
        assertEquals("", model.getFilteredPersonList().getFirst().getRemark().value);
        assertEquals("", rosterStorage.readRoster().orElseThrow().getPersonList().getFirst().getRemark().value);
    }

    @Test
    public void execute_escapedSearch_combinesWithFieldSearch() throws Exception {
        // EP: find command combining escaped slash keyword with field prefix
        logic.execute("edit 1 addr/Block A//B");
        logic.execute("add n/Ben Lim a/Open p/92345678 e/ben@example.com addr/Block C");

        logic.execute("find A//B");
        assertEquals(List.of("Avery Tan"),
                model.getFilteredPersonList().stream().map(person -> person.getName().fullName).toList());
        logic.execute("find A//B n/Ben");
        assertEquals(2, model.getFilteredPersonList().size());
    }

    @Test
    public void execute_autocompleteAfterEscapedAddress_addsActualMissingPrefix() throws Exception {
        // EP: autocomplete prefix suggestion on commands containing escaped address tokens
        AutocompleteEngine autocomplete = new AutocompleteEngine();
        String command = "edit 1 n/Avery Tan p/91234567 e/avery@example.com addr/Block A//B";
        String nextPrefix = autocomplete.getNextPrefix(command);
        assertEquals(" a/", nextPrefix);

        logic.execute(command + nextPrefix + "Open");
        Person athlete = model.getFilteredPersonList().getFirst();
        assertEquals("Block A/B", athlete.getAddress().value);
        assertEquals("Open", athlete.getAgeCategory().value);

        String addInput = "add N/Ben P/92345678 E/ben@example.com ADDR/Block A//B x//value ";
        String completion = autocomplete.getNextPrefix(addInput);
        assertEquals("a/", completion);
        logic.execute(addInput + completion + "Open");
        assertEquals("Block A/B x/value", model.getFilteredPersonList().getLast().getAddress().value);
    }

    @Test
    public void execute_slashLedValues_requireSpaceAfterRealPrefix() throws Exception {
        // EP: argument values starting with single leading slash separated by whitespace
        logic.execute("edit 1 addr/ /home/coach");
        logic.execute("remark 1 r/ /note");
        Person updated = model.getFilteredPersonList().getFirst();
        assertEquals("/home/coach", updated.getAddress().value);
        assertEquals("/note", updated.getRemark().value);
        assertEquals(updated, rosterStorage.readRoster().orElseThrow().getPersonList().getFirst());
    }

    @Test
    public void execute_repeatedSlashEscapes_surviveReloadAndExactFieldSearch() throws Exception {
        // EP: repeated slash escapes ranging from 2 slashes to 5 slashes
        for (int count = 2; count <= 5; count++) {
            String entered = "A" + "/".repeat(count) + "B";
            String expected = "A" + "/".repeat(count - 1) + "B";
            logic.execute("edit 1 addr/Block " + entered);
            Person athlete = rosterStorage.readRoster().orElseThrow().getPersonList().getFirst();
            assertEquals("Block " + expected, athlete.getAddress().value);
            logic.execute("find addr/Block " + entered);
            assertEquals(List.of(athlete), model.getFilteredPersonList());
        }
    }

    @Test
    public void execute_urlsAndNetworkPaths_preservesDoubleSlashes() throws Exception {
        // EP: URL schemes, UNC network paths, and standalone slash strings
        for (String value : List.of("https://example.com/a//b", "http://host/?n/value&p//other#addr//here",
                "file:///home/coach", "ftp://host/a//b", "//server/share", "x://host/path", "/", "//", "///")) {
            logic.execute("edit 1 addr/ " + value);
            logic.execute("remark 1 r/ " + value);
            Person athlete = rosterStorage.readRoster().orElseThrow().getPersonList().getFirst();
            assertEquals(value, athlete.getAddress().value);
            assertEquals(value, athlete.getRemark().value);
            logic.execute("find addr/ " + value);
            assertEquals(List.of(athlete), model.getFilteredPersonList());
            logic.execute("find r/ " + value);
            assertEquals(List.of(athlete), model.getFilteredPersonList());
        }
    }

    @Test
    public void execute_duplicateParametersInEveryField_rejectsEvenWithEmptyOrEscapedValues() throws Exception {
        // EP: duplicate parameter prefixes across all command fields
        List<String> validFields = List.of("n/Ben", "a/Open", "p/92345678", "e/ben@example.com", "addr/Home");
        List<String> invalidCommands = new ArrayList<>();
        TrackFlowParser parser = new TrackFlowParser();
        for (String field : List.of("n", "a", "p", "e", "addr", "r")) {
            String upper = field.toUpperCase(Locale.ROOT);
            for (String repeated : List.of(field + "/first " + field + "//literal " + upper + "/second",
                    field + "/ " + upper + "/second", field + "/first " + upper + "/",
                    upper + "/same " + field + "/same")) {
                List<String> commands = new ArrayList<>();
                commands.add("find " + repeated);
                if (field.equals("r")) {
                    commands.add("remark 1 " + repeated);
                } else {
                    commands.add("edit 1 " + repeated);
                    String remaining = String.join(" ", validFields.stream()
                            .filter(value -> !value.startsWith(field + "/")).toList());
                    commands.add("add " + remaining + " " + repeated);
                    if (field.equals("a")) {
                        commands.add("filter " + repeated);
                    }
                }
                for (String command : commands) {
                    // Check the reason too: invalid field content must not hide a duplicate parameter.
                    assertThrows(ParseException.class, Messages.getErrorMessageForDuplicatePrefixes(
                            new Prefix(field + "/")), () -> parser.parseCommand(command));
                }
                invalidCommands.addAll(commands);
            }
        }
        assertRejectedWithoutChanges(ParseException.class, List.of(invalidCommands.getFirst()));
    }

    @Test
    public void execute_emptyAndMissingRequiredValues_rejectsWithoutWriting() throws Exception {
        // EP: missing mandatory field prefixes, empty values, and missing index
        List<String> validFields = List.of("n/Ben", "a/Open", "p/92345678", "e/ben@example.com", "addr/Home");
        List<String> commands = new ArrayList<>();
        for (String field : List.of("n", "a", "p", "e", "addr")) {
            String remaining = String.join(" ", validFields.stream()
                    .filter(value -> !value.startsWith(field + "/")).toList());
            commands.add("add " + remaining);
            for (String blank : List.of("", " ", "\t", "\u2003")) {
                commands.add("add " + remaining + " " + field + "/" + blank);
                commands.add("edit 1 " + field + "/" + blank);
                commands.add("find " + field + "/" + blank);
            }
            commands.add("add " + field + "//literal " + remaining);
        }
        commands.addAll(List.of("remark 1", "remark 1 r//", "find r/", "find", "filter", "filter a/",
                "edit 1", "delete", "delete null", "edit null addr/Home", "remark null r/Note"));
        assertRejectedWithoutChanges(ParseException.class, commands);
    }

    @Test
    public void execute_quotesDoNotEscapeParametersOrSuppressDuplicateChecks() throws Exception {
        // EP: quotes do not escape parameters or suppress duplicate checks
        assertRejectedWithoutChanges(ParseException.class, List.of(
                "remark 1 r/\"first R/second\"", "edit 1 addr/'first ADDR/second'",
                "remark 1 r/\"first x/unknown\"", "remark 1 r/'first x/unknown'"));
        logic.execute("remark 1 r/\"first r//second\"");
        assertEquals("\"first r/second\"", model.getFilteredPersonList().getFirst().getRemark().value);
    }

    @Test
    public void execute_literalNull_isTextAndDoesNotBypassFieldValidation() throws Exception {
        // EP: literal string \"null\" as free-text values
        logic.execute("edit 1 n/null addr/null");
        logic.execute("remark 1 r/null");
        Person athlete = rosterStorage.readRoster().orElseThrow().getPersonList().getFirst();
        assertEquals("null", athlete.getName().fullName);
        assertEquals("null", athlete.getAddress().value);
        assertEquals("null", athlete.getRemark().value);
        logic.execute("find n/null");
        assertEquals(List.of(athlete), model.getFilteredPersonList());
        // EP: literal string \"null\" failing validation in constrained fields
        assertRejectedWithoutChanges(ParseException.class, List.of("edit 1 p/null", "edit 1 e/null",
                "edit 1 a/null", "filter a/null"));
        // EP: null input to logic execution
        assertRejectedWithoutChanges(NullPointerException.class,
                java.util.Collections.singletonList(null)); // Boundary value
    }

    @Test
    public void execute_escapedText_doesNotBypassDuplicateAthleteChecks() throws Exception {
        // EP: duplicate athlete detection against hidden record with escaped argument tokens
        logic.execute("add n/Ben Lim a/Open p/92345678 e/ben@example.com addr/Home");
        logic.execute("find n/Ben");
        assertRejectedWithoutChanges(CommandException.class, List.of(
                "add n/AVERY TAN a/Open p/91234567 e/other@example.com addr/Block A//B",
                "edit 1 n/AVERY TAN p/91234567 addr/Block A//B"));
    }

    @Test
    public void execute_reloadingAndUnrelatedEdits_doNotDecodeStoredTextAgain() throws Exception {
        // EP: idempotent storage and reload of multi-slash escaped text across mutations
        logic.execute("edit 1 addr/Block A////B addr///annex");
        logic.execute("remark 1 r/Use r////relay https://host/a//b");
        for (int restart = 0; restart < 3; restart++) {
            model = new ModelManager(rosterStorage.readRoster().orElseThrow(), model.getUserPrefs());
            logic = new LogicManager(model, new StorageManager(rosterStorage,
                    new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
            logic.execute("edit 1 p/9123456" + restart);
            Person athlete = model.getFilteredPersonList().getFirst();
            assertEquals("Block A///B addr//annex", athlete.getAddress().value);
            assertEquals("Use r///relay https://host/a//b", athlete.getRemark().value);
            logic.execute("find addr/Block A////B addr///annex");
            assertEquals(List.of(athlete), model.getFilteredPersonList());
        }
    }

    @Test
    public void execute_multilineSingleCommand_acceptsParameterSeparators() throws Exception {
        // EP: multi-line commands with mixed tabs, carriage returns, and newlines as parameter delimiters
        logic.execute("\tEdIt\r\n0001\tADDR/Training centre\nA/Under 16\t");

        Person athlete = model.getFilteredPersonList().getFirst();
        assertEquals("Training centre", athlete.getAddress().value);
        assertEquals("Under 16", athlete.getAgeCategory().value);
        logic.execute("FiNd\nN/Avery");
        assertEquals(List.of(athlete), model.getFilteredPersonList());
        logic.execute("SoRt\r\nname\tdesc");
        assertEquals(List.of(athlete), model.getFilteredPersonList());
        assertEquals(athlete, rosterStorage.readRoster().orElseThrow().getPersonList().getFirst());
    }

    @Test
    public void execute_invalidInput_preservesRosterDisplayAndFile() throws Exception {
        // EP: invalid syntax, empty strings, unrecognized commands, and line breaks in address
        logic.execute("sort name desc");
        logic.execute("find n/Avery");
        assertRejectedWithoutChanges(ParseException.class, List.of(
                "", " \t\r\n", "unknown", "list extra", "help extra",
                "exit extra", "clear\nexit", "sort name asc extra",
                "find n/Avery N/Ben", "edit 1 p/999 P/888", "remark 1 r/first R/second",
                "edit 1 x/value", "remark 1 r/Note\u2003x/value",
                "edit 1 addr/Block\nAnnex", "edit 1 addr/Block\rAnnex", "edit 1 addr/Block\u2028Annex"));
    }

    @Test
    public void execute_pathologicalEmail_preservesRosterDisplayAndFile() throws Exception {
        // EP: pathological email inputs exceeding bounds
        logic.execute("sort name desc");
        logic.execute("find n/Avery");
        Duration timeout = Duration.ofMillis(200);
        List<String> pathologicalCommands = List.of(
                "add n/Bob a/Under 14 p/91234567 e/" + "a".repeat(5000) + " addr/Block A",
                "add n/Bob a/Under 14 p/91234567 e/a@" + "b".repeat(5000) + " addr/Block A",
                "edit 1 e/" + "a".repeat(5000),
                "edit 1 e/a@" + "b".repeat(5000),
                "edit 1 e/a@" + "a.".repeat(2500));
        for (String command : pathologicalCommands) {
            assertTimeoutPreemptively(timeout, () ->
                    assertRejectedWithoutChanges(ParseException.class, List.of(command)));
        }
    }

    @Test
    public void execute_extremeIndexes_rejectsWithoutChangingSavedData() throws Exception {
        // EP: extreme, non-numeric, zero, negative, and overflow index inputs
        String savedBefore = Files.readString(savedPath);
        Person athlete = model.getFilteredPersonList().getFirst();

        for (String index : List.of("0", "000", "-1", "+1", "1.0", "1 0", "\u0661", "9".repeat(10000))) {
            for (String command : List.of("delete " + index, "edit " + index + " p/999",
                    "remark " + index + " r/Changed")) {
                if (index.length() == 10000) {
                    assertThrows(CommandException.class, () -> logic.execute(command));
                } else {
                    assertThrows(ParseException.class, () -> logic.execute(command));
                }
                assertEquals(List.of(athlete), model.getFilteredPersonList());
                assertEquals(savedBefore, Files.readString(savedPath));
            }
        }
    }

    /**
     * Checks parser/command failures against the full roster, active view, and byte-for-byte saved contents.
     */
    private void assertRejectedWithoutChanges(Class<? extends Exception> exceptionType, List<String> commands)
            throws Exception {
        ReadOnlyRoster rosterBefore = new ModelManager(model.getRoster(), model.getUserPrefs()).getRoster();
        List<Person> displayedBefore = List.copyOf(model.getFilteredPersonList());
        var predicateBefore = model.getPersonListPredicate();
        var comparatorBefore = model.getPersonListComparator();
        String savedBefore = Files.readString(savedPath);
        for (String command : commands) {
            assertThrows(exceptionType, () -> logic.execute(command));
            assertEquals(rosterBefore, model.getRoster(), command);
            assertEquals(displayedBefore, model.getFilteredPersonList(), command);
            assertSame(predicateBefore, model.getPersonListPredicate(), command);
            assertSame(comparatorBefore, model.getPersonListComparator(), command);
            assertEquals(savedBefore, Files.readString(savedPath), command);
        }
    }
}
