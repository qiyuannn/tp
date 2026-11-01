// @@author antonydchakola-hub
package trackflow.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import trackflow.logic.commands.EditCommand;
import trackflow.logic.parser.TrackFlowParser;

public class AutocompleteEngineTest {

    private AutocompleteEngine autocompleteEngine;

    @BeforeEach
    public void setUp() {
        autocompleteEngine = new AutocompleteEngine();
    }

    // ========================================
    // Tests for getSuggestions
    // ========================================

    @Test
    public void getSuggestions_nullInput_returnsEmptyList() {
        // EP: null input
        List<String> suggestions = autocompleteEngine.getSuggestions(null); // Boundary value
        assertTrue(suggestions.isEmpty());
    }

    @Test
    // @@author
    public void getSuggestions_emptyOrWhitespaceOnly_returnsEmptyList() {
        // EP: empty string ("") and whitespace-only strings
        for (String input : List.of("", "   ", "\t")) { // Boundary value
            assertTrue(autocompleteEngine.getSuggestions(input).isEmpty());
        }
    }

    @Test
    public void getSuggestions_partialCommandWord_returnsMatchingSuggestions() {
        // EP: single-character prefix matching single command
        List<String> suggestions = autocompleteEngine.getSuggestions("a"); // Boundary value
        assertEquals(List.of("add n/ a/ p/ e/ addr/"), suggestions);

        suggestions = autocompleteEngine.getSuggestions("d");
        assertEquals(List.of("delete INDEX"), suggestions);

        // EP: multi-character prefix matching single command
        suggestions = autocompleteEngine.getSuggestions("ed");
        assertEquals(List.of("edit INDEX n/ a/ p/ e/ addr/"), suggestions);

        suggestions = autocompleteEngine.getSuggestions("r");
        assertEquals(List.of("remark INDEX r/"), suggestions);

        suggestions = autocompleteEngine.getSuggestions("ta");
        assertEquals(List.of("tag INDEX t/"), suggestions);

        // sort shows its supported fields and orders
        suggestions = autocompleteEngine.getSuggestions("so");
        assertEquals(List.of("sort name|age|phone|email|address [asc|desc]"), suggestions);

        // EP: exact full command word matching template
        suggestions = autocompleteEngine.getSuggestions("sort");
        assertEquals(List.of("sort name|age|phone|email|address [asc|desc]"), suggestions);

        suggestions = autocompleteEngine.getSuggestions("u");
        assertEquals(List.of("untag INDEX t/"), suggestions);
    }

    @Test
    public void getNextPrefix_tagCommands_suggestsEventPrefix() {
        // EP: tag command without trailing space
        assertEquals(" t/", autocompleteEngine.getNextPrefix("tag 1"));
        // EP: tag command with trailing space
        assertEquals("t/", autocompleteEngine.getNextPrefix("untag 1 "));
        // EP: tag command with event prefix already present
        assertEquals("", autocompleteEngine.getNextPrefix("tag 1 t/100m"));
    }

    @Test
    public void getSuggestions_multipleMatches_returnsAllMatches() {
        // EP: prefix matching multiple command templates
        List<String> suggestions = autocompleteEngine.getSuggestions("f");
        assertEquals(List.of("filter a/", "find "), suggestions);
    }

    @Test
    public void getSuggestions_caseInsensitive_returnsMatchingSuggestions() {
        // EP: uppercase command word
        List<String> uppercaseSuggestions = autocompleteEngine.getSuggestions("ADD");
        assertEquals(List.of("add n/ a/ p/ e/ addr/"), uppercaseSuggestions);

        // EP: mixed-case command word
        List<String> mixedCaseSuggestions = autocompleteEngine.getSuggestions("ClEaR");
        assertEquals(List.of("clear"), mixedCaseSuggestions);
    }

    @Test
    public void getSuggestions_leadingWhitespace_returnsMatchingSuggestions() {
        // EP: command word with leading whitespace
        List<String> suggestions = autocompleteEngine.getSuggestions("   add");
        assertEquals(List.of("add n/ a/ p/ e/ addr/"), suggestions);
    }

    @Test
    public void getSuggestions_unicodeWhitespace_distinguishesLeadingAndTrailingSeparators() {
        // EP: unicode whitespace separators
        for (String separator : List.of("\u00a0", "\u2003", "\u2007", "\u202f", "\u3000")) {
            assertEquals(List.of("edit INDEX n/ a/ p/ e/ addr/"),
                    autocompleteEngine.getSuggestions(separator + "ed"));
            assertTrue(autocompleteEngine.getSuggestions("edit" + separator).isEmpty());
            assertTrue(autocompleteEngine.getSuggestions(separator).isEmpty());
        }
    }

    @Test
    public void getNextPrefix_unicodeWhitespace_agreesWithCommandParser() throws Exception {
        // EP: unicode whitespace argument separators
        TrackFlowParser parser = new TrackFlowParser();
        for (String separator : List.of("\u00a0", "\u2003", "\u2007", "\u202f", "\u3000")) {
            String command = separator + "edit" + separator + "1";
            assertEquals(" n/", autocompleteEngine.getNextPrefix(command));
            assertEquals("n/", autocompleteEngine.getNextPrefix(command + separator));
            assertEquals(" a/", autocompleteEngine.getNextPrefix(command + separator + "n/Avery"));
            assertEquals(EditCommand.class, parser.parseCommand(command + separator + "n/Avery").getClass());
        }
    }

    @Test
    public void getSuggestions_trailingWhitespace_returnsEmptyList() {
        // EP: command word followed by standard whitespace
        // Once the user enters a space after the command word, command templates are no longer suggested
        List<String> suggestions = autocompleteEngine.getSuggestions("add ");
        assertTrue(suggestions.isEmpty());

        suggestions = autocompleteEngine.getSuggestions("add\t");
        assertTrue(suggestions.isEmpty());

        // EP: command word followed by newline or unicode whitespace
        // other whitespace after the command word also ends command-word suggestions
        assertTrue(autocompleteEngine.getSuggestions("add\n").isEmpty());
        assertTrue(autocompleteEngine.getSuggestions("add ").isEmpty()); // em space

        suggestions = autocompleteEngine.getSuggestions("clear ");
        assertTrue(suggestions.isEmpty());
    }

    @Test
    public void getSuggestions_withArguments_returnsEmptyList() {
        // EP: command word with arguments present
        List<String> suggestions = autocompleteEngine.getSuggestions("add n/John");
        assertTrue(suggestions.isEmpty());

        suggestions = autocompleteEngine.getSuggestions("filter a/Under 20");
        assertTrue(suggestions.isEmpty());
    }

    @Test
    public void getSuggestions_unknownCommand_returnsEmptyList() {
        // EP: unknown command word
        List<String> suggestions = autocompleteEngine.getSuggestions("xyz");
        assertTrue(suggestions.isEmpty());
    }

    // ========================================
    // Tests for getNextPrefix
    // ========================================

    @Test
    public void getNextPrefix_nullOrEmptyInput_returnsEmptyString() {
        // EP: null input
        assertEquals("", autocompleteEngine.getNextPrefix(null)); // Boundary value
        // EP: empty string
        assertEquals("", autocompleteEngine.getNextPrefix("")); // Boundary value
        // EP: whitespace-only string
        assertEquals("", autocompleteEngine.getNextPrefix("   "));
    }

    @Test
    public void getNextPrefix_unknownCommand_returnsEmptyString() {
        // EP: unknown command word
        assertEquals("", autocompleteEngine.getNextPrefix("unknown"));
        assertEquals("", autocompleteEngine.getNextPrefix("unknown n/John"));
    }

    @Test
    public void getNextPrefix_commandWithoutPrefixes_returnsEmptyString() {
        // EP: commands without parameter prefixes
        assertEquals("", autocompleteEngine.getNextPrefix("clear"));
        assertEquals("", autocompleteEngine.getNextPrefix("list"));
        assertEquals("", autocompleteEngine.getNextPrefix("exit"));
        assertEquals("", autocompleteEngine.getNextPrefix("delete"));
        assertEquals("", autocompleteEngine.getNextPrefix("find"));
        assertEquals("", autocompleteEngine.getNextPrefix("sort"));
        assertEquals("", autocompleteEngine.getNextPrefix("sort name ")); // sort arguments are not prefixes
        assertEquals("", autocompleteEngine.getNextPrefix("help"));
    }

    @Test
    public void getNextPrefix_addCommandWithAndWithoutTrailingSpace_returnsNextMissingPrefix() {
        // EP: add command without trailing space
        assertEquals(" n/", autocompleteEngine.getNextPrefix("add"));
        // EP: add command with trailing space
        assertEquals("n/", autocompleteEngine.getNextPrefix("add "));
        // EP: uppercase add command with trailing space
        assertEquals("n/", autocompleteEngine.getNextPrefix("ADD "));
    }

    @Test
    public void getNextPrefix_addCommandSequentialPrefixes_returnsNextMissingPrefix() {
        // EP: 1 of 5 prefixes present
        // Starting with name prefix
        assertEquals(" a/", autocompleteEngine.getNextPrefix("add n/John"));
        assertEquals("a/", autocompleteEngine.getNextPrefix("add n/John "));

        // EP: 2 of 5 prefixes present
        // With name and age
        assertEquals(" p/", autocompleteEngine.getNextPrefix("add n/John a/Under 20"));

        // EP: 3 of 5 prefixes present
        // With name, age, phone
        assertEquals(" e/", autocompleteEngine.getNextPrefix("add n/John a/Under 20 p/91234567"));

        // EP: 4 of 5 prefixes present
        // With name, age, phone, email
        assertEquals(" addr/",
                autocompleteEngine.getNextPrefix("add n/John a/Under 20 p/91234567 e/j@e.com"));

        // EP: all 5 prefixes present
        // All prefixes present
        assertEquals("",
                autocompleteEngine.getNextPrefix(
                        "add n/John a/Under 20 p/91234567 e/j@e.com addr/NUS")); // Boundary value
    }

    @Test
    public void getNextPrefix_uppercaseCommandAndPrefixes_matchesCaseInsensitively() {
        // EP: uppercase command and prefixes
        // Uppercase prefixes are recognized, so prefixes already entered are not inserted again
        assertEquals("n/", autocompleteEngine.getNextPrefix("EDIT 1 "));
        assertEquals(" a/", autocompleteEngine.getNextPrefix("add N/John"));
        assertEquals(" p/", autocompleteEngine.getNextPrefix("add N/John A/Under 20"));
        assertEquals(" e/", autocompleteEngine.getNextPrefix("add n/John a/Under 20 P/91234567"));
        assertEquals(" addr/",
                autocompleteEngine.getNextPrefix("add n/John a/Under 20 p/91234567 E/j@e.com"));
        assertEquals("",
                autocompleteEngine.getNextPrefix("add N/John A/Under 20 P/91234567 E/j@e.com ADDR/NUS"));
    }

    @Test
    public void getNextPrefix_escapedPrefixes_notCountedAsParameters() {
        // EP: escaped prefixes in argument values
        assertEquals(" n/", autocompleteEngine.getNextPrefix("add n//literal"));
        assertEquals(" r/", autocompleteEngine.getNextPrefix("remark 1 r//literal"));
        assertEquals(" a/", autocompleteEngine.getNextPrefix("filter a//Open"));
        assertEquals(" e/", autocompleteEngine.getNextPrefix("add n/Alice a/Open p/91234567 addr/Block A//B"));
    }

    @Test
    public void getNextPrefix_outOfOrderPrefixes_returnsFirstMissingTemplatePrefix() {
        // EP: non-sequential / out-of-order prefixes
        // Age is present, name is missing
        assertEquals(" n/", autocompleteEngine.getNextPrefix("add a/Under 20"));

        // Name and address are present, age is missing
        assertEquals(" a/", autocompleteEngine.getNextPrefix("add n/John addr/NUS"));

        // Name, age, email present, phone is missing
        assertEquals(" p/", autocompleteEngine.getNextPrefix("add n/John a/Under 20 e/j@e.com"));
    }

    @Test
    public void getNextPrefix_editCommand_returnsPrefixes() {
        // EP: edit command with index only
        assertEquals(" n/", autocompleteEngine.getNextPrefix("edit 1")); // Boundary value
        // EP: edit command with partial prefixes
        assertEquals(" a/", autocompleteEngine.getNextPrefix("edit 1 n/Alex"));
        // EP: edit command with all prefixes present
        assertEquals("",
                autocompleteEngine.getNextPrefix("edit 1 n/Alex a/Under 20 p/91234567 e/a@b.com addr/NUS"));
    }

    @Test
    public void getNextPrefix_missingOrNonNumericIndex_doesNotInsertPrefixes() {
        // EP: commands missing index or having placeholder index
        for (String command : List.of("edit", "remark", "tag", "untag")) {
            assertEquals("", autocompleteEngine.getNextPrefix(command));
            assertEquals("", autocompleteEngine.getNextPrefix(command + " "));
            assertEquals("", autocompleteEngine.getNextPrefix(command + " INDEX"));
            assertEquals("", autocompleteEngine.getNextPrefix(command + " n/Avery"));
        }
        // EP: command with numeric index containing leading zeros
        assertEquals(" n/", autocompleteEngine.getNextPrefix("EDIT 0001"));
    }

    @Test
    public void getNextPrefix_filterCommand_returnsPrefix() {
        // EP: filter command without prefix
        assertEquals("a/", autocompleteEngine.getNextPrefix("filter "));
        assertEquals(" a/", autocompleteEngine.getNextPrefix("filter"));
        // EP: filter command with prefix present
        assertEquals("", autocompleteEngine.getNextPrefix("filter a/Under 20"));
        assertEquals("", autocompleteEngine.getNextPrefix("filter A/Under 20"));
    }

    @Test
    public void getNextPrefix_remarkCommand_returnsPrefix() {
        // EP: remark command without prefix
        assertEquals("r/", autocompleteEngine.getNextPrefix("remark 1 "));
        assertEquals(" r/", autocompleteEngine.getNextPrefix("remark 1"));
        // EP: remark command with prefix present
        assertEquals("", autocompleteEngine.getNextPrefix("remark 1 r/Likes coffee"));
        assertEquals("", autocompleteEngine.getNextPrefix("remark 1 R/Likes coffee"));
    }
}
