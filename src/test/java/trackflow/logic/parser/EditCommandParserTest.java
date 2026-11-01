// @@author qiyuannn
package trackflow.logic.parser;
// @@author

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static trackflow.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
// @@author
import static trackflow.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
// @@author qiyuannn
import static trackflow.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static trackflow.logic.commands.CommandTestUtil.INVALID_EMAIL_DESC;
import static trackflow.logic.commands.CommandTestUtil.INVALID_NAME_DESC;
import static trackflow.logic.commands.CommandTestUtil.INVALID_PHONE_DESC;
import static trackflow.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static trackflow.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
// @@author
import static trackflow.logic.commands.CommandTestUtil.VALID_ADDRESS_AMY;
// @@author qiyuannn
import static trackflow.logic.commands.CommandTestUtil.VALID_EMAIL_AMY;
import static trackflow.logic.commands.CommandTestUtil.VALID_NAME_AMY;
import static trackflow.logic.commands.CommandTestUtil.VALID_PHONE_AMY;
import static trackflow.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static trackflow.logic.parser.CliSyntax.PREFIX_AGE_CATEGORY;
import static trackflow.logic.parser.CliSyntax.PREFIX_EMAIL;
import static trackflow.logic.parser.CliSyntax.PREFIX_NAME;
import static trackflow.logic.parser.CliSyntax.PREFIX_PHONE;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseFailure;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
// @@author

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

// @@author qiyuannn
import trackflow.logic.Messages;
import trackflow.logic.commands.EditCommand;
import trackflow.model.person.Address;
import trackflow.model.person.AgeCategory;
import trackflow.model.person.Email;
import trackflow.model.person.Name;
import trackflow.model.person.Phone;
import trackflow.testutil.EditPersonDescriptorBuilder;
// @@author

public class EditCommandParserTest {

    private static final String MESSAGE_INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditCommand.MESSAGE_USAGE);

    private EditCommandParser parser = new EditCommandParser();

    @Test
    public void parse_missingParts_failure() {
        // EP: missing index preamble (field value without index)
        assertParseFailure(parser, VALID_NAME_AMY, MESSAGE_INVALID_FORMAT);

        // EP: valid index with zero field prefixes to edit (lower boundary of edited fields)
        assertParseFailure(parser, "1", EditCommand.MESSAGE_NOT_EDITED); // Boundary value

        // EP: empty input string
        assertParseFailure(parser, "", MESSAGE_INVALID_FORMAT); // Boundary value
    }

    @Test
    public void parse_invalidPreamble_failure() {
        // EP: invalid preamble (negative number, zero boundary, non-numeric string, extraneous words)
        for (String preamble : List.of("-5", "0", "abc", "1 some random string")) {
            assertParseFailure(parser, preamble + NAME_DESC_AMY, MESSAGE_INVALID_FORMAT);
        }

        // EP: unknown prefix parameter in preamble position
        assertParseFailure(parser, "1 i/string" + NAME_DESC_AMY,
                String.format(Messages.MESSAGE_UNKNOWN_PARAMETER, "i/", EditCommand.MESSAGE_USAGE)
                        + Messages.MESSAGE_LITERAL_PREFIX_HINT);
    }

    @Test
    public void parse_invalidValue_failure() {
        // EP: invalid field values containing illegal characters
        assertParseFailure(parser, "1" + INVALID_NAME_DESC, Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1" + INVALID_PHONE_DESC, Phone.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1" + INVALID_EMAIL_DESC, Email.MESSAGE_CONSTRAINTS);

        // EP: empty field values (boundary value for each prefix)
        assertParseFailure(parser, "1 n/", Name.MESSAGE_CONSTRAINTS); // Boundary value
        assertParseFailure(parser, "1 p/", Phone.MESSAGE_CONSTRAINTS); // Boundary value
        assertParseFailure(parser, "1 e/", Email.MESSAGE_CONSTRAINTS); // Boundary value

        // EP: invalid age category (empty string boundary, unsupported category, malformed string)
        for (String category : List.of("", "Under 15", "123 Main Street")) {
            assertParseFailure(parser, "1 a/" + category, AgeCategory.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_pathologicalEmail_throwsParseExceptionQuickly() {
        Duration timeout = Duration.ofMillis(200);

        // EP: pathological email missing '@' with extreme length (ReDoS boundary)
        assertTimeoutPreemptively(timeout, () ->
                assertParseFailure(parser, "1 e/" + "a".repeat(5000), Email.MESSAGE_CONSTRAINTS)); // Boundary value

        // EP: pathological email domain with extreme length (ReDoS boundary)
        assertTimeoutPreemptively(timeout, () ->
                assertParseFailure(parser, "1 e/a@" + "b".repeat(5000), Email.MESSAGE_CONSTRAINTS)); // Boundary value

        // EP: pathological email with repeated domain segments (ReDoS boundary)
        assertTimeoutPreemptively(timeout, () ->
                assertParseFailure(parser, "1 e/a@" + "a.".repeat(2500), Email.MESSAGE_CONSTRAINTS)); // Boundary value
    }

    // EP: all editable fields specified with valid index (all 5 fields present)
    @Test
    public void parse_allFieldsSpecified_success() {
        String input = "1" + PHONE_DESC_AMY + " a/Under 16" + EMAIL_DESC_AMY + ADDRESS_DESC_AMY + NAME_DESC_AMY;
        EditCommand expected = new EditCommand(INDEX_FIRST_PERSON, new EditPersonDescriptorBuilder()
                .withName(VALID_NAME_AMY).withAgeCategory("Under 16")
                .withPhone(VALID_PHONE_AMY).withEmail(VALID_EMAIL_AMY)
                .withAddress(VALID_ADDRESS_AMY).build());
        assertParseSuccess(parser, input, expected);
    }

    // EP: multiple fields specified (subset of editable fields)
    @Test
    public void parse_someFieldsSpecified_success() {
        EditCommand expected = new EditCommand(INDEX_FIRST_PERSON, new EditPersonDescriptorBuilder()
                .withPhone(VALID_PHONE_AMY).withEmail(VALID_EMAIL_AMY).build());
        assertParseSuccess(parser, "1" + PHONE_DESC_AMY + EMAIL_DESC_AMY, expected);
    }

    @Test
    public void parse_oneFieldSpecified_success() {
        // EP: exactly one field specified — name (minimum edited fields boundary)
        assertParseSuccess(parser, "1" + NAME_DESC_AMY, new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withName(VALID_NAME_AMY).build())); // Boundary value

        // EP: exactly one field specified — phone
        assertParseSuccess(parser, "1" + PHONE_DESC_AMY, new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withPhone(VALID_PHONE_AMY).build()));

        // EP: exactly one field specified — email
        assertParseSuccess(parser, "1" + EMAIL_DESC_AMY, new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withEmail(VALID_EMAIL_AMY).build()));

        // EP: exactly one field specified — each valid age category with whitespace and casing variations
        for (String category : List.of("Under 14", "Under 16", "Under 18", "Under 20", "Open")) {
            EditCommand expected = new EditCommand(INDEX_FIRST_PERSON,
                    new EditPersonDescriptorBuilder().withAgeCategory(category).build());
            assertParseSuccess(parser, "1 a/" + category, expected);
            assertParseSuccess(parser, "1 a/  " + category.toLowerCase(java.util.Locale.ROOT)
                    .replace(" ", "   ") + "  ", expected);
        }
    }

    @Test
    public void parse_repeatedFields_failure() {
        // EP: duplicate occurrence of a single prefix
        for (Prefix prefix : List.of(PREFIX_NAME, PREFIX_AGE_CATEGORY, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ADDRESS)) {
            assertParseFailure(parser, "1 " + prefix + " " + prefix + "invalid",
                    Messages.getErrorMessageForDuplicatePrefixes(prefix));
        }

        // EP: duplicate occurrences of multiple prefixes simultaneously
        assertParseFailure(parser, "1 n/Amy n/Bob a/Open a/Under 16 p/123 p/456 e/a@b.com e/b@b.com",
                Messages.getErrorMessageForDuplicatePrefixes(
                        PREFIX_NAME, PREFIX_AGE_CATEGORY, PREFIX_PHONE, PREFIX_EMAIL));
    }

    @Test
    public void parse_addressOnlyAndCombinedWithAge_success() {
        // EP: address field specified alone with whitespace padding
        assertParseSuccess(parser, "1 addr/  123 Main Street  ", new EditCommand(INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withAddress("123 Main Street").build()));

        // EP: address combined with age category in arbitrary prefix order
        for (String input : List.of("1 a/Under 16 addr/123 Main Street", "1 addr/123 Main Street a/Under 16")) {
            assertParseSuccess(parser, input, new EditCommand(INDEX_FIRST_PERSON,
                    new EditPersonDescriptorBuilder().withAddress("123 Main Street")
                            .withAgeCategory("Under 16").build()));
        }
    }

    @Test
    public void parse_blankAddress_failure() {
        // EP: empty address value
        assertParseFailure(parser, "1 addr/", Address.MESSAGE_CONSTRAINTS); // Boundary value

        // EP: blank / whitespace-only address value followed by another prefix
        assertParseFailure(parser, "1 addr/   a/Open", Address.MESSAGE_CONSTRAINTS);
    }

    // EP: unsupported prefix (r/ or t/) in standalone, trailing, and leading positions
    @Test
    public void parse_unsupportedFields_failure() {
        for (String unsupported : List.of("r/", "r/fast runner", "t/", "t/sprinter",
                "t/sprinter t/relay", "t/invalid*")) {
            String suppliedPrefix = unsupported.substring(0, 2);
            String expected = String.format(Messages.MESSAGE_UNKNOWN_PARAMETER,
                    suppliedPrefix, EditCommand.MESSAGE_USAGE) + Messages.MESSAGE_LITERAL_PREFIX_HINT;
            assertParseFailure(parser, "1 " + unsupported, expected);
            assertParseFailure(parser, "1 n/Amy " + unsupported, expected);
            assertParseFailure(parser, "1 " + unsupported + " a/Open", expected);
        }
    }
}
