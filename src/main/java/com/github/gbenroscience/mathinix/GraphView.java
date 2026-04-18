package com.github.gbenroscience.mathinix;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import com.github.gbenroscience.math.Point;
import com.github.gbenroscience.math.graph.AbstractView;
import com.github.gbenroscience.math.graph.Grid;
import com.github.gbenroscience.math.graph.tools.FontStyle;
import com.github.gbenroscience.math.graph.tools.GraphColor;
import com.github.gbenroscience.math.graph.tools.GraphFont;
import java.util.Arrays;
import javafx.scene.paint.Color;

public class GraphView implements AbstractView {

    private final Canvas canvas;

    private boolean isPending = false;

    private Grid grid;
    private String function = "p(x)=3*x^2";
    private double zoomLevel = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private boolean reloadGraphics;

    private int gridSize = 8;
    private boolean showGridLines = true;
    private boolean labelAxis = true;
    private GraphColor gridColor = GraphColor.green;
    private GraphColor majorAxesColor = GraphColor.black;
    private GraphColor tickColor = GraphColor.black;
    private GraphColor plotColor = GraphColor.red;
    private GraphColor bgColor = GraphColor.WHITE;
    private int majorTickLength = 8;
    private int minorTickLength = 4;
    private double lowerXLimit = -200;
    private double upperXLimit = 200;
    private double xStep = 0.1;
    private double yStep = 0.1;
    private GraphFont font = new GraphFont("Times New Roman", FontStyle.BOLD, 14);
    private Point locationOfOrigin = new Point();
    private JavaFXDrawingContext context;

    // For mouse dragging functionality
    private double startX;
    private double startY;
    private final Tooltip tooltip;

    public GraphView(Canvas canvas) {
        this.canvas = canvas;
        this.context = new JavaFXDrawingContext(canvas.getGraphicsContext2D());
        this.context.setStrokeWidth(1);

        this.grid = new Grid(function, showGridLines, labelAxis, gridSize, gridColor, majorAxesColor, tickColor, plotColor, majorTickLength,
                minorTickLength, lowerXLimit, upperXLimit, xStep, yStep, font, false, this);

        // 2. Set Grid origin to the physical center of the canvas
        double centerX = canvas.getWidth() / 2;
        double centerY = canvas.getHeight() / 2;
        this.grid.setLocationOfOrigin(new Point(centerX, centerY));

        this.tooltip = new Tooltip();
        Tooltip.install(canvas, tooltip);

        initMouseListeners();
    }

    private void initMouseListeners() {
        // Change cursor on hover
        canvas.setOnMouseEntered(e -> canvas.setCursor(Cursor.HAND));
        canvas.setOnMouseExited(e -> canvas.setCursor(Cursor.DEFAULT));

        // Tooltip for coordinate tracking
        canvas.setOnMouseMoved(this::formMouseMoved);

        // Pan/Drag Tracking
        canvas.setOnMousePressed(this::formMousePressed);
        canvas.setOnMouseDragged(this::formMouseDragged);
        canvas.setOnMouseReleased(this::formMouseReleased);
        canvas.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {
                resetView();
            }
        });
    }

    public void resetView() {
        // 1. Reset viewport variables
        this.offsetX = 0;
        this.offsetY = 0;
        this.zoomLevel = 1.0;

        // 2. Set Grid origin to the physical center of the canvas
        double centerX = canvas.getWidth() / 2;
        double centerY = canvas.getHeight() / 2;
        grid.setLocationOfOrigin(new Point(centerX, centerY));

        repaint();
    }

    private String doubleXYToString(double[] xy) {
        return "[x,y] = [" + xy[0] + "," + xy[1] + "]";
    }

    private void formMouseMoved(MouseEvent evt) {
        try {
            double[] coords = grid.convertScreenPointToGraphCoords((int) evt.getX(), (int) evt.getY());
            tooltip.setText(doubleXYToString(coords));
        } catch (NullPointerException ignored) {
        }
    }

    private void formMousePressed(MouseEvent evt) {
        startX = evt.getX();
        startY = evt.getY();
    }

    private void formMouseDragged(MouseEvent evt) {
        double shiftX = evt.getX() - startX;
        double shiftY = evt.getY() - startY;

        Point p = grid.getLocationOfOrigin();
        grid.setLocationOfOrigin(new Point(p.x + shiftX, p.y + shiftY));

        startX = evt.getX();
        startY = evt.getY();

        repaint();
    }

    private void formMouseReleased(MouseEvent evt) {
        startX = evt.getX();
        startY = evt.getY();
    }

    @Override
    public int getWidth() {
        return (int) canvas.getWidth();
    }

    @Override
    public int getHeight() {
        return (int) canvas.getHeight();
    }

    public JavaFXDrawingContext getContext() {
        return context;
    }

    public Grid getGrid() {
        return grid;
    }

    // --- Transform Controls ---
    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = zoomLevel;
        repaint();
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
        repaint();
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
        repaint();
    }

    public void applySettings(double zoomLevel, double offsetX, double offsetY, Color plotColor, Color gridColor, Color majorAxesColor, Color bgColor, Color tickColor) {
        this.zoomLevel = zoomLevel;
        this.offsetX = offsetX;
        this.offsetY = offsetY;

        // Correctly scale 0.0-1.0 doubles to 0-255 integers
        this.bgColor = new GraphColor(
                (int) (bgColor.getRed() * 255), (int) (bgColor.getGreen() * 255), (int) (bgColor.getBlue() * 255), (int) (bgColor.getOpacity() * 255));

        this.gridColor = new GraphColor(
                (int) (gridColor.getRed() * 255), (int) (gridColor.getGreen() * 255), (int) (gridColor.getBlue() * 255), (int) (gridColor.getOpacity() * 255));

        this.plotColor = new GraphColor(
                (int) (plotColor.getRed() * 255), (int) (plotColor.getGreen() * 255), (int) (plotColor.getBlue() * 255), (int) (plotColor.getOpacity() * 255));

        this.majorAxesColor = new GraphColor(
                (int) (majorAxesColor.getRed() * 255), (int) (majorAxesColor.getGreen() * 255), (int) (majorAxesColor.getBlue() * 255), (int) (majorAxesColor.getOpacity() * 255));

        this.tickColor = new GraphColor(
                (int) (tickColor.getRed() * 255), (int) (tickColor.getGreen() * 255), (int) (tickColor.getBlue() * 255), (int) (tickColor.getOpacity() * 255));

        grid.setPlotColor(this.plotColor);
        grid.setGridColor(this.gridColor);
        grid.setMajorAxesColor(this.majorAxesColor);
        grid.setTickColor(this.tickColor);

        repaint();
    }

    // --- Graph Property Setters ---
    public void setGrid(Grid grid) {
        this.grid = grid;
        repaint();
    }

    public void setFunction(String function) {
        this.function = function;
        grid.setFunction(function);
        grid.setRefreshingIndices(true);
        repaint();
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
        grid.setGridSize(gridSize, gridSize);
        repaint();
    }

    public void setShowGridLines(boolean showGridLines) {
        this.showGridLines = showGridLines;
        grid.setShowGridLines(showGridLines);
        repaint();
    }

    public void setLabelAxis(boolean labelAxis) {
        this.labelAxis = labelAxis;
        grid.setLabelAxis(labelAxis);
        repaint();
    }

    public void setGridColor(GraphColor gridColor) {
        this.gridColor = gridColor;
        grid.setGridColor(gridColor);
        repaint();
    }

    public void setMajorAxesColor(GraphColor majorAxesColor) {
        this.majorAxesColor = majorAxesColor;
        grid.setMajorAxesColor(majorAxesColor);
        repaint();
    }

    public void setTickColor(GraphColor tickColor) {
        this.tickColor = tickColor;
        grid.setTickColor(tickColor);
        repaint();
    }

    public void setPlotColor(GraphColor plotColor) {
        this.plotColor = plotColor;
        grid.setPlotColor(plotColor);
        repaint();
    }

    public void setBgColor(GraphColor bgColor) {
        this.bgColor = bgColor;
        repaint();
    }

    public GraphColor getBgColor() {
        return bgColor;
    }

    public void setMajorTickLength(int majorTickLength) {
        this.majorTickLength = majorTickLength;
        grid.setMajorTickLength(majorTickLength);
        repaint();
    }

    public void setMinorTickLength(int minorTickLength) {
        this.minorTickLength = minorTickLength;
        grid.setMinorTickLength(minorTickLength);
        repaint();
    }

    public void setLowerXLimit(double lowerXLimit) {
        this.lowerXLimit = lowerXLimit;
        grid.setLowerXLimit(lowerXLimit);
        repaint();
    }

    public void setUpperXLimit(double upperXLimit) {
        this.upperXLimit = upperXLimit;
        grid.setUpperXLimit(upperXLimit);
        repaint();
    }

    public void setxStep(double xStep) {
        this.xStep = xStep;
        grid.setxStep(xStep);
        repaint();
    }

    public void setyStep(double yStep) {
        this.yStep = yStep;
        grid.setyStep(yStep);
        repaint();
    }

    public void setLocationOfOrigin(Point locationOfOrigin) {
        this.locationOfOrigin = locationOfOrigin;
        grid.setLocationOfOrigin(locationOfOrigin);
        repaint();
    }

    public void setLocationOfOrigin(double originX, double originY) {
        this.locationOfOrigin.x = originX;
        this.locationOfOrigin.y = originY;
        setLocationOfOrigin(locationOfOrigin);
    }

    public void setFont(GraphFont font) {
        this.font = font;
        grid.setFont(this.font);
        repaint();
    }

    // --- Rendering Logic ---
    @Override
    public void repaint(int x, int y, int width, int height) {
        if (isPending) {
            return; // Already a frame in the queue, skip this one
        }
        isPending = true;
        javafx.application.Platform.runLater(() -> {
            try {
                GraphicsContext gc = canvas.getGraphicsContext2D();
                context.setColor(bgColor);
                gc.fillRect(0, 0, width, height);
                gc.save(); // Save the original state
                // Apply any specific manual offset 
                gc.translate(offsetX, offsetY);
                // Apply the Zoom
                gc.scale(zoomLevel, zoomLevel);
                drawCoordinateSystem(gc);
                gc.restore(); // Restore to clean state for next frame
            } finally {
                isPending = false;
            }
        });

    }

    @Override
    public void repaint() {
        repaint(0, 0, (int) canvas.getWidth(), (int) canvas.getHeight());
    }

    private void drawCoordinateSystem(GraphicsContext gc) {
        if (context == null || !reloadGraphics) {
            context = new JavaFXDrawingContext(gc);
            reloadGraphics = true;
        }
        grid.draw(context);

    }

}
