package trackflow.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class EmailTest {

    // EP: null email
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Email(null));
    }

    // EP: invalid email (empty string)
    @Test
    public void constructor_invalidEmail_throwsIllegalArgumentException() {
        String invalidEmail = ""; // Boundary value
        assertThrows(IllegalArgumentException.class, Email.MESSAGE_CONSTRAINTS, () -> new Email(invalidEmail));
    }

    @Test
    public void isValidEmail() {
        // EP: null email
        assertThrows(NullPointerException.class, () -> Email.isValidEmail(null));

        // EP: blank / empty string
        assertFalse(Email.isValidEmail("")); // Boundary value
        assertFalse(Email.isValidEmail(" "));

        // EP: missing mandatory email parts (local-part, '@', or domain)
        assertFalse(Email.isValidEmail("@example.com"));
        assertFalse(Email.isValidEmail("peterjackexample.com"));
        assertFalse(Email.isValidEmail("peterjack@"));

        // EP: invalid characters or spaces in local-part or domain
        assertFalse(Email.isValidEmail("peterjack@-"));
        assertFalse(Email.isValidEmail("peterjack@exam_ple.com"));
        assertFalse(Email.isValidEmail("peter jack@example.com"));
        assertFalse(Email.isValidEmail("peterjack@exam ple.com"));
        assertFalse(Email.isValidEmail(" peterjack@example.com"));
        assertFalse(Email.isValidEmail("peterjack@example.com "));

        // EP: multiple '@' delimiters
        assertFalse(Email.isValidEmail("peterjack@@example.com"));
        assertFalse(Email.isValidEmail("peter@jack@example.com"));

        // EP: invalid hyphen or period placement at boundaries of local-part
        assertFalse(Email.isValidEmail("-peterjack@example.com"));
        assertFalse(Email.isValidEmail("peterjack-@example.com"));
        assertFalse(Email.isValidEmail("peter..jack@example.com"));

        // EP: invalid domain format (embedded '@', leading/trailing periods, leading/trailing hyphens)
        assertFalse(Email.isValidEmail("peterjack@example@com"));
        assertFalse(Email.isValidEmail("peterjack@.example.com"));
        assertFalse(Email.isValidEmail("peterjack@example.com."));
        assertFalse(Email.isValidEmail("peterjack@-example.com"));
        assertFalse(Email.isValidEmail("peterjack@example.com-"));

        // EP: top level domain label less than two characters
        assertFalse(Email.isValidEmail("peterjack@example.c")); // Boundary value

        // EP: valid email with permitted special characters in local-part (+, _, ., -)
        assertTrue(Email.isValidEmail("PeterJack_1190@example.com"));
        assertTrue(Email.isValidEmail("PeterJack.1190@example.com"));
        assertTrue(Email.isValidEmail("PeterJack+1190@example.com"));
        assertTrue(Email.isValidEmail("PeterJack-1190@example.com"));

        // EP: valid minimal email (shortest local-part and domain)
        assertTrue(Email.isValidEmail("a@bc")); // Boundary value

        // EP: valid typical emails (standard alphabetic, numeric, long names, subdomains)
        assertTrue(Email.isValidEmail("test@localhost"));
        assertTrue(Email.isValidEmail("123@145"));
        assertTrue(Email.isValidEmail("a1+be.d@example1.com"));
        assertTrue(Email.isValidEmail("peter_jack@very-very-very-long-example.com"));
        assertTrue(Email.isValidEmail("if.you.dream.it_you.can.do.it@example.com"));
        assertTrue(Email.isValidEmail("e1234567@u.nus.edu"));

        // EP: valid domain with hyphenated labels
        assertTrue(Email.isValidEmail("test@example.a-b"));
        assertTrue(Email.isValidEmail("test@example.a-b-c"));
        assertTrue(Email.isValidEmail("test@example.co-op"));

        // EP: invalid domain label hyphens (consecutive, trailing, or leading)
        assertFalse(Email.isValidEmail("test@example.a--b"));
        assertFalse(Email.isValidEmail("test@example.a-b-")); // Boundary value
        assertFalse(Email.isValidEmail("test@example.-a-b")); // Boundary value

        // EP: invalid local-part boundary special characters (leading / trailing ., +, _, -)
        assertFalse(Email.isValidEmail(".peterjack@example.com")); // Boundary value
        assertFalse(Email.isValidEmail("peterjack.@example.com")); // Boundary value
        assertFalse(Email.isValidEmail("+peterjack@example.com")); // Boundary value
        assertFalse(Email.isValidEmail("peterjack+@example.com")); // Boundary value
        assertFalse(Email.isValidEmail("_peterjack@example.com")); // Boundary value
        assertFalse(Email.isValidEmail("peterjack_@example.com")); // Boundary value

        // EP: invalid local-part consecutive special characters
        assertFalse(Email.isValidEmail("peter.+jack@example.com"));
        assertFalse(Email.isValidEmail("peter._jack@example.com"));
        assertFalse(Email.isValidEmail("peter+-jack@example.com"));
        assertFalse(Email.isValidEmail("peter__jack@example.com"));
        assertFalse(Email.isValidEmail("peter++jack@example.com"));

        // EP: invalid domain label hyphens and consecutive periods
        assertFalse(Email.isValidEmail("peterjack@sub.-domain.com")); // Boundary value
        assertFalse(Email.isValidEmail("peterjack@sub-.domain.com")); // Boundary value
        assertFalse(Email.isValidEmail("peterjack@sub--domain.com"));
        assertFalse(Email.isValidEmail("peterjack@example..com"));

        // EP: invalid domain length / missing components at boundary
        assertFalse(Email.isValidEmail("a@b")); // Boundary value
        assertFalse(Email.isValidEmail("@bc")); // Boundary value
        assertFalse(Email.isValidEmail("a@")); // Boundary value

        // EP: valid complex email structures (single domain label, multiple periods, all special chars, subdomains)
        assertTrue(Email.isValidEmail("peterjack@local-host"));
        assertTrue(Email.isValidEmail("a.b.c@example.com"));
        assertTrue(Email.isValidEmail("a+b-c_d.e@example.com"));
        assertTrue(Email.isValidEmail("test@sub-domain.example.com"));
    }

    // EP: pathological long inputs and adversarial backtracking patterns (ReDoS resilience)
    @Test
    public void isValidEmail_pathologicalInputs_evaluatesQuicklyWithoutStackOverflow() {
        Duration timeout = Duration.ofMillis(200);

        // Pathological long inputs
        String longLocalWithoutAt = "a".repeat(5000);
        String longDomain = "a@" + "b".repeat(5000);
        String longLocalWithSpecialChars = "a" + ".a".repeat(2500);
        String longDomainWithHyphens = "a@a" + "-a".repeat(2500) + ".com";
        String longDomainWithPeriods = "a@" + "a.".repeat(2500) + "com";

        // Adversarial backtracking patterns with trailing invalid characters
        String adversarialDomain = "a@" + "b".repeat(50) + "!";
        String adversarialDomainHyphens = "a@" + "b-b".repeat(25) + "-";
        String adversarialLocal = "a" + ".a".repeat(50) + "!";
        String adversarialNearMax = "a@" + "b".repeat(250) + "!";

        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(longLocalWithoutAt)));
        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(longDomain)));
        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(longLocalWithSpecialChars)));
        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(longDomainWithHyphens)));
        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(longDomainWithPeriods)));
        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(adversarialDomain)));
        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(adversarialDomainHyphens)));
        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(adversarialLocal)));
        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(adversarialNearMax)));

        // Verify VALIDATION_REGEX itself evaluates without StackOverflowError or freezing
        java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(Email.VALIDATION_REGEX);
        assertTimeoutPreemptively(timeout, () -> assertFalse(regexPattern.matcher(longLocalWithoutAt).matches()));
        assertTimeoutPreemptively(timeout, () ->
                assertFalse(regexPattern.matcher(longLocalWithSpecialChars).matches()));
        assertTimeoutPreemptively(timeout, () ->
                assertFalse(regexPattern.matcher("a@" + "a.".repeat(2500)).matches()));
        assertTimeoutPreemptively(timeout, () -> assertFalse(regexPattern.matcher(adversarialNearMax).matches()));
    }

    // EP: invalid Unicode characters and surrogate pairs in email
    @Test
    public void isValidEmail_unicodeInputs_rejectedQuickly() {
        Duration timeout = Duration.ofMillis(200);

        // Unicode and surrogate pair sequences in local-part and domain
        assertFalse(Email.isValidEmail("test\uD83D\uDE00@example.com"));
        assertFalse(Email.isValidEmail("\u4E2D\u6587@example.com"));
        assertFalse(Email.isValidEmail("user@\u4E2D\u6587.com"));
        assertFalse(Email.isValidEmail("user@example.\uD83D\uDE00"));
        assertFalse(Email.isValidEmail("a\u0300@example.com"));

        // Pathological Unicode inputs
        String longSurrogatePair = "\uD83D\uDE00".repeat(2500);
        String longCjk = "a@" + "\u4E2D".repeat(5000);
        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(longSurrogatePair)));
        assertTimeoutPreemptively(timeout, () -> assertFalse(Email.isValidEmail(longCjk)));
    }

    @Test
    public void isValidEmail_boundaryLengths() {
        // EP: maximum allowed email length boundary (254 characters)
        String maxValidEmail = "a@" + "b".repeat(250) + "cd";
        assertEquals(Email.MAX_LENGTH, maxValidEmail.length());
        assertTrue(Email.isValidEmail(maxValidEmail)); // Boundary value

        // EP: exceeding maximum allowed email length (255 characters)
        String exceedMaxEmail = "a@" + "b".repeat(251) + "cd";
        assertEquals(Email.MAX_LENGTH + 1, exceedMaxEmail.length());
        assertFalse(Email.isValidEmail(exceedMaxEmail)); // Boundary value
    }

    // EP: pathological long input to constructor (performance & exception verification)
    @Test
    public void constructor_pathologicalInput_throwsIllegalArgumentExceptionQuickly() {
        String longInput = "a".repeat(5000);
        assertTimeoutPreemptively(Duration.ofMillis(200), () ->
                assertThrows(IllegalArgumentException.class, Email.MESSAGE_CONSTRAINTS, () -> new Email(longInput)));
    }

    @Test
    public void equals() {
        Email email = new Email("valid@email");

        // EP: symmetric equality (different instance with same email value) & string representation
        assertTrue(email.equals(new Email("valid@email")));
        assertEquals(email.hashCode(), new Email("valid@email").hashCode());
        assertEquals("valid@email", email.toString());

        // EP: reflexive equality (same object instance)
        assertTrue(email.equals(email));

        // EP: null comparison
        assertFalse(email.equals(null));

        // EP: different object type
        assertFalse(email.equals(5.0f));

        // EP: different field value
        assertFalse(email.equals(new Email("other.valid@email")));
    }
}
