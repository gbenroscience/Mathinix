/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.github.gbenroscience.mathinix;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author GBEMIRO
 */
public class GraphDisplay {

    private Stage stage;
    private StackPane root;

    public GraphDisplay() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("ParserNG - Visualizer");

        root = new StackPane();
        root.setPrefSize(800, 600);
        // Reuse your existing style.css for glassmorphism
        root.getStylesheets().add(getClass().getResource("/static/css/style.css").toExternalForm());
        root.getStyleClass().add("root");

        // Placeholder for the actual Jzy3d/FXyz3d Canvas
        Label placeholder = new Label("3D ARCHIVE INITIALIZING...");
        placeholder.setStyle("-fx-text-fill: #00D4FF; -fx-font-size: 20px;");
        root.getChildren().add(placeholder);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
    }

    public void show() {
        stage.show();
    }

    public void updateGraph(String expression) {
        // Logic to clear old plot and render new expression
        System.out.println("Updating graph for: " + expression);
    }
}