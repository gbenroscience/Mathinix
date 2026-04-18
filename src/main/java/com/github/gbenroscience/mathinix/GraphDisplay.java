package com.github.gbenroscience.mathinix;

import com.github.gbenroscience.math.graph.tools.GraphColor;
import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jzy3d.chart.AWTChart;
import org.jzy3d.chart.factories.IChartComponentFactory;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.javafx.JavaFXChartFactory;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.AbstractDrawable;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.modes.ViewBoundMode;

public class GraphDisplay {

    private Stage stage;
    private BorderPane root;
    private StackPane canvasArea;
    private Node sidebar;   // Reference to hide/show
    private Node inspector; // Reference to hide/show

    // 2D components
    private Canvas canvas;
    private GraphView graphView;

    // Data Lists
    private final ObservableList<String[]> geoData = FXCollections.observableArrayList();
    private final ObservableList<String[]> cartData = FXCollections.observableArrayList();

    private Node jzyNode;
    private boolean is3DMode = false;

    // === JZY3D FIELDS ===
    private JavaFXChartFactory factory;
    private AWTChart chart;
    private Shape currentSurface;

    private double zoomLevel = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private Color bgColor = Color.WHITE;
    private Color plotColor = Color.RED;
    private Color tickColor = Color.BLACK;
    private Color gridColor = Color.RED;
    private Color majorAxesColor = Color.BLACK;

    public GraphDisplay() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        root = new BorderPane();
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

        double decimal = 0.95; // Slightly larger window
        root.setPrefSize(d.getWidth() * decimal, d.getHeight() * decimal);
        root.getStylesheets().add(getClass().getResource("/static/css/graph.css").toExternalForm());
        root.getStyleClass().add("root-pane");

        // 2D Canvas setup
        this.canvas = new Canvas(600, 500);
        this.graphView = new GraphView(canvas);

        canvasArea = new StackPane(canvas);
        canvasArea.setStyle("-fx-background-color: #050505; -fx-background-radius: 10;");

        canvas.widthProperty().bind(canvasArea.widthProperty());
        canvas.heightProperty().bind(canvasArea.heightProperty());
        canvas.widthProperty().addListener(e -> graphView.repaint());
        canvas.heightProperty().addListener(e -> graphView.repaint());

        // Store references for layout switching
        this.sidebar = createDataSidebar();
        this.inspector = createInspector();

        root.setLeft(sidebar);
        root.setRight(inspector);
        root.setCenter(canvasArea);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
    }

    private VBox createDataSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(15));
        sidebar.setPrefWidth(300);
        sidebar.getStyleClass().add("sidebar-glass");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox cartBox = new VBox(10);
        TableView<String[]> cartTable = createSimpleTable(new String[]{"ID", "FUNCTION"}, cartData);
        TextField fInput = new TextField();
        fInput.setPromptText("f(x) = ...");
        fInput.setOnAction(e -> {
            updateGraph(fInput.getText());
            fInput.clear();
        });
        Button btnPlotF = new Button("PLOT FUNCTION");
        btnPlotF.getStyleClass().add("plot-btn");
        btnPlotF.setOnAction(e -> {
            updateGraph(fInput.getText());
            fInput.clear();
        });
        cartBox.getChildren().addAll(cartTable, fInput, btnPlotF);

        VBox geoBox = new VBox(10);
        TableView<String[]> geoTable = createSimpleTable(new String[]{"ID", "X-SET", "Y-SET"}, geoData);
        TextField xIn = new TextField();
        xIn.setPromptText("X: 1, 2, 3...");
        TextField yIn = new TextField();
        yIn.setPromptText("Y: 4, 5, 6...");
        Button btnPlotG = new Button("PLOT POINTS");
        btnPlotG.getStyleClass().add("plot-btn");
        btnPlotG.setOnAction(e -> {
            String xText = xIn.getText();
            String yText = yIn.getText();
            if (!xText.isEmpty() && !yText.isEmpty()) {
                String id = String.valueOf(geoData.size() + 1);
                geoData.add(new String[]{id, xText, yText});
                updateCompoundFunction();
            }
        });
        geoBox.getChildren().addAll(geoTable, xIn, yIn, btnPlotG);

        tabs.getTabs().addAll(new Tab("CARTESIAN", cartBox), new Tab("GEOMETRY", geoBox));
        sidebar.getChildren().addAll(new Label("DATA INPUTS"), tabs);
        return sidebar;
    }

    private ScrollPane createInspector() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(250);
        panel.getStyleClass().add("inspector-glass");

        HBox xRange = new HBox(5, new TextField("-10"), new Label("to"), new TextField("10"));
        CheckBox grid = new CheckBox("Show Grid");
        CheckBox auto = new CheckBox("Autoscale");
        CheckBox cal = new CheckBox("Calibrate");

        grid.setSelected(true);
        grid.selectedProperty().addListener((obs, was, is) -> graphView.setShowGridLines(is));
        auto.selectedProperty().addListener((obs, was, is) -> graphView.getGrid().setAutoScaleOn(is));
        cal.selectedProperty().addListener((obs, was, is) -> graphView.setLabelAxis(is));

        Spinner<Integer> gridSize = new Spinner<>(1, 100, 10);
        ToggleGroup drg = new ToggleGroup();
        RadioButton deg = new RadioButton("DEG");
        RadioButton rad = new RadioButton("RAD");
        RadioButton grad = new RadioButton("GRAD");
        deg.setToggleGroup(drg);
        rad.setToggleGroup(drg);
        grad.setToggleGroup(drg);
        rad.setSelected(true);
        HBox drgBox = new HBox(10, deg, rad, grad);

        ComboBox<String> colorTgt = new ComboBox<>(FXCollections.observableArrayList("Grid", "Axes", "Plot", "Tick", "BG"));
        colorTgt.getSelectionModel().selectFirst();
        Button cpBtn = new Button("Pick Color");
        cpBtn.getStyleClass().add("action-btn");
        cpBtn.setOnAction(e -> openColorChooser(colorTgt.getValue()));

        HBox actions = new HBox(10, new Button("SAVE"), new Button("PRINT"));

        panel.getChildren().addAll(
                new Label("RANGE"), xRange, new Separator(),
                grid, auto, cal,
                new Label("GRID SIZE"), gridSize, new Separator(),
                new Label("ANGLE MODE"), drgBox,
                new Label("STYLE"), colorTgt, cpBtn, new Separator(),
                actions
        );

        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    private TableView<String[]> createSimpleTable(String[] headers, ObservableList<String[]> data) {
        TableView<String[]> table = new TableView<>(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(250);
        for (int i = 0; i < headers.length; i++) {
            final int idx = i;
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[idx]));
            table.getColumns().add(col);
        }
        return table;
    }

    private Node create3DChartNode() {
        factory = new JavaFXChartFactory();
        Quality quality = Quality.Advanced;
        chart = (AWTChart) factory.newChart(quality, IChartComponentFactory.Toolkit.offscreen);

        chart.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        chart.getAxeLayout().setMainColor(org.jzy3d.colors.Color.GRAY);
        chart.getAxeLayout().setXAxeLabel("X");
        chart.getAxeLayout().setYAxeLabel("Y");
        chart.getAxeLayout().setZAxeLabel("Z");

        ImageView imageView = factory.bindImageView(chart);
        imageView.setPreserveRatio(true);

        // Ensure the ImageView expands to its container
        imageView.fitWidthProperty().bind(canvasArea.widthProperty());
        imageView.fitHeightProperty().bind(canvasArea.heightProperty());

        imageView.setUserData(chart);
        jzyNode = imageView;
        return imageView;
    }

    private void update3DPlot(String expression) {
        if (chart == null) {
            return;
        }

        if (expression.toLowerCase().startsWith("plot3d(") && expression.endsWith(")")) {
            expression = expression.substring(expression.indexOf("(") + 1, expression.lastIndexOf(")")).trim();
        }

        if (expression.contains("@")) {
            expression = expression.replaceAll("@\\(.*?\\)", "").trim();
        }

        try {
            final MathExpression expr = new MathExpression(expression);
            ScalarTurboEvaluator ste = new ScalarTurboEvaluator(expr, false);
            final FastCompositeExpression fce = ste.compile();
            final double[] vars = new double[expr.getRegistry().size()];

            if (currentSurface != null) {
                updateSurfaceInPlace(fce, vars);
            } else {
                buildNewSurface(fce, vars);
            }

            chart.getView().updateBounds();
            chart.getView().setBoundMode(ViewBoundMode.AUTO_FIT);
            chart.render();
            chart.getView().shoot();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void updateSurfaceInPlace(FastCompositeExpression fce, double[] vars) {
        List<AbstractDrawable> drawables = currentSurface.getDrawables();
        for (AbstractDrawable d : drawables) {
            if (d instanceof Polygon) {
                Polygon poly = (Polygon) d;
                for (Point p : poly.getPoints()) {
                    if (vars.length == 2) {
                        vars[0] = p.xyz.x;
                        vars[1] = p.xyz.y;
                    } else if (vars.length == 1) {
                        vars[0] = p.xyz.x;
                    }
                    p.xyz.z = (float) fce.applyScalar(vars);
                }
                poly.updateBounds();
            }
        }
        currentSurface.updateBounds();

        double zMin = currentSurface.getBounds().getZmin();
        double zMax = currentSurface.getBounds().getZmax();
        if (Math.abs(zMax - zMin) < 0.0001) {
            zMax = zMin + 1.0;
        }

        currentSurface.setColorMapper(new ColorMapper(
                new ColorMapRainbow(), zMin, zMax, new org.jzy3d.colors.Color(1f, 1f, 1f, 0.85f)
        ));
    }

    private void buildNewSurface(FastCompositeExpression fce, double[] vars) {
        Mapper mapper = new Mapper() {
            @Override
            public double f(double x, double y) {
                if (vars.length == 2) {
                    vars[0] = x;
                    vars[1] = y;
                } else if (vars.length == 1) {
                    vars[0] = x;
                }
                return fce.applyScalar(vars);
            }
        };

        Range range = new Range(-5, 5);
        int steps = 40;
        currentSurface = Builder.buildOrthonormal(new OrthonormalGrid(range, steps), mapper);

        currentSurface.setColorMapper(new ColorMapper(
                new ColorMapRainbow(),
                currentSurface.getBounds().getZmin(),
                currentSurface.getBounds().getZmax(),
                new org.jzy3d.colors.Color(1f, 1f, 1f, 0.85f)
        ));

        currentSurface.setFaceDisplayed(true);
        currentSurface.setWireframeDisplayed(true);
        currentSurface.setWireframeColor(new org.jzy3d.colors.Color(0, 0, 0, 0.15f));

        chart.getScene().getGraph().getAll().clear();
        chart.getScene().getGraph().add(currentSurface);
    }

    private void switchTo2D(String expression) {
        if (is3DMode) {
            // Restore UI panels
            root.setLeft(sidebar);
            root.setRight(inspector);

            canvasArea.getChildren().clear();
            canvasArea.getChildren().add(canvas);
            is3DMode = false;
        }
        if (expression != null) { 
                updateCompoundFunction();
        }
        graphView.repaint();
    }

    private void switchTo3D(String expression) {
        if (!is3DMode) {
            // Remove UI panels to allow full-screen center plot
            root.setLeft(null);
            root.setRight(null);

            canvasArea.getChildren().clear();
            if (jzyNode == null) {
                jzyNode = create3DChartNode();
            }
            canvasArea.getChildren().add(jzyNode);
            is3DMode = true;
        }
        update3DPlot(expression);
    }

    public void updateGraph(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return;
        }
        String cleanExpr = expression.trim();
        boolean is3D = cleanExpr.toLowerCase().startsWith("plot3d");

        try {
            if (is3D) {
                switchTo3D(cleanExpr);
            } else {
                if (cleanExpr.startsWith("plot2d(")) {
                    if (cleanExpr.endsWith(")")) {
                        cleanExpr = cleanExpr.substring(cleanExpr.indexOf("(") + 1, cleanExpr.lastIndexOf(")"));
                    }
                } else if (cleanExpr.startsWith("plot(")) {
                    if (cleanExpr.endsWith(")")) {
                        cleanExpr = cleanExpr.substring(cleanExpr.indexOf("(") + 1, cleanExpr.lastIndexOf(")"));
                    }
                } else if (cleanExpr.startsWith("plot2d") || cleanExpr.startsWith("plot")) {
                    cleanExpr = null;
                    //just 2d plot command.open page and ignore
                } 
                if (cleanExpr != null) {
                    String id = String.valueOf(cartData.size() + 1);
                    cartData.add(new String[]{id, cleanExpr});
                }
                switchTo2D(cleanExpr);

            }
        } catch (Exception ex) {
            System.err.println("Math Error: " + ex.getMessage());
        }
    }

    public void updateCompoundFunction() {
        StringBuilder compound = new StringBuilder();
        for (String[] row : geoData) {
            if (row.length >= 3) {
                String h = row[1];
                String v = row[2];
                if (h != null && v != null && !h.trim().isEmpty() && !v.trim().isEmpty()) {
                    compound.append(h).append(v).append(";");
                }
            }
        }
        for (String[] row : cartData) {
            if (row.length >= 2) {
                String fun = row[1];
                if (fun != null && !fun.trim().isEmpty()) {
                    compound.append(fun).append(";");
                }
            }
        }
        String finalExpression = compound.toString();
        if (!finalExpression.isEmpty()) {
            try {
                graphView.setFunction(finalExpression);
                graphView.repaint();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openColorChooser(String target) {
        ColorPicker picker = new ColorPicker();
        picker.setValue(Color.ORANGE);
        Dialog<Color> dialog = new Dialog<>();
        dialog.setTitle("Select " + target + " Color");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(new VBox(10, new Label("Pick a color for " + target), picker));
        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? picker.getValue() : null);
        dialog.showAndWait().ifPresent(selectedColor -> applyColorToGraph(target, selectedColor));
    }

    private void applyColorToGraph(String target, Color c) {
        GraphColor gc = new GraphColor(
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255), (int) (c.getOpacity() * 255)
        );

        switch (target) {
            case "Grid":
                graphView.setGridColor(gc);
                break;
            case "Axes":
                graphView.setMajorAxesColor(gc);
                break;
            case "Plot":
                graphView.setPlotColor(gc);
                break;
            case "Tick":
                graphView.setTickColor(gc);
                break;
            case "BG":
                graphView.setBgColor(gc);
                break;
        }
    }

    public void show() {
        stage.show();
    }
}
