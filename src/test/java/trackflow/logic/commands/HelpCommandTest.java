// @@author qiyuannn
package trackflow.logic.commands;
// @@author

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
// @@author qiyuannn
import static trackflow.logic.commands.CommandTestUtil.assertCommandSuccess;
import static trackflow.logic.commands.HelpCommand.SHOWING_HELP_MESSAGE;
// @@author

import java.util.List;

import org.junit.jupiter.api.Test;

// @@author qiyuannn
import trackflow.model.Model;
import trackflow.model.ModelManager;
// @@author

public class HelpCommandTest {
    private Model model = new ModelManager();
    private Model expectedModel = new ModelManager();

    // EP: valid help command execution producing help flag in command result
    @Test
    public void execute_help_success() {
        CommandResult expectedCommandResult = new CommandResult(SHOWING_HELP_MESSAGE, true, false);
        assertCommandSuccess(new HelpCommand(), model, expectedCommandResult, expectedModel);
    }

    // EP: help message contains instructions and usage strings for all commands
    @Test
    public void helpMessage_containsEveryCommandUsageAndInstructions() {
        for (String usage : HelpCommand.COMMAND_USAGES) {
            assertTrue(HelpCommand.HELP_MESSAGE.contains(usage));
        }
        assertTrue(HelpCommand.HELP_MESSAGE.startsWith(HelpCommand.HELP_INTRODUCTION));
        assertTrue(HelpCommand.HELP_MESSAGE.contains("square brackets mark optional fields"));
        assertTrue(HelpCommand.HELP_MESSAGE.contains("remark 1 r/"));
    }

    // EP: command usages list all 13 command words in defined order without duplicates
    @Test
    public void commandUsages_listEveryCommandOnceInOrder() {
        List<String> expected = List.of(AddCommand.COMMAND_WORD, EditCommand.COMMAND_WORD,
                DeleteCommand.COMMAND_WORD, ListAthleteCommand.COMMAND_WORD, FindCommand.COMMAND_WORD,
                FilterCommand.COMMAND_WORD, SortCommand.COMMAND_WORD, RemarkCommand.COMMAND_WORD,
                TagCommand.COMMAND_WORD, UntagCommand.COMMAND_WORD, ClearCommand.COMMAND_WORD,
                HelpCommand.COMMAND_WORD, ExitCommand.COMMAND_WORD);
        List<String> listed = HelpCommand.COMMAND_USAGES.stream()
                .map(usage -> usage.substring(0, usage.indexOf(':')))
                .toList();
        assertEquals(expected, listed);
    }

    // EP: commands accepting age categories document all valid categories
    @Test
    public void commandUsages_ageCategoryCommandsListAllCategories() {
        List<String> validCategories = List.of("Under 14", "Under 16", "Under 18", "Under 20", "Open");
        List<String> targetPrefixes = List.of(
                AddCommand.COMMAND_WORD + ":",
                EditCommand.COMMAND_WORD + ":",
                FilterCommand.COMMAND_WORD + ":");

        List<String> matchingUsages = HelpCommand.COMMAND_USAGES.stream()
                .filter(usage -> targetPrefixes.stream().anyMatch(usage::startsWith))
                .toList();

        assertEquals(targetPrefixes.size(), matchingUsages.size());
        assertTrue(HelpCommand.COMMAND_USAGES.contains(AddCommand.MESSAGE_USAGE));
        assertTrue(HelpCommand.COMMAND_USAGES.contains(EditCommand.MESSAGE_USAGE));
        assertTrue(HelpCommand.COMMAND_USAGES.contains(FilterCommand.MESSAGE_USAGE));

        assertTrue(AddCommand.MESSAGE_USAGE.contains(
                "a/AGE_CATEGORY (Under 14, Under 16, Under 18, Under 20, or Open)"));
        assertTrue(EditCommand.MESSAGE_USAGE.contains(
                "[a/AGE_CATEGORY (Under 14, Under 16, Under 18, Under 20, or Open)]"));
        assertTrue(FilterCommand.MESSAGE_USAGE.contains(
                "a/AGE_CATEGORY (Under 14, Under 16, Under 18, Under 20, or Open)"));

        for (String usage : matchingUsages) {
            for (String category : validCategories) {
                assertTrue(usage.contains(category));
            }
        }
        for (String category : validCategories) {
            assertTrue(HelpCommand.HELP_MESSAGE.contains(category));
        }
    }
}
