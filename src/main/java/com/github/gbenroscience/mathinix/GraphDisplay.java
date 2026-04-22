package com.github.gbenroscience.mathinix;

import com.github.gbenroscience.math.graph.tools.GraphColor;
import com.github.gbenroscience.parser.Function;
import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.util.FunctionManager;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
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
import org.jzy3d.maths.Coord3d;
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
    private Node sidebar;
    private Node inspector;

    // 2D components
    private Canvas canvas;
    private GraphView graphView;
    private int resolution = 50;

    // Data Lists
    final ObservableList<String[]> geoData = FXCollections.observableArrayList();
    final ObservableList<String[]> cartData = FXCollections.observableArrayList();

    private Node jzyNode;
    private boolean is3DMode = false;

    // === JZY3D FIELDS ===
    private JavaFXChartFactory factory;
    private AWTChart chart;
    private Shape currentSurface;

    public GraphDisplay() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        root = new BorderPane();
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        double decimal = 0.95;
        root.setPrefSize(d.getWidth() * decimal, d.getHeight() * decimal);

        root.getStylesheets().add(getClass().getResource("/static/css/graph.css").toExternalForm());
        root.getStyleClass().add("root-pane");

        this.canvas = new Canvas(600, 500);
        this.graphView = new GraphView(canvas);

        canvasArea = new StackPane(canvas);
        canvasArea.setStyle("-fx-background-color: #050505; -fx-background-radius: 10;");

        canvas.widthProperty().bind(canvasArea.widthProperty());
        canvas.heightProperty().bind(canvasArea.heightProperty());
        canvas.widthProperty().addListener(e -> graphView.repaint());
        canvas.heightProperty().addListener(e -> graphView.repaint());

        this.sidebar = createDataSidebar();
        this.inspector = createInspector();

        root.setLeft(sidebar);
        root.setRight(inspector);
        root.setCenter(canvasArea);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
    }

    // ==================== ParallelMapper (unchanged but kept) ====================
    private static class ParallelMapper extends Mapper {

        private final double[][] gridCache;
        private final double xMin, yMin, stepX, stepY;
        private final int steps;

        // One compiled instance per thread — eliminates O(steps) compiles
        private final ThreadLocal<FastCompositeExpression> THREAD_LOCAL_FC = new ThreadLocal<>();

        public ParallelMapper(FastCompositeExpression fce, Range range, int steps, int varsCount) {
            this.steps = steps;
            this.gridCache = new double[steps][steps];
            this.xMin = range.getMin();
            this.yMin = range.getMin();
            double span = range.getRange();
            this.stepX = span / (steps - 1.0);
            this.stepY = span / (steps - 1.0);

            IntStream.range(0, steps).parallel().forEach(i -> {
                double x = xMin + i * stepX;
                double[] vars = new double[Math.max(varsCount, 2)];
                vars[0] = x;

                // Compile exactly once per thread
                FastCompositeExpression fc = THREAD_LOCAL_FC.get();
                if (fc == null) {
                    try {
                        MathExpression m = fce.getRoot().copy();
                        fc = m.compileTurbo();
                        THREAD_LOCAL_FC.set(fc);
                    } catch (Throwable ex) {
                        System.getLogger(GraphDisplay.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                    }
                }

                for (int j = 0; j < steps; j++) {
                    double y = yMin + j * stepY;
                    if (varsCount > 1) {
                        vars[1] = y;
                    }
                    gridCache[i][j] = fc.applyScalar(vars);
                }
            });
        }

        @Override
        public double f(double x, double y) {
            double gx = (x - xMin) / stepX;
            double gy = (y - yMin) / stepY;

            int i = (int) Math.floor(gx + 1e-10);
            int j = (int) Math.floor(gy + 1e-10);

            i = Math.max(0, Math.min(steps - 2, i));
            j = Math.max(0, Math.min(steps - 2, j));

            double fx = gx - i;
            double fy = gy - j;

            double v00 = gridCache[i][j];
            double v10 = gridCache[i + 1][j];
            double v01 = gridCache[i][j + 1];
            double v11 = gridCache[i + 1][j + 1];

            return (1 - fx) * (1 - fy) * v00
                    + fx * (1 - fy) * v10
                    + (1 - fx) * fy * v01
                    + fx * fy * v11;
        }
    }

    private Node create3DChartNode() {
        factory = new JavaFXChartFactory();

        // Correct quality setup for version 1.0.3
        Quality quality = Quality.Advanced;
        quality.setAlphaActivated(true);
        quality.setSmoothColor(true);
        quality.setSmoothPoint(true);
        quality.setSmoothEdge(true);
        quality.setSmoothPolygon(true);

        chart = (AWTChart) factory.newChart(quality, IChartComponentFactory.Toolkit.offscreen);

        chart.getView().setBackgroundColor(org.jzy3d.colors.Color.WHITE);
        chart.getAxeLayout().setMainColor(org.jzy3d.colors.Color.GRAY);
        chart.getAxeLayout().setXAxeLabel("X");
        chart.getAxeLayout().setYAxeLabel("Y");
        chart.getAxeLayout().setZAxeLabel("Z");

        // Better camera view
        chart.getView().getCamera().setEye(new org.jzy3d.maths.Coord3d(12, 12, 12));
        chart.getView().getCamera().setTarget(new org.jzy3d.maths.Coord3d(0, 0, 2));

        ImageView imageView = factory.bindImageView(chart);
        imageView.setPreserveRatio(true);
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

        if (expression.toLowerCase().startsWith("plot3d")) {
            if (expression.contains("(") && expression.contains(")")) {
                expression = expression.substring(expression.indexOf("(") + 1, expression.lastIndexOf(")")).trim();
            }
        }
        if (expression.contains("@")) {
            expression = expression.replaceAll("@\\(.*?\\)", "").trim();
        }

        try {
            final MathExpression expr = new MathExpression(expression);
            var ofc = expr.compileTurbo();
            var fce = new FastCompositeExpression() {
                @Override
                public MathExpression.EvalResult apply(double[] variables) {
                    return ofc.apply(variables);
                }

                @Override
                public double applyScalar(double[] variables) {
                    return ofc.applyScalar(variables);
                }

                @Override
                public MathExpression getRoot() {
                    return expr;
                }

            };

            final double[] vars = new double[expr.getRegistry().size()];
            int xIdx = expr.getRegistry().getSlot("x");
            int yIdx = expr.getRegistry().getSlot("y");
            

            if (currentSurface != null) {
                updateSurfaceInPlace(fce, vars, xIdx, yIdx);
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

    private void buildNewSurface(FastCompositeExpression fce, double[] vars) {
        int steps = resolution;
        Range range = new Range(-8, 8);

        ParallelMapper parallelMapper = new ParallelMapper(fce, range, steps, vars.length);

        currentSurface = Builder.buildOrthonormal(new OrthonormalGrid(range, steps), parallelMapper);

        double zMin = currentSurface.getBounds().getZmin();
        double zMax = currentSurface.getBounds().getZmax();

        currentSurface.setColorMapper(new ColorMapper(
                new ColorMapRainbow(),
                zMin, zMax,
                new org.jzy3d.colors.Color(1f, 1f, 1f, 0.92f)
        ));

        currentSurface.setFaceDisplayed(true);

//currentSurface.setWireframeDisplayed(false);   // try turning wireframe off first
        //// or make it very faint
//currentSurface.setWireframeColor(new org.jzy3d.colors.Color(0.1f, 0.1f, 0.1f, 0.2f));
//        
//        
        
       currentSurface.setWireframeDisplayed(true);
        currentSurface.setWireframeColor(new org.jzy3d.colors.Color(0.05f, 0.05f, 0.05f, 0.3f));

        chart.getScene().getGraph().getAll().clear();
        chart.getScene().getGraph().add(currentSurface);

        // Camera adjustment
        chart.getView().getCamera().setEye(new Coord3d(12, 12, 12));
        chart.getView().getCamera().setTarget(new Coord3d(0, 0, 1));
    }

    private void updateSurfaceInPlace(FastCompositeExpression fce, double[] vars, int xIdx, int yIdx) {
        List<AbstractDrawable> drawables = currentSurface.getDrawables();
        for (AbstractDrawable d : drawables) {
            if (d instanceof Polygon) {
                Polygon poly = (Polygon) d;
                for (Point p : poly.getPoints()) {
// Safely inject coordinates only if the variable exists in the function
                    if (xIdx != -1) {
                        vars[xIdx] = p.xyz.x;
                    }
                    if (yIdx != -1) {
                        vars[yIdx] = p.xyz.y;
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
                new ColorMapRainbow(), zMin, zMax,
                new org.jzy3d.colors.Color(1f, 1f, 1f, 0.92f)
        ));
    }

    // ==================== Rest of your methods (unchanged) ====================
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
        int commaIndex = expression.lastIndexOf(",");
        if (commaIndex == -1) {//plot3d(expr)
            update3DPlot(expression);
        } else {//plot3d(expr,resolution)
            try {
                resolution = Integer.parseInt(expression.substring(commaIndex + 1, expression.lastIndexOf(")")).trim());
                System.out.println("resolution = " + resolution);
                expression = expression.substring(0, expression.lastIndexOf(",")) + ")";
                currentSurface = null;
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Invalid resolution!");
            }
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
                }
                if (cleanExpr != null) {
                    Function f = null;
                    if ((f = FunctionManager.lookUp(cleanExpr)) != null) {//e.g... plot2d(f)
                        cleanExpr = f.getName() + "=" + f.expressionForm();
                    } else if (!cleanExpr.contains("=")) {//a plain math expression was entered.. enforce that it must have only 1 variable
                        MathExpression m = new MathExpression(cleanExpr);
                        String varNames[] = m.getVariablesNames();
                        if (varNames.length == 1) {
                            cleanExpr = "@(" + varNames[0] + ")" + m.getExpression();
                            Function anonFunc = new Function(cleanExpr);
                            String fName = anonFunc.getName();
                            cleanExpr = fName + "=" + cleanExpr;
                        } else {
                            cleanExpr = null;
                            System.err.println("Wrong! a plain math expression must not have more than 1 variable! found: " + Arrays.toString(varNames));
                        }
                    } else {
                        //cool, just pass the expression, it is already in h(x)=expression form
                    }

                    if (cleanExpr != null) {
                        String id = String.valueOf(cartData.size() + 1);
                        cartData.add(new String[]{id, cleanExpr});
                        switchTo2D(cleanExpr);
                    }
                }

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
        graphView.setFunction(finalExpression);
        graphView.repaint();
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

        final TextField lowerRange = new TextField("-10");
        final TextField upperRange = new TextField("10");

        lowerRange.setOnAction((t) -> {
            try {
                double lower = Double.parseDouble(lowerRange.getText());
                graphView.setLowerXLimit(lower);
            } catch (Exception e) {
                System.out.println("Bad value!!! on lower range for x");
                //handle error
            }

        });

        upperRange.setOnAction((t) -> {
            try {
                double upper = Double.parseDouble(upperRange.getText());
                graphView.setUpperXLimit(upper);
            } catch (Exception e) {
                System.out.println("Bad value!!! on upper range for x");
                //handle error
            }
        });

        HBox xRange = new HBox(5, lowerRange, new Label("to"), upperRange);
        CheckBox grid = new CheckBox("Show Grid");
        CheckBox auto = new CheckBox("Autoscale");
        CheckBox cal = new CheckBox("Calibrate");

        grid.setSelected(true);
        grid.selectedProperty().addListener((obs, was, is) -> graphView.setShowGridLines(is));
        auto.selectedProperty().addListener((obs, was, is) -> graphView.setAutoScaleOn(is));
        cal.selectedProperty().addListener((obs, was, is) -> graphView.setLabelAxis(is));

        Spinner<Integer> gridSize = new Spinner<>(1, 100, 10);

// Listen to the value property for changes to the integer
        gridSize.valueProperty().addListener((obs, oldValue, newValue) -> {
            graphView.setGridSize(newValue);
        });

        ToggleGroup drg = new ToggleGroup();
        RadioButton deg = new RadioButton("DEG");
        RadioButton rad = new RadioButton("RAD");
        RadioButton grad = new RadioButton("GRAD");
        deg.setToggleGroup(drg);
        rad.setToggleGroup(drg);
        grad.setToggleGroup(drg);
        rad.setSelected(true);
        HBox drgBox = new HBox(10, deg, rad, grad);

        deg.selectedProperty().addListener((obs, was, is) -> {
            if (is) {
                graphView.getGrid().setDRG(0);
                graphView.repaint();
            }
        });
        rad.selectedProperty().addListener((obs, was, is) -> {
            if (is) {
                graphView.getGrid().setDRG(1);
                graphView.repaint();
            }
        });
        grad.selectedProperty().addListener((obs, was, is) -> {
            if (is) {
                graphView.getGrid().setDRG(2);
                graphView.repaint();
            }
        });

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

    void clearGraphData() {
        cartData.clear();
        geoData.clear();
        updateCompoundFunction();
    }

    void clearCartesianGraphData() {
        cartData.clear();
        updateCompoundFunction();
    }

    void refreshGraphData() {
        ObservableList<String[]> fresh = FXCollections.observableArrayList();
        for (int i = 0; i < cartData.size(); i++) {
            String[] a = cartData.get(i);
            String in = a[1].substring(0, a[1].indexOf("="));
            if (FunctionManager.lookUp(in) == null) {
                FunctionManager.delete(in);
            } else {
                fresh.add(a);
            }
        }
        cartData.clear();
        cartData.addAll(fresh);

        updateCompoundFunction();
    }

    public void show() {
        stage.show();
    }
}
