package com.github.gbenroscience.mathinix;

/**
 *
 * @author GBEMIRO
 */
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import com.github.gbenroscience.math.graph.DrawingContext;
import com.github.gbenroscience.math.graph.tools.FontStyle;
import com.github.gbenroscience.math.graph.tools.GraphColor;
import com.github.gbenroscience.math.graph.tools.GraphFont;
import com.github.gbenroscience.math.graph.tools.TextDimensions;

class JavaFXDrawingContext implements DrawingContext {
    private final GraphicsContext gc;
    private final float scale = 1.0f;

    public JavaFXDrawingContext(GraphicsContext gc) {
        this.gc = gc;
    }

    public GraphicsContext getGc() {
        return gc;
    }
    
    

    @Override
    public void setColor(GraphColor c) {
        Color fxColor = Color.rgb(c.r, c.g, c.b, c.a / 255.0);
        gc.setStroke(fxColor);
        gc.setFill(fxColor);
    }
    
    
    
    
    @Override
    public void setStrokeWidth(float w) {
        // Safeguard 1: Prevent invisible hairlines
        gc.setLineWidth(w <= 0.0f ? 1.0f : w);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        // Safeguard 2: Prevent JavaFX from choking on asymptotes or invalid math
        if (!Float.isFinite(x1) || !Float.isFinite(y1) || !Float.isFinite(x2) || !Float.isFinite(y2)) {
            return; // Skip invalid math points safely
        }
        gc.strokeLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(float x, float y, float w, float h) {
        // Safeguard 3: Prevent zero-dimension points from vanishing
        gc.fillRect(x, y, w <= 0 ? 1.0f : w, h <= 0 ? 1.0f : h);
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
        gc.strokeRect(x, y, w <= 0 ? 1.0f : w, h <= 0 ? 1.0f : h);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        gc.strokeOval(x, y, width <= 0 ? 1.0f : width, height <= 0 ? 1.0f : height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        gc.fillOval(x, y, width <= 0 ? 1.0f : width, height <= 0 ? 1.0f : height);
    }
    
    @Override
    public void setFont(GraphFont f) {
        FontWeight weight = FontWeight.NORMAL;
        FontPosture posture = FontPosture.REGULAR;

        if (f.getStyle() == FontStyle.BOLD) weight = FontWeight.BOLD;
        if (f.getStyle() == FontStyle.ITALIC) posture = FontPosture.ITALIC;
        if (f.getStyle() == FontStyle.BOLD_ITALIC) {
            weight = FontWeight.BOLD;
            posture = FontPosture.ITALIC;
        }

        gc.setFont(Font.font(f.getFamily(), weight, posture, f.getSize()));
    }

    @Override
    public void drawText(String text, float x, float y) {
        gc.fillText(text, x, y);
    }

    @Override
    public TextDimensions measureText(String text) {
        Text helper = new Text(text);
        helper.setFont(gc.getFont());
        return new TextDimensions(
            (float) helper.getLayoutBounds().getWidth(),
            (float) helper.getLayoutBounds().getHeight()
        );
    }

    @Override
    public float getScale() {
        return scale;
    }
}