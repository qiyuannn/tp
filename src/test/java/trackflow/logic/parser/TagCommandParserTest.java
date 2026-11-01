package trackflow.logic.parser;

import static trackflow.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static trackflow.logic.Messages.MESSAGE_UNKNOWN_PARAMETER;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseFailure;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static trackflow.model.util.SampleDataUtil.getTagSet;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import trackflow.logic.commands.TagCommand;
import trackflow.model.tag.Tag;

public class TagCommandParserTest {

    private static final String MESSAGE_INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, TagCommand.MESSAGE_USAGE);

    private final TagCommandParser parser = new TagCommandParser();

    @Test
    public void parse_singleEvent_success() {
        // EP: valid index and single tag
        assertParseSuccess(parser, "1 t/100m", new TagCommand(INDEX_FIRST_PERSON, getTagSet("100m"))); // Boundary value
    }

    @Test
    public void parse_multipleEventsWithSpacesAndHyphens_success() {
        // EP: valid index with whitespace padding and multiple tags with spaces and hyphens
        assertParseSuccess(parser, " 1 t/Long Jump t/110m-Hurdles t/4x100m ", // Boundary value
                new TagCommand(INDEX_FIRST_PERSON, getTagSet("Long Jump", "110m-Hurdles", "4x100m")));
    }

    @Test
    public void parse_nonEnglishLetters_success() {
        // EP: valid index and tag with non-ASCII accented letters
        assertParseSuccess(parser, "1 t/Saut-Élan", // Boundary value
                new TagCommand(INDEX_FIRST_PERSON, getTagSet("Saut-Élan")));
    }

    @Test
    public void parse_apostropheAndDecimalPoint_success() {
        // EP: valid index and tags containing apostrophe and decimal point
        assertParseSuccess(parser, "1 t/Women's 100m t/Hammer 7.26kg", // Boundary value
                new TagCommand(INDEX_FIRST_PERSON, getTagSet("Women's 100m", "Hammer 7.26kg")));
    }

    @Test
    public void parse_accentTypedAsSeparateMark_success() {
        // EP: valid index and tag with decomposed Unicode combining characters
        assertParseSuccess(parser, "1 t/Saut-Élan", // Boundary value
                new TagCommand(INDEX_FIRST_PERSON, getTagSet("Saut-Élan")));
    }

    @Test
    public void parse_extraWhitespaceInEvent_collapsed() {
        // EP: valid index and tag with redundant internal/trailing whitespace collapsed
        assertParseSuccess(parser, "1 t/  Long   Jump  ", // Boundary value
                new TagCommand(INDEX_FIRST_PERSON, getTagSet("Long Jump")));
    }

    @Test
    public void parse_repeatedEventDifferentCase_treatedAsOne() {
        // EP: valid index and duplicate tags differing only by case deduplicated
        assertParseSuccess(parser, "1 t/100m t/100M", // Boundary value
                new TagCommand(INDEX_FIRST_PERSON, getTagSet("100m")));
    }

    @Test
    public void parse_mixedCasePrefix_success() {
        // EP: valid index and uppercase tag prefix
        assertParseSuccess(parser, "1 T/100m", new TagCommand(INDEX_FIRST_PERSON, getTagSet("100m"))); // Boundary value
    }

    @Test
    public void parse_missingParts_failure() {
        // EP: empty arguments string
        // no arguments
        assertParseFailure(parser, "", MESSAGE_INVALID_FORMAT); // Boundary value

        // EP: missing preamble index
        // no index
        assertParseFailure(parser, "t/100m", MESSAGE_INVALID_FORMAT);

        // EP: missing tag prefix
        // no event prefix
        assertParseFailure(parser, "1", MESSAGE_INVALID_FORMAT); // Boundary value

        // EP: preamble index followed by un-prefixed arguments
        // event without prefix
        assertParseFailure(parser, "1 100m", MESSAGE_INVALID_FORMAT); // Boundary value
    }

    @Test
    public void parse_invalidIndex_failure() {
        // EP: zero index (boundary just below 1)
        assertParseFailure(parser, "0 t/100m", MESSAGE_INVALID_FORMAT); // Boundary value

        // EP: negative index
        assertParseFailure(parser, "-1 t/100m", MESSAGE_INVALID_FORMAT); // Boundary value

        // EP: non-numeric index string
        assertParseFailure(parser, "one t/100m", MESSAGE_INVALID_FORMAT);
    }

    @Test
    public void parse_invalidEvent_failure() {
        // EP: empty tag argument value
        // empty event
        assertParseFailure(parser, "1 t/", Tag.MESSAGE_CONSTRAINTS); // Boundary value

        // EP: tag containing invalid symbols
        // invalid symbol
        assertParseFailure(parser, "1 t/100m*", Tag.MESSAGE_CONSTRAINTS);

        // EP: combination of valid tag and empty tag
        // one valid and one invalid event
        assertParseFailure(parser, "1 t/100m t/", Tag.MESSAGE_CONSTRAINTS); // Boundary value

        // EP: tag length exceeding maximum limit (Tag.MAX_LENGTH + 1)
        // too long
        assertParseFailure(parser, "1 t/" + "a".repeat(Tag.MAX_LENGTH + 1), Tag.MESSAGE_CONSTRAINTS); // Boundary value

        // EP: tag containing isolated hyphen surrounded by spaces
        // separators that are not a single space or a single hyphen
        assertParseFailure(parser, "1 t/Long - Jump", Tag.MESSAGE_CONSTRAINTS);

        // EP: tag containing consecutive hyphens
        assertParseFailure(parser, "1 t/Long--Jump", Tag.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_unknownPrefix_failure() {
        // EP: unrecognized prefix present in input
        assertParseFailure(parser, "1 t/100m n/Amy",
                String.format(MESSAGE_UNKNOWN_PARAMETER, "n/", TagCommand.MESSAGE_USAGE));
    }
}
