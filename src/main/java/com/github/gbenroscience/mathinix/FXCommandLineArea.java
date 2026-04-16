package com.github.gbenroscience.mathinix;

/**
 *
 * @author GBEMIRO
 */
import javafx.application.Platform;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A JavaFX implementation of a protected command line area.
 *
 * @author GBEMIRO (Ported to JavaFX)
 */
public class FXCommandLineArea extends TextArea {

    public interface CommandAction {

        void onEnter(String command);

        void onUpPressed();

        void onDownPressed();
    }

    private String prompt = "ParserNG>>";
    private int inputStartOffset;
    private boolean isSystemChange = false;

    private final List<String> history = new ArrayList<>();
    private int historyIndex = 0;
    private CommandAction commandAction;

    public FXCommandLineArea(String prompt) {
        this.prompt = prompt;
        this.getStyleClass().add("command-line-area");
        init();
    }

    private void init() {
        // 1. Setup the protection filter using TextFormatter
        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (isSystemChange) {
                return change; // System can do anything
            }

            // Prevent any changes (insertion or deletion) before the prompt
            if (change.getRangeStart() < inputStartOffset) {
                return null;
            }

            return change;
        };
        this.setTextFormatter(new TextFormatter<>(filter));

        // 2. Initial prompt
        insertSystemText(prompt);

        // 3. Keep caret at the end if user clicks elsewhere
        this.caretPositionProperty().addListener((obs, oldVal, newVal) -> {
            if (!isSystemChange && newVal.intValue() < inputStartOffset) {
                // Run later to avoid listener collision
                Platform.runLater(() -> this.positionCaret(this.getLength()));
            }
        });

        // 4. Handle Key Bindings
        this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleEnter();
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                handleUp();
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN) {
                handleDown();
                event.consume();
            } else if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.LEFT) {
                // Additional safety for backspace/left at the edge of the prompt
                if (this.getCaretPosition() <= inputStartOffset) {
                    event.consume();
                }
            }
        });
    }

    private void insertSystemText(String text) {
        isSystemChange = true;
        this.appendText(text);
        inputStartOffset = this.getLength();
        this.positionCaret(inputStartOffset);
        isSystemChange = false;
    }

    public void setCommandAction(CommandAction commandAction) {
        this.commandAction = commandAction;
    }

    public void clear() {
        isSystemChange = true;
        super.clear(); // Built-in TextArea clear
        this.appendText(prompt);
        inputStartOffset = this.getLength();
        this.positionCaret(inputStartOffset);
        historyIndex = history.size();
        isSystemChange = false;
    }
 

    public String getCurrentCommand() {
        return this.getText(inputStartOffset, this.getLength());
    }

    private void handleEnter() {
        String cmd = getCurrentCommand();
        if (!cmd.trim().isEmpty()) {
            history.add(cmd);
        }
        historyIndex = history.size();

        insertSystemText("\n");

        if (commandAction != null) {
            commandAction.onEnter(cmd);
        }

        // Only add a new prompt if the action didn't trigger a clear()
        if (this.getLength() >= prompt.length() && !this.getText().endsWith(prompt)) {
            insertSystemText(prompt);
        }
    }

    public void printOutput(String text) {
        insertSystemText(text + "\n");
    }

    private void handleUp() {
        if (historyIndex > 0) {
            historyIndex--;
            replaceUserInput(history.get(historyIndex));
            if (commandAction != null) {
                commandAction.onUpPressed();
            }
        }
    }

    private void handleDown() {
        if (historyIndex < history.size()) {
            historyIndex++;
            String text = (historyIndex == history.size()) ? "" : history.get(historyIndex);
            replaceUserInput(text);
            if (commandAction != null) {
                commandAction.onDownPressed();
            }
        }
    }

    private void replaceUserInput(String text) {
        isSystemChange = true;
        this.replaceText(inputStartOffset, this.getLength(), text);
        isSystemChange = false;
    }
}
