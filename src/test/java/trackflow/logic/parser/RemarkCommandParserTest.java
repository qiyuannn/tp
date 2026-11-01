package trackflow.logic.parser;

import static trackflow.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static trackflow.logic.parser.CliSyntax.PREFIX_REMARK;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseFailure;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import trackflow.commons.core.index.Index;
import trackflow.logic.Messages;
import trackflow.logic.commands.RemarkCommand;
import trackflow.model.person.Remark;

public class RemarkCommandParserTest {
    private final RemarkCommandParser parser = new RemarkCommandParser();
    private final String nonEmptyRemark = "Some remark.";

    @Test
    public void parse_indexSpecified_success() {
        // EP: valid index with non-empty remark value
        Index targetIndex = INDEX_FIRST_PERSON; // Boundary value
        String userInput = targetIndex.getOneBased() + " " + PREFIX_REMARK + nonEmptyRemark;
        RemarkCommand expectedCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(nonEmptyRemark));
        assertParseSuccess(parser, userInput, expectedCommand);

        // EP: valid index with empty remark value (clearing remark)
        userInput = targetIndex.getOneBased() + " " + PREFIX_REMARK;
        expectedCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark("")); // Boundary value
        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_missingCompulsoryField_failure() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);

        // EP: empty input or command word only without arguments
        assertParseFailure(parser, "", expectedMessage); // Boundary value
        assertParseFailure(parser, RemarkCommand.COMMAND_WORD, expectedMessage);

        // EP: missing index preamble (command word followed by remark text)
        assertParseFailure(parser, RemarkCommand.COMMAND_WORD + " " + nonEmptyRemark, expectedMessage);

        // EP: missing remark prefix (valid index without r/ prefix)
        assertParseFailure(parser, INDEX_FIRST_PERSON.getOneBased() + "", expectedMessage);
    }

    // EP: unknown prefix parameter accompanying valid remark
    @Test
    public void parse_unknownParameter_failure() {
        assertParseFailure(parser, "1 r/Note n/Alice",
                String.format(Messages.MESSAGE_UNKNOWN_PARAMETER, "n/", RemarkCommand.MESSAGE_USAGE)
                        + Messages.MESSAGE_LITERAL_PREFIX_HINT);
    }

    // EP: duplicate remark prefix occurrence
    @Test
    public void parse_repeatedRemarkPrefix_failure() {
        assertParseFailure(parser, "1 r/first r/second",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_REMARK));
    }

    // EP: uppercase remark prefix (case-insensitive prefix recognition)
    @Test
    public void parse_mixedCaseRemarkPrefix_success() {
        assertParseSuccess(parser, "1 R/Some remark.",
                new RemarkCommand(INDEX_FIRST_PERSON, new Remark(nonEmptyRemark)));
    }

    // EP: remark value containing forward slash character (not parsed as prefix delimiter)
    @Test
    public void parse_slashInRemark_success() {
        String remarkWithSlash = "Available Mon/Tue";
        assertParseSuccess(parser, "1 r/" + remarkWithSlash,
                new RemarkCommand(INDEX_FIRST_PERSON, new Remark(remarkWithSlash)));
    }
}
