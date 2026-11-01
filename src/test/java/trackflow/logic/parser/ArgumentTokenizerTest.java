package trackflow.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ArgumentTokenizerTest {

    private final Prefix unknownPrefix = new Prefix("--u");
    private final Prefix pSlash = new Prefix("p/");
    private final Prefix dashT = new Prefix("-t");
    private final Prefix hatQ = new Prefix("^Q");

    private void assertPreamblePresent(ArgumentMultimap argMultimap, String expectedPreamble) {
        assertEquals(expectedPreamble, argMultimap.getPreamble());
    }

    private void assertPreambleEmpty(ArgumentMultimap argMultimap) {
        assertTrue(argMultimap.getPreamble().isEmpty());
    }

    /**
     * Asserts all the arguments in {@code argMultimap} with {@code prefix} match the {@code expectedValues}
     * and only the last value is returned upon calling {@code ArgumentMultimap#getValue(Prefix)}.
     */
    private void assertArgumentPresent(ArgumentMultimap argMultimap, Prefix prefix, String... expectedValues) {

        // Verify the last value is returned
        assertEquals(expectedValues[expectedValues.length - 1], argMultimap.getValue(prefix).get());

        // Verify the number of values returned is as expected
        assertEquals(expectedValues.length, argMultimap.getAllValues(prefix).size());

        // Verify all values returned are as expected and in order
        for (int i = 0; i < expectedValues.length; i++) {
            assertEquals(expectedValues[i], argMultimap.getAllValues(prefix).get(i));
        }
    }

    private void assertArgumentAbsent(ArgumentMultimap argMultimap, Prefix prefix) {
        assertFalse(argMultimap.getValue(prefix).isPresent());
    }

    @Test
    public void tokenize_noPrefixes_allTakenAsPreamble() {
        // EP: arguments string without registered prefixes taken entirely as preamble
        String argsString = "  some random string /t tag with leading and trailing spaces ";
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(argsString);

        // Same string expected as preamble, but leading/trailing spaces should be trimmed
        assertPreamblePresent(argMultimap, argsString.trim());

    }

    @Test
    public void tokenize_oneArgument() {
        // EP: single prefix with non-empty preamble
        String argsString = "  Some preamble string p/ Argument value ";
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(argsString, pSlash);
        assertPreamblePresent(argMultimap, "Some preamble string");
        assertArgumentPresent(argMultimap, pSlash, "Argument value");

        // EP: single prefix with empty preamble
        argsString = " p/   Argument value ";
        argMultimap = ArgumentTokenizer.tokenize(argsString, pSlash);
        assertPreambleEmpty(argMultimap); // Boundary value
        assertArgumentPresent(argMultimap, pSlash, "Argument value");

    }

    @Test
    public void tokenize_mixedCasePrefix_recognizedAsCanonicalPrefix() {
        // EP: uppercase prefix matching canonical lowercase prefix definition
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(" P/91234567", pSlash);

        assertPreambleEmpty(argMultimap); // Boundary value
        assertArgumentPresent(argMultimap, pSlash, "91234567");
    }

    @Test
    public void tokenize_prefixAtStartOrAfterWhitespace_recognized() {
        // EP: prefix at start of input or preceded by valid whitespace separators
        for (String separator : new String[] {
            "", // Boundary value
            " ", "\t", "\r\n", "\u2003", "\u00a0", "\u2007", "\u202f"
        }) {
            ArgumentMultimap arguments = ArgumentTokenizer.tokenize(separator + "P/123", pSlash);
            assertPreambleEmpty(arguments); // Boundary value
            assertArgumentPresent(arguments, pSlash, "123");
        }
    }

    @Test
    public void tokenize_escapedPrefixes_preservesLiteralValues() {
        // EP: escaped prefixes (double slash) decoded as literal text in preamble and argument value
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(
                "P//value p/Call p//office A//B ADDR//annex C:\\Training Mon/Tue", pSlash);

        assertPreamblePresent(arguments, "P/value");
        assertArgumentPresent(arguments, pSlash, "Call p/office A/B ADDR/annex C:\\Training Mon/Tue");
    }

    @Test
    public void tokenize_otherSlashesAndQuotes_preservesLiteralText() {
        // EP: URLs, UNC paths, quoted text, and solitary slashes preserved literally
        for (String value : new String[] {"https://example.com/a//b", "//server/share",
            "wrong//value", "C:\\Training", "\"Coach said go\"", "\"unfinished",
            "/" // Boundary value
        }) {
            assertArgumentPresent(ArgumentTokenizer.tokenize("p/ " + value, pSlash), pSlash, value);
        }
    }

    @Test
    public void tokenize_backslashesBeforePrefix_preservesAllBackslashes() {
        // EP: backslashes preceding potential prefix pattern preserved literally
        for (int count = 1; count <= 4; count++) {
            String backslashes = "\\".repeat(count);
            ArgumentMultimap arguments = ArgumentTokenizer.tokenize(
                    "p/" + backslashes + "A/B", pSlash);
            assertArgumentPresent(arguments, pSlash, backslashes + "A/B"); // Boundary value
        }
    }

    @Test
    public void tokenize_nullArguments_throwsNullPointerException() {
        // EP: null arguments string
        assertThrows(NullPointerException.class, () -> ArgumentTokenizer.tokenize(null, pSlash)); // Boundary value
        assertThrows(NullPointerException.class, () -> ArgumentTokenizer.tokenize(null)); // Boundary value
    }

    @Test
    public void tokenize_blankArguments_hasNoNullOrPhantomValues() {
        // EP: empty or whitespace-only input strings
        for (String input : List.of("", " ", "\t", "\r\n", "\f", "\u2003", "\u00a0", "\u2007", "\u202f",
                " \t\u2003\n")) {
            ArgumentMultimap arguments = ArgumentTokenizer.tokenize(input, pSlash);
            assertPreambleEmpty(arguments); // Boundary value
            assertArgumentAbsent(arguments, pSlash);
        }

        // EP: prefix with empty argument value
        assertArgumentPresent(ArgumentTokenizer.tokenize("p/", pSlash), pSlash, ""); // Boundary value

        // EP: prefix with whitespace-only argument value
        assertArgumentPresent(ArgumentTokenizer.tokenize("p/ \t\u2003", pSlash), pSlash, ""); // Boundary value

        // EP: prefix with literal string "null" value
        assertArgumentPresent(ArgumentTokenizer.tokenize("p/null", pSlash), pSlash, "null");
    }

    @Test
    public void tokenize_allLetterEscapesAtWhitespaceBoundaries_decodesOnceWithoutCreatingParameters() {
        // EP: escaped single-letter and multi-letter prefixes at whitespace boundaries
        // Build the expected text directly, independently of the tokenizer's regular expression.
        for (char letter = 'a'; letter <= 'z'; letter++) {
            for (String token : List.of(String.valueOf(letter), String.valueOf(Character.toUpperCase(letter)))) {
                assertEscapeBoundaries(token);
            }
        }
        assertEscapeBoundaries("addr");
        assertEscapeBoundaries("AdDr");
    }

    private void assertEscapeBoundaries(String token) {
        for (String boundary : List.of(" ", "\t", "\n", "\r\n", "\f", "\u2003", "\u00a0", "\u2007", "\u202f")) {
            for (int slashCount = 2; slashCount <= 5; slashCount++) {
                String entered = token + "/".repeat(slashCount) + "literal";
                String decoded = token + "/".repeat(slashCount - 1) + "literal";
                ArgumentMultimap arguments = ArgumentTokenizer.tokenize(
                        entered + boundary + "P/first" + boundary + entered + boundary + "p/last", pSlash);
                assertPreamblePresent(arguments, decoded);
                assertArgumentPresent(arguments, pSlash, "first" + boundary + decoded, "last");
            }
        }
    }

    @Test
    public void tokenize_slashesWithoutTokenBoundary_remainUnchanged() {
        // EP: slashes without preceding token boundary remain unescaped and literal
        for (String text : List.of("http://host/?n/value&p//other#addr//here",
                "file:///home/coach", "mailto:coach@example.com", "///server/share",
                "AA//B", "addrx//annex", "0A//B", "_A//B", "(A//B)", "\"A//B\"", "'A//B'",
                "\\A//B", "😀A//B", "\u200bA//B")) {
            assertArgumentPresent(ArgumentTokenizer.tokenize("p/ " + text, pSlash), pSlash, text);
        }
    }

    @Test
    public void tokenize_quotesDoNotProtectRealPrefixes() {
        // EP: real prefixes inside quotation marks recognized and tokenized
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize("p/\"first P/second\"", pSlash);
        assertArgumentPresent(arguments, pSlash, "\"first", "second\"");
    }

    @Test
    public void tokenize_longEscapedText_doesNotRescanOrDropSlashes() {
        // EP: extreme input length with consecutive slashes
        String entered = "A" + "/".repeat(100000) + "B"; // Boundary value
        String decoded = "A" + "/".repeat(99999) + "B";
        assertArgumentPresent(ArgumentTokenizer.tokenize("p/" + entered, pSlash), pSlash, decoded);

        // EP: high repetition of multiple escaped prefix tokens
        String manyEscapes = "A//B p///value addr////annex ".repeat(2000).strip();
        String expected = "A/B p//value addr///annex ".repeat(2000).strip();
        assertArgumentPresent(ArgumentTokenizer.tokenize("p/" + manyEscapes, pSlash), pSlash, expected);
    }

    @Test
    public void tokenize_multipleArguments() {
        // EP: subset of registered prefixes present in input
        // Only two arguments are present
        String argsString = "SomePreambleString -t dashT-Value p/pSlash value";
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(argsString, pSlash, dashT, hatQ);
        assertPreamblePresent(argMultimap, "SomePreambleString");
        assertArgumentPresent(argMultimap, pSlash, "pSlash value");
        assertArgumentPresent(argMultimap, dashT, "dashT-Value");
        assertArgumentAbsent(argMultimap, hatQ);

        // EP: all registered prefixes present in input
        // All three arguments are present
        argsString = "Different Preamble String ^Q111 -t dashT-Value p/pSlash value";
        argMultimap = ArgumentTokenizer.tokenize(argsString, pSlash, dashT, hatQ);
        assertPreamblePresent(argMultimap, "Different Preamble String");
        assertArgumentPresent(argMultimap, pSlash, "pSlash value");
        assertArgumentPresent(argMultimap, dashT, "dashT-Value");
        assertArgumentPresent(argMultimap, hatQ, "111");

        /* Also covers: Reusing of the tokenizer multiple times */

        // EP: empty string input verifying tokenizer state reset
        // Reuse tokenizer on an empty string to ensure ArgumentMultimap is correctly reset
        // (i.e. no stale values from the previous tokenizing remain)
        argsString = ""; // Boundary value
        argMultimap = ArgumentTokenizer.tokenize(argsString, pSlash, dashT, hatQ);
        assertPreambleEmpty(argMultimap); // Boundary value
        assertArgumentAbsent(argMultimap, pSlash);

        /* Also covers: testing for prefixes not specified as a prefix */

        // EP: unregistered prefix treated as part of preamble
        // Prefixes not previously given to the tokenizer should not return any values
        argsString = unknownPrefix + "some value";
        argMultimap = ArgumentTokenizer.tokenize(argsString, pSlash, dashT, hatQ);
        assertArgumentAbsent(argMultimap, unknownPrefix);
        assertPreamblePresent(argMultimap, argsString); // Unknown prefix is taken as part of preamble
    }

    @Test
    public void tokenize_multipleArgumentsWithRepeats() {
        // EP: repeated occurrences of prefixes, including empty values
        // Two arguments repeated, some have empty values
        String argsString = "SomePreambleString -t dashT-Value ^Q ^Q -t another dashT value p/ pSlash value -t";
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(argsString, pSlash, dashT, hatQ);
        assertPreamblePresent(argMultimap, "SomePreambleString");
        assertArgumentPresent(argMultimap, pSlash, "pSlash value");
        assertArgumentPresent(argMultimap, dashT, "dashT-Value", "another dashT value", ""); // Boundary value
        assertArgumentPresent(argMultimap, hatQ, "", ""); // Boundary value
    }

    @Test
    public void tokenize_multipleArgumentsJoined() {
        // EP: prefixes joined without whitespace separator (treated as preamble)
        String argsString = "SomePreambleStringp/ pSlash joined-tjoined -t not joined^Qjoined";
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(argsString, pSlash, dashT, hatQ);
        assertPreamblePresent(argMultimap, "SomePreambleStringp/ pSlash joined-tjoined");
        assertArgumentAbsent(argMultimap, pSlash);
        assertArgumentPresent(argMultimap, dashT, "not joined^Qjoined");
        assertArgumentAbsent(argMultimap, hatQ);
    }

    @Test
    public void equalsMethod() {
        Prefix aaa = new Prefix("aaa");

        // EP: reflexive equality (same object instance)
        assertEquals(aaa, aaa);

        // EP: symmetric equality (different instance with same prefix string)
        assertEquals(aaa, new Prefix("aaa"));

        // EP: different object type
        assertNotEquals(aaa, "aaa");

        // EP: different prefix string value
        assertNotEquals(aaa, new Prefix("aab"));
    }

}
