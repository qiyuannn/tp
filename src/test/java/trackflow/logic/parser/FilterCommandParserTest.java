// @@author Austin-Chia
package trackflow.logic.parser;

import static trackflow.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseFailure;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import trackflow.logic.Messages;
import trackflow.logic.commands.FilterCommand;
import trackflow.model.person.AgeCategory;

public class FilterCommandParserTest {
    private final FilterCommandParser parser = new FilterCommandParser();

    @Test
    public void parse_supportedCategories_success() {
        // EP: all valid supported age categories
        for (String category : new String[] {"Under 14", "Under 16", "Under 18", "Under 20", "Open"}) {
            assertParseSuccess(parser, " a/" + category, new FilterCommand(new AgeCategory(category)));
        }

        // EP: valid category with case variation and internal/surrounding whitespace
        assertParseSuccess(parser, "  a/uNdEr   14  ", new FilterCommand(new AgeCategory("Under 14")));
        // @@author

        // EP: uppercase age category prefix (case-insensitive prefix)
        assertParseSuccess(parser, "  A/OPEN  ", new FilterCommand(new AgeCategory("Open")));
    }

    // EP: empty input, whitespace-only input, missing prefix, or unexpected non-empty preamble
    @Test
    public void parse_missingPrefixOrUnexpectedPreamble_failure() {
        for (String input : new String[] {"", "  ", " Under 14", " extra a/Open"}) {
            assertParseFailure(parser, input,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE));
        }
    }

    @Test
    public void parse_invalidCategoryOrExtraArguments_failure() {
        // EP: invalid age category values (empty value, unsupported category, partial name, extra tokens)
        for (String input : new String[] {"", "Under 15", "14", "Under", "Open extra", "Under 14, Under 16"}) {
            assertParseFailure(parser, " a/" + input, AgeCategory.MESSAGE_CONSTRAINTS);
        }

        // EP: disallowed or unknown parameter prefixes
        for (String prefix : new String[] {"n/", "t/", "x/"}) {
            assertParseFailure(parser, " a/Open " + prefix + "other",
                    String.format(Messages.MESSAGE_UNKNOWN_PARAMETER, prefix, FilterCommand.MESSAGE_USAGE));
        }
    }

    // EP: duplicate age category prefix
    @Test
    public void parse_duplicatePrefix_failure() {
        assertParseFailure(parser, " a/Open a/Under 14",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_AGE_CATEGORY));
    }
}
