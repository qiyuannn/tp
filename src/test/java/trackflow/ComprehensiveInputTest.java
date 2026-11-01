package trackflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.logic.AutocompleteEngine;
import trackflow.logic.Logic;
import trackflow.logic.LogicManager;
import trackflow.logic.commands.CommandResult;
import trackflow.logic.commands.exceptions.CommandException;
import trackflow.logic.parser.exceptions.ParseException;
import trackflow.model.Model;
import trackflow.model.ModelManager;
import trackflow.model.ReadOnlyRoster;
import trackflow.model.person.Person;
import trackflow.storage.JsonRosterStorage;
import trackflow.storage.JsonUserPrefsStorage;
import trackflow.storage.StorageManager;

/**
 * Automated test runner that executes comprehensive input testing
 * across commands, syntax forms, edge cases, boundaries, and validation rules.
 */
public class ComprehensiveInputTest {

    @TempDir
    public Path tempDir;

    private Logic logic;
    private Model model;
    private StorageManager storage;
    private Path rosterPath;
    private Path prefsPath;
    private AutocompleteEngine autocompleteEngine;
    private final List<TestRecord> records = new ArrayList<>();

    /**
     * Represents a single test invocation and its result.
     */
    public static class TestRecord {
        public final String category;
        public final String input;
        public final boolean successExpected;
        public final boolean succeeded;
        public final String output;
        public final boolean matchesExpectation;

        /**
         * Constructs a test record.
         */
        public TestRecord(String category, String input, boolean successExpected, boolean succeeded, String output) {
            this.category = category;
            this.input = input;
            this.successExpected = successExpected;
            this.succeeded = succeeded;
            this.output = output;
            this.matchesExpectation = (successExpected == succeeded);
        }
    }

    @BeforeEach
    public void setUp() {
        rosterPath = tempDir.resolve("trackflow.json");
        prefsPath = tempDir.resolve("userPrefs.json");
        storage = new StorageManager(
                new JsonRosterStorage(rosterPath),
                new JsonUserPrefsStorage(prefsPath));
        model = new ModelManager();
        logic = new LogicManager(model, storage);
        autocompleteEngine = new AutocompleteEngine();
    }

    private void runCommand(String category, String input, boolean expectSuccess) {
        boolean succeeded = false;
        String message = "";
        try {
            CommandResult result = logic.execute(input);
            succeeded = true;
            message = result.getFeedbackToUser();
        } catch (ParseException | CommandException e) {
            succeeded = false;
            message = e.getMessage();
        } catch (Exception e) {
            succeeded = false;
            message = "UNEXPECTED EXCEPTION: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
        records.add(new TestRecord(category, input, expectSuccess, succeeded, message));
    }

    @Test
    public void executeComprehensiveTestBattery() throws Exception {
        // --- 1. SYSTEM & GENERAL SYNTAX ---
        runCommand("System", "help", true);
        runCommand("System", "HELP", true);
        runCommand("System", "hElP", true);
        runCommand("System", "help extra", false);
        runCommand("System", "list", true);
        runCommand("System", "LIST", true);
        runCommand("System", "list 123", false);
        runCommand("System", "exit", true);
        runCommand("System", "exit now", false);
        runCommand("System", "", false);
        runCommand("System", "   ", false);
        runCommand("System", "\t", false);
        runCommand("System", "unknowncommand", false);
        runCommand("System", "addd n/Test", false);

        // Unicode Whitespace tests
        runCommand("Syntax - Unicode Space", "list\u00A0", true); // Non-breaking space
        runCommand("Syntax - Unicode Space", "list\u2007", true); // Figure space
        runCommand("Syntax - Unicode Space", "list\u202F", true); // Narrow non-breaking space
        runCommand("Syntax - Unicode Space", "list\u2003", true); // Em space

        // --- 2. ADD COMMAND: FIELD COMBINATIONS & NORMALIZATION ---
        runCommand("Add - Valid",
                "add n/Alice Tan a/Under 14 p/91234567 e/alice@example.com addr/10 Stadium Way", true);
        runCommand("Add - Permuted Order",
                "add e/bob@example.com p/92345678 a/Open n/Bob Lee addr/20 Stadium Way", true);
        runCommand("Add - Hyphenated Name",
                "add addr/30 Stadium Way n/Charlie-Brown a/Under 16 p/93456789 e/charlie@example.com", true);
        runCommand("Add - Apostrophe Name",
                "add n/Daisy O'Connor a/Under 18 p/(+65) 94567890 e/daisy@example.org addr/40 Stadium Way", true);
        runCommand("Add - Phone Notes",
                "add n/Evan Neo a/Under 20 p/9567-8901 (HP) e/evan+track@example.com addr/50 Stadium Way", true);
        runCommand("Add - Category Spacing",
                "add n/Fiona Lim a/under   14 p/9678 1234 e/fiona@sub.domain.co addr/Block 12//A", true);
        runCommand("Add - Leading Slash Address",
                "add n/George Tan a/OPEN p/97890123 e/george@test.com addr/ /home/stadium", true);
        runCommand("Add - Numbers in Name",
                "add n/Runner 007 a/Open p/98901234 e/runner007@test.com addr/Base 7", true);
        runCommand("Add - Short Phone (3 digits)",
                "add n/Ian Short a/Under 14 p/999 e/ian@test.com addr/Camp 1", true);

        // Duplicate handling
        runCommand("Add - Exact Duplicate",
                "add n/Alice Tan a/Under 14 p/91234567 e/alice@example.com addr/Other Rd", false);
        runCommand("Add - Normalized Duplicate",
                "add n/alice tan a/Under 16 p/91234567 e/unique1@example.com addr/Other Rd", false);
        runCommand("Add - Normalized Duplicate",
                "add n/ALICE   TAN a/Under 18 p/90000000 e/alice@example.com addr/Other Rd", false);
        runCommand("Add - Permitted Same Name",
                "add n/Alice Tan a/Open p/91110000 e/alice2@example.com addr/Other Rd", true);
        runCommand("Add - Warning Shared Phone",
                "add n/Jacky Chan a/Open p/91234567 e/jacky@example.com addr/Studio 1", true);
        runCommand("Add - Warning Shared Email",
                "add n/Kelly Rowland a/Open p/91113333 e/bob@example.com addr/Studio 2", true);

        // Invalid Add: Name constraints
        runCommand("Add - Invalid Name", "add n/ a/Open p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Name", "add n/José a/Open p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Name", "add n/-Leading a/Open p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Name", "add n/Trailing- a/Open p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Name", "add n/Double--Hyphen a/Open p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Name", "add n/Spaced - Hyphen a/Open p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Name", "add n/Symbols@Here a/Open p/91234567 e/a@b.com addr/Street", false);

        // Invalid Add: Age Category constraints
        runCommand("Add - Invalid Age", "add n/John Doe a/Under 12 p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Age", "add n/John Doe a/Under 15 p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Age", "add n/John Doe a/Masters p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Age", "add n/John Doe a/Senior p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Age", "add n/John Doe a/ p/91234567 e/a@b.com addr/Street", false);

        // Invalid Add: Phone constraints
        runCommand("Add - Invalid Phone", "add n/John Doe a/Open p/12 e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Phone", "add n/John Doe a/Open p/9123abc e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Phone", "add n/John Doe a/Open p/9123 (HP e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Phone", "add n/John Doe a/Open p/9123 () e/a@b.com addr/Street", false);
        runCommand("Add - Invalid Phone", "add n/John Doe a/Open p/91+234567 e/a@b.com addr/Street", false);

        // Invalid Add: Email constraints
        runCommand("Add - Invalid Email", "add n/John Doe a/Open p/91234567 e/missingdomain addr/Street", false);
        runCommand("Add - Invalid Email", "add n/John Doe a/Open p/91234567 e/@domain.com addr/Street", false);
        runCommand("Add - Accepted Single Label TLD",
                "add n/John Doe a/Open p/91234567 e/user@com addr/Street", true);
        runCommand("Add - Invalid Email", "add n/John Doe a/Open p/91234567 e/user@domain.c addr/Street", false);
        runCommand("Add - Invalid Email",
                "add n/John Doe a/Open p/91234567 e/user space@domain.com addr/Street", false);

        // Invalid Add: Address constraints
        runCommand("Add - Invalid Address", "add n/John Doe a/Open p/91234567 e/a@b.com addr/", false);
        runCommand("Add - Invalid Address", "add n/John Doe a/Open p/91234567 e/a@b.com addr/   ", false);

        // Invalid Add: Unsupported & Duplicate Prefixes
        runCommand("Add - Unsupported Prefix",
                "add n/John Doe a/Open p/91234567 e/a@b.com addr/Street t/100m", false);
        runCommand("Add - Unsupported Prefix",
                "add n/John Doe a/Open p/91234567 e/a@b.com addr/Street r/relay", false);
        runCommand("Add - Duplicate Prefix",
                "add n/John a/Open n/Johnny p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Duplicate Prefix",
                "add n/John a/Open a/Under 14 p/91234567 e/a@b.com addr/Street", false);
        runCommand("Add - Missing Field", "add n/John a/Open p/91234567 e/a@b.com", false);

        // --- 3. FILTER COMMAND ---
        runCommand("Filter - Valid", "filter a/Under 14", true);
        runCommand("Filter - Valid Case Spacing", "filter a/under   14", true);
        runCommand("Filter - Valid", "filter a/Under 16", true);
        runCommand("Filter - Valid", "filter a/Under 18", true);
        runCommand("Filter - Valid", "filter a/Under 20", true);
        runCommand("Filter - Valid", "filter a/Open", true);
        runCommand("Filter - Valid Empty Results", "filter a/Under 20", true);
        runCommand("Filter - Invalid Category", "filter a/Under 15", false);
        runCommand("Filter - Invalid Category", "filter a/Masters", false);
        runCommand("Filter - Empty Value", "filter a/", false);
        runCommand("Filter - Missing Prefix", "filter Under 14", false);
        runCommand("Filter - Duplicate Prefix", "filter a/Open a/Under 14", false);
        runCommand("Filter - Extra Arguments", "filter a/Open extra", false);
        runCommand("System", "list", true);

        // --- 4. TAG AND UNTAG COMMANDS ---
        runCommand("Tag - Single", "tag 1 t/100m", true);
        runCommand("Tag - Multiple", "tag 1 t/200m t/Long Jump t/4x100m", true);
        runCommand("Tag - Accented", "tag 1 t/Saut-Élan", true);
        runCommand("Tag - Hyphenated", "tag 1 t/110m-Hurdles", true);
        runCommand("Tag - Partial Overlap", "tag 1 t/100m t/High Jump", true);
        runCommand("Tag - Full Duplicate Error", "tag 1 t/100m t/200m", false);
        runCommand("Tag - Max Length Boundary (30 chars)", "tag 1 t/123456789012345678901234567890", true);
        runCommand("Tag - Exceeds Max Length (31 chars)", "tag 1 t/1234567890123456789012345678901", false);
        runCommand("Tag - Invalid Symbol", "tag 1 t/100m*", false);
        runCommand("Tag - Invalid Spacing Hyphen", "tag 1 t/Long - Jump", false);
        runCommand("Tag - Invalid Double Hyphen", "tag 1 t/Long--Jump", false);
        runCommand("Tag - Invalid Index 0", "tag 0 t/100m", false);
        runCommand("Tag - Invalid Index Out Of Bounds", "tag 999 t/100m", false);
        runCommand("Tag - Missing Prefix", "tag 1 100m", false);

        runCommand("Untag - Single", "untag 1 t/High Jump", true);
        runCommand("Untag - Case Insensitive", "untag 1 t/saut-élan", true);
        runCommand("Untag - Multiple", "untag 1 t/200m t/Long Jump", true);
        runCommand("Untag - Nonexistent Event", "untag 1 t/Discus", false);
        runCommand("Untag - Nonexistent In Set", "untag 1 t/100m t/Discus", false);
        runCommand("Untag - Invalid Index 0", "untag 0 t/100m", false);
        runCommand("Untag - Invalid Index Huge", "untag 999 t/100m", false);

        // --- 5. REMARK COMMAND ---
        runCommand("Remark - Add", "remark 1 r/Sprinter captain", true);
        runCommand("Remark - Update", "remark 1 r/PB: 10.50s", true);
        runCommand("Remark - Double Slash", "remark 1 r/Practise r//relay Mon/Tue", true);
        runCommand("Remark - Clear Remark", "remark 1 r/", true);
        runCommand("Remark - Clear When Already Empty", "remark 1 r/", false);
        runCommand("Remark - Invalid Index 0", "remark 0 r/Note", false);
        runCommand("Remark - Invalid Index Out Of Bounds", "remark 999 r/Note", false);
        runCommand("Remark - Missing Prefix", "remark 1 Note", false);
        runCommand("Remark - Duplicate Prefix", "remark 1 r/first r/second", false);

        // --- 6. FIND COMMAND ---
        runCommand("Find - Broad Single Keyword", "find Alice", true);
        runCommand("Find - Broad Multiple Keywords (OR)", "find Alice Bob", true);
        runCommand("Find - Scoped Name (n/)", "find n/Alice", true);
        runCommand("Find - Scoped Age (a/)", "find a/Under 14", true);
        runCommand("Find - Scoped Phone (p/)", "find p/9123", true);
        runCommand("Find - Scoped Email (e/)", "find e/example.com", true);
        runCommand("Find - Scoped Address (addr/)", "find addr/Stadium", true);
        runCommand("Find - Scoped Tag (t/)", "find t/100m", true);
        runCommand("Find - Scoped Tag Partial", "find t/100", true);
        runCommand("Find - Multi Scoped AND Match", "find n/Alice a/Under 14", true);
        runCommand("Find - Multi Scoped AND Mismatch", "find n/Alice a/Open", true);
        runCommand("Find - Combined Broad OR Scoped AND", "find Bob n/Alice a/Under 14", true);
        runCommand("Find - Double Slash Address", "find addr/12//A", true);
        runCommand("Find - Empty Search", "find", false);
        runCommand("Find - Whitespace Only", "find    ", false);
        runCommand("Find - Empty Field Prefix", "find n/", false);
        runCommand("Find - Unknown Prefix", "find x/keyword", false);
        runCommand("Find - Duplicate Prefix", "find n/Alice n/Bob", false);
        runCommand("System", "list", true);

        // --- 7. SORT COMMAND ---
        runCommand("Sort - Name Asc", "sort name", true);
        runCommand("Sort - Name Desc", "sort name desc", true);
        runCommand("Sort - Name Descending", "sort name descending", true);
        runCommand("Sort - Age Asc", "sort age", true);
        runCommand("Sort - Age Desc", "sort age desc", true);
        runCommand("Sort - Phone Asc", "sort phone", true);
        runCommand("Sort - Phone Desc", "sort phone desc", true);
        runCommand("Sort - Email Asc", "sort email", true);
        runCommand("Sort - Email Desc", "sort email desc", true);
        runCommand("Sort - Address Asc", "sort address", true);
        runCommand("Sort - Address Desc", "sort address desc", true);
        runCommand("Sort - Case Insensitive Order", "sort name DESC", true);
        runCommand("Sort - Case Insensitive Field", "sort NAME asc", true);
        runCommand("Sort - Invalid Field", "sort height", false);
        runCommand("Sort - Invalid Order", "sort name upwards", false);
        runCommand("Sort - Extra Arguments", "sort name asc extra", false);
        runCommand("Sort - Missing Field", "sort", false);
        runCommand("System", "list", true);

        // --- 8. EDIT COMMAND ---
        runCommand("Edit - Name", "edit 1 n/Alicia Tan", true);
        runCommand("Edit - Age", "edit 1 a/Under 16", true);
        runCommand("Edit - Phone", "edit 1 p/98765432", true);
        runCommand("Edit - Email", "edit 1 e/alicia@example.com", true);
        runCommand("Edit - Address", "edit 1 addr/99 New Road", true);
        runCommand("Edit - Multiple Fields", "edit 1 n/Alice Tan a/Under 14 addr/10 Stadium Way", true);
        runCommand("Edit - Leading Zero Index", "edit 0001 addr/10 Stadium Way North", true);
        runCommand("Edit - Escaped Slash Address", "edit 1 addr/Block B//C", true);
        runCommand("Edit - No Fields Provided", "edit 1", false);
        runCommand("Edit - Redundant Edit (Identical Values)", "edit 1 a/Under 14", false);
        runCommand("Edit - Invalid Index 0", "edit 0 n/New", false);
        runCommand("Edit - Invalid Index Negative", "edit -1 n/New", false);
        runCommand("Edit - Invalid Index Out Of Bounds", "edit 9999 n/New", false);
        runCommand("Edit - Unsupported Prefix Tag", "edit 1 t/100m", false);
        runCommand("Edit - Unsupported Prefix Remark", "edit 1 r/Note", false);
        runCommand("Edit - Duplicate Prefix", "edit 1 p/11112222 p/22223333", false);
        runCommand("Edit - Invalid Value", "edit 1 a/Under 15", false);
        runCommand("Edit - Duplicate Conflict", "edit 1 n/Bob Lee p/92345678", false);

        // --- 9. DELETE COMMAND ---
        runCommand("Delete - Valid", "delete 1", true);
        runCommand("Delete - Invalid Index 0", "delete 0", false);
        runCommand("Delete - Invalid Index Negative", "delete -1", false);
        runCommand("Delete - Invalid Index Huge", "delete 999999999999999999999999999999", false);
        runCommand("Delete - Invalid Non-numeric", "delete abc", false);
        runCommand("Delete - Missing Index", "delete", false);

        // Deletion under filtered view
        runCommand("Filter - For Delete", "filter a/Under 16", true);
        runCommand("Delete - Under Filter Out of Range", "delete 99", false);
        runCommand("Delete - Under Filter Valid", "delete 1", true);
        runCommand("System", "list", true);

        // --- 10. CLEAR COMMAND ---
        runCommand("Clear - Valid", "clear", true);
        runCommand("Clear - With Arguments", "clear all", false);
        runCommand("System - Empty List", "list", true);

        // Operations on empty roster
        runCommand("Delete - On Empty Roster", "delete 1", false);
        runCommand("Edit - On Empty Roster", "edit 1 n/New", false);
        runCommand("Remark - On Empty Roster", "remark 1 r/test", false);
        runCommand("Tag - On Empty Roster", "tag 1 t/100m", false);
        runCommand("Untag - On Empty Roster", "untag 1 t/100m", false);
        runCommand("Filter - On Empty Roster", "filter a/Under 14", true);
        runCommand("Find - On Empty Roster", "find Alice", true);
        runCommand("Sort - On Empty Roster", "sort name", true);

        // --- 11. ADVANCED LOGIC: PHONE & AGE SORTING, SLASH ESCAPING ---
        testAdvancedScenarios();

        // --- 12. AUTOCOMPLETE LOGIC ---
        testAutocomplete();

        // Write report
        writeReportFile();

        long discrepancies = records.stream().filter(r -> !r.matchesExpectation).count();
        assertEquals(0, discrepancies, "All test cases should match expected behavior");
    }

    private void testAdvancedScenarios() throws Exception {
        // Clear roster for clean setup
        logic.execute("clear");

        // Test Phone sorting numerical order with notes & country codes
        logic.execute("add n/Aaa Tan a/Open p/200 (5) e/aaa@example.com addr/Home");
        logic.execute("add n/Bbb Tan a/Open p/300 e/bbb@example.com addr/Home");
        logic.execute("add n/Ccc Tan a/Open p/(+65) 200 e/ccc@example.com addr/Home");

        logic.execute("sort phone");
        List<Person> listAsc = model.getFilteredPersonList();
        boolean phoneAscCorrect = "Aaa Tan".equals(listAsc.get(0).getName().fullName)
                && "Bbb Tan".equals(listAsc.get(1).getName().fullName)
                && "Ccc Tan".equals(listAsc.get(2).getName().fullName);
        records.add(new TestRecord("Sort - Phone Logic", "sort phone with notes and (+65)",
                true, phoneAscCorrect, "Ordered: " + listAsc.stream().map(p -> p.getName().fullName).toList()));

        logic.execute("sort phone desc");
        List<Person> listDesc = model.getFilteredPersonList();
        boolean phoneDescCorrect = "Ccc Tan".equals(listDesc.get(0).getName().fullName)
                && "Bbb Tan".equals(listDesc.get(1).getName().fullName)
                && "Aaa Tan".equals(listDesc.get(2).getName().fullName);
        records.add(new TestRecord("Sort - Phone Logic", "sort phone desc with notes and (+65)",
                true, phoneDescCorrect, "Ordered: " + listDesc.stream().map(p -> p.getName().fullName).toList()));

        // Test Age Category Sorting progression
        logic.execute("clear");
        logic.execute("add n/AthOpen a/Open p/9100 e/open@test.com addr/Home");
        logic.execute("add n/Ath18 a/Under 18 p/9200 e/u18@test.com addr/Home");
        logic.execute("add n/Ath14 a/Under 14 p/9300 e/u14@test.com addr/Home");
        logic.execute("add n/Ath20 a/Under 20 p/9400 e/u20@test.com addr/Home");
        logic.execute("add n/Ath16 a/Under 16 p/9500 e/u16@test.com addr/Home");

        logic.execute("sort age");
        List<String> sortedNames = model.getFilteredPersonList().stream().map(p -> p.getName().fullName).toList();
        List<String> expectedAgeOrder = List.of("Ath14", "Ath16", "Ath18", "Ath20", "AthOpen");
        boolean ageOrderCorrect = expectedAgeOrder.equals(sortedNames);
        records.add(new TestRecord("Sort - Age Progression", "sort age (Under 14 -> 16 -> 18 -> 20 -> Open)",
                true, ageOrderCorrect, "Ordered: " + sortedNames));

        // Test Escaped slashes (A///B -> stores A//B)
        logic.execute("edit 1 addr/Block A///B");
        String storedAddr = model.getFilteredPersonList().get(0).getAddress().value;
        boolean slashCorrect = "Block A//B".equals(storedAddr);
        records.add(new TestRecord("Escaping - Triple Slash", "edit 1 addr/Block A///B stores Block A//B",
                true, slashCorrect, "Stored: '" + storedAddr + "'"));

        // Test Network path escaping in remark
        logic.execute("remark 1 r/ //server/share");
        String storedRemark = model.getFilteredPersonList().get(0).getRemark().value;
        boolean remarkCorrect = "//server/share".equals(storedRemark);
        records.add(new TestRecord("Escaping - Network Path", "remark 1 r/ //server/share stores //server/share",
                true, remarkCorrect, "Stored: '" + storedRemark + "'"));

        // Test Storage Round-Trip Persistence
        ReadOnlyRoster loadedRoster = storage.readRoster().orElseThrow();
        boolean persistenceCorrect = loadedRoster.getPersonList().size() == model.getRoster().getPersonList().size();
        records.add(new TestRecord("Storage - Round Trip", "Save and reload roster JSON from disk",
                true, persistenceCorrect, "Disk entries matched live roster size: " + persistenceCorrect));
    }

    private void testAutocomplete() {
        assertNotNull(autocompleteEngine.getSuggestions("a"));
        assertNotNull(autocompleteEngine.getSuggestions("ed"));
        assertNotNull(autocompleteEngine.getSuggestions("s"));
        assertNotNull(autocompleteEngine.getSuggestions("f"));

        String p1 = autocompleteEngine.getNextPrefix("add ");
        String p2 = autocompleteEngine.getNextPrefix("add n/John ");
        String p3 = autocompleteEngine.getNextPrefix("edit 1 ");
        String p4 = autocompleteEngine.getNextPrefix("remark 1 ");

        records.add(new TestRecord("Autocomplete", "Suggestions for 'a'", true, true,
                "Returned suggestions: " + autocompleteEngine.getSuggestions("a")));
        records.add(new TestRecord("Autocomplete", "Prefix for 'add '", true, "n/".equals(p1.trim()),
                "Returned prefix: '" + p1 + "'"));
        records.add(new TestRecord("Autocomplete", "Prefix for 'add n/John '", true, "a/".equals(p2.trim()),
                "Returned prefix: '" + p2 + "'"));
        records.add(new TestRecord("Autocomplete", "Prefix for 'edit 1 '", true, "n/".equals(p3.trim()),
                "Returned prefix: '" + p3 + "'"));
        records.add(new TestRecord("Autocomplete", "Prefix for 'remark 1 '", true, "r/".equals(p4.trim()),
                "Returned prefix: '" + p4 + "'"));
    }

    private void writeReportFile() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# TrackFlow Application Input Testing Report\n\n");
        sb.append(String.format("Total test cases executed: %d\n", records.size()));

        long passed = records.stream().filter(r -> r.matchesExpectation).count();
        long failed = records.size() - passed;

        sb.append(String.format("Matches Specification / Expected Behavior: %d\n", passed));
        sb.append(String.format("Discrepancies / Anomalies: %d\n\n", failed));

        sb.append("| # | Category | Input Tested | Expected | Actual | Feedback Message |\n");
        sb.append("|---|---|---|---|---|---|\n");

        int index = 1;
        for (TestRecord r : records) {
            String exp = r.successExpected ? "SUCCESS" : "REJECTED";
            String act = r.succeeded ? "SUCCESS" : "REJECTED";
            String cleanMsg = r.output.replace("|", "\\|").replace("\n", " ");
            sb.append(String.format("| %d | %s | `%s` | %s | %s | %s |\n",
                    index++, r.category, r.input, exp, act, cleanMsg));
        }

        Path reportPath = Paths.get("build", "test_execution_log.md");
        Files.writeString(reportPath, sb.toString());
    }
}
