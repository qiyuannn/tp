---
  layout: default.md
  title: "Antony Dennis Chakola - Project Portfolio"
---

# Antony Dennis Chakola - Project Portfolio

### Project: TrackFlow
TrackFlow is a desktop application for track and field coaches to manage their athlete rosters. It provides a fast, command-driven interface alongside a graphical view of athletes, their contact details, age categories, and events.

### Code contributions
- **Command Autocomplete**: Built the autocomplete engine and dropdown in the command box. It suggests command templates as the command word is typed, and Tab inserts the next missing prefix, recognizing prefixes already typed in any order or case.
- **Event Tags**: Authored the `tag` and `untag` commands on a shared base command and generic parser, and enabled `find` to search athletes by event. Designed the event name rules, including case-insensitive matching, accented and non-English letters, apostrophes, and decimals (e.g. `Hammer 7.26kg`).
- **Other Features**: Implemented the `list` command and tightened name validation to reject dangling or repeated hyphens and apostrophes.

### Testing
- Wrote the unit tests for event tags, the tag and untag commands, and the autocomplete engine, covering every line of these classes. Added JavaFX tests for the autocomplete dropdown.

### Documentation
- **User Guide**: Wrote the command autocomplete and `tag`/`untag` sections, with examples that match the sample data.
- **Developer Guide**: Wrote the command autocomplete and event tagging sections, including the event tagging class diagram and their manual testing steps.

### Project management and team contributions
- **Role**: Deliverables and deadlines expert
- Reviewed 26 teammate pull requests, reported 5 alpha-testing bugs, and tested the whole product against the guides before the v1.5 release, reporting the bugs found to their owners.
