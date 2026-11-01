package trackflow.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import trackflow.logic.parser.ParserUtil;

/**
 * Checks that displayed indexes remain usable command arguments under locales with non-ASCII numerals.
 */
public class PersonCardIndexTest {

    // EP: index formatting under non-ASCII numeral locales guaranteeing parser compatibility
    @Test
    public void formatDisplayedIndex_nonAsciiNumeralLocale_remainsAcceptedByParser() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            for (String language : new String[] {"ar-EG", "fa-IR", "th-TH"}) {
                Locale.setDefault(Locale.forLanguageTag(language));
                // EP: single-digit 1-based index formatted with leading zero
                assertEquals("01", PersonCard.formatDisplayedIndex(1)); // Boundary value

                // EP: double-digit 1-based index formatting
                assertEquals("12", PersonCard.formatDisplayedIndex(12));

                // EP: triple-digit 1-based index formatting
                assertEquals("123", PersonCard.formatDisplayedIndex(123));

                // EP: formatted index round-trip parsing via ParserUtil
                for (int index : new int[] {1, 12, 123}) {
                    assertEquals(index, ParserUtil.parseIndex(PersonCard.formatDisplayedIndex(index)).getOneBased());
                }
            }
        } finally {
            Locale.setDefault(previous);
        }
    }
}
