package trackflow.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.logic.Messages.MESSAGE_EMPTY_COMMAND;
import static trackflow.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static trackflow.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static trackflow.logic.parser.CliSyntax.PREFIX_REMARK;
import static trackflow.logic.parser.CliSyntax.PREFIX_TAG;
import static trackflow.testutil.Assert.assertThrows;
import static trackflow.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import trackflow.logic.commands.AddCommand;
import trackflow.logic.commands.ClearCommand;
import trackflow.logic.commands.Command;
import trackflow.logic.commands.DeleteCommand;
import trackflow.logic.commands.EditCommand;
import trackflow.logic.commands.EditCommand.EditPersonDescriptor;
import trackflow.logic.commands.ExitCommand;
import trackflow.logic.commands.FilterCommand;
import trackflow.logic.commands.FindCommand;
import trackflow.logic.commands.HelpCommand;
import trackflow.logic.commands.ListAthleteCommand;
import trackflow.logic.commands.RemarkCommand;
import trackflow.logic.commands.SortCommand;
import trackflow.logic.commands.SortCommand.SortField;
import trackflow.logic.commands.SortCommand.SortOrder;
import trackflow.logic.commands.TagCommand;
import trackflow.logic.commands.UntagCommand;
import trackflow.logic.parser.exceptions.ParseException;
import trackflow.model.ModelManager;
import trackflow.model.person.AgeCategory;
import trackflow.model.person.Person;
import trackflow.model.person.PersonContainsKeywordsPredicate;
import trackflow.model.person.Remark;
import trackflow.model.util.SampleDataUtil;
import trackflow.testutil.EditPersonDescriptorBuilder;
import trackflow.testutil.PersonBuilder;
import trackflow.testutil.PersonUtil;

public class TrackFlowParserTest {

    private final TrackFlowParser parser = new TrackFlowParser();

    @Test
    public void parseCommand_unicodeSeparators_matchOrdinarySpaces() throws Exception {
        for (String separator : List.of("\t", "\r\n", "\u00a0", "\u1680", "\u2003", "\u2007",
                "\u2028", "\u2029", "\u202f", "\u205f", "\u3000")) {
            // EP: valid command and arguments padded with Unicode whitespace separators
            assertEquals(parser.parseCommand("find Avery Ben"),
                    parser.parseCommand(separator + "find" + separator + "Avery" + separator + "Ben" + separator));

            // EP: multi-token command padded with Unicode whitespace separators
            assertEquals(parser.parseCommand("sort name desc"),
                    parser.parseCommand(separator + "sort" + separator + "name" + separator + "desc" + separator));

            // EP: index-based command padded with Unicode whitespace separators
            assertEquals(parser.parseCommand("delete 1"),
                    parser.parseCommand("delete" + separator + "1" + separator)); // Boundary value

            // EP: single-word commands padded with Unicode whitespace separators
            for (String commandWord : List.of("list", "clear", "help", "exit")) {
                assertEquals(parser.parseCommand(commandWord).getClass(),
                        parser.parseCommand(separator + commandWord + separator).getClass());

                // EP: single-word commands with unexpected trailing arguments
                assertThrows(ParseException.class, () -> parser.parseCommand(commandWord + separator + "extra"));
            }

            // EP: whitespace-only command string
            assertThrows(ParseException.class, MESSAGE_EMPTY_COMMAND, () ->
                    parser.parseCommand(separator)); // Boundary value
        }
    }

    @Test
    public void parseCommand_add() throws Exception {
        Person person = new PersonBuilder().withAddress("123 Main Street").withRemark("").withTags().build();
        String addCommandText = PersonUtil.getAddCommand(person);

        // EP: valid add command with standard lowercase command word
        AddCommand command = (AddCommand) parser.parseCommand(addCommandText);
        assertEquals(new AddCommand(person), command);

        // EP: valid add command with uppercase command word (case-insensitivity)
        AddCommand uppercaseCommand = (AddCommand) parser.parseCommand("ADD" + addCommandText.substring(3));
        assertEquals(new AddCommand(person), uppercaseCommand);
    }

    @Test
    public void parseCommand_allAddFieldOrdersWithEscapedAddress_produceSameAthlete() throws Exception {
        // EP: all permutations of field prefix ordering and whitespace delimiters produce identical AddCommand
        List<List<String>> permutations = List.of(List.of());
        for (String field : List.of("N/Ben Lim", "A/Open", "P/92345678", "E/ben@example.com",
                "ADDR/Block A//B p//literal https://host/a//b")) {
            List<List<String>> expanded = new ArrayList<>();
            for (List<String> permutation : permutations) {
                for (int index = 0; index <= permutation.size(); index++) {
                    List<String> reordered = new ArrayList<>(permutation);
                    reordered.add(index, field);
                    expanded.add(reordered);
                }
            }
            permutations = expanded;
        }
        AddCommand expected = new AddCommand(new PersonBuilder().withName("Ben Lim").withPhone("92345678")
                .withEmail("ben@example.com").withAddress("Block A/B p/literal https://host/a//b")
                .withAgeCategory("Open").withRemark("").withTags().build());
        assertEquals(120, permutations.size());
        for (List<String> fields : permutations) {
            for (String separator : List.of(" ", "\t", "\r\n", "\u2003", "\u00a0", "\u2007", "\u202f")) {
                String input = "AdD" + separator + String.join(separator, fields);
                assertEquals(expected, parser.parseCommand(input), input);
            }
        }
    }

    @Test
    public void parseCommand_nullInput_failsFastWithoutInventingACommand() {
        // EP: null input to top-level parser
        assertThrows(NullPointerException.class, () -> parser.parseCommand(null)); // Boundary value

        // EP: null input to individual command parsers
        List<Parser<?>> commandParsers = List.of(new AddCommandParser(), new EditCommandParser(),
                new DeleteCommandParser(), new RemarkCommandParser(), new FindCommandParser(),
                new FilterCommandParser(), new SortCommandParser(), new ListAthleteCommandParser(),
                new TagCommandParser(), new UntagCommandParser());
        for (Parser<?> commandParser : commandParsers) {
            assertThrows(NullPointerException.class, () -> commandParser.parse(null)); // Boundary value
        }
    }

    @Test
    public void parseCommand_clear() throws Exception {
        // EP: valid clear command word
        Command command = parser.parseCommand(ClearCommand.COMMAND_WORD);
        assertTrue(command instanceof ClearCommand);
        assertFalse(command.changesRoster());

        // EP: invalid clear command with unexpected non-numeric arguments
        assertThrows(ParseException.class, () -> parser.parseCommand("clear confirm"));
        assertThrows(ParseException.class, () -> parser.parseCommand("clear confirm extra"));

        // EP: invalid clear command with unexpected numeric argument
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ClearCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, expectedMessage, () ->
                parser.parseCommand(ClearCommand.COMMAND_WORD + " 3"));
    }

    @Test
    public void parseCommand_delete() throws Exception {
        // EP: valid delete command with 1-based index
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + INDEX_FIRST_PERSON.getOneBased()); // Boundary value
        assertEquals(new DeleteCommand(INDEX_FIRST_PERSON), command);
    }

    @Test
    public void parseCommand_edit() throws Exception {
        // EP: valid edit command with 1-based index and person details
        Person person = new PersonBuilder().build();
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(person).build();
        descriptor.setTags(null);
        EditCommand command = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                + INDEX_FIRST_PERSON.getOneBased() + " " // Boundary value
                + PersonUtil.getEditPersonDescriptorDetails(descriptor));
        assertEquals(new EditCommand(INDEX_FIRST_PERSON, descriptor), command);
    }

    @Test
    public void parseCommand_exit() throws Exception {
        // EP: valid exit command word
        Command command = parser.parseCommand(ExitCommand.COMMAND_WORD);
        assertTrue(command instanceof ExitCommand);
        assertFalse(command.changesRoster());
        assertTrue(command.execute(new ModelManager()).isExit());

        // EP: invalid exit command with unexpected arguments
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ExitCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, expectedMessage, () ->
                parser.parseCommand(ExitCommand.COMMAND_WORD + " 3"));
    }

    @Test
    public void parseCommand_filter() throws Exception {
        // EP: valid filter command with age category prefix
        FilterCommand command = (FilterCommand) parser.parseCommand("filter a/Under 18");
        assertEquals(new FilterCommand(new AgeCategory("Under 18")), command);
    }

    @Test
    public void parseCommand_find() throws Exception {
        // EP: valid find command with multiple search keywords
        List<String> keywords = List.of("foo", "bar", "baz");
        FindCommand command = (FindCommand) parser.parseCommand(
                FindCommand.COMMAND_WORD + " " + keywords.stream().collect(Collectors.joining(" ")));
        assertEquals(new FindCommand(new PersonContainsKeywordsPredicate(keywords)), command);
    }

    @Test
    public void parseCommand_help() throws Exception {
        // EP: valid help command word
        Command command = parser.parseCommand(HelpCommand.COMMAND_WORD);
        assertTrue(command instanceof HelpCommand);
        assertFalse(command.changesRoster());
        assertTrue(command.execute(new ModelManager()).isShowHelp());

        // EP: invalid help command with unexpected arguments
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, expectedMessage, () ->
                parser.parseCommand(HelpCommand.COMMAND_WORD + " 3"));
    }

    @Test
    public void parseCommand_list() throws Exception {
        // EP: valid list command with case variations and leading/trailing whitespace
        for (String input : List.of(ListAthleteCommand.COMMAND_WORD, "LIST", "  LiSt  ")) {
            Command command = parser.parseCommand(input);
            assertTrue(command instanceof ListAthleteCommand);
            assertFalse(command.changesRoster());
            assertEquals(ListAthleteCommand.MESSAGE_EMPTY_ROSTER,
                    command.execute(new ModelManager()).getFeedbackToUser());
        }

        // EP: invalid list command with unexpected arguments
        assertThrows(ParseException.class, ListAthleteCommand.MESSAGE_USAGE, () -> parser.parseCommand("LiSt 3"));
    }

    @Test
    public void parseCommand_remark() throws Exception {
        // EP: valid remark command with 1-based index and remark prefix
        final Remark remark = new Remark("Some remark.");
        RemarkCommand command = (RemarkCommand) parser.parseCommand(RemarkCommand.COMMAND_WORD + " "
                + INDEX_FIRST_PERSON.getOneBased() + " " + PREFIX_REMARK + remark.value); // Boundary value
        assertEquals(new RemarkCommand(INDEX_FIRST_PERSON, remark), command);
    }

    @Test
    public void parseCommand_tag() throws Exception {
        // EP: valid tag command with 1-based index and tag prefix
        TagCommand command = (TagCommand) parser.parseCommand(TagCommand.COMMAND_WORD + " "
                + INDEX_FIRST_PERSON.getOneBased() + " " + PREFIX_TAG + "100m"); // Boundary value
        assertEquals(new TagCommand(INDEX_FIRST_PERSON, SampleDataUtil.getTagSet("100m")), command);
    }

    @Test
    public void parseCommand_untag() throws Exception {
        // EP: valid untag command with 1-based index and tag prefix
        UntagCommand command = (UntagCommand) parser.parseCommand(UntagCommand.COMMAND_WORD + " "
                + INDEX_FIRST_PERSON.getOneBased() + " " + PREFIX_TAG + "100m"); // Boundary value
        assertEquals(new UntagCommand(INDEX_FIRST_PERSON, SampleDataUtil.getTagSet("100m")), command);
    }

    @Test
    public void parseCommand_sort() throws Exception {
        // EP: valid sort command with mixed-case command word, field, and order
        assertEquals(new SortCommand(SortField.AGE, SortOrder.DESCENDING), parser.parseCommand("SoRt age desc"));
    }

    @Test
    public void parseCommand_unrecognizedInput_throwsParseException() {
        // EP: empty command string
        assertThrows(ParseException.class, MESSAGE_EMPTY_COMMAND, () -> parser.parseCommand("")); // Boundary value

        // EP: whitespace-only command string
        assertThrows(ParseException.class, MESSAGE_EMPTY_COMMAND, () -> parser.parseCommand("   \t\n"));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        // EP: unknown or unsupported command word
        String unknownCommand = "unknownCommand";
        String expectedMessage = String.format(MESSAGE_UNKNOWN_COMMAND, unknownCommand);
        assertThrows(ParseException.class, expectedMessage, () -> parser.parseCommand(unknownCommand + " arguments"));
    }
}
