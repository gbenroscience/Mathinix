package com.github.gbenroscience.mathinix;

 

/**
 * JavaFX App
 */



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

public class App extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

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
        root.setTop(topBar);

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
        VBox centerPanel = new VBox(15);
        centerPanel.setPadding(new Insets(10, 20, 10, 20));
        
        TextField commandInput = new TextField("> solve(2x^2 + 5x - 3 = 0, x)");
        commandInput.getStyleClass().add("command-input");
        
        // Placeholder for Jzy3d or FXyz3d plot
        StackPane graphPlaceholder = new StackPane();
        graphPlaceholder.getStyleClass().add("graph-area");
        VBox.setVgrow(graphPlaceholder, Priority.ALWAYS);
        Label graphLabel = new Label("[ 3D Plot Area (e.g., Jzy3d/FXyz3d Canvas) ]");
        graphLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 18px;");
        graphPlaceholder.getChildren().add(graphLabel);

        centerPanel.getChildren().addAll(
                createSectionHeader("COMMAND LINE EXECUTION"),
                commandInput,
                graphPlaceholder
        );
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
        
        stage.setScene(scene);
        stage.show();
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
            new String[][]{ {"x", "3.5", "(scalar)"}, {"y", "[1, 2, 3]", "(vector)"}, {"A", "[[...]]", "(matrix)"} }
        );
    }

    private TableView<String[]> createFunctionsTable() {
        return createSimpleTable(
            new String[]{"FUNCTION", "DEF"},
            new String[][]{ {"sq(n)", "n*n"}, {"area_circ(r)", "pi * r^2"} }
        );
    }

    private TableView<String[]> createMethodsTable() {
        return createSimpleTable(
            new String[]{"METHOD", "SYNTAX"},
            new String[][]{ {"integrate", "(expr, var)"}, {"differentiate", "(expr, var)"}, {"plot", "(expr, range)"} }
        );
    }

    private TableView<String[]> createConstantsTable() {
        return createSimpleTable(
            new String[]{"SYMBOL", "VALUE"},
            new String[][]{ {"π", "3.14159"}, {"e", "2.71828"}, {"c", "299792458"} }
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