// @@author qiyuannn
package trackflow.logic.parser;
// @@author

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static trackflow.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseFailure;
import static trackflow.logic.parser.CommandParserTestUtil.assertParseSuccess;
// @@author

import java.time.Duration;

import org.junit.jupiter.api.Test;

// @@author qiyuannn
import trackflow.logic.Messages;
import trackflow.logic.commands.AddCommand;
import trackflow.model.person.Address;
import trackflow.model.person.AgeCategory;
import trackflow.model.person.Email;
import trackflow.model.person.Name;
import trackflow.model.person.Person;
import trackflow.model.person.Phone;
import trackflow.testutil.PersonBuilder;
// @@author

public class AddCommandParserTest {
    private final AddCommandParser parser = new AddCommandParser();
    private final String validArgs = " n/Avery Tan a/Under 14 p/91234567 e/avery.tan@example.com"
            + " addr/123 Main Street";

    @Test
    public void parse_allFieldsPresent_success() {
        Person expected = new PersonBuilder().withName("Avery Tan").withAgeCategory("Under 14")
                .withPhone("91234567").withEmail("avery.tan@example.com")
                .withAddress("123 Main Street").withRemark("").withTags().build();

        // EP: all compulsory fields present in standard canonical order
        assertParseSuccess(parser, validArgs, new AddCommand(expected));

        // EP: compulsory fields in non-canonical order with irregular whitespace and lowercase age category
        assertParseSuccess(parser, "  e/avery.tan@example.com p/91234567 a/under   14"
                + " addr/123 Main Street n/Avery Tan  ",
                new AddCommand(expected));

        // EP: compulsory fields with case-insensitive uppercase prefixes
        assertParseSuccess(parser, "  E/avery.tan@example.com P/91234567 A/under 14"
                + " ADDR/123 Main Street N/Avery Tan  ",
                new AddCommand(expected));
    }

    // EP: address prefix positioned first before other compulsory fields
    @Test
    public void parse_addressInAnyOrder_success() {
        Person expected = new PersonBuilder().withName("Avery Tan").withAgeCategory("Under 14")
                .withPhone("91234567").withEmail("avery.tan@example.com")
                .withAddress("123 Main Street").withRemark("").withTags().build();
        assertParseSuccess(parser, " addr/123 Main Street" + validArgs.replace(" addr/123 Main Street", ""),
                new AddCommand(expected));
    }

    @Test
    public void parse_invalidAddress_failure() {
        // EP: address prefix with empty value
        assertParseFailure(parser, validArgs.replace("addr/123 Main Street", "addr/"), // Boundary value
                Address.MESSAGE_CONSTRAINTS);

        // EP: blank / whitespace-only address value
        assertParseFailure(parser, validArgs.replace("addr/123 Main Street", "addr/   "),
                Address.MESSAGE_CONSTRAINTS);
    }

    // EP: each valid discrete age category ("Under 14", "Under 16", "Under 18", "Under 20", "Open")
    @Test
    public void parse_eachAgeCategory_success() {
        for (String category : new String[] {"Under 14", "Under 16", "Under 18", "Under 20", "Open"}) {
            Person expected = new PersonBuilder().withName("Avery Tan").withAgeCategory(category)
                    .withPhone("91234567").withEmail("avery.tan@example.com")
                    .withAddress("123 Main Street").withRemark("").withTags().build();
            assertParseSuccess(parser, validArgs.replace("Under 14", category), new AddCommand(expected));
        }
    }

    @Test
    public void parse_missingField_failure() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE);

        // EP: missing compulsory field prefix (tested individually)
        for (String field : new String[] {" n/Avery Tan", " a/Under 14", " p/91234567", " e/avery.tan@example.com",
            " addr/123 Main Street"}) {
            assertParseFailure(parser, validArgs.replace(field, ""), expected);
        }

        // EP: non-empty preamble before command arguments
        assertParseFailure(parser, "unexpected" + validArgs, expected);
    }

    // EP: unknown prefix parameter
    @Test
    public void parse_unknownParameter_failure() {
        assertParseFailure(parser, validArgs + " x/value",
                String.format(Messages.MESSAGE_UNKNOWN_PARAMETER, "x/", AddCommand.MESSAGE_USAGE)
                        + Messages.MESSAGE_LITERAL_PREFIX_HINT);
    }

    // EP: forward slash inside free-text address value (not parsed as prefix delimiter)
    @Test
    public void parse_slashInFreeText_success() {
        Person expected = new PersonBuilder().withName("Avery Tan").withAgeCategory("Under 14")
                .withPhone("91234567").withEmail("avery.tan@example.com")
                .withAddress("Level Two/Three").withRemark("").withTags().build();

        assertParseSuccess(parser, validArgs.replace("123 Main Street", "Level Two/Three"), new AddCommand(expected));
    }

    // EP: duplicate prefix occurrence for non-repeatable fields
    @Test
    public void parse_repeatedField_failure() {
        for (Prefix prefix : new Prefix[] {CliSyntax.PREFIX_NAME, CliSyntax.PREFIX_AGE_CATEGORY,
            CliSyntax.PREFIX_PHONE, CliSyntax.PREFIX_EMAIL, CliSyntax.PREFIX_ADDRESS}) {
            assertParseFailure(parser, validArgs + " " + prefix + "invalid",
                    Messages.getErrorMessageForDuplicatePrefixes(prefix));
        }
    }

    @Test
    public void parse_invalidValue_failure() {
        // EP: invalid name (empty string boundary vs disallowed characters)
        assertParseFailure(parser, validArgs.replace("Avery Tan", ""), Name.MESSAGE_CONSTRAINTS); // Boundary value
        assertParseFailure(parser, validArgs.replace("Avery Tan", "Avery*Tan"), Name.MESSAGE_CONSTRAINTS);

        // EP: invalid age category (empty string boundary vs unsupported category)
        assertParseFailure(parser, validArgs.replace("Under 14", ""), // Boundary value
                AgeCategory.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, validArgs.replace("Under 14", "Under 15"), AgeCategory.MESSAGE_CONSTRAINTS);

        // EP: invalid phone (empty string boundary vs less than 3 digits boundary)
        assertParseFailure(parser, validArgs.replace("91234567", ""), Phone.MESSAGE_CONSTRAINTS); // Boundary value
        assertParseFailure(parser, validArgs.replace("91234567", "12"), Phone.MESSAGE_CONSTRAINTS); // Boundary value

        // EP: invalid email (empty string boundary vs missing format components)
        assertParseFailure(parser, validArgs.replace("avery.tan@example.com", ""), // Boundary value
                Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, validArgs.replace("avery.tan@example.com", "invalid"), Email.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_unsupportedParameter_failure() {
        String tagError = String.format(Messages.MESSAGE_UNKNOWN_PARAMETER, "t/", AddCommand.MESSAGE_USAGE)
                + Messages.MESSAGE_LITERAL_PREFIX_HINT;
        String remarkError = String.format(Messages.MESSAGE_UNKNOWN_PARAMETER, "r/", AddCommand.MESSAGE_USAGE)
                + Messages.MESSAGE_LITERAL_PREFIX_HINT;

        // EP: unsupported tag prefix with non-empty value
        assertParseFailure(parser, validArgs + " t/sprints", tagError);

        // EP: unsupported remark prefix with non-empty value
        assertParseFailure(parser, validArgs + " r/note", remarkError);

        // EP: unsupported tag prefix with empty value boundary
        assertParseFailure(parser, validArgs + " t/", tagError); // Boundary value

        // EP: unsupported remark prefix with empty value boundary
        assertParseFailure(parser, validArgs + " r/", remarkError); // Boundary value

        // EP: multiple unsupported prefixes preceding valid command arguments
        assertParseFailure(parser, " t/sprints t/relay" + validArgs, tagError);
    }

    @Test
    public void parse_pathologicalEmail_throwsParseExceptionQuickly() {
        Duration timeout = Duration.ofMillis(200);

        // EP: pathological email missing '@' with extreme length (ReDoS boundary)
        assertTimeoutPreemptively(timeout, () ->
                assertParseFailure(parser,
                        validArgs.replace("avery.tan@example.com", "a".repeat(5000)), // Boundary value
                        Email.MESSAGE_CONSTRAINTS));

        // EP: pathological email domain with extreme length (ReDoS boundary)
        assertTimeoutPreemptively(timeout, () ->
                assertParseFailure(parser,
                        validArgs.replace("avery.tan@example.com", "a@" + "b".repeat(5000)), // Boundary value
                        Email.MESSAGE_CONSTRAINTS));

        // EP: pathological email with repeated domain segments (ReDoS boundary)
        assertTimeoutPreemptively(timeout, () ->
                assertParseFailure(parser,
                        validArgs.replace("avery.tan@example.com", "a@" + "a.".repeat(2500)), // Boundary value
                        Email.MESSAGE_CONSTRAINTS));
    }
}
