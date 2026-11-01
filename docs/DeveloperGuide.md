---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# TrackFlow Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* This project is based on the AddressBook-Level3 project created by the [SE-EDU initiative](https://se-education.org).
* Libraries used: [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson), [JUnit5](https://github.com/junit-team/junit5).
* Font used: [Plus Jakarta Sans](https://github.com/tokotype/PlusJakartaSans) by Tokotype, licensed under [OFL 1.1](https://scripts.sil.org/OFL).
* Chetan (`chetangent`) used OpenAI Codex extensively to generate and refine project code and documentation.
* Antony (`antonydchakola-hub`) used Claude (Anthropic) extensively to review and refactor code, write tests, find
  and reproduce bugs, and draft User Guide and Developer Guide text. He reviewed and tested all generated output
  before committing it.
* Austin (`Austin-Chia`) used OpenAI Codex extensively to generate and refine project code, assist with testing
  and debugging, and review and improve documentation.

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

--------------------------------------------------------------------------------------------------------------------

## **Design**

### Architecture

<puml src="diagrams/ArchitectureDiagram.puml" width="750" alt="Conceptual TrackFlow components and their dependencies, with Main covering Main and MainApp" />

The ***Architecture Diagram*** given above explains the high-level design of the App.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/AY2627S1-CS2103T-T17-2/tp/tree/master/src/main/java/trackflow/Main.java) and [`MainApp`](https://github.com/AY2627S1-CS2103T-T17-2/tp/tree/master/src/main/java/trackflow/MainApp.java)) is in charge of the app launch and shut down.

* At app launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The sequence diagram below follows a successful `delete 1` across the main components. The `ref` frame summarizes
parsing and execution on a detached model; [delete execution](#logic-component) is expanded separately.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" width="800" alt="Successful delete: the UI calls Logic, which saves the staged roster before updating the live model" />

`updatedRoster` means `stagedModel.getRoster()`, and `predicate` means `stagedModel.getPersonListPredicate()`.
Preparing the staged model copies the live roster, preferences, filter, and sort comparator; those accessor calls
are omitted. A snapshot of the displayed positions breaks comparator ties in the detached copy, preserving the
athlete addressed by each displayed index after consecutive sorts. This tie breaker is not installed in the live
model and does not reorder saved records. After saving and updating the live model, `withDataSaved()` creates
`savedResult`; its call is omitted.
The live sort comparator is retained. JavaFX observes the published change, and `MainWindow` displays the result.

The separate failure trace shows what happens if saving throws an `IOException`, including an
`AccessDeniedException`. `LogicManager` wraps it in `CommandException` before `MainWindow` displays the error.
No staged change is published to the live model.

<puml src="diagrams/SaveFailureSequenceDiagram.puml" width="550" alt="Failed save: Storage throws IOException, Logic reports CommandException, and the live model stays unchanged" />

Both traces assume valid input and a valid displayed index. Unlabelled dashed replies return control from void
methods or constructors; exception replies show propagation rather than a normal return value.

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<puml src="diagrams/ComponentManagers.puml" width="750" alt="UI, Logic, Model, and Storage managers implementing their interfaces and retaining component references" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-T17-2/tp/tree/master/src/main/java/trackflow/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" width="1100" alt="UI parts inheriting UiPart, MainWindow references, the WindowPlacement helper and GuiSettings dependencies, and CommandBox autocomplete and PersonCard model dependencies" />

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `PersonListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

`MainWindow` eagerly creates one `HelpWindow`. Its `fillInnerParts()` method creates the remaining parts;
`CommandBox` is attached to the scene graph without a direct `MainWindow` field. `CommandBox` owns an
`AutocompleteEngine`, and each `PersonCard` holds the `Person` it displays. Generic JavaFX root parameters and
toolkit details are omitted from this partial view.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-T17-2/tp/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-T17-2/tp/tree/master/src/main/java/trackflow/ui/MainWindow.java).

`MainWindow` reads the saved window bounds from `GuiSettings` and uses the static `WindowPlacement.fit` helper to
fit those outer bounds, including decorations, inside the visual bounds of a connected screen. A reachable saved
position selects the screen with the largest overlap; an unreachable position falls back to the primary screen's
center. Both restoration paths cap dimensions and
clamp coordinates. The preferred minimum is 450 by 400 pixels, limited by the selected screen's usable area.
Normal bounds are remembered in preferences in memory as the window moves or resizes, allowing normal bounds to
survive maximized, minimized, full-screen and OS quit paths. Preference storage writes them on shutdown.
The roster can shrink and scroll independently of the input and footer; command execution does not resize the stage.
`HelpWindow` uses the same fitting helper with its own 480 by 320 minimum, starts on its owner's screen, and
retains later user placement within the session.

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-T17-2/tp/tree/master/src/main/java/trackflow/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="800" alt="Logic interfaces, parser ownership, representative commands, and model and storage dependencies" />

`LogicManager` implements `Logic`, owns its `TrackFlowParser`, and holds references to the live `Model` and
`Storage`. The diagram shows `EditCommand` and `FindCommand` as representative subclasses of `Command`:
`changesRoster()` determines whether execution needs a detached model and persistence. Commands return a
`CommandResult`; `LogicManager` calls `withDataSaved()` only after saving a roster change successfully.
The other command subclasses and dedicated command parsers are omitted from this overview.

The delete trace focuses on the command's work. Parsing has already created `DeleteCommand`, and `LogicManager`
has prepared a detached model with the current roster and display state. The command resolves index 1 against the
staged model's filtered and sorted list, so it selects the same athlete the user sees.

<puml src="diagrams/DeleteSequenceDiagram.puml" width="550" alt="DeleteCommand selects the displayed athlete and removes it from the staged model" />

This excerpt assumes a valid index and omits list access and result construction. An out-of-range index throws
`CommandException` before deletion. Saving and publication happen after this excerpt, as shown in the
[architecture traces](#architecture); they are not repeated here.



How the `Logic` component works:

1. When `Logic` is called upon to execute a command, `LogicManager` passes its text to `TrackFlowParser`.
   Parameterized commands are delegated to a dedicated parser (e.g., `DeleteCommandParser`); `clear`, `help`, and
   `exit` are validated and constructed directly by `TrackFlowParser`.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a person).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
1. A roster-changing command identifies itself through `Command.changesRoster()`. `LogicManager` executes it against a
   detached model containing the current roster, filter, and sort order. It saves the staged roster before publishing
   the change to the live model. If saving fails, the live roster and displayed list are never changed. View-only
   commands such as `list`, `find`, `filter`, `sort`, `help`, and `exit` execute directly and do not write the roster.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

The following partial class diagram expands the parsing helpers, using the concrete `EditCommandParser` as an
example. The other dedicated parsers, including the shared tag/untag parser hierarchy, are omitted for clarity.

<puml src="diagrams/ParserClasses.puml" width="800" alt="Edit command parser and its validation, tokenization, argument-map, and prefix dependencies" />

How the parsing works:

* `TrackFlowParser` recognizes command words without considering letter case. For parameterized commands such as
  `edit`, it creates the dedicated parser and returns the resulting command. The no-argument commands `clear`,
  `help`, and `exit` are validated and constructed directly instead.
* `EditCommandParser` implements `Parser<EditCommand>`. It uses prefix constants from `CliSyntax`, calls
  `ParserUtil.verifyNoUnknownPrefixes`, and tokenizes arguments through `ArgumentTokenizer`.
* `ArgumentTokenizer` recognizes supported prefixes at whitespace boundaries without considering letter case.
  Escaped prefix-like text stays within a value, and exactly one extra slash is removed after arguments are split.
* `ArgumentMultimap` retains every supplied value for a prefix. `EditCommandParser` validates the index, rejects
  duplicate supported prefixes, parses the supplied fields, and requires at least one field before constructing
  an `EditCommand`. Parser and field-validation failures are reported as `ParseException`.

The edit-parsing example is split into dispatch and argument validation. First, `TrackFlowParser` separates the
command word from its arguments, creates `EditCommandParser`, and delegates parsing. The `ref` frame is expanded
in the next diagram. Created objects' lifelines begin at their constructor messages.

<puml src="diagrams/tracing/LogicSequenceDiagram.puml" width="850" alt="Edit parsing: TrackFlowParser delegates to EditCommandParser, which creates and returns EditCommand" />

Within `EditCommandParser.parse`, validation and tokenization turn `edit 1 addr/Block A//B` into index 1 and address
`Block A/B`. `ParserUtil` and `ArgumentTokenizer` participants marked `«class»` receive static calls.

<puml src="diagrams/tracing/EditArgumentsSequenceDiagram.puml" width="550" alt="Edit argument validation: reject unknown prefixes, tokenize and decode once, then parse the index and address" />

This validation excerpt omits `ArgumentMultimap` accessor calls, the duplicate-prefix check after index parsing,
checks for absent optional fields, and descriptor assembly. The parser puts the parsed address into an
`EditPersonDescriptor` and checks that at least one field is edited before constructing the command. The command
constructor copies that descriptor. Argument lists marked `...` abbreviate usage text and supported prefixes.
Unlabelled dashed replies return control without displaying a return value.

Syntax and field-validation failures throw `ParseException` before command creation. The successful command
contains the address `Block A/B`, but no athlete has been edited and no file saved. `LogicManager` subsequently
executes it on a detached model; `EditCommand` resolves the displayed index during execution, and saving precedes
publication as described above.

### Model component

**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-T17-2/tp/tree/master/src/main/java/trackflow/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="950" alt="ModelManager, roster and preferences ownership, athlete list, and JavaFX filtered and sorted views" />


The `Model` component,

* stores the athlete roster data i.e., all `Person` objects (which are contained in a `UniquePersonList` object).
* stores the `Person` objects selected by the current filter, such as search results, in a `FilteredList`, and wraps
  that view in a `SortedList` for display ordering. It exposes the sorted view as an
  `ObservableList<Person>` that the UI can observe and bind to, so the UI updates when filtering, sorting, or roster
  data changes.
* retains the active `Predicate<Person>` and optional `Comparator<Person>` separately as display state. Changing
  either updates the corresponding JavaFX view without changing the stored roster order.
* distinguishes duplicate identity from complete value equality. `Person.isSamePerson` compares the normalized name
  and matching contact details, while `Person.equals` and `hashCode` include every stored field, including the remark.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)


The following diagram focuses on stored athlete fields. Each `Tag` is one track event of the athlete (e.g. `100m`);
see [Event tagging](#event-tagging). `UniqueTagList` is not part of the implementation.
`AthleteRoster` implements `ReadOnlyRoster`, exposing its athletes through an unmodifiable
`ObservableList<Person>`. It owns one `UniquePersonList`, which retains athletes in list order and enforces
uniqueness using `Person.isSamePerson`. The diagram omits the intermediate JavaFX collection classes.

<puml src="diagrams/BetterModelClassDiagram.puml" width="1000" alt="Read-only roster interface, ordered athlete list, stored athlete fields, and unique event tags" />

The value-field links are ordinary associations: immutable value objects can be shared between different `Person`
instances, such as during an edit. `Person` copies its tag set, while the individual immutable `Tag` objects may
be shared. The tag association is an unordered set: equality ignores case, so differently cased spellings
represent the same event, while display retains the stored spelling.

### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-T17-2/tp/tree/master/src/main/java/trackflow/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="800" alt="StorageManager delegates to JSON preferences and roster storage, with roster and athlete DTOs" />

This partial view shows the storage adapters and DTOs; model conversion and file utilities are omitted. The stored
JSON roster property remains `persons` so that existing roster files continue to load.

The `Storage` component,

* can save both athlete roster data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonRosterStorage` and `JsonUserPrefsStorage` (one class per data file).
* writes JSON to a temporary sibling file before replacing the destination. `FileUtil` uses an atomic move when the
  filesystem supports it and cleans up the temporary file after success or failure.
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

#### Roster JSON mapping

The following partial class diagram expands the storage overview's model conversion dependencies.
`JsonSerializableRoster` retains a list of `JsonAdaptedPerson`, each of which retains a list of
`JsonAdaptedTag`. These are ordinary navigable associations; the adapters do not retain model objects.
The JSON roster key remains `persons` for compatibility with existing files.

<puml src="diagrams/RosterJsonMappingClassDiagram.puml" width="800" alt="Roster JSON adapters retain person and tag lists, use model objects when saving, and create AthleteRoster, Person and Tag when loading; Person retains an unordered tag set" />

On saving, `JsonRosterStorage` constructs `JsonSerializableRoster` from a `ReadOnlyRoster`.
Its constructor maps each `Person` to a `JsonAdaptedPerson`, which maps the model's `Set<Tag>` to a
`List<JsonAdaptedTag>`. `JsonAdaptedTag` serializes as a tag-name string, so the persisted `tags` array
contains strings. The model tag set has no ordering guarantee.

On loading, Jackson reconstructs the adapters and `toModelType()` validates stored values before creating
`Tag` and `Person` objects and adding them to a new `AthleteRoster`, which implements `ReadOnlyRoster`.
Tag conversion checks tag names; person conversion checks required fields and the constraints on name,
age category, phone, email and address. A missing age category defaults to `Open`, and missing or null
`tags` become an empty list. The JSON tag list becomes a `HashSet<Tag>`: repeated tags, including
case variants, collapse according to `Tag.equals` and `hashCode`.

A missing or null `persons` list, null athlete/tag entries, invalid field values, and duplicate athletes
(according to `Person.isSamePerson`) cause `IllegalValueException`; `JsonRosterStorage` wraps it in
`DataLoadingException`. File-reading or JSON-decoding failures, including a JSON null root, also produce
`DataLoadingException` through `JsonUtil`. An absent file instead returns `Optional.empty()`.

### Common classes

Classes used by multiple components are in the `trackflow.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Command parsing and validation

`TrackFlowParser` treats command words without considering letter case and routes commands with parameters to their
dedicated parsers. Its initial matcher accepts line breaks in arguments; the entire submission is still parsed as one
command, not a batch. Each field keeps its existing validation rules. The JavaFX command box is a single-line
`TextField` that removes pasted tabs and line breaks before parsing; users should paste commands on one line with
spaces. Parser-level tests exercise multiline input directly.

`CommandWhitespace` defines syntax separators as characters accepted by `Character.isWhitespace` or
`Character.isSpaceChar`, including non-breaking, figure, narrow non-breaking, and em spaces. Command-word
splitting, no-argument validation, index boundaries, broad search keywords, and sort arguments share this rule.
Only boundary whitespace is stripped; whitespace inside field values retains its existing validation and storage
behaviour. Zero-width spaces are not separators.

`ArgumentTokenizer` recognizes supported parameter prefixes without considering letter case at the start of the
arguments or after these syntax separators, storing them under their canonical prefix.
`ParserUtil.verifyNoUnknownPrefixes` uses the same boundary to reject unsupported parameter-like tokens of the
reserved single-letter or `addr/` form before values are parsed. For example, unsupported `t/` produces a
command-specific error. Longer slash-containing words such as `wrong/value` remain text.

An extra slash marks literal parameter-like text, such as `A//B` or `r//relay`. Both prefix detection and
unknown-prefix validation skip slash-terminated prefixes followed by another slash. After splitting arguments, the
tokenizer strips surrounding whitespace and removes exactly one slash from a run of at least two slashes after a
single-letter or `addr` token. Decoding happens once, including in the broad-search preamble, so decoded text cannot
introduce new arguments. `A///B` becomes `A//B`, not `A/B`. URLs, network paths, quotation marks, and backslashes
are not special syntax. Escaped tokens cannot supply required fields or bypass ordinary field validation.

A slash-led value must be separated from its real prefix by whitespace: `addr/ /home/coach` supplies the address
`/home/coach`, while `addr//home/coach` is literal text and does not introduce an address parameter.

Commands that take no arguments (`clear`, `help`, and `exit`) reject extra text.
`remark` requires exactly one `r/` prefix. Index parsing scans ASCII digits once, capping the accumulated value at `Integer.MAX_VALUE` without building
an arbitrary-precision number. It continues validating the remaining characters after reaching the cap, so an
oversized number followed by text is still rejected. Leading zeroes are accepted; all-zero and malformed indexes are
rejected. Oversized positive numbers reach the command's normal invalid-displayed-index check.

<span id="broad-and-field-scoped-finding"></span>
<span id="flexible-athlete-search"></span>

### Finding athletes

`TrackFlowParser` dispatches `find` to `FindCommandParser`. The parser recognizes broad preamble keywords and
`n/`, `a/`, `p/`, `e/`, `addr/`, `r/`, and `t/` criteria. Broad keywords must precede the first field prefix.
Unknown reserved prefixes, duplicate field prefixes, empty field values, and an empty search are rejected before
the model is changed. `ArgumentTokenizer` decodes escaped prefix-like text once, before predicate construction.

`PersonContainsKeywordsPredicate` combines the two groups as follows:

* **Broad keywords:** any keyword may occur in any searchable field (OR).
* **Scoped fields:** every supplied field must match its own value (AND). Each prefixed value is one phrase;
  `find n/Avery Tan` does not behave like the two broad keywords in `find Avery Tan`.
* **Both groups present:** an athlete matches a broad keyword OR satisfies all supplied field criteria.
* **No criteria:** matches no athletes. The nonempty-field guard prevents an empty `allMatch` from making a broad
  search match the entire roster.

Matching is case-insensitive substring matching. Thus `find n/avery a/Under 14` requires both the name and category,
while `find Blake n/avery a/Under 14` additionally includes athletes matching the broad keyword `Blake`.
`StringUtil.normalizeForCaseInsensitiveMatching` supplies the comparison key for both search text and event identity,
composing Unicode accents and applying the same case normalization. Broad and event-scoped searches therefore
recognize equivalent spellings such as Greek `Σ`/`ς` and composed/decomposed `É`, including after saved-data reload.
For `n/` phrases, repeated ordinary spaces in both the stored name and query are collapsed for comparison only.
Stored names, phrase order, partial matching, and other fields' internal-spacing semantics are preserved.
`find t/100m` also matches an event such as `4x100m`; exact-event matching is not implemented. Event names are joined
with line breaks, which keeps ordinary single-line keywords from spanning two names.

`FindCommand` replaces the active predicate through `Model.updateFilteredPersonList`. It searches the full roster,
preserves the active sort, and starts displayed indexes at 1. Consecutive `find` commands do not narrow earlier
results: put all required fields in one command. `find` and `filter` replace one another; `list` removes the filter.
None of these view changes saves the roster.

**Design choices:** broad OR matching helps when the relevant field is unknown; AND between named fields allows
precise searches without adding another command. Substring matching supports partial names, contacts and events,
with the tradeoff that short terms can produce extra matches. `filter` remains the exact age-category operation.

### Command autocomplete

The command autocomplete feature is facilitated by `CommandBox` and `AutocompleteEngine`.
It provides real-time dropdown suggestions for commands based on the user's input, and an intelligent prefix insertion mechanism to guide the user in completing complex commands.

#### Implementation Details

* `AutocompleteEngine` stores a predefined list of command templates (e.g., `"add n/ a/ p/ e/ addr/"`, `"edit INDEX n/ a/ p/ e/ addr/"`, `"delete INDEX"`, `"remark INDEX r/"`).
* `CommandBox` listens to changes in the `TextField`'s `textProperty`. While typing an initial command word, it queries `AutocompleteEngine#getSuggestions` for matching templates (matching case-insensitively) and displays them using a JavaFX `Popup` containing a `ListView`. Once whitespace or parameters are entered, or if there are no matches, the popup is hidden.
* **Selection:** When the user selects a suggestion (via Mouse Click or `Tab`), `CommandBox` inserts the command word followed by a space (e.g., `add `), positions the caret, and hides the popup. This leaves space for the user to insert an index or parameters if required.
* **Separation of Selection and Execution:** Pressing `Enter` does **not** select or apply autocomplete suggestions. If the user presses `Enter` while the suggestion dropdown is visible, `CommandBox` hides the popup and executes the command as typed in the text field. This prevents accidental template insertions when a fast typist intends to submit their input.
* **Incremental Prefix Insertion:** If the user presses `Tab` while the suggestion dropdown is closed (or after selecting a command), `CommandBox` uses `AutocompleteEngine#getNextPrefix` to scan the current input and append the next missing prefix based on the command's template. Existing prefixes are matched case-insensitively and regardless of order, allowing the user to construct the command sequentially by pressing `Tab` after entering each parameter's data.
  Prefix completion uses `ArgumentTokenizer` so escaped literal tokens do not count as supplied parameters.

#### Design Considerations

* **Aspect: Suggestion selection key (`Tab` only vs `Tab` and `Enter`)**
  * **Alternative 1:** Allow both `Tab` and `Enter` to select suggestions.
    * Pros: Familiar to users accustomed to web search bars that accept suggestions on `Enter`.
    * Cons: Interferes with command execution. A fast typist who types a command and presses `Enter` may accidentally apply an unwanted suggestion template instead of executing what they typed.
  * **Alternative 2 (Current Choice):** Use `Tab` exclusively to select suggestions and insert prefixes; reserve `Enter` strictly for command execution.
    * Pros: Follows established CLI and code-editor conventions (e.g., bash, zsh, IDE snippets) where `Tab` completes and `Enter` executes. Eliminates accidental command template overwrites upon submission.
    * Cons: Users who expect `Enter` to confirm a suggestion must use `Tab` or mouse click.
* **Aspect: Prefix insertion strategy**
  * **Alternative 1:** Insert the entire command template at once (e.g., `add n/ a/ p/ e/ addr/`).
    * Pros: Shows the user all required fields immediately.
    * Cons: If the user accidentally executes the command before filling all fields, the parser fails with an invalid format error. It also requires manual navigation between the placeholders.
  * **Alternative 2 (Current Choice):** Incrementally insert prefixes using `Tab`.
    * Pros: Creates a guided, IDE-like snippet experience. Prevents trailing empty prefixes from causing parsing errors.
    * Cons: Requires slightly more complex logic to parse the current text and determine the next missing prefix.

### Athlete remarks

`RemarkCommandParser` implements `Parser<RemarkCommand>` and reports invalid arguments with `ParseException`.
It requires one `r/` prefix and parses a positive, one-based index before creating the command.
The command resolves that index against the displayed list during execution.

<puml src="diagrams/add-remark/ParserClass.puml" width="650" alt="RemarkCommandParser realizes Parser of RemarkCommand and reports ParseException" />

`RemarkCommand` retains the new `Remark` and index, replaces the selected athlete with a copy, and restores the
full roster view. An empty remark removes the note. If both the existing and replacement remarks are empty,
the command throws `CommandException` before mutation, so the current view is retained and no save is attempted.
It declares `changesRoster()` as true, so `LogicManager`
uses the staged save-before-publication path. Success messages use the same athlete summary as add, edit, and
delete, including age category, phone, and email. These diagrams omit helpers, the index field, and result internals.

<puml src="diagrams/add-remark/RemarkClass.puml" width="800" alt="RemarkCommand extends abstract Command, retains Remark, and uses Model during execution" />

### Event tagging

Coaches record the track events each athlete competes in with `tag INDEX t/EVENT...` and remove them with
`untag INDEX t/EVENT...`. Events are stored as the athlete's `Tag` set, so no change to `Person` or the JSON format
was needed. Existing rosters should be backed up before upgrading. Older alphanumeric tags, including names longer
than 30 characters, are preserved without truncation and remain removable with `untag`.

#### Implementation details

* `TagCommandParser` and `UntagCommandParser` extend `EventTagCommandParser`, which parses the shared
  `INDEX t/EVENT [t/EVENT]...` format. It rejects a missing or invalid index, a missing `t/`, and unknown prefixes.
  New events go through `ParserUtil.parseTag`, which trims them and reduces repeated inner whitespace to one space.
  Removal uses the same whitespace normalization but also accepts preserved legacy alphanumeric names.
* `TagCommand` and `UntagCommand` extend `EventTagCommand`, which holds the index and events, looks up the target
  athlete in the displayed list, and replaces that athlete with a copy that has the updated events.
* New `Tag` names are words of letters or digits separated by exactly one space or one hyphen (e.g. `Long Jump`,
  `110m-Hurdles`, but not `Long - Jump`), up to 30 characters; `MESSAGE_CONSTRAINTS` states the same rule. `Tag.fromStoredName` also accepts the older unlimited alphanumeric format without truncation.
  `JsonAdaptedTag` uses it when loading, so a valid legacy event does not trigger recovery of the entire roster.
  `Tag#equals` and `Tag#hashCode` ignore case, so normal `Set` operations treat `100m` and `100M` as
  the same event while the first-entered spelling is kept for display.

The class diagram below is a partial view of the event tagging classes. It shows how the two commands share
parsing and execution steps; message constants, parser helpers such as `ParserUtil`, and the athlete's own event
set are omitted.

<puml src="diagrams/EventTagClassDiagram.puml" width="725" alt="Tag and untag parsers extend generic EventTagCommandParser and create TagCommand and UntagCommand, which extend EventTagCommand; EventTagCommand keeps its events, uses Model during execution, and creates an updated Person" />

`EventTagCommandParser` implements `Parser<T>` with its own type parameter `T`. `TagCommandParser` and
`UntagCommandParser` bind `T` to their command and implement `getUsage()` and `createCommand(...)`, so the shared
`parse(...)` method returns the correct command. Each command keeps the displayed-list `Index` and its non-empty set
of events, but uses `Model` only while executing. `EventTagCommand` provides the shared steps:
`getTargetAthlete(...)` finds the athlete at the index, and the static `replaceEvents(...)` creates and stores an
updated copy of that athlete. `TagCommand` and `UntagCommand` differ only in how they work out the updated events.

The shared parser's `parseEvent(...)` helper applies current constraints to new events. `UntagCommandParser`
overrides it to recognize preserved legacy names too, allowing their removal while `tag` still rejects new names
over 30 characters. These parsing helpers are omitted from the partial diagram.

`TagCommand` adds only the events the athlete does not already have and lists any skipped events in its message.
It fails only when every given event is already present, so that the coach knows nothing changed. `UntagCommand`
fails without removing anything if any given event is not one of the athlete's events, as that usually signals a
typo. Both commands then show the full roster, matching `edit` and `remark`.

#### Design considerations

* **Aspect: How events are entered**
  * **Alternative 1 (current choice):** Free text with light validation.
    * Pros: Supports any event, including unusual ones such as `Javelin 500g`, and avoids rejecting valid input.
    * Cons: The same event can be entered in different forms (e.g. `100m` and `100 m`).
  * **Alternative 2:** Choose from a fixed list of track events.
    * Pros: Consistent names across athletes.
    * Cons: Rejects events not on the list, which is overly strict for coaches whose events differ.
* **Aspect: Commands for changing events**
  * **Alternative 1 (current choice):** Separate `tag` and `untag` commands that add to or remove from the
    existing events.
    * Pros: One event can be changed without retyping the others.
    * Cons: Two commands to learn.
  * **Alternative 2:** Replace all events through `edit INDEX t/...`, as in AB3.
    * Pros: One command.
    * Cons: Adding one event requires retyping every existing event, and a mistake silently removes events.

### Age-category filtering

`TrackFlowParser` dispatches `filter` to `FilterCommandParser`. The parser requires exactly one case-insensitive `a/`
prefix and no preamble, rejects duplicates and unknown prefixes, and reuses `ParserUtil.parseAgeCategory` for the same
canonical categories and normalization as `add`. Unsupported trailing text fails category validation.

`FilterCommand` installs an `AgeCategoryPredicate` through `Model.updateFilteredPersonList`. The predicate compares `Person.getAgeCategory()` with the normalized category. Replacing the predicate searches the full underlying roster, retains the current displayed ordering, including any active sort, and does not mutate records. Feedback distinguishes zero, one, and multiple matches. The filter is a view state and is not persisted across restarts.

The existing `DeleteCommand` resolves indexes against `Model.getFilteredPersonList()`, so it already targets the correct displayed athlete and keeps the predicate active. No delete implementation changes are needed for filtering. `ListAthleteCommand` restores `PREDICATE_SHOW_ALL_PERSONS`; `find` and `filter` replace one another.

`FilterCommandParserTest` covers categories and invalid syntax, `AgeCategoryPredicateTest` covers exact category matching, and `FilterCommandTest` covers routing, feedback, unchanged records, replacing searches, invalid input, compatibility with existing deletion, and `list` restoration.

### Multi-field sorting

`TrackFlowParser` dispatches `sort` to `SortCommandParser`. The parser requires one supported field and accepts one
optional order, defaulting to ascending. It accepts short (`asc`, `desc`) and long (`ascending`, `descending`) order
names without considering letter case, while rejecting missing fields, unsupported values, and extra arguments.

`ModelManager` wraps its existing `FilteredList<Person>` in a `SortedList<Person>`. `SortCommand` installs the selected
comparator through `Model.updateSortedPersonList`, changing only the displayed view rather than the underlying
`roster` order. Sorting composes with `find` and `filter`, and indexed commands continue to resolve athletes against
exactly what the user sees. `ListAthleteCommand` clears both the filter and comparator, restoring the full roster in
saved order. Sorting and filtering are session-only view changes and are not persisted.

Text fields are compared without considering letter case. Name comparison also collapses repeated ordinary spaces
in the comparison key without rewriting the displayed or saved name. Phone sorting converts
`Phone.getSignificantDigits()` to an arbitrary-precision numeric value. The shared key includes a bracketed country
code such as `(+65)` and excludes other bracketed notes, including digits in `(5)` or `(Office 9)`. Displayed and
saved text is unchanged, and large numeric values do not overflow. Duplicate identity uses the same significant
digits key, so note digits are ignored there too.
Age categories use the competition order
`Under 14`, `Under 16`, `Under 18`, `Under 20`, `Open`.

### Future features [Future]

Undo/redo, archiving, guardian links, relay squads, and event grouping are not
implemented in v1.6. Their user stories record possible future scope; they do not describe existing classes or
commit to a release version.

--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**:

* High-school or club track-and-field coaches who manage large student-athlete rosters.
* Coaches who need to maintain athletes' contact details and age categories on their own computer.
* Keyboard-proficient users who type quickly, prefer typing to mouse interactions, and are comfortable learning short text commands.
* Individual users who need a desktop application that works without an account or internet connection.

**Value proposition**: TrackFlow helps a coach maintain and retrieve an organized athlete roster through short keyboard commands, reducing the effort of navigating forms and keeping contact information available between training sessions.

**v1.6 scope**: The release supports adding, listing, editing, remarking, searching, filtering, sorting, tagging and
untagging events, deleting, clearing, built-in help, and exit. New athletes require a name, age category, phone
number, email address, and nonblank address. Remarks can be added or removed separately. Roster changes are saved locally before being published
to the live model; view-only commands do not save roster data.

Future features are marked **[Future]** below. Cloud synchronization and collaborative editing are excluded from
this single-user, local product. Athletes are records, not application users.

### Persistence and recovery status

`ApplicationHome` resolves the real location of the code archive. For a packaged JAR, its parent directory is home,
including when launched through a symbolic link or from another working directory. An IDE/test launch from
unpacked class files uses the process working directory. Resolution does not log because `LogsCenter` needs the
same home before normal application startup. An unresolvable code location stops initialization instead of
silently choosing another folder.

`MainApp` loads `data/trackflow.json` inside that home. If no file exists, it supplies the sample roster; the first
successful roster change creates the file. An intentionally saved empty roster remains empty on restart.
`preferences.json` and rotated logs share the same home. Backups and temporary roster saves use roster-file siblings.

`JsonUserPrefsStorage` rejects null GUI settings and dimensions that are nonpositive or nonfinite, including values
Jackson assigns directly to fields. Missing properties retain their defaults. Coordinates may be null (automatic
placement) or integer positions; the UI handles actual screen visibility. `MainApp.initPrefs` recovers from invalid
preferences with defaults and attempts to save them without changing the roster.

`JsonUtil` requires exactly one JSON root followed only by whitespace, for both rosters and preferences. Extra roots,
garbage and truncation cause a loading error; reading never silently accepts just the first root.
`JsonRosterStorage` converts malformed JSON, repeated keys, null roots, missing or null persons lists, null athletes,
null tag entries, invalid saved field values, and duplicate athletes into `DataLoadingException`. A leading UTF-8
byte order mark is tolerated, and an athlete whose `address` or `remark` is absent loads with an empty value rather
than failing the whole file. When loading fails, `MainApp` preserves the original bytes in a sibling file named
`trackflow.json.unreadable-<UUID>.bak`, logs its path, shows the path in the result display at start-up, and starts
an empty roster. Each failed load creates a separate backup without overwriting earlier backups. Subsequent
successful roster changes can replace the active file while the backup remains available for manual recovery. If
backup creation fails, startup stops before commands become available, protecting the original from being
overwritten.

### User stories

Priorities: `* * *` = high, `* *` = medium, `*` = low. **Implemented** describes v1.6 behavior;
**[Future]** records unimplemented scope without committing it to a release; **Excluded** records an idea outside
this product. Priority does not imply implementation status.

| ID | Priority | Status | As a ... | I want to ... | So that I can ... |
|----|----------|-------|----------|---------------|------------------|
| US01 | `* * *` | Implemented | coach | add an athlete's name, age category, phone number, email, and address | maintain the essential information needed to contact and organize athletes |
| US02 | `* * *` | Implemented | coach | view all athletes and sort them by useful fields and directions | locate athletes and select the correct displayed record |
| US03 | `* * *` | Implemented | coach | permanently delete one selected athlete | remove a record I no longer need |
| US04 | `* * *` | Implemented | returning coach | recover saved roster changes when I reopen TrackFlow | continue work without re-entering athletes |
| US05 | `* * *` | Implemented | coach | receive clear success messages and actionable errors | know whether a command worked and correct mistakes |
| US06 | `* * *` | Implemented | coach | have matching-name records with a shared phone or email rejected, and receive warnings for differently named records sharing contacts | avoid redundant entries without excluding different athletes |
| US07 | `* * *` | Implemented | coach | keep my previous roster unchanged when a change cannot be saved | avoid believing an unsaved update is permanent |
| US08 | `* * *` | Implemented | coach | have unreadable saved data preserved separately when loading fails | retain the possibility of recovering it while starting a new roster |
| US09 | `* *` | Implemented | coach | edit an athlete's details directly | correct information without deleting and re-entering the record |
| US10 | `* *` | Implemented | coach | find athletes using partial values from any searchable field | locate records without scanning the full roster or remembering an exact value |
| US11 | `* *` | Implemented | coach | filter athletes by age category | review athletes in a competition category |
| US12 | `* *` | Implemented | coach | assign and remove event-specialization tags for athletes | identify athletes who train for particular events and keep their specializations current |
| US13 | `* *` | Implemented | new coach using TrackFlow | view built-in command help | learn or recall how to operate the application |
| US14 | `* *` | [Future] | frequent user | recall previously entered commands | reduce repeated typing |
| US15 | `* *` | [Future] | coach | archive and restore athlete records | keep departed athletes' information without including them in the active roster |
| US16 | `* *` | [Future] | coach | record guardians and add or remove their links to athletes | find the appropriate family contact and keep those links current |
| US17 | `* *` | [Future] | coach | organize athletes into relay squads and event groups | review who belongs to each team or event |
| US18 | `* *` | Excluded | coach | record and review personal bests by event | track an athlete's performance progress |
| US19 | `* *` | [Future] | coach | record and review relevant medical notes | consult recorded considerations when planning training |
| US20 | `* *` | [Future] | coach | record and review competition eligibility | identify athletes recorded as eligible for an event |
| US21 | `* *` | [Future] | coach | record and review participation logs | track athletes' involvement in training or competitions |
| US22 | `* *` | [Future] | coach | undo and redo roster changes | recover from accidental changes or reapply them |
| US23 | `* *` | [Future] | coach | view an athlete's contact details, event tags, guardian links, and status notes together | prepare for a meet without searching through separate records |
| US25 | `* *` | [Future] | coach | view all event-specialization tags currently used in the roster | review which disciplines the team covers |
| US26 | `* *` | [Future] | coach | view the contact information of athletes in an event group | communicate practice or meet updates to that group |
| US27 | `* *` | [Future] | coach | link multiple guardians to one athlete | retain alternative contacts when a guardian is unavailable |
| US28 | `* *` | [Future] | coach managing siblings | link one guardian record to multiple athletes | update shared contact details only once |
| US29 | `* *` | [Future] | coach organizing travel | identify athletes without a guardian phone number | collect missing emergency contact information before departure |
| US31 | `* *` | [Future] | relay coach | assign or replace athletes on specific relay legs | maintain a clear running order and adjust the lineup when availability changes |
| US32 | `* *` | [Future] | relay coach | designate alternate runners for a relay squad | record replacements before a meet |
| US34 | `* *` | [Future] | coach | view an athlete's relay-squad and event-group memberships | review the athlete's assignments before making changes |
| US35 | `* *` | [Future] | coach | correct an inaccurate status log entry | keep the athlete's recorded history reliable |
| US36 | `*` | [Future] | coach | import and export roster records in bulk | transfer my own records without entering each one manually |
| US37 | `*` | [Future] | coach | store names with characters beyond v1.6's supported set | preserve athletes' preferred name spellings |
| US38 | `*` | [Future] | coach | store multiple phone numbers and phone extensions | retain alternative ways of contacting an athlete |
| US39 | `*` | [Future] | coach | check whether an email address can receive mail | distinguish a structurally valid address from a reachable one |
| US40 | `*` | Implemented | keyboard-oriented coach | close TrackFlow with a text command | finish a session from the command box |
| US41 | `*` | Excluded | coach using several computers | synchronize my roster through cloud storage | access the same changes on different devices |
| US42 | `*` | Excluded | coach working with other coaches | share and collaboratively edit a roster | coordinate updates with colleagues |

Email reachability checking (US39) is a future enhancement beyond structural email validation. Its design must preserve offline access to the roster and must not require a TrackFlow-operated remote server.

### Use cases

For every use case below, the **system** is TrackFlow and the **primary actor** is the coach. **MSS** means main
success scenario. UC01–UC07 describe implemented workflows. Future requirements remain in the user stories.

#### UC01: Register an athlete

**Related stories**: US01, US02, US05, US06, US07.<br>
**Precondition**: TrackFlow is open.<br>
**Success postcondition**: One complete athlete record is saved and displayed in the roster.

**MSS**

1. The coach requests to view the roster.
2. TrackFlow displays the roster with current indexes, or indicates that it is empty.
3. The coach submits the new athlete's name, age category, phone number, email address, and address.
4. TrackFlow validates the input, checks for a duplicate, and saves the new record.
5. TrackFlow displays the full roster in the current sort order, or saved roster order if no sort is active, and displays the added athlete's details.

Use case ends.

**Extensions**

* 3a. A required field is missing, repeated, or invalid, or an unsupported parameter is supplied.
  * 3a1. TrackFlow shows the applicable error and retains the entered command for correction. The roster is unchanged.
  * Use case resumes at step 3.
* 4a. The normalized name and either phone or email match an existing athlete.
  * 4a1. TrackFlow reports the duplicate and leaves the roster unchanged. Age category does not affect this check. A shared name alone is allowed.
  * Use case resumes at step 3.
* 4b. The change cannot be saved.
  * 4b1. TrackFlow reports the save failure. No athlete is added, and the previous roster remains visible.
  * Use case ends.
* 4c. A differently named athlete shares the phone or email.
  * 4c1. TrackFlow saves the new record and includes a possible-duplicate warning listing each matching athlete's
    name, age category, phone, email, and the matching contact field(s). Hidden athletes are included without using
    displayed indexes.
  * Use case resumes at step 5.

#### UC02: Remove an athlete

**Related stories**: US02, US03, US05, US07.<br>
**Precondition**: TrackFlow is open.<br>
**Success postcondition**: Exactly the selected athlete is permanently removed from the saved roster.

**MSS**

1. The coach requests to list athletes.
2. TrackFlow displays the complete roster in saved roster order with current indexes.
3. The coach checks the athlete's details and requests deletion using the athlete's displayed index.
4. TrackFlow saves the deletion, refreshes and renumbers the roster, and displays the deleted athlete's details.

Use case ends. Deletion has no confirmation prompt, archive, or undo operation.

**Extensions**

* 2a. The roster is empty.
  * 2a1. TrackFlow reports that the roster is empty.
  * Use case ends.
* 3a. The index is missing, malformed, or does not identify an athlete in the current list, or extra arguments are supplied.
  * 3a1. TrackFlow explains the error and leaves the roster unchanged.
  * Use case resumes at step 3.
* 4a. The deletion cannot be saved.
  * 4a1. TrackFlow reports the failure and retains the athlete in the roster.
  * Use case ends.

#### UC03: Edit an athlete's details

**Related stories**: US02, US05, US06, US07, US09.<br>
**Precondition**: TrackFlow is open and the athlete is in the roster.<br>
**Success postcondition**: The selected athlete's updated details are saved; omitted fields, remarks, and events
are retained.

**MSS**

1. The coach views the roster, optionally using find, filter, or sort.
2. TrackFlow displays the matching athletes with current indexes and ordering.
3. The coach submits `edit INDEX` with at least one supported field to change.
4. TrackFlow resolves the displayed index, validates the proposed record, and checks the full roster for duplicates.
5. TrackFlow saves the change, displays the full roster with the active sort retained, and confirms the edit.

Use case ends.

**Extensions**

* 3a. The index or field values are missing, invalid, repeated, or unsupported.
  * 3a1. TrackFlow reports the error and leaves the roster and display unchanged.
  * Use case resumes at step 3.
* 3b. All supplied values are the same as the athlete's stored details.
  * 3b1. TrackFlow reports that at least one field must differ, without saving or changing the view. A change to
    the stored phone formatting or note is treated as an edit.
  * Use case resumes at step 3.
* 4a. The edit would duplicate another athlete, including one hidden by the current filter.
  * 4a1. TrackFlow rejects the edit and leaves the roster unchanged.
  * Use case resumes at step 3.
* 5a. Saving fails.
  * 5a1. TrackFlow reports the failure and retains the previous roster, filter, and sort order.
  * Use case ends.

#### UC04: Continue working with a saved roster

**Related stories**: US04, US07, US08.<br>
**Precondition**: A saved roster exists, or this is the first launch.<br>
**Success postcondition**: On reopening, the coach sees the saved roster, including successful additions, edits, remark updates, deletions, and clearing.

**MSS**

1. The coach closes TrackFlow.
2. The coach launches TrackFlow again.
3. TrackFlow loads the saved roster into the displayed list.
4. The coach requests to list athletes.
5. TrackFlow displays the saved athletes with their current indexes.

Use case ends.

**Extensions**

* 3a. No saved roster exists, as on first use.
  * 3a1. TrackFlow starts with sample athletes and logs that no saved roster was found. The data file is created after the first successful roster change.
  * Use case resumes at step 4.
* 3b. The saved roster cannot be loaded.
  * 3b1. TrackFlow logs the loading failure and backup path, and starts with an empty roster while preserving the unrecovered data in a separate file.
  * 3b2. Subsequent successful changes create a new active roster without replacing the preserved data. TrackFlow does not claim the old records were restored; the preserved copy remains until the coach removes it outside TrackFlow.
  * Use case resumes at step 4.
* 3c. The saved roster cannot be loaded and its backup cannot be created.
  * 3c1. TrackFlow stops startup before accepting commands to protect the original file.
  * Use case ends.

#### UC05: Filter athletes by age category

**Status**: Implemented in v1.6.<br>
**Related stories**: US02, US03, US05, US11.<br>
**Precondition**: TrackFlow is open.<br>
**Success postcondition**: Only matching athletes are displayed; stored records are unchanged.

**MSS**

1. The coach enters `filter a/AGE_CATEGORY` with one supported category.
1. TrackFlow searches the full roster and displays matching athletes in the current displayed ordering, including any active sort, with current indexes and the match count.
1. The coach enters `list` to restore the complete roster, or uses the displayed indexes with existing commands.

**Extensions**

* **1a.** The category is missing, invalid, repeated, or accompanied by extra arguments. TrackFlow reports the error and retains the previous display and roster.
* **2a.** No athletes match, including when the roster is empty. TrackFlow displays an empty list and an explicit zero-match message. `list` remains available.

#### UC06: Replace or remove an athlete remark

**Related stories**: US05, US07.<br>
**Precondition**: TrackFlow displays the athlete to update.<br>
**Success postcondition**: The replacement remark, or its removal, is saved without changing other stored fields.

**MSS**

1. The coach submits `remark INDEX r/REMARK` using the current displayed index.
2. TrackFlow replaces the existing remark; an empty value removes it.
3. TrackFlow saves the change, restores the full roster with the active sort retained, and confirms the update.

**Extensions**

* **1a.** The index is invalid or the required `r/` prefix is missing or repeated. TrackFlow reports the error and
  leaves the roster unchanged.
* **2a.** The coach requests removal when the athlete has no remark. TrackFlow reports that no remark can be
  removed, retaining the current roster and view without saving.
* **3a.** Saving fails. TrackFlow reports the failure and leaves the live roster, filter, and sort order unchanged.

#### UC07: Add or remove an athlete's track events

**Related stories**: US05, US07, US12.<br>
**Precondition**: TrackFlow displays the athlete to update.<br>
**Success postcondition**: The athlete's events are updated and saved without changing other stored fields.

**MSS**

1. The coach submits `tag INDEX t/EVENT [t/EVENT]...` using the current displayed index.
2. TrackFlow adds the events the athlete does not already have.
3. TrackFlow saves the change, shows the full roster, and confirms which events were added.

Use case ends.

**Extensions**

* **1a.** The index is invalid, no `t/` is given, or an event is invalid. TrackFlow reports the error and leaves the
  roster unchanged.
* **2a.** The athlete already has some of the events. TrackFlow adds the rest and also lists the skipped events.
* **2b.** The athlete already has all of the events. TrackFlow reports this and leaves the roster unchanged.
* **3a.** Saving fails. TrackFlow reports the failure and leaves the live roster unchanged.

Removing events with `untag INDEX t/EVENT [t/EVENT]...` follows the same steps, except that if any event is not one of
the athlete's events, TrackFlow reports it and removes nothing.

### Non-Functional Requirements

These are product requirements and acceptance targets, not claims about the current build. Platform, packaging, storage, and display requirements reflect the relevant [course product constraints](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-constraints.html).

| ID | Requirement |
|----|-------------|
| NFR01 | TrackFlow shall run on Windows, Linux, and macOS with Java 25 installed, without requiring another Java version. |
| NFR02 | TrackFlow shall be distributed as a single JAR that runs without an installer or separately installed application dependencies other than the specified Azul Zulu JDK 25 runtime (JDK FX on macOS). |
| NFR03 | Core roster operations and local persistence shall work without internet access, an account, or a TrackFlow-operated remote server. |
| NFR04 | TrackFlow shall support one coach using a local roster. Shared accounts, simultaneous editing, and another user's access to the data file during normal operation are outside its scope. |
| NFR05 | Roster data shall use locally stored, human-editable text files rather than a database management system. Valid manual edits made while TrackFlow is closed shall be loaded on the next launch. |
| NFR06 | Adding, listing, and deleting athletes shall be possible entirely by keyboard. Successful commands shall clear and refocus the command box; failed commands shall retain their text for correction. |
| NFR07 | Success and failure shall be communicated in text, without relying on color alone. Validation failures shall identify the input problem and shall not change the roster. |
| NFR08 | A change shall be reported as successful only after it has been saved. A save failure shall leave the observable roster unchanged. Successfully saved changes shall survive normal closure and reopening. |
| NFR09 | A loading failure shall not silently overwrite unrecovered data. That data shall remain separately preserved even if the coach makes changes to the new empty roster. |
| NFR10 | As a performance target, with 1,000 athlete records on a computer with a dual-core processor, 8 GB RAM, and local SSD storage, at least 95 of 100 consecutive add, list, or delete commands shall update the display within two seconds of submission, with persistence included for changes. |
| NFR11 | At 1920 × 1080 or higher with 100% and 125% display scaling, the interface shall avoid clipping essential controls and feedback. At 1280 × 720 or higher with 150% scaling, all functions shall remain accessible, including through scrolling or resizing where necessary. |
| NFR12 | For identical input and roster state, validation shall report the same first error. Listing shall use saved roster order. An active sort shall remain applied across find, filter, and roster changes until list or restart; sorting shall not change saved roster order. |

### Glossary

| Term | Definition |
|------|------------|
| Active roster | The athlete records currently maintained and displayed by the coach, excluding any separately preserved unrecovered data and, if archiving is introduced, archived records. |
| Age category | A coach-assigned competition grouping: Under 14, Under 16, Under 18, Under 20, or Open. TrackFlow stores the category, not an exact age or date of birth, and does not verify competition eligibility. |
| Athlete | A student-athlete whose contact and age-category information is recorded in TrackFlow. An athlete does not log in to the application. |
| Archive / restore | A future capability to remove a record from the active roster while retaining it, and later return it to the active roster. This differs from permanent deletion. |
| CLI / GUI | Command-line interface / graphical user interface. TrackFlow accepts typed commands in a graphical window and uses that window to display records and feedback. |
| Displayed index | An athlete's positive, one-based position in the current displayed list. It is not a permanent identifier and may change after roster updates. |
| Duplicate athlete record | A record with the same normalized name and either the same phone or email as another record, regardless of age category. Differently named records sharing contacts are allowed with a possible-duplicate warning on add or edit. |
| Eligibility | Whether an athlete meets the recorded participation conditions for a competition or event. Eligibility logs are a future feature, distinct from assigning an age category. |
| Event specialization | An athletics discipline, such as sprinting or long jump, associated with an athlete through the `tag` and `untag` commands. |
| Guardian | A parent or other responsible adult whose future contact record may be linked to one or more athletes. |
| Local persistence | Automatically saving roster changes on the coach's computer and loading them in a later application session. |
| Implemented | A feature available in the current v1.6 code. Future features are identified separately. |
| Normalized field | A value prepared for comparison: surrounding whitespace is removed; name and age-category internal spaces are collapsed; name, category, and email comparisons ignore letter case; phone comparisons ignore permitted formatting characters and use the digits. Display formatting may be retained. |
| Participation log | A future record of an athlete's involvement in training or competition. |
| Relay squad / event group | A future grouping of athletes for a relay team or a particular athletics event. |
| Unrecovered data | Previously saved roster data that TrackFlow could not load, preserved in a separate `.bak` file before starting an empty roster. If preservation fails, startup stops. |

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

<box type="info" seamless>

**Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.
</box>

### Before testing

Use a disposable, writable folder containing a copy of the release JAR. These procedures add, replace and delete
saved records. Keep real rosters elsewhere, and use only one running instance for each test folder.
Record the release tag/JAR filename, `java -version`, operating system, and display scaling when reporting results.
Use the runtime from the [setup guide](SettingUp.md#runtime-and-build-requirements).

Enter **one command at a time**, even in the command blocks below: the GUI input is one line and removes pasted
line breaks. The steps test the packaged JAR. Automated classes mentioned separately require the source checkout.

When a procedure asks for the **three-athlete fixture**, restart TrackFlow first to reset session sorting, then run:

```text
clear
Y
add n/Blake Lee a/Open p/92222222 e/blake@example.com addr/20 Stadium Road
add n/Avery Tan a/Under 14 p/91111111 e/avery@example.com addr/10 Stadium Road
add n/Casey Lim a/Under 14 p/93333333 e/casey@example.com addr/30 Stadium Road
```

Expected saved order: Blake, Avery, Casey. No filter or sort is active. Preparing this fixture replaces all records
in that disposable roster; it is not a command to use on real athlete data.

### Launch and shutdown

1. Initial launch

   1. Download the JAR file and copy it into an empty folder.

   1. In that folder, run `java -jar TrackFlow.jar` with the runtime from the setup guide.<br>
      Expected: The GUI opens with a set of sample athletes. The window size may not be optimal.

1. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   1. Relaunch from the same folder using `java -jar TrackFlow.jar`.<br>
       Expected: The most recent normal window size and location are retained, fitted inside the current screen's
       usable area. Maximized, minimized and full-screen dimensions do not replace normal bounds.

   1. With the app closed, try oversized saved dimensions and coordinates belonging to a disconnected monitor.
      Relaunch and verify that the whole window fits on an available screen, with input and footer reachable.

   1. Try a 450 by 400 window, long fields and many event tags. Scroll the roster and long feedback, execute commands,
      and verify that the stage does not grow automatically. Check 1920 by 1080 at 100% and 125% scaling, and
      usability at 1280 by 720 at 150%. Record the actual display/scaling tested, separately from helper tests.

1. Run `exit`, then relaunch using `java -jar TrackFlow.jar`. Expect the saved roster to reload; session filters and sorting are reset.

### Deleting an athlete

1. Deleting an athlete while all athletes are being shown

   1. Prerequisites: List all athletes using the `list` command, with multiple athletes in the list.

   1. Test case: `delete 1`<br>
      Expected: The first athlete is deleted from the list. The status message shows the deleted athlete's details.

   1. Test case: `delete 0`<br>
      Expected: No athlete is deleted. The status message shows error details.

   1. Other incorrect delete commands to try: `delete`, `delete x`, `delete 999999999999999999999999999999`<br>
      Expected: Similar to previous.

1. Run `sort name desc`, then `delete 1`. Expect the first visibly displayed athlete to be removed. Restart and confirm the deletion persists.

### Filtering by age category

Use a separate test roster for this procedure. The deletion steps test compatibility with the existing delete command and permanently remove test records.

1. Prepare the [three-athlete fixture](#before-testing). Blake is `Open`; Avery and Casey are `Under 14`.
1. Run `filter a/under   14`. Expect the two `Under 14` athletes in roster order, numbered 1 and 2, and `Displaying 2 athletes in age category Under 14.`
1. Run `delete 3`. Expect an invalid-index error and no changes, even though the full roster has three athletes.
1. Run `delete 1`. Expect the first `Under 14` athlete to be deleted and the other to appear at index 1.
1. Run `list`. Expect both remaining athletes, including the `Open` athlete.
1. Run `filter a/Under 20`. Expect an empty display and `No athletes found in age category Under 20 (0 matches).`
1. Run `list`. Expect both remaining athletes.
1. Run `filter a/Open`. Expect one match and `Displaying 1 athlete in age category Open.`
1. Try each invalid command separately: `filter`, `filter a/Under 15`, `filter a/`, and `filter a/Open a/Under 14`. Expect an error after each command and no change to the display.
1. Run `find n/Casey`. Expect Casey, the remaining `Under 14` athlete.
1. Run `filter a/Open`. Expect the `Open` athlete, confirming that filtering searches the full roster.
1. Restart TrackFlow. Expect both remaining athletes. The filter is not saved between sessions.

### Sorting the displayed roster

Prepare the [three-athlete fixture](#before-testing), then add the following athletes one command at a time so all
five age categories can be tested:

```text
add n/Dan Ho a/Under 16 p/94444444 e/dan@example.com addr/40 Stadium Road
add n/Eve Ng a/Under 18 p/95555555 e/eve@example.com addr/50 Stadium Road
add n/Faye Teo a/Under 20 p/96666666 e/faye@example.com addr/60 Stadium Road
```

1. Run `sort name`. Expect case-insensitive alphabetical name order and the ascending success message.
1. Run `sort name desc`. Expect the reverse name order.
1. Run `filter a/Under 14`, then `sort name desc`, then `list`. Expect every athlete in saved roster order.
1. Run `sort age`. Expect `Under 14`, `Under 16`, `Under 18`, `Under 20`, then `Open`.
1. Run `sort age descending`. Expect the reverse category order.
1. Run `sort phone`, `sort email`, and `sort address` separately, then repeat each with `desc`. Verify numeric phone
   ordering and case-insensitive text ordering. This fixture has no empty addresses; if testing an older roster
   containing one, it should appear first in ascending address order and last in descending order.
1. Run `filter a/Under 14`, then `sort name desc`. Expect only Casey and Avery, in that order.
1. Run `list`. Expect the full roster in saved roster order.
1. Run `sort name desc`, note the athlete visibly shown at index 1, then run `delete 1`. Expect that athlete to be
   deleted.
1. Try `sort`, `sort height`, `sort name upwards`, and `sort name asc extra`. Expect actionable errors and no display
   change.
1. Restart TrackFlow. Expect the remaining athletes in saved roster order; the deletion persists but sorting does not.

For a separate name-spacing regression, use another disposable home folder and enter these commands one at a time:

```text
clear
Y
add n/Lee  Bob a/Open p/911 e/bob@example.com addr/Home
add n/Lee Ann a/Open p/922 e/ann@example.com addr/Home
sort name
```

Expect Lee Ann before Lee Bob, even though Bob's stored name has two spaces. Run `sort name desc` and expect the
reverse order. Both names must retain their original spacing on screen and in the saved file. Restart: saved roster
order is Bob then Ann, and Bob's two spaces remain intact. `SortCommandTest` and `LogicManagerTest` also cover
equivalent name keys, both directions, subsequent adds/edits, and unchanged text after a fresh storage read.

For a separate phone-note regression, use another disposable home folder and enter these commands one at a time:

```text
clear
Y
add n/Aaa Tan a/Open p/200 (5) e/aaa@example.com addr/Home
add n/Bbb Tan a/Open p/300 e/bbb@example.com addr/Home
add n/Ccc Tan a/Open p/(+65) 200 e/ccc@example.com addr/Home
sort phone
```

Expect Aaa, Bbb, Ccc: the note digit is ignored, while the country code contributes to the numeric key. Run
`sort phone desc` and expect Ccc, Bbb, Aaa. Phone text must remain unchanged. Restart, sort again, and verify both
orders. `SortCommandTest` and `LogicManagerTest` also cover formatted values, zero-valued numbers with numeric notes,
large numbers, subsequent updates, and exact phone-text preservation after a fresh storage read.

For an indexed-command regression with tied sort values, prepare a separate disposable roster:

```text
clear
Y
add n/Alice a/Open p/111 e/alice@example.com addr/Home
add n/Bob a/Open p/222 e/bob@example.com addr/Home
tag 1 t/Original
tag 2 t/Original
sort name desc
sort age
```

Expect Bob at index 1 and Alice at index 2. Run `delete 1` and verify that Bob is removed, including after restart.
Recreate the fixture before each of `edit 1 addr/Changed`, `remark 1 r/Changed`, `tag 1 t/Changed`, and
`untag 1 t/Original`. Each must change Bob, leave Alice unchanged, and persist the result after restart.
Repeat after adding a third athlete in `Under 14` and running `filter a/Open` before the two sorts.
`DisplayedIndexPersistenceTest` also verifies these cases with failed saves, preserving the live roster, predicate,
comparator, displayed order and saved file.

### Command autocomplete

When a step asks you to type a new command below, replace the previous input rather than appending to it.

1. Real-time command suggestions
   1. Type `a` in the command box.<br>
      Expected: A dropdown popup appears showing `add n/ a/ p/ e/ addr/`.
   2. Type `f`.<br>
      Expected: The dropdown shows `filter a/` and `find `.
   3. Type `ed`.<br>
      Expected: The dropdown shows `edit INDEX n/ a/ p/ e/ addr/`.
   4. Type `d`.<br>
      Expected: The dropdown shows `delete INDEX`.
   5. Replace the input with `f`, then press the Down and Up arrow keys.<br>
      Expected: Selection moves between the suggestions in the list.
   6. Press Enter while suggestions are displayed.<br>
      Expected: The dropdown closes without auto-completing, and TrackFlow executes the typed input (showing an unknown command error for `f`).
   7. Type `ed`, then press Escape.<br>
      Expected: The dropdown closes and the command box still contains `ed`.

2. Selecting a suggestion with Tab
   1. Type `ad` and press Tab (or click the suggestion with the mouse).<br>
      Expected: The suggestion is applied and the command box updates to `add `. The dropdown closes.

3. Incremental prefix insertion with Tab
   1. With `add ` in the command box, press Tab.<br>
      Expected: The first required prefix is inserted (`add n/ `).
   2. Type `John Doe` and press Tab.<br>
      Expected: The next missing prefix is inserted (`add n/ John Doe a/ `).
   3. Continue pressing Tab after typing parameter values for each field.<br>
      Expected: Prefixes `p/`, `e/`, `addr/` are sequentially inserted with appropriate spacing.
   4. Enter prefixes out of order, e.g., type `add a/Open ` and press Tab.<br>
      Expected: The first missing prefix `n/` is inserted (`add a/Open n/ `).

### Saving data

1. Run `clear`, then `Y`, `add n/Avery Tan a/Under 14 p/91234567 e/avery@example.com addr/Home`, and `tag 1 t/100m`
   separately, then restart TrackFlow.<br>
   Expected: Avery and the `100m` event remain after the restart.
1. On this disposable roster, temporarily remove write permission from
   the folder containing the roster data file. Then attempt each roster-changing command separately:
   `add n/Blake Tan a/Open p/81234567 e/blake@example.com addr/Home`, `edit 1 p/999`, `remark 1 r/Test`,
   `tag 1 t/400m`, `untag 1 t/100m`, `delete 1`, and `clear`, then `Y`.<br>
   Expected: TrackFlow reports that it could not save. The roster, displayed athletes, filter, sort order, and data file
   remain unchanged after every attempt.
1. While storage is unwritable, run `list`, `find n/Avery`, `filter a/Under 14`, `sort name`, `help`, and `exit` separately.<br>
   Expected: Each command succeeds because it does not need to rewrite roster data.
1. Restore write permission, relaunch the app if you used `exit`, and run a roster-changing command.<br>
   Expected: The command succeeds, the status indicates that all changes are saved, and the change survives restart.

### Missing or corrupted data file recovery

1. Missing data file
   1. Delete `data/trackflow.json` in a test directory (or launch TrackFlow in a fresh folder without a `data` directory).
   2. Launch TrackFlow with `java -jar TrackFlow.jar`.<br>
      Expected: TrackFlow detects the absent file, logs that it is starting with the sample roster, and opens with sample athletes displayed.
   3. Run a roster-changing command (e.g., `remark 1 r/Test`).<br>
      Expected: `data/trackflow.json` is created containing the updated roster data.
2. Corrupted data file with writable folder
   1. Close TrackFlow. Back up the disposable `data/trackflow.json`, then replace its contents with `{` and save.
   2. Launch TrackFlow.<br>
      Expected: TrackFlow copies the corrupted file to `data/trackflow.json.unreadable-<UUID>.bak`, logs the backup path, displays a recovery warning with that path, and launches with an empty roster.
   3. Inspect the `data` folder.<br>
      Expected: The `.bak` file contains the exact unreadable data. Any new roster modifications write to `data/trackflow.json` without modifying or deleting the backup.
3. Corrupted data file with read-only folder
   1. Close TrackFlow. Replace `data/trackflow.json` with `{` as above, and use folder permissions to block creation of a backup file. Verify that the operating system actually denies a write in that folder.
   2. Launch TrackFlow.<br>
      Expected: TrackFlow logs a severe error and displays a blocking error dialog. No commands can execute and the
      original file remains unchanged. Dismiss the dialog; TrackFlow then exits.

### Adding, editing, and remarks

Use a disposable home folder; these procedures change saved data.

1. Run `clear`, then `Y`, then `add n/Avery Tan a/Under 14 p/91234567 e/avery@example.com addr/Home`.
   Expect one saved athlete. Restart from the same folder and confirm every supplied field remains.
1. Run `remark 1 r/Sprints`, then `edit 1 a/Under 16 addr/Training centre`. Expect the new category and address,
   with the original name, phone, email, and remark retained. Restart and verify those values again.
1. Run `edit 1 n/Avery Tan` without changing any other details. Expect
   `At least one field must be different from the athlete's current details.`, with no save or change to the view.
1. Run `remark 1 r/Recovery`, then restart. Expect `Recovery` to replace `Sprints`.
1. Run `remark 1 r/`, then restart. Expect the remark to be empty and the category still `Under 16`.
1. Run `remark 1 r/` again. Expect `Athlete has no remark to remove` with the athlete's details, with no save
   or change to the current view.
1. Try `add n/Incomplete`, `edit 1`, `edit 0 p/999`, `edit 1 addr/`, and `remark 1`.
   Expect errors with no roster changes. Adding `t/sprinter` to an otherwise valid add or edit command must produce
   an error identifying `t/` as an unsupported prefix in both cases.
1. Run `clear`, then `Y`, restart, and confirm the roster stays empty rather than loading samples again.

### Parser robustness

Use a disposable roster with at least one athlete. Check the roster and saved file after each rejected command.

1. Run `edit 1 addr/Block A//B addr//Annex`, followed by `remark 1 r/Practise r//relay x//y Mon/Tue`.
   Expect address `Block A/B addr/Annex` and remark `Practise r/relay x/y Mon/Tue`. Restart and verify both values.
1. Run `find A//B`, `find addr/Block A//B`, and `find r/r//relay` separately.
   Expect the athlete in each result. Search decodes its input once; the saved address and remark remain unchanged.
1. Run `edit 1 addr/C:\Training`. Expect the ordinary backslash to remain in the stored address.
1. Paste commands only as single lines, replacing tabs and line breaks with ordinary spaces. The GUI removes these
   characters; it does not run a multiline script. Developer-only multiline parser checks are in
   `ParserRobustnessTest`, described in the [testing guide](Testing.md#build-and-focused-tests).
1. Try `edit 1 p/999 P/888`, `remark 1 r/first R/second`, `find n/`, and `edit 1 x/value`.
   Expect errors without roster, display, or file changes.
1. Run `edit 1 addr/Block A///B`, then `find addr/Block A///B`.
   Expect the stored address to contain `A//B`; it must not be decoded twice.
1. Run `edit 1 addr/ /home/coach` and `remark 1 r/ //server/share`.
   Expect the leading slash and network path to remain intact. `remark 1 r/` should still remove the remark.
1. Try `delete 0`, `delete -1`, `delete +1`, `delete 1.0`, and a delete index containing thousands of digits.
   Expect controlled errors without deletion. `edit 0001 p/999` should update the first displayed athlete.

### Finding athletes

Prepare the [three-athlete fixture](#before-testing), then run these setup commands separately:

```text
remark 2 r/Injured sprinter
remark 1 r/Relay captain
tag 2 t/100m
tag 1 t/4x100m
```

1. Run `find ave under`. Expect Avery and Casey: broad keywords are combined with OR and may match any field.
1. Run `find n/Avery`. Expect only Avery; matching text in another field does not satisfy a name criterion.
1. Run `find a/Under 14`. Expect Avery and Casey.
1. Run `find t/100m`. Expect Avery and Blake because event matching is partial, so `4x100m` also matches.
1. Run `find r/injured`. Expect only Avery.
1. Run `find n/Avery a/Open`. Expect no matches, because different scoped fields are combined with AND.
1. Run `find n/Avery a/Under 14`. Expect only Avery.
1. Run `find Blake n/Avery a/Under 14`. Expect Blake OR an athlete satisfying both scoped fields: Blake and Avery.
1. Run `find n/Avery`, followed by `find a/Open`. Expect Blake after the second command; consecutive searches replace
   the previous selection rather than intersecting it.
1. Try `find`, `find n/`, and `find x/keyword` separately. Expect errors and the previous display to remain unchanged.
1. Run `list`. Expect all three athletes again.

### Tagging athletes with events

Use a disposable roster. Run `clear`, then `Y`, then the following commands separately to ensure a known starting state:

```text
add n/Avery Tan a/Under 14 p/91234567 e/avery@example.com addr/Home
tag 1 t/100m
```

1. Run `tag 1 t/Javelin t/4x100m`. Expect both new labels alongside `100m`.
1. Run `tag 1 t/javelin t/Shot Put`. Expect only `Shot Put` to be added, with `Javelin` (the stored name) reported
   as already present.
1. Run `tag 1 t/JAVELIN`. Expect an already-present error and no changes.
1. Run `untag 1 t/Shot Put t/Discus`. Expect an error naming `Discus`; `Shot Put` must remain.
1. Run `untag 1 t/shot put`. Expect `Shot Put` to be removed. Restart and verify the remaining labels persist.
1. Run `tag 1 t/Hammer 7.26kg t/Women's 100m`. Expect both events to be added, keeping the decimal point and the
   apostrophe.
1. Try `tag 1`, `tag t/100m`, `tag 0 t/100m`, `tag 1 t/`, `tag 1 t/100m*`, `tag 1 t/Long - Jump`, `tag 1 t/7.kg`,
   `tag 1 t/Women''s`, and `tag 1 t/` followed by 31 letters. Expect an error after each command and no changes.

### Help and clearing a test roster

1. Run `help`. Expect a scrollable command guide. Resize it and check that command descriptions remain reachable.
1. Close help, then run `help` again. Expect it to reopen without changing the roster or losing the current filter.
1. On the disposable roster only, run `clear`. Expect a yellow warning; the roster must remain unchanged, including after
   restarting. Run `clear`, then `Y` to delete all athletes permanently. Restart and verify the roster remains empty.

### Duplicate identity and complete equality

Duplicate checks use normalized name plus **either** matching significant phone digits or matching email, regardless
of age category. Bracketed notes are ignored; a country code such as `(+65)` counts as part of the phone number.
Name normalization ignores case and repeated spaces; email matching ignores case. Complete equality applies each
field's equality rules across every field, including category, address, remark, and events. Phone formatting and
event-name case do not by themselves make records unequal. These are separate contracts.

1. Start with only Avery: run `clear`, then `Y`, then `add n/Avery Tan a/Under 14 p/91234567 e/avery@example.com addr/Home`. Attempt to add `AVERY   TAN` with the same phone and a different
   email and category. Expect a duplicate error, with no saved changes. Repeat with a different phone and matching
   email (including different email case); expect rejection again.
1. Add a record with the same name but both a different phone and a different email. Expect success.
1. Add `Jordan Lee` with Avery's phone and a different email. Expect success with a possible-duplicate warning listing
   Avery's name, category, phone and email, with `Matches: phone`. Edit Jordan to a different phone and Avery's email
   in different case; expect `Matches: email`. Match both contacts and expect one Avery entry with
   `Matches: phone and email`. Different names sharing contact details are permitted.
1. Run `find n/Jordan`, then attempt to add Avery's duplicate. Expect rejection even though Avery is hidden.
1. With Jordan displayed, attempt to edit the name to `AVERY TAN` while retaining Avery's phone. Expect rejection
   because duplicate checks search the complete roster. The previous filter and roster remain unchanged.
1. Restart and repeat the duplicate attempts. Expect the same decisions after reloading.
1. In a separate empty disposable roster, add two `Alex Tan` records with different phones and emails. Hide both
   using `find n/Nobody`, then add `Blake Tan` with the first Alex's phone and the second Alex's email. Expect two
   separate warning entries identifying each Alex's contact details, with `Matches: phone` and `Matches: email`
   respectively. Repeat after a restart and an edit while only Blake is displayed. The warning must not borrow
   indexes from the filtered list or include Blake as their own match.
Developer-only check: the automated `PersonTest` and `DuplicatePersistenceTest` checks in the
[testing guide](Testing.md#build-and-focused-tests) verify complete equality separately from duplicate identity.
Address-only and remark-only changes affect full equality without changing duplicate identity.
