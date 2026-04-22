package com.github.gbenroscience.mathinix;

/**
 * JavaFX App
 */
import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.STRING;
import com.github.gbenroscience.parser.Variable;
import com.github.gbenroscience.parser.methods.Method;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.MatrixTurboEvaluator;
import com.github.gbenroscience.parser.turbo.tools.TurboEvaluatorFactory;
import com.github.gbenroscience.util.FunctionManager;
import com.github.gbenroscience.util.VariableManager;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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

    // Tables
    private TableView<String[]> varTable;
    private TableView<String[]> funcTable;
    private TableView<String[]> constTable;
    private TableView<String[]> methodsTable;

// Data Lists (The "Source of Truth" for the UI)
    private final ObservableList<String[]> varData = FXCollections.observableArrayList();
    private final ObservableList<String[]> funcData = FXCollections.observableArrayList();
    private final ObservableList<String[]> constData = FXCollections.observableArrayList();
    private final ObservableList<String[]> methodsData = FXCollections.observableArrayList();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        var javaVersion = SystemInfo.javaVersion();
        var javafxVersion = SystemInfo.javafxVersion();
        System.out.println("Java Version: " + javaVersion + "\nJavaFX Version: " + javafxVersion);
        // Remove standard OS window borders
        stage.initStyle(StageStyle.TRANSPARENT);

        BorderPane root = new BorderPane();
        // Adjust this if your CSS file is in a different location
        root.getStylesheets().add(getClass().getResource("/static/css/style.css").toExternalForm());

        // --- TOP: Menu Bar (Draggable Area) ---
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

        // --- TOP: The Merged Top Bar ---
        StackPane topBar = createTopBar(stage, modeIndicator, root);
        root.setTop(topBar);

        // Initialize the tables with their respective lists
        this.varTable = createLiveTable(new String[]{"NAME", "VALUE", "TYPE"}, varData);
        this.funcTable = createLiveTable(new String[]{"FUNCTION", "DEF"}, funcData);
        this.constTable = createLiveTable(new String[]{"SYMBOL", "VALUE"}, constData);
        this.methodsTable = createLiveTable(new String[]{"METHOD", "SYNTAX"}, methodsData);

        // --- LEFT: Variables & Functions ---
        VBox leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(280);
        leftPanel.getChildren().addAll(
                createSectionHeader("CREATED VARIABLES"),
                this.varTable,
                createSectionHeader("USER DEFINED FUNCTIONS"),
                this.funcTable
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

// Load initial data (pi, e, etc.)
        refreshTables();

// Load the static data
        loadInbuiltMethods();

        // --- RIGHT: Methods & Constants ---
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(280);
        rightPanel.getChildren().addAll(
                createSectionHeader("INBUILT METHODS"),
                this.methodsTable,
                createSectionHeader("CONSTANTS"),
                this.constTable
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
                    sb.append("Use `wipe` to clear all stored functions and variables\n");
                    sb.append("Use `wipe-funcs` to clear all stored functions\n");
                    sb.append("Use `wipe-vars` to clear all stored variables\n");
                    sb.append("Use `wipe-var:varName` e.g. wipe-var:a1 to delete the specified variable e.g. a1\n");
                    sb.append("Use `wipe-func:funcName` e.g. wipe-func:f to delete the specified function e.g. f\n");
                    
                    
                    sb.append("Use `plot` or `plot2d` command to generate 2d plots e.g plot(y(x)=sin(x)) or plot2d(v(x)=cosh(x)) to generate a 2D plot... the 2D plotter is ParserNG's native plotter\n");
                    sb.append("Use `plot3d` command to generate 3d plots. e.g plot3d(cosh(x-2*y)) to generate a 3D plot....the 3D plotter is powered by `Jzy3d`\n");
                    sb.append("For power users, do: plot3d(expression, resolution)... e.g. plot3d(expression, 80).  `resolution` should be between 50 and 120 for modest PCs.\n "
                            + "This increases the detail of the graph, by using more points from the math engine, but the graph will be less responsive for higher resolution, so beware.");
                    
                    cli.printOutput(sb.toString());
                    return;
                }

                if (cmd.equals("wipe")) {
                    int sz = FunctionManager.FUNCTIONS.size();
                    FunctionManager.clear();
                    sz = sz - FunctionManager.FUNCTIONS.size();

                    int sz1 = VariableManager.VARIABLES.size();
                    VariableManager.clearVariables();
                    sz1 = sz1 - VariableManager.VARIABLES.size();
                    cli.printOutput("All variables(" + sz1 + " of them) and functions(" + sz + " of them) cleared from memory");
                    refreshTables();
                    visualizer.clearGraphData();
                    return;
                }
                if (cmd.equals("wipe-vars")) {
                    int sz = VariableManager.VARIABLES.size();
                    VariableManager.clearVariables();
                    sz = sz - VariableManager.VARIABLES.size();
                    cli.printOutput(sz + " variables cleared");
                    refreshTables();
                    return;
                }
                if (cmd.equals("wipe-funcs")) {
                    int sz = FunctionManager.FUNCTIONS.size();
                    FunctionManager.clear();
                    visualizer.clearCartesianGraphData();
                    sz = sz - FunctionManager.FUNCTIONS.size();
                    cli.printOutput(sz + " functions cleared");
                    refreshTables();
                    return;
                }
                if (cmd.startsWith("wipe-var:")) {
                    String target = cmd.substring(cmd.indexOf(":") + 1);
                    if (Variable.isVariableString(target.trim())) {
                        VariableManager.delete(target);
                        cli.printOutput("Variable " + target + " deleted");
                    } else {
                        cli.printOutput("Invalid Variable `" + target + "` specified");
                    }
                    refreshTables();
                    return;
                }
                if (cmd.startsWith("wipe-func:")) {
                    String target = cmd.substring(cmd.indexOf(":") + 1);
                    if (Variable.isVariableString(target.trim())) {
                        FunctionManager.delete(target);
                        visualizer.refreshGraphData();
                        cli.printOutput("Function " + target + " deleted");
                    } else {
                        cli.printOutput("Invalid Function `" + target + "` specified");
                    }
                    refreshTables();
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
                    //  TRIGGER THE LIVE UPDATE
                    refreshTables();
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
                        // TRIGGER THE LIVE UPDATE
                        refreshTables();
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

        if (cmd != null && !cmd.isEmpty()) {
            if (!cmd.equals("plot")) {
                visualizer.updateGraph(cmd);
            } else if (cmd.trim().startsWith("plot(") && cmd.endsWith(")")) {
                cmd = cmd.substring(5, cmd.length() - 1);
                visualizer.updateGraph(cmd);
            }
        }

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

    private StackPane createTopBar(Stage stage, Label modeIndicator, BorderPane root) {
        StackPane topBar = new StackPane();
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.getStyleClass().add("top-bar"); // For CSS styling

        // 1. LEFT SIDE: Mode Badge + Menu Items
        Label menuLabel = new Label("FILE   EDIT   VIEW   SESSION   HELP");
        menuLabel.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 11px; -fx-font-family: 'Consolas';");

        HBox leftArea = new HBox(20, modeIndicator, menuLabel);
        leftArea.setAlignment(Pos.CENTER_LEFT);
        leftArea.setPickOnBounds(false); // Allows drag events to pass through to StackPane

        // 2. CENTER: Branding
        Label appTitle = new Label("MATHINIX");
        appTitle.getStyleClass().add("app-title");
        StackPane.setAlignment(appTitle, Pos.CENTER);

        // 3. RIGHT SIDE: Close Button
        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4444; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> System.exit(0));

        HBox rightArea = new HBox(closeBtn);
        rightArea.setAlignment(Pos.CENTER_RIGHT);
        rightArea.setPickOnBounds(false);

        // Add everything to the StackPane
        topBar.getChildren().addAll(appTitle, leftArea, rightArea);

        // --- WINDOW LOGIC ---
        // Double-click to Maximize
        topBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                stage.setMaximized(!stage.isMaximized());
                if (stage.isMaximized()) {
                    root.setStyle("-fx-background-radius: 0; -fx-border-radius: 0;");
                } else {
                    root.setStyle("-fx-background-radius: 20; -fx-border-radius: 20;");
                }
            }
        });

        // Draggable Logic
        topBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        topBar.setOnMouseDragged(event -> {
            if (!stage.isMaximized()) { // Only drag if not maximized
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        return topBar;
    }

    // --- Dummy Data Tables ---
    private TableView<String[]> createLiveTable(String[] headers, ObservableList<String[]> dataList) {
        TableView<String[]> table = new TableView<>(dataList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        for (int i = 0; i < headers.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[colIndex]));
            table.getColumns().add(col);
        }

        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    /**
     * Refreshes all tables by pulling the latest data from the VariableManager
     * and FunctionManager.
     */
    public void refreshTables() {
        // 1. Sync Variables & Constants
        varData.clear();
        constData.clear();

        VariableManager.VARIABLES.forEach((name, var) -> {
            String value = String.valueOf(var.getValue());
            String type = var.getType().toString(); // e.g., SCALAR, VECTOR, MATRIX

            if (var.isConstant()) {
                constData.add(new String[]{name, value});
            } else {
                varData.add(new String[]{name, value, type});
            }
        });

        // 2. Sync Custom Functions
        funcData.clear();
        FunctionManager.FUNCTIONS.forEach((name, func) -> {
            // Assuming func.toString() or similar gives the definition like "n*n"
            funcData.add(new String[]{name, func.isMatrix() ? func.getMatrix().toString() : func.getMathExpression().getExpression()});
        });
    }

    /**
     * Loads the static inbuilt methods from the ParserNG library. This only
     * needs to be called once during startup.
     */
    private void loadInbuiltMethods() {
        methodsData.clear();
        String[] inbuilt = Method.getAllFunctions();

        for (String name : inbuilt) {
            // We'll place the name in the first column. 
            // For the syntax column, we can provide a generic "func(...)" 
            // or a specific signature if your library provides one.
            methodsData.add(new String[]{name, "arg..."});
        }
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
