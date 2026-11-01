package trackflow.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import trackflow.logic.commands.CommandResult;

public class CommandBoxTest {

    private static boolean isJavaFxAvailable = false;

    private final List<String> executedCommands = new ArrayList<>();
    private Stage stage;
    private TextField commandTextField;

    @BeforeAll
    public static void initJavaFx() {
        if (GraphicsEnvironment.isHeadless()) {
            // Without a display (e.g. Linux CI), starting JavaFX fails and can leave it unusable for later UI tests
            isJavaFxAvailable = false;
            return;
        }
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            isJavaFxAvailable = latch.await(5, TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            isJavaFxAvailable = true;
        } catch (Throwable t) {
            isJavaFxAvailable = false;
        }
        if (isJavaFxAvailable) {
            // Keeps the toolkit running after each test closes its window, so later UI tests can still run
            Platform.setImplicitExit(false);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        Assumptions.assumeTrue(isJavaFxAvailable, "JavaFX toolkit not available in this environment.");
        runOnFxThread(() -> {
            CommandBox commandBox = new CommandBox(commandText -> {
                executedCommands.add(commandText);
                return new CommandResult("");
            });
            stage = new Stage();
            stage.setScene(new Scene(commandBox.getRoot(), 600, 80));
            stage.show();
            commandTextField = (TextField) commandBox.getRoot().lookup("#commandTextField");
            commandTextField.requestFocus();
            return null;
        });
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (stage != null) {
            runOnFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    // EP: partial command prefix matching existing commands
    @Test
    public void typingCommandWord_showsSuggestions() throws Exception {
        type("ed");
        assertTrue(isSuggestionsPopupShowing());
    }

    // EP: escape key press dismissing active suggestions popup
    @Test
    public void escape_hidesSuggestionsWithoutChangingText() throws Exception {
        type("ed");
        press(KeyCode.ESCAPE);
        assertFalse(isSuggestionsPopupShowing());
        assertEquals("ed", getCommandText());
    }

    @Test
    public void tab_appliesSelectedSuggestionThenInsertsNextPrefix() throws Exception {
        // EP: tab key applying highlighted command word suggestion
        type("ad");
        press(KeyCode.TAB);
        assertEquals("add ", getCommandText());
        assertFalse(isSuggestionsPopupShowing());

        // EP: tab key cycling / auto-inserting next parameter prefix
        press(KeyCode.TAB);
        assertEquals("add n/ ", getCommandText());
    }

    // EP: down arrow key navigating to subsequent suggestion followed by tab completion
    @Test
    public void downThenTab_appliesSecondSuggestion() throws Exception {
        type("f"); // Boundary value
        press(KeyCode.DOWN);
        press(KeyCode.TAB);
        assertEquals("find ", getCommandText());
    }

    // EP: enter key executing raw input directly without auto-applying suggestion
    @Test
    public void enter_executesTypedTextInsteadOfSuggestion() throws Exception {
        type("li");
        press(KeyCode.ENTER);
        assertEquals(List.of("li"), executedCommands);
        assertFalse(isSuggestionsPopupShowing());
    }

    private void type(String text) throws Exception {
        runOnFxThread(() -> {
            commandTextField.setText(text);
            commandTextField.positionCaret(text.length());
            return null;
        });
    }

    /**
     * Presses a key in the command box. While suggestions are showing, JavaFX redirects the key to the popup,
     * as it does for real key presses.
     */
    private void press(KeyCode keyCode) throws Exception {
        runOnFxThread(() -> {
            commandTextField.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode,
                    false, false, false, false));
            return null;
        });
        // Lets actions scheduled with Platform.runLater, such as applying a suggestion, finish
        runOnFxThread(() -> null);
    }

    private String getCommandText() throws Exception {
        return runOnFxThread(() -> commandTextField.getText());
    }

    private boolean isSuggestionsPopupShowing() throws Exception {
        return runOnFxThread(() -> Window.getWindows().stream()
                .anyMatch(window -> window instanceof Popup && window.isShowing()));
    }

    private static <T> T runOnFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }
}
