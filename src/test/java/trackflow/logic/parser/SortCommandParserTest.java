package trackflow.logic.parser;

import static trackflow.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static trackflow.logic.commands.SortCommand.SortField.ADDRESS;
import static trackflow.logic.commands.SortCommand.SortField.AGE;
import static trackflow.logic.commands.SortCommand.SortField.EMAIL;
import static trackflow.logic.commands.SortCommand.SortField.NAME;
import static trackflow.logic.commands.SortCommand.SortField.PHONE;
import static trackflow.logic.commands.SortCommand.SortOrder.ASCENDING;
import static trackflow.logic.commands.SortCommand.SortOrder.DESCENDING;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseFailure;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import trackflow.logic.commands.SortCommand;

public class SortCommandParserTest {

    private final SortCommandParser parser = new SortCommandParser();

    // EP: valid sort field without order parameter (defaults to ASCENDING)
    @Test
    public void parse_fieldOnly_defaultsToAscending() {
        assertParseSuccess(parser, "name", new SortCommand(NAME, ASCENDING));
        assertParseSuccess(parser, "age", new SortCommand(AGE, ASCENDING));
        assertParseSuccess(parser, "phone", new SortCommand(PHONE, ASCENDING));
        assertParseSuccess(parser, "email", new SortCommand(EMAIL, ASCENDING));
        assertParseSuccess(parser, "address", new SortCommand(ADDRESS, ASCENDING));
    }

    @Test
    public void parse_fieldAndOrder_returnsSortCommand() {
        // EP: valid sort field with ascending order ('asc' and 'ascending')
        assertParseSuccess(parser, "name asc", new SortCommand(NAME, ASCENDING));
        assertParseSuccess(parser, "age ascending", new SortCommand(AGE, ASCENDING));

        // EP: valid sort field with descending order ('desc' and 'descending')
        assertParseSuccess(parser, "phone desc", new SortCommand(PHONE, DESCENDING));
        assertParseSuccess(parser, "email descending", new SortCommand(EMAIL, DESCENDING));
    }

    // EP: mixed-case field and order with surrounding and internal whitespace
    @Test
    public void parse_mixedCaseAndWhitespace_returnsSortCommand() {
        assertParseSuccess(parser, "  AgE   DeSc  ", new SortCommand(AGE, DESCENDING));
    }

    @Test
    public void parse_unicodeWhitespace_validatesAllArguments() {
        for (String separator : List.of("\u00a0", "\u2003", "\u2007", "\u202f", "\u3000")) {
            // EP: valid field and order separated and surrounded by Unicode whitespace
            assertParseSuccess(parser, separator + "phone" + separator + "desc" + separator,
                    new SortCommand(PHONE, DESCENDING));
            String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortCommand.MESSAGE_USAGE);

            // EP: whitespace-only input using Unicode whitespace
            assertParseFailure(parser, separator, expectedMessage); // Boundary value

            // EP: too many arguments (> 2 tokens) separated by Unicode whitespace
            assertParseFailure(parser, "name" + separator + "asc" + separator + "extra", expectedMessage);
        }
    }

    @Test
    public void parse_missingOrExtraArguments_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortCommand.MESSAGE_USAGE);

        // EP: empty or whitespace-only input (missing sort field)
        assertParseFailure(parser, "   ", expectedMessage); // Boundary value

        // EP: extra arguments beyond field and order (> 2 tokens)
        assertParseFailure(parser, "name asc extra", expectedMessage);
    }

    // EP: unsupported sort field
    @Test
    public void parse_unknownField_throwsParseException() {
        assertParseFailure(parser, "height asc", String.format(SortCommand.MESSAGE_INVALID_FIELD, "height"));
    }

    // EP: unsupported sort order
    @Test
    public void parse_unknownOrder_throwsParseException() {
        assertParseFailure(parser, "name upwards", String.format(SortCommand.MESSAGE_INVALID_ORDER, "upwards"));
    }
}
