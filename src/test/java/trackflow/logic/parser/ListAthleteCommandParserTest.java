package trackflow.logic.parser;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseFailure;

import org.junit.jupiter.api.Test;

import trackflow.logic.commands.ListAthleteCommand;

public class ListAthleteCommandParserTest {

    private final ListAthleteCommandParser parser = new ListAthleteCommandParser();

    @Test
    public void parse_noArguments_returnsListAthleteCommand() throws Exception {
        // EP: empty arguments string
        assertInstanceOf(ListAthleteCommand.class, parser.parse("")); // Boundary value

        // EP: whitespace-only arguments string
        assertInstanceOf(ListAthleteCommand.class, parser.parse("   \t\n"));
    }

    @Test
    public void parse_argumentsPresent_throwsParseException() {
        // EP: unexpected preamble index argument
        assertParseFailure(parser, "1", ListAthleteCommand.MESSAGE_USAGE); // Boundary value

        // EP: unexpected prefix argument
        assertParseFailure(parser, " n/Avery Tan", ListAthleteCommand.MESSAGE_USAGE);
    }
}
