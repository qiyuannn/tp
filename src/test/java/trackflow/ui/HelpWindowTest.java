package trackflow.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import trackflow.logic.commands.AddCommand;
import trackflow.logic.commands.EditCommand;
import trackflow.logic.commands.FilterCommand;
import trackflow.logic.commands.HelpCommand;

public class HelpWindowTest {

    private static boolean javaFxAvailable = false;
    private final AtomicReference<Stage> currentStageRef = new AtomicReference<>();

    @BeforeAll
    public static void initJavaFx() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            javaFxAvailable = latch.await(5, TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    public void checkJavaFx() {
        Assumptions.assumeTrue(javaFxAvailable, "JavaFX toolkit not available in this environment.");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (!javaFxAvailable) {
            return;
        }
        Stage stage = currentStageRef.getAndSet(null);
        if (stage != null) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    stage.close();
                } finally {
                    latch.countDown();
                }
            });
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }

    // EP: default constructor instantiation (new stage created implicitly)
    @Test
    public void defaultConstructor_initializesHelpWindow() throws Exception {
        AtomicReference<HelpWindow> helpWindowRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                HelpWindow helpWindow = new HelpWindow();
                currentStageRef.set(helpWindow.getRoot());
                helpWindowRef.set(helpWindow);
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (errorRef.get() != null) {
            throw new AssertionError("Exception on JavaFX thread", errorRef.get());
        }
        assertNotNull(helpWindowRef.get());
        assertNotNull(helpWindowRef.get().getRoot());
    }

    @Test
    public void helpWindow_rendersAllCardsWithAgeCategories() throws Exception {
        AtomicReference<HelpWindow> helpWindowRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                currentStageRef.set(stage);
                HelpWindow helpWindow = new HelpWindow(stage);
                helpWindowRef.set(helpWindow);
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (errorRef.get() != null) {
            throw new AssertionError("Exception on JavaFX thread", errorRef.get());
        }
        HelpWindow helpWindow = helpWindowRef.get();
        assertNotNull(helpWindow);

        // EP: explicit stage constructor and full command usage card rendering
        // Verify root stage and scene
        Stage stage = helpWindow.getRoot();
        assertNotNull(stage.getScene());

        ScrollPane scrollPane = (ScrollPane) stage.getScene().getRoot();
        assertTrue(scrollPane.isFitToWidth());
        assertEquals(ScrollPane.ScrollBarPolicy.NEVER, scrollPane.getHbarPolicy());

        Field commandListField = HelpWindow.class.getDeclaredField("commandList");
        commandListField.setAccessible(true);
        VBox commandList = (VBox) commandListField.get(helpWindow);
        assertNotNull(commandList);
        assertEquals(HelpCommand.COMMAND_USAGES.size(), commandList.getChildren().size());

        // EP: parameter label verification for age category options
        List<String> validCategories = List.of("Under 14", "Under 16", "Under 18", "Under 20", "Open");

        for (Node cardNode : commandList.getChildren()) {
            assertTrue(cardNode instanceof VBox);
            VBox card = (VBox) cardNode;
            Label heading = (Label) card.getChildren().get(0);
            String commandWord = heading.getText();

            if (commandWord.equals(AddCommand.COMMAND_WORD)
                    || commandWord.equals(EditCommand.COMMAND_WORD)
                    || commandWord.equals(FilterCommand.COMMAND_WORD)) {

                // Search for parameters field inside card
                boolean foundParameters = false;
                for (Node child : card.getChildren()) {
                    if (child instanceof VBox fieldBox) {
                        Label titleLabel = (Label) fieldBox.getChildren().get(0);
                        if ("Parameters".equals(titleLabel.getText())) {
                            foundParameters = true;
                            Label valueLabel = (Label) fieldBox.getChildren().get(1);
                            assertTrue(valueLabel.isWrapText());
                            for (String category : validCategories) {
                                assertTrue(valueLabel.getText().contains(category),
                                        commandWord + " parameters missing " + category);
                            }
                        }
                    }
                }
                assertTrue(foundParameters, "Parameters field missing in " + commandWord + " card");
            }
        }

        // EP: window lifecycle operations (show, focus restoration, snapshot rendering, hide)
        // Test show, snapshot rendering, isShowing, focus, and hide lifecycle
        CountDownLatch showLatch = new CountDownLatch(1);
        AtomicReference<Throwable> showErrRef = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                helpWindow.show();
                assertTrue(helpWindow.isShowing());

                stage.setIconified(true);
                helpWindow.focus();
                assertFalse(stage.isIconified());

                WritableImage snapshot = stage.getScene().snapshot(null);
                assertNotNull(snapshot);
                assertTrue(snapshot.getWidth() > 0); // Boundary value
                assertTrue(snapshot.getHeight() > 0); // Boundary value

                helpWindow.hide();
                assertFalse(helpWindow.isShowing());
            } catch (Throwable t) {
                showErrRef.set(t);
            } finally {
                showLatch.countDown();
            }
        });
        assertTrue(showLatch.await(5, TimeUnit.SECONDS));
        if (showErrRef.get() != null) {
            throw new AssertionError("Exception during show lifecycle on JavaFX thread", showErrRef.get());
        }
    }

    // EP: minimum dimension boundary rendering (480x320 window bounds)
    @Test
    public void helpWindow_minimumDimensions_wrapsWithoutHorizontalScroll() throws Exception {
        AtomicReference<HelpWindow> helpWindowRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                currentStageRef.set(stage);
                stage.setMinWidth(480.0); // Boundary value
                stage.setMinHeight(320.0); // Boundary value
                stage.setWidth(480.0); // Boundary value
                stage.setHeight(320.0); // Boundary value

                HelpWindow helpWindow = new HelpWindow(stage);
                helpWindowRef.set(helpWindow);

                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (errorRef.get() != null) {
            throw new AssertionError("Exception on JavaFX thread", errorRef.get());
        }

        HelpWindow helpWindow = helpWindowRef.get();
        assertNotNull(helpWindow);
        Stage stage = helpWindow.getRoot();
        ScrollPane scrollPane = (ScrollPane) stage.getScene().getRoot();
        assertTrue(scrollPane.isFitToWidth());
        assertEquals(ScrollPane.ScrollBarPolicy.NEVER, scrollPane.getHbarPolicy());
    }
}
