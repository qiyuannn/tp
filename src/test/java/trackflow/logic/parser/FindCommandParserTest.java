package trackflow.logic.parser;

import static trackflow.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static trackflow.logic.parser.CliSyntax.PREFIX_NAME;
import static trackflow.logic.parser.CliSyntax.PREFIX_TAG;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseFailure;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import trackflow.logic.Messages;
import trackflow.logic.commands.FindCommand;
import trackflow.model.person.PersonContainsKeywordsPredicate;
import trackflow.model.person.PersonContainsKeywordsPredicate.SearchField;

public class FindCommandParserTest {

    private FindCommandParser parser = new FindCommandParser();

    // EP: empty or whitespace-only input
    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "     ", // Boundary value
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_validArgs_returnsFindCommand() {
        // EP: valid broad keywords without leading/trailing whitespaces
        // no leading and trailing whitespaces
        FindCommand expectedFindCommand =
                new FindCommand(new PersonContainsKeywordsPredicate(List.of("Alice", "9123")));
        assertParseSuccess(parser, "Alice 9123", expectedFindCommand);

        // EP: valid broad keywords with multiple whitespaces between and around keywords
        // multiple whitespaces between keywords
        assertParseSuccess(parser, " \n Alice \n \t 9123  \t", expectedFindCommand);
    }

    @Test
    public void parse_fieldScopedArgs_returnsFindCommand() {
        PersonContainsKeywordsPredicate predicate = new PersonContainsKeywordsPredicate(List.of(), Map.of(
                SearchField.NAME, List.of("und"),
                SearchField.AGE_CATEGORY, List.of("Open"),
                SearchField.PHONE, List.of("9123"),
                SearchField.ADDRESS, List.of("Main Street"),
                SearchField.REMARK, List.of("injured")));

        // EP: multiple field-scoped arguments with lowercase prefixes
        assertParseSuccess(parser, " n/und a/Open p/9123 addr/Main Street r/injured", new FindCommand(predicate));

        // EP: multiple field-scoped arguments with uppercase prefixes (case-insensitivity)
        assertParseSuccess(parser, " N/und A/Open P/9123 ADDR/Main Street R/injured", new FindCommand(predicate));
    }

    // EP: combination of broad keywords and field-scoped prefixes
    @Test
    public void parse_broadAndFieldScopedArgs_returnsFindCommand() {
        PersonContainsKeywordsPredicate predicate = new PersonContainsKeywordsPredicate(
                List.of("avery", "9123"), Map.of(
                        SearchField.NAME, List.of("Avery"),
                        SearchField.EMAIL, List.of("example.com")));

        assertParseSuccess(parser, " avery 9123 n/Avery e/example.com", new FindCommand(predicate));
    }

    // EP: empty field value after prefix
    @Test
    public void parse_emptyFieldValue_throwsParseException() {
        assertParseFailure(parser, " n/", // Boundary value
                String.format(FindCommand.MESSAGE_EMPTY_FIELD, PREFIX_NAME, FindCommand.MESSAGE_USAGE));
    }

    // EP: duplicate field prefix
    @Test
    public void parse_repeatedFieldPrefix_throwsParseException() {
        assertParseFailure(parser, " n/avery n/tan",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME));
    }

    // EP: unknown parameter prefix
    @Test
    public void parse_unknownParameter_throwsParseException() {
        assertParseFailure(parser, " x/value",
                String.format(Messages.MESSAGE_UNKNOWN_PARAMETER, "x/", FindCommand.MESSAGE_USAGE)
                        + Messages.MESSAGE_LITERAL_PREFIX_HINT);
    }

    @Test
    public void parse_eventScopedArgs_returnsFindCommand() {
        PersonContainsKeywordsPredicate predicate = new PersonContainsKeywordsPredicate(List.of(),
                Map.of(SearchField.EVENT, List.of("long jump")));

        // EP: valid tag/event prefix with lowercase 't/'
        assertParseSuccess(parser, " t/long jump", new FindCommand(predicate));

        // EP: valid tag/event prefix with uppercase 'T/' (case-insensitivity)
        assertParseSuccess(parser, " T/long jump", new FindCommand(predicate));

        // EP: empty tag/event field value
        assertParseFailure(parser, " t/", // Boundary value
                String.format(FindCommand.MESSAGE_EMPTY_FIELD, PREFIX_TAG, FindCommand.MESSAGE_USAGE));

        // EP: duplicate tag/event prefix
        assertParseFailure(parser, " t/100m t/200m",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TAG));
    }

    // EP: slash character inside scoped remark argument treated as text
    @Test
    public void parse_slashInScopedRemark_returnsFindCommand() {
        PersonContainsKeywordsPredicate predicate = new PersonContainsKeywordsPredicate(List.of(),
                Map.of(SearchField.REMARK, List.of("Available Mon/Tue")));

        assertParseSuccess(parser, " r/Available Mon/Tue", new FindCommand(predicate));
    }

}
