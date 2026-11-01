package trackflow.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import trackflow.commons.core.index.Index;
import trackflow.logic.parser.exceptions.ParseException;
import trackflow.model.person.Address;
import trackflow.model.person.AgeCategory;
import trackflow.model.person.Email;
import trackflow.model.person.Name;
import trackflow.model.person.Phone;
import trackflow.model.tag.Tag;

public class ParserUtilTest {
    private static final String INVALID_NAME = "R@chel";
    private static final String INVALID_PHONE = "911a";
    private static final String INVALID_ADDRESS = " ";
    private static final String INVALID_EMAIL = "example.com";
    private static final String INVALID_AGE_CATEGORY = "Under 15";
    private static final String INVALID_TAG = "#friend";

    private static final String VALID_NAME = "Rachel Walker";
    private static final String VALID_PHONE = "123456";
    private static final String VALID_ADDRESS = "123 Main Street #0505";
    private static final String VALID_EMAIL = "rachel@example.com";
    private static final String VALID_AGE_CATEGORY = "Under 16";
    private static final String VALID_TAG_1 = "friend";
    private static final String VALID_TAG_2 = "neighbour";

    private static final String WHITESPACE = " \t\r\n";

    // EP: null age category
    @Test
    public void parseAgeCategory_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseAgeCategory(null));
    }

    // EP: invalid age category (unsupported category name)
    @Test
    public void parseAgeCategory_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, AgeCategory.MESSAGE_CONSTRAINTS, () ->
                ParserUtil.parseAgeCategory(INVALID_AGE_CATEGORY));
    }

    @Test
    public void parseAgeCategory_validValue_returnsNormalizedAgeCategory() throws Exception {
        AgeCategory expected = new AgeCategory(VALID_AGE_CATEGORY);

        // EP: valid canonical age category without whitespace
        assertEquals(expected, ParserUtil.parseAgeCategory(VALID_AGE_CATEGORY));

        // EP: valid age category with whitespace and non-canonical case
        assertEquals(expected, ParserUtil.parseAgeCategory(WHITESPACE + "under   16" + WHITESPACE));
    }

    // EP: invalid index (alphanumeric with spaces)
    @Test
    public void parseIndex_invalidInput_throwsParseException() {
        assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () -> ParserUtil.parseIndex("10 a"));
    }

    @Test
    public void parseIndex_validInput_success() throws Exception {
        // EP: valid 1-based index without whitespace (minimum valid index)
        assertEquals(INDEX_FIRST_PERSON, ParserUtil.parseIndex("1")); // Boundary value

        // EP: valid index with leading and trailing ASCII whitespaces
        assertEquals(INDEX_FIRST_PERSON, ParserUtil.parseIndex("  1  "));

        // EP: valid index with leading zeros
        assertEquals(INDEX_FIRST_PERSON, ParserUtil.parseIndex("0001"));

        // EP: valid index surrounded by non-ASCII Unicode whitespaces
        for (String separator : List.of("\u00a0", "\u2003", "\u2007", "\u202f", "\u3000")) {
            assertEquals(INDEX_FIRST_PERSON, ParserUtil.parseIndex(separator + "0001" + separator));
        }
    }

    @Test
    public void parseIndex_extremeDigitSequences_validatesEntireInput() throws Exception {
        // EP: extreme leading zeros evaluating to valid index 1
        assertEquals(INDEX_FIRST_PERSON, ParserUtil.parseIndex("0".repeat(10000) + "1")); // Boundary value

        // EP: extreme digit sequences exceeding Integer.MAX_VALUE (capped at Integer.MAX_VALUE)
        assertEquals(Index.fromOneBased(Integer.MAX_VALUE), ParserUtil.parseIndex("9".repeat(10000)));
        assertEquals(Index.fromOneBased(Integer.MAX_VALUE), ParserUtil.parseIndex("2147483648")); // Boundary value

        // EP: maximum 32-bit signed integer boundary (Integer.MAX_VALUE)
        assertEquals(Index.fromOneBased(Integer.MAX_VALUE), ParserUtil.parseIndex("2147483647")); // Boundary value

        // EP: invalid extreme values and non-unsigned formats (zeros, trailing letters, signs, decimals)
        for (String invalid : new String[] {"0".repeat(10000), "9".repeat(10000) + "x", "-1", "+1", "1.0"}) {
            assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () ->
                    ParserUtil.parseIndex(invalid));
        }
    }

    // EP: randomized arbitrary-length digit sequences (zero, bounded positive integer, trailing non-digits)
    @Test
    public void parseIndex_randomDigitSequences_matchesBoundedBigInteger() throws Exception {
        Random random = new Random(2103);
        BigInteger maximumIndex = BigInteger.valueOf(Integer.MAX_VALUE);
        for (int sample = 0; sample < 500; sample++) {
            StringBuilder digits = new StringBuilder();
            for (int digit = 0, length = 1 + random.nextInt(100); digit < length; digit++) {
                digits.append(random.nextInt(10));
            }
            String input = digits.toString();
            BigInteger number = new BigInteger(input);
            if (number.signum() == 0) {
                assertThrows(ParseException.class, () -> ParserUtil.parseIndex(input));
            } else {
                assertEquals(number.min(maximumIndex).intValue(), ParserUtil.parseIndex(input).getOneBased());
            }
            assertThrows(ParseException.class, () -> ParserUtil.parseIndex(input + "x"));
        }
    }

    // EP: single-letter and multi-letter parameter prefixes with whitespace boundaries and slash escaping
    @Test
    public void verifyNoUnknownPrefixes_realAndEscapedTokens_useSameBoundaries() throws Exception {
        for (String boundary : List.of("", " ", "\t", "\r\n", "\f", "\u2003", "\u00a0", "\u2007", "\u202f")) {
            for (char letter = 'a'; letter <= 'z'; letter++) {
                String token = String.valueOf(letter);
                String unknown = boundary + token + "/value";
                assertThrows(ParseException.class, () ->
                        ParserUtil.verifyNoUnknownPrefixes(unknown, "usage"));
                ParserUtil.verifyNoUnknownPrefixes(unknown, "usage", new Prefix(token + "/"));
                ParserUtil.verifyNoUnknownPrefixes(boundary + token + "//value", "usage");
                ParserUtil.verifyNoUnknownPrefixes(boundary + token + "////value", "usage");
            }
            assertThrows(ParseException.class, () ->
                    ParserUtil.verifyNoUnknownPrefixes(boundary + "AdDr/value", "usage"));
            ParserUtil.verifyNoUnknownPrefixes(boundary + "AdDr/value", "usage", new Prefix("addr/"));
            ParserUtil.verifyNoUnknownPrefixes(boundary + "AdDr//value", "usage");
        }
    }

    // EP: escaped literal token followed by unescaped unknown prefix
    @Test
    public void verifyNoUnknownPrefixes_escapedTokenBeforeRealUnknown_stillRejectsUnknown() {
        for (String unknown : List.of("x/", "X/value", "addr/value", "r/value")) {
            assertThrows(ParseException.class, () ->
                    ParserUtil.verifyNoUnknownPrefixes("x//literal " + unknown, "usage"));
        }
    }

    @Test
    public void verifyNoUnknownPrefixes_urlsPathsAndPunctuation_areOrdinaryText() throws Exception {
        // EP: URLs, file paths, and punctuation containing slashes treated as ordinary text
        for (String text : List.of("https://example.com/n/value?p//other", "file:///home/coach",
                "x://host/path", "//server/share", "C:\\Training", "Mon/Tue", "wrong/value",
                "(x/value)", "\"x/value\"", "'x/value'", "\\x/value", "\u200bx/value")) {
            ParserUtil.verifyNoUnknownPrefixes(text, "usage");
        }

        // EP: whitespace inside quoted text introducing an unknown parameter prefix
        // Quotes are ordinary characters, so whitespace inside them still introduces a parameter.
        assertThrows(ParseException.class, () ->
                ParserUtil.verifyNoUnknownPrefixes("\"some text x/value\"", "usage"));
    }

    @Test
    public void parseIndex_nullAndNonAsciiDigits_areNotValidIndexes() {
        // EP: null index
        assertThrows(NullPointerException.class, () -> ParserUtil.parseIndex(null));

        // EP: empty string, non-ASCII digits, special numeric strings, and internal whitespace
        for (String input : List.of("", "null", "NaN", "Infinity", "\u0661", "\uff11", "1\u0000", "1\u200b",
                "1 0", "1\t0", "9".repeat(10000) + "\uff11")) {
            assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () ->
                    ParserUtil.parseIndex(input));
        }
    }

    // EP: null name string
    @Test
    public void parseName_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseName((String) null));
    }

    // EP: invalid name with disallowed symbol
    @Test
    public void parseName_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, Name.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseName(INVALID_NAME));
    }

    // EP: valid name without surrounding whitespace
    @Test
    public void parseName_validValueWithoutWhitespace_returnsName() throws Exception {
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(VALID_NAME));
    }

    // EP: valid name with leading and trailing whitespaces (trimmed)
    @Test
    public void parseName_validValueWithWhitespace_returnsTrimmedName() throws Exception {
        String nameWithWhitespace = WHITESPACE + VALID_NAME + WHITESPACE;
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(nameWithWhitespace));
    }

    // EP: null phone string
    @Test
    public void parsePhone_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parsePhone((String) null));
    }

    // EP: invalid phone containing non-digit characters
    @Test
    public void parsePhone_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, Phone.MESSAGE_CONSTRAINTS, () -> ParserUtil.parsePhone(INVALID_PHONE));
    }

    // EP: valid phone without surrounding whitespace
    @Test
    public void parsePhone_validValueWithoutWhitespace_returnsPhone() throws Exception {
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(VALID_PHONE));
    }

    // EP: valid phone with leading and trailing whitespaces (trimmed)
    @Test
    public void parsePhone_validValueWithWhitespace_returnsTrimmedPhone() throws Exception {
        String phoneWithWhitespace = WHITESPACE + VALID_PHONE + WHITESPACE;
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(phoneWithWhitespace));
    }

    // EP: null address string
    @Test
    public void parseAddress_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseAddress((String) null));
    }

    // EP: invalid address (whitespace only, trims to empty string)
    @Test
    public void parseAddress_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, Address.MESSAGE_CONSTRAINTS, () ->
                ParserUtil.parseAddress(INVALID_ADDRESS)); // Boundary value
    }

    // EP: invalid address containing Next Line character (\u0085)
    @Test
    public void parseAddress_nextLineCharacter_throwsParseException() {
        for (String address : List.of("\u0085", "\u0085House", "House\u0085", "Block\u0085House")) {
            assertThrows(ParseException.class, Address.MESSAGE_CONSTRAINTS, () ->
                    ParserUtil.parseAddress(address));
        }
    }

    // EP: valid address without surrounding whitespace
    @Test
    public void parseAddress_validValueWithoutWhitespace_returnsAddress() throws Exception {
        Address expectedAddress = new Address(VALID_ADDRESS);
        assertEquals(expectedAddress, ParserUtil.parseAddress(VALID_ADDRESS));
    }

    // EP: valid address with leading and trailing whitespaces (trimmed)
    @Test
    public void parseAddress_validValueWithWhitespace_returnsTrimmedAddress() throws Exception {
        String addressWithWhitespace = WHITESPACE + VALID_ADDRESS + WHITESPACE;
        Address expectedAddress = new Address(VALID_ADDRESS);
        assertEquals(expectedAddress, ParserUtil.parseAddress(addressWithWhitespace));
    }

    // EP: null email string
    @Test
    public void parseEmail_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseEmail((String) null));
    }

    // EP: invalid email (missing @ and local-part)
    @Test
    public void parseEmail_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, Email.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseEmail(INVALID_EMAIL));
    }

    // EP: valid email without surrounding whitespace
    @Test
    public void parseEmail_validValueWithoutWhitespace_returnsEmail() throws Exception {
        Email expectedEmail = new Email(VALID_EMAIL);
        assertEquals(expectedEmail, ParserUtil.parseEmail(VALID_EMAIL));
    }

    // EP: valid email with leading and trailing whitespaces (trimmed)
    @Test
    public void parseEmail_validValueWithWhitespace_returnsTrimmedEmail() throws Exception {
        String emailWithWhitespace = WHITESPACE + VALID_EMAIL + WHITESPACE;
        Email expectedEmail = new Email(VALID_EMAIL);
        assertEquals(expectedEmail, ParserUtil.parseEmail(emailWithWhitespace));
    }

    @Test
    public void parseEmail_pathologicalInput_throwsParseExceptionQuickly() {
        Duration timeout = Duration.ofMillis(200);

        // EP: pathological long string without @ symbol (ReDoS defense)
        assertTimeoutPreemptively(timeout, () ->
                assertThrows(ParseException.class, Email.MESSAGE_CONSTRAINTS, () ->
                        ParserUtil.parseEmail("a".repeat(5000))));

        // EP: pathological long domain label without top-level domain
        assertTimeoutPreemptively(timeout, () ->
                assertThrows(ParseException.class, Email.MESSAGE_CONSTRAINTS, () ->
                        ParserUtil.parseEmail("a@" + "b".repeat(5000))));

        // EP: pathological repeating subdomain sequence
        assertTimeoutPreemptively(timeout, () ->
                assertThrows(ParseException.class, Email.MESSAGE_CONSTRAINTS, () ->
                        ParserUtil.parseEmail("a@" + "a.".repeat(2500))));
    }

    // EP: email exceeding maximum allowed length (255 characters)
    @Test
    public void parseEmail_exceedsMaxLength_throwsParseException() {
        String exceedMaxEmail = "a@" + "b".repeat(251) + "cd"; // Boundary value
        assertThrows(ParseException.class, Email.MESSAGE_CONSTRAINTS, () ->
                ParserUtil.parseEmail(exceedMaxEmail));
    }

    // EP: null tag string
    @Test
    public void parseTag_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseTag(null));
    }

    // EP: invalid tag containing non-alphanumeric character
    @Test
    public void parseTag_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, Tag.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseTag(INVALID_TAG));
    }

    // EP: valid tag without surrounding whitespace
    @Test
    public void parseTag_validValueWithoutWhitespace_returnsTag() throws Exception {
        Tag expectedTag = new Tag(VALID_TAG_1);
        assertEquals(expectedTag, ParserUtil.parseTag(VALID_TAG_1));
    }

    // EP: valid tag with leading and trailing whitespaces (trimmed)
    @Test
    public void parseTag_validValueWithWhitespace_returnsTrimmedTag() throws Exception {
        String tagWithWhitespace = WHITESPACE + VALID_TAG_1 + WHITESPACE;
        Tag expectedTag = new Tag(VALID_TAG_1);
        assertEquals(expectedTag, ParserUtil.parseTag(tagWithWhitespace));
    }

    // EP: valid multi-word tag with consecutive inner whitespaces (collapsed to single space)
    @Test
    public void parseTag_repeatedInnerWhitespace_collapsesToSingleSpace() throws Exception {
        assertEquals("Long Jump", ParserUtil.parseTag("Long  \t Jump").tagName);
    }

    // EP: null tag collection
    @Test
    public void parseTags_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseTags(null));
    }

    // EP: tag collection containing an invalid tag
    @Test
    public void parseTags_collectionWithInvalidTags_throwsParseException() {
        assertThrows(ParseException.class, Tag.MESSAGE_CONSTRAINTS, () ->
                ParserUtil.parseTags(List.of(VALID_TAG_1, INVALID_TAG)));
    }

    // EP: empty tag collection (zero tags)
    @Test
    public void parseTags_emptyCollection_returnsEmptySet() throws Exception {
        assertTrue(ParserUtil.parseTags(List.of()).isEmpty()); // Boundary value
    }

    // EP: tag collection with multiple valid tags
    @Test
    public void parseTags_collectionWithValidTags_returnsTagSet() throws Exception {
        Set<Tag> actualTagSet = ParserUtil.parseTags(List.of(VALID_TAG_1, VALID_TAG_2));
        Set<Tag> expectedTagSet = Set.of(new Tag(VALID_TAG_1), new Tag(VALID_TAG_2));

        assertEquals(expectedTagSet, actualTagSet);
    }
}
