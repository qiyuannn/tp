---
  layout: default.md
  title: "Testing guide"
  pageNav: 3
---

# Testing guide

<!-- * Table of Contents -->
<page-nav-print />

<!-- -------------------------------------------------------------------------------------------------------------------- -->

## Running tests

Use the [Zulu JDK 25 setup](SettingUp.md#runtime-and-build-requirements) for both IntelliJ and Gradle.
You can run tests in two ways.

* **Method 1: Using IntelliJ JUnit test runner**
  * To run all tests, right-click on the `src/test/java` folder and choose `Run 'All Tests'`
  * To run a subset of tests, you can right-click on a test package,
    test class, or a test and choose the corresponding run action (for example, `Run 'PersonTest'`)
* **Method 2: Using Gradle**
  * Open a console and run the command `gradlew clean test` (Mac/Linux: `./gradlew clean test`)

<box type="info" seamless>

**Link**: Read [this Gradle Tutorial from the se-edu/guides](https://se-education.org/guides/tutorials/gradle.html) to learn more about using Gradle.
</box>

--------------------------------------------------------------------------------------------------------------------

## Types of tests

This project has three types of tests:

1. *Unit tests* target the lowest-level methods and classes.<br>
   For example: `trackflow.commons.util.StringUtilTest`
1. *Integration tests* check how multiple code units work together; the individual units are assumed to work.<br>
   For example: `trackflow.storage.StorageManagerTest`
1. *Hybrid tests* combine unit and integration testing. These tests check both the individual units and how they work together.<br>
   For example: `trackflow.logic.LogicManagerTest`

## Reading and reviewing tests

Test names describe the operation, scenario and expected outcome. Read the fixture setup first: each `@BeforeEach`
or `@TempDir` establishes the starting roster and isolates saved files from a real installation. An assertion on the
displayed list checks the view; it does not by itself establish that the complete roster or saved JSON is correct.

| Area | Useful starting points | What the checks establish |
| --- | --- | --- |
| Command syntax and escaping | `TrackFlowParserTest`, `ArgumentTokenizerTest`, `ParserRobustnessTest` | Accepted/rejected input, one-time slash decoding, and unchanged state after rejection |
| Search and indexed commands | `PersonContainsKeywordsPredicateTest`, `FilterCommandTest`, `SortCommandTest` | Broad OR versus scoped AND, replacement of filters, and indexes in the displayed order |
| Identity and saved records | `PersonTest`, `DuplicatePersistenceTest`, `JsonSerializableRosterTest` | Duplicate identity, complete equality, and decisions after reloading |
| Save failures and startup | `LogicManagerTest`, `MainAppTest`, `StartupProcessTest` | Publication only after saving, preservation of unreadable data, and application-home paths |
| Screen placement | `WindowPlacementTest` | Bounds calculations for screens and saved preferences; actual rendering still needs manual checks |

For failure tests, check the expected exception or feedback as well as the unchanged roster, view and saved file.
For persistence tests, look for a fresh storage read or model reconstruction rather than an assertion against only
the original in-memory object. Comments explain unusual inputs, isolation decisions and regression risks; ordinary
setup and self-explanatory assertions do not need a comment on every line.

The automated suite does not drive the complete JavaFX interface. Follow the DG's
[manual testing appendix](DeveloperGuide.md#appendix-instructions-for-manual-testing) on the final packaged JAR for
keyboard behaviour, rendering, scrolling, screen scaling and platform/runtime compatibility. Record the exact build
and environment tested; passing helper tests alone is not evidence of those UI properties.

## Build and focused tests

Run `./gradlew clean check coverage shadowJar` to build `build/libs/TrackFlow.jar`, run the automated tests,
and check Java style. On Windows, use `gradlew.bat` in place of `./gradlew`.

For focused duplicate/equality checks, run:

```bash
./gradlew test --tests trackflow.model.person.PersonTest --tests trackflow.logic.DuplicatePersistenceTest
```

For parser boundaries, double-slash escapes, literal paths, empty values, duplicate parameters, null inputs,
multiline input, large indexes, and rejected-command checks, run:

```bash
./gradlew test \
    --tests trackflow.logic.ParserRobustnessTest \
    --tests trackflow.logic.parser.ArgumentTokenizerTest \
    --tests trackflow.logic.parser.ParserUtilTest \
    --tests trackflow.logic.parser.TrackFlowParserTest
```

`ParserRobustnessTest` uses real JSON storage in a temporary folder to check that parsed values survive reload and
that invalid commands preserve the roster, current predicate/comparator, displayed athletes, and saved file.
The tests distinguish Java `null` API inputs from the literal text `null`, exercise duplicate parameters even when
one value is empty or escaped, and verify that restarts and unrelated edits do not decode stored text again.
The tokenizer tests cover every single-letter escape in both cases, multiple whitespace boundaries, long slash runs,
URLs, network paths, and ordinary quotes/backslashes. Parser tests check all 120 orders of the five add fields.
Unicode-boundary regressions cover non-breaking, figure, narrow non-breaking, and em spaces in command words,
prefixes, escaped text, broad searches, and sort arguments. They also check that whitespace inside addresses and
remarks survives JSON reload and that unknown or duplicate prefixes still fail without changing saved data.
Existing `JsonRosterStorageTest` and `MainAppTest` also check null JSON roots/entries and preservation of malformed data.

Name-sort regressions in `SortCommandTest` and `LogicManagerTest` cover repeated spaces, equivalent case/spacing
keys, ascending and descending order, and subsequent adds/edits. A fresh JSON read verifies that comparison-key
normalization does not rewrite name text or saved roster order.

Phone-sort regressions use the shared significant-digit rule: bracketed notes are ignored and `(+65)` country codes
are retained. They cover both directions, numeric notes, formatting, zero-valued and large numbers, add/edit updates,
and unchanged phone text after JSON reload.

For startup/preferences, strict JSON roots, application-home paths and unavailable file logging, run:

```bash
./gradlew test \
    --tests trackflow.MainAppTest \
    --tests trackflow.storage.JsonUserPrefsStorageTest \
    --tests trackflow.commons.core.GuiSettingsTest \
    --tests trackflow.commons.util.JsonUtilTest \
    --tests trackflow.commons.util.ApplicationHomeTest \
    --tests trackflow.StartupProcessTest
```

`StartupProcessTest` packages the compiled application classes into a temporary JAR and starts bounded child JVMs
with the normal test runtime dependencies. It checks recovery, saving and reopening from another directory, a home
path containing spaces and `%u`, temporary-file cleanup, and console fallback when the log path is blocked.
It initializes the application and runs real commands/storage without opening a GUI; it does not replace native
window tests or launch verification of the final release JAR on each supported runtime/platform.

Automated results are in `build/reports/tests/test/index.html`; Checkstyle reports are in
`build/reports/checkstyle/`. Verify guide examples and startup behavior using the
[manual testing procedures](DeveloperGuide.md#appendix-instructions-for-manual-testing).

## Adversarial release testing

Run these checks against a copy of the final release JAR in a disposable home folder. Do not share that folder
with a running development instance. Reset the fixture between unrelated scenarios so one test's filter, sort,
deleted records, or pending clear confirmation does not change the next test's prerequisites. The DG's
[three-athlete fixture](DeveloperGuide.md#before-testing) provides a known starting order.

For every rejected command, check the error, retained command text, full roster, displayed order and indexes,
filter/sort state, and saved JSON. For a successful change, close and reopen the app and verify the affected record
and every field that should have been preserved. An error message alone does not prove that no data changed.

| Area | Inputs and sequences to try | Expected result or invariant |
| --- | --- | --- |
| Empty and malformed input | Empty input; only spaces; unknown commands; missing fields; unsupported prefixes; duplicate prefixes with different case; leading/trailing spaces | A controlled error for invalid input; no roster or view change. Valid boundary spaces and case variants follow the UG. |
| Displayed indexes | `0`, `-1`, `+1`, `1.0`, words, mixed digits/text, thousands of digits, `0001`; repeat after a zero-result find, filter, and consecutive sorts with tied values | Only a valid displayed index selects an athlete. `0001` selects index 1. Oversized numbers report an error without overflow or deletion. |
| Field boundaries | Each required field empty; email lengths 254 and 255 with otherwise valid syntax; event lengths 30 and 31; phone numbers with two and three significant digits; punctuation at the start/end of a name | Each field follows its documented limits. Rejection does not partially apply other fields. |
| Long input | Long valid and invalid names, phones, addresses, remarks, event lists and indexes; increase size in stages and record actual lengths | The app remains responsive or gives a controlled error. Record hangs/crashes separately from a normal validation error; do not infer a supported size limit from one successful probe. |
| Unicode and pasting | Non-breaking, figure, narrow non-breaking and em spaces at command boundaries; accented event names in composed/decomposed forms; unsupported name characters; zero-width characters; pasted tabs and line breaks | Boundary-space and event-equivalence behavior matches the UG. Unsupported names fail. Check the text actually retained by the single-line command box before judging parser behavior. |
| Literal prefixes | `edit 1 addr/Block A//B`; `remark 1 r/Practise r//relay`; `edit 1 addr/ /home/coach`; URLs, network paths, quotes, backslashes and runs of slashes | Literal text is decoded exactly once. Saved text survives restart and unrelated edits. Quotes do not hide a real prefix. |
| Search and ordering | Broad OR, scoped AND, mixed search, empty results; consecutive `find`/`filter`; `sort`, then a roster change; `list`; restart | Search replaces the previous selection. Active sorting survives changes until `list` or restart. Sorting never reorders the saved roster. |
| Hidden duplicates | Filter out an existing athlete, then add/edit a normalized-name duplicate with matching phone or email; try different names sharing contacts | Duplicate decisions use the full roster. Allowed contact matches produce accurate warnings without misleading displayed indexes. |
| Atomic event updates | A `tag` containing existing and new events; an `untag` containing existing and missing events; duplicate case variants in one command | Only new tags are added. If any requested removal is missing, none are removed. Stored event spelling and unrelated fields are preserved. |
| Clear confirmation | `clear` then `N`; `clear` then a normal command; `clear` then invalid input; `clear` then `Y`; repeated `Y`; restart while confirmation is pending | Only a response to a pending confirmation can clear data. Any intervening submission cancels that pending confirmation. A confirmed clear persists after restart. |
| Failed saves | Block writes to the disposable data folder, then try each modifying command while a filter and sort are active | Failure leaves the live roster, display and old file unchanged. View-only commands still work. Restore permissions and verify a later update saves and reloads. |
| Damaged saved files | With the app closed: truncated JSON, extra JSON roots, repeated keys, null lists/entries, invalid values and duplicates | The exact unreadable file is backed up and recovery is visible. If backup creation is blocked, startup stops without overwriting the original. |
| Launch paths and settings | JAR home containing spaces/non-ASCII text; launch from another folder; missing or corrupt preferences; oversized/off-screen window settings | Data remains beside the actual JAR. Invalid preferences recover; the main and help windows remain reachable. Check behavior on the actual platform. |
| GUI and keyboard | Minimum window size, long feedback, long unbroken fields, many tags, scrolling, Tab/Shift+Tab, arrows, Escape, Enter, help reopened after minimizing | Essential controls and complete content remain reachable. Enter executes only the typed command; completion and focus follow the UG. |
| Guide examples | Execute each UG example from its stated starting state and each DG procedure independently; inspect the built website and diagrams | Commands, feedback, screenshots and instructions agree with the packaged app; internal links and images load. |

For performance, separately measure the DG's [NFR10 target](DeveloperGuide.md#non-functional-requirements) with
1,000 athletes and 100 commands. Record individual timings, including saving and visible updates, plus hardware
and runtime details. Parser or model timings alone do not verify GUI response time.

Record each run's commit, JAR checksum, Java distribution/version, OS/architecture, display scaling, fixture,
exact input, expected and actual output, and whether the result survives a restart. Mark checks as passed,
failed, or not run. In particular, source tests, a successful JAR build, and a documentation build do not establish
Windows/Linux/macOS launch compatibility, native clipboard behavior, monitor changes, or every display scale.
Run those checks on the supported environments before release, and repeat affected scenarios after every fix.

### Beta audit: 30 October 2026

This run tested the working tree based on commit `8f4cf3b9`, including the audit fixes, on macOS arm64 with
Zulu JDK FX 25.0.3. It is evidence for that build, which was labelled v1.5 before the v1.6 release.

* `./gradlew clean check coverage shadowJar`: passed; 614 tests, zero failures, errors or skips; both Checkstyle
  checks passed. JaCoCo reported 82.5% line and 83.3% branch coverage.
* A seeded parser probe (seed 2103) exercised 50,000 mutations of 16 command seeds, including control characters,
  Unicode spaces, isolated surrogates and malformed prefixes: 3,665 accepted, 46,335 controlled parse errors,
  no unchecked crashes. A separate long-input probe passed 17 validation and 17 parser cases containing up to
  100,000 repeated name/phone groups. These probes do not establish GUI responsiveness for such inputs.
* Real filesystem replacement failures covered add, edit, remark, tag, untag, delete and confirmed clear.
  `StorageAdversarialTest` checks unchanged live state, original bytes, temporary-file cleanup and successful retry.
  It also verifies byte-exact backup preservation for malformed UTF-8, including after a subsequent successful save.
* Storage probes rejected malformed JSON shapes and duplicate keys, handled deeply nested unknown fields, and
  verified packaged startup and persistence through a symlink from another working directory with spaces and
  `%u` in the actual JAR home. Those startup probes did not open a window.
* Twenty-two guide assertions checked exact feedback, displayed athletes and reload results. The MarkBind build
  passed for all 15 pages; 929 generated local link/asset references had no missing files or fragment targets.

Confirmed fixes include Unicode-space handling for clear confirmation, rejection of leading Unicode line breaks
and Unicode-blank addresses, and applying screen-fitted main-window minima. Guide and built-in help corrections
clarify initial sample data, saved-order restoration, mixed broad/scoped searches and tag-example prerequisites.

Native end-to-end keyboard/clipboard interaction, actual small-screen and multi-monitor behavior, Windows/Linux
launches, and the end-to-end 1,000-athlete GUI performance target remain unverified in this run. The desktop
automation tool could not attach to the standalone Java process. Run the manual matrix above on the final release
artifact; JavaFX component tests do not substitute for those checks.
