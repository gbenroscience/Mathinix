package com.github.gbenroscience.mathinix;

/**
 * JavaFX App
 */
import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.STRING;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.MatrixTurboEvaluator;
import com.github.gbenroscience.parser.turbo.tools.TurboEvaluatorFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.application.Application;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.binding.Bindings;

public class App extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    private static final int MODE_STD = 0;
    private static final int MODE_TURBO_ARR = 1;
    private static final int MODE_TURBO_WIDE = 2;
    private static final int MODE_TURBO_MATRIX = 3;

    private static int mode = MODE_STD;

    // A property that UI elements can "listen" to
    private final ObjectProperty<ParserMode> currentMode = new SimpleObjectProperty<>(ParserMode.STANDARD);

    GraphDisplay visualizer;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // Remove standard OS window borders
        stage.initStyle(StageStyle.TRANSPARENT);

        BorderPane root = new BorderPane();
        // Adjust this if your CSS file is in a different location
        root.getStylesheets().add(getClass().getResource("/static/css/style.css").toExternalForm());

        // --- TOP: Menu Bar (Draggable Area) ---
        HBox topBar = createTopBar(stage);
        // Inside your start() method or a controller
        topBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double-click to toggle
                stage.setMaximized(!stage.isMaximized());

                if (stage.isMaximized()) {
                    // Optional: Adjust the root's border radius when fullscreen
                    root.setStyle("-fx-background-radius: 0; -fx-border-radius: 0;");
                } else {
                    root.setStyle("-fx-background-radius: 20; -fx-border-radius: 20;");
                }
            }
        });
        root.setTop(topBar);

        // In your start() method:
// 1. Bind the Stage Title to the mode
        stage.titleProperty()
                .bind(Bindings.concat("ParserNG - ",
                        Bindings.createStringBinding(() -> currentMode.get().getDisplayName(), currentMode)));
// 2. Create a glowing HUD label for the UI
        Label modeIndicator = new Label();
        modeIndicator.getStyleClass().add("mode-badge");
        modeIndicator.textProperty().bind(Bindings.createStringBinding(
                () -> currentMode.get().getDisplayName().toUpperCase(), currentMode));
// Add modeIndicator to your TopBar or Status Bar
        topBar.getChildren().add(0, modeIndicator);

        // --- LEFT: Variables & Functions ---
        VBox leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(280);
        leftPanel.getChildren().addAll(
                createSectionHeader("CREATED VARIABLES"),
                createVariablesTable(),
                createSectionHeader("USER DEFINED FUNCTIONS"),
                createFunctionsTable()
        );
        root.setLeft(leftPanel);

        // --- CENTER: Command Line & Graph ---
        // --- CENTER: Command Line & Graph & Results ---
        VBox centerPanel = new VBox(15);
        centerPanel.setPadding(new Insets(10, 20, 10, 20));

        FXCommandLineArea cli = initTerminal(centerPanel);

        centerPanel.getChildren().addAll(
                createSectionHeader("COMMAND LINE EXECUTION"),
                cli
        );

        VBox.setVgrow(cli, Priority.ALWAYS);
        root.setCenter(centerPanel);

        // --- RIGHT: Methods & Constants ---
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(280);
        rightPanel.getChildren().addAll(
                createSectionHeader("INBUILT METHODS"),
                createMethodsTable(),
                createSectionHeader("CONSTANTS"),
                createConstantsTable()
        );
        root.setRight(rightPanel);

        // --- BOTTOM: Status Bar ---
        HBox statusBar = new HBox(30);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getChildren().addAll(
                createStatusLabel("STATUS: IDLE"),
                createStatusLabel("MEMORY: 1.2 GB"),
                createStatusLabel("CPU: 8%"),
                createStatusLabel("SESSION: MATH_01")
        );
        root.setBottom(statusBar);

        // Scene setup
        Scene scene = new Scene(root, 1200, 750);
        scene.setFill(Color.TRANSPARENT); // Required for glassmorphism

        //MAXIMIZE:
        // To Maximize manually to the usable area:
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(visualBounds.getMinX());
        stage.setY(visualBounds.getMinY());
        stage.setWidth(visualBounds.getWidth());
        stage.setHeight(visualBounds.getHeight());

        stage.setScene(scene);
        stage.show();
    }

    private FXCommandLineArea initTerminal(VBox centerPanel) {
        FXCommandLineArea cli = new FXCommandLineArea("ParserNG>>");

        final List<String> clearCmds = new ArrayList<>(Arrays.asList("clear", "cls", "clr"));

        cli.setCommandAction(new FXCommandLineArea.CommandAction() {
            @Override
            public void onEnter(String cmd) {
                String comd = cmd;
                if (clearCmds.contains(cmd)) {
                    cli.clear();
                    return;
                }
                if (cmd.equals("help")) {
                    StringBuilder sb = new StringBuilder();

                    sb.append("Convert to Standard Mode:").append("\n");
                    sb.append("mode=std").append("\n");
                    sb.append("mode=standard").append("\n");
                    sb.append("mode=0").append("\n\n");

                    sb.append("Convert to Turbo Mode(Type 1):").append("\n");
                    sb.append("mode=arr").append("\n");
                    sb.append("mode=turbo_arr").append("\n");
                    sb.append("mode=turbo-arr").append("\n");
                    sb.append("mode=turbo_array").append("\n");
                    sb.append("mode=turbo-array").append("\n");
                    sb.append("mode=1").append("\n\n");

                    sb.append("Convert to Turbo Mode(Type 2):").append("\n");
                    sb.append("mode=wide").append("\n");
                    sb.append("mode=turbo_wid").append("\n");
                    sb.append("mode=turbo_wide").append("\n");
                    sb.append("mode=turbo_widen").append("\n");
                    sb.append("mode=turbo_widening").append("\n");
                    sb.append("mode=turbo-wid").append("\n");
                    sb.append("mode=turbo-wide").append("\n");
                    sb.append("mode=turbo-widen").append("\n");
                    sb.append("mode=2").append("\n\n");

                    sb.append("Convert to Turbo Mode(Type 3- Matrices):").append("\n");
                    sb.append("mode=mat").append("\n");
                    sb.append("mode=matrix").append("\n");
                    sb.append("mode=turbo_mat").append("\n");
                    sb.append("mode=turbo_matrix").append("\n");
                    sb.append("mode=turbo-mat").append("\n");
                    sb.append("mode=turbo-matrix").append("\n");
                    sb.append("mode=3").append("\n\n");
                    cli.printOutput(sb.toString());
                    return;
                }

                if (cmd.equalsIgnoreCase("hello")) {
                    cli.printOutput("Hello, User! Welcome to the ParserNG CLI.");
                    return;
                }
                if (cmd.contains("mode")) {
                    cmd = STRING.purifier(cmd);
                    if (cmd.startsWith("mode:") || cmd.startsWith("mode=")) {
                        try {
                            String modeText = cmd.substring("mode".length() + 1).toLowerCase();

                            switch (modeText) {
                                case "std":
                                case "standard":
                                case "0":
                                    mode = MODE_STD;
                                    currentMode.set(ParserMode.STANDARD);
                                    break;
                                case "arr":
                                case "turbo_arr":
                                case "turbo-arr":
                                case "turbo_array":
                                case "turbo-array":
                                case "1":
                                    mode = MODE_TURBO_ARR;
                                    currentMode.set(ParserMode.TURBO_ARR);
                                    break;
                                case "wide":
                                case "turbo_wid":
                                case "turbo_wide":
                                case "turbo_widen":
                                case "turbo_widening":
                                case "turbo-wid":
                                case "turbo-wide":
                                case "turbo-widen":
                                case "turbo-widening":
                                case "2":
                                    mode = MODE_TURBO_WIDE;
                                    currentMode.set(ParserMode.TURBO_WIDE);
                                    break;
                                case "mat":
                                case "matrix":
                                case "turbo_mat":
                                case "turbo_matrix":
                                case "turbo-mat":
                                case "turbo-matrix":
                                case "3":
                                    mode = MODE_TURBO_MATRIX;
                                    currentMode.set(ParserMode.TURBO_MATRIX);

                                    break;
                                default:
                                    cli.printOutput("Invalid mode command: `" + modeText + "`");
                                    break;
                            }

                        } catch (Exception e) {
                            cli.printOutput("Bad command...`" + comd + "`");
                        }
                        return;
                    }
                }
                // Echo back the command as an example
                cli.printOutput("Executing: " + cmd);
                  if (cmd.startsWith("plot")) {
                        openGraph(cmd);
                        return;
                    }
                MathExpression me = new MathExpression(cmd);
                if (mode == MODE_STD) {
                    String soln = me.solve();
                    cli.printOutput(soln);
                } else {
                  
                    try {
                        FastCompositeExpression fce = null;
                        if (mode == MODE_TURBO_ARR) {
                            fce = TurboEvaluatorFactory.getCompiler(me, false).compile();
                        } else if (mode == MODE_TURBO_WIDE) {
                            fce = TurboEvaluatorFactory.getCompiler(me, true).compile();
                        } else if (mode == MODE_TURBO_MATRIX) {
                            fce = new MatrixTurboEvaluator(me).compile();
                        }

                        MathExpression.EvalResult soln = fce.apply(me.getExecutionFrame());
                        cli.printOutput(soln.toString());
                    } catch (Throwable ex) {
                        System.getLogger(App.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                    }
                }

            }

            @Override
            public void onUpPressed() {

            }

            @Override
            public void onDownPressed() {

            }
        });

        return cli;
    }

    private GraphDisplay openGraph(String cmd) {
        // Initialize the satellite window
        if (visualizer == null) {
            visualizer = new GraphDisplay();
        }
        visualizer.show();

        visualizer.updateGraph(cmd);

        return visualizer;
    }

    // --- Helper Methods to Keep Code Clean ---
    private Label createSectionHeader(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("header-label");
        return label;
    }

    private Label createStatusLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("status-label");
        return label;
    }

    private HBox createTopBar(Stage stage) {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 10, 20));
        Label title = new Label("FILE   EDIT   VIEW   SESSION   HELP");
        title.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Exit button
        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4444; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> System.exit(0));

        topBar.getChildren().addAll(title, spacer, closeBtn);

        // Make window draggable
        topBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        topBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        return topBar;
    }

    // --- Dummy Data Tables ---
    private TableView<String[]> createVariablesTable() {
        return createSimpleTable(
                new String[]{"NAME", "VALUE", "TYPE"},
                new String[][]{{"x", "3.5", "(scalar)"}, {"y", "[1, 2, 3]", "(vector)"}, {"A", "[[...]]", "(matrix)"}}
        );
    }

    private TableView<String[]> createFunctionsTable() {
        return createSimpleTable(
                new String[]{"FUNCTION", "DEF"},
                new String[][]{{"sq(n)", "n*n"}, {"area_circ(r)", "pi * r^2"}}
        );
    }

    private TableView<String[]> createMethodsTable() {
        return createSimpleTable(
                new String[]{"METHOD", "SYNTAX"},
                new String[][]{{"integrate", "(expr, var)"}, {"differentiate", "(expr, var)"}, {"plot", "(expr, range)"}}
        );
    }

    private TableView<String[]> createConstantsTable() {
        return createSimpleTable(
                new String[]{"SYMBOL", "VALUE"},
                new String[][]{{"π", "3.14159"}, {"e", "2.71828"}, {"c", "299792458"}}
        );
    }

    // Utility to quickly spin up a TableView with basic string arrays
    private TableView<String[]> createSimpleTable(String[] headers, String[][] data) {
        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        for (int i = 0; i < headers.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[colIndex]));
            table.getColumns().add(col);
        }

        table.getItems().addAll(data);
        VBox.setVgrow(table, Priority.ALWAYS); // Let tables stretch to fit space
        return table;
    }
}

/*
public class App extends Application {

    @Override
    public void start(Stage stage) {
        var javaVersion = SystemInfo.javaVersion();
        var javafxVersion = SystemInfo.javafxVersion();

        var label = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
        var scene = new Scene(new StackPane(label), 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}
 */
