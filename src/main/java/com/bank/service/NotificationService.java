package com.bank.service;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.scene.Scene;
import javafx.util.Duration;

public class NotificationService {

    public enum Type { SUCCESS, ERROR, INFO, WARNING }

    private static Stage ownerStage;

    public static void setOwnerStage(Stage stage) {
        ownerStage = stage;
    }

    public static void show(String title, String message, Type type) {
        if (ownerStage == null) return;

        String icon = switch (type) {
            case SUCCESS -> "✅";
            case ERROR   -> "❌";
            case INFO    -> "ℹ️";
            case WARNING -> "⚠️";
        };

        String styleClass = switch (type) {
            case SUCCESS -> "notif-success";
            case ERROR   -> "notif-error";
            case INFO    -> "notif-info";
            case WARNING -> "notif-warning";
        };

        // Build popup content
        VBox content = new VBox(6);
        content.getStyleClass().addAll("notif-popup", styleClass);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setMinWidth(300);
        content.setMaxWidth(360);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 14px;");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("notif-title");
        titleRow.getChildren().addAll(iconLabel, titleLabel);

        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("notif-message");
        msgLabel.setWrapText(true);

        // Progress bar (auto-close timer)
        Region progress = new Region();
        String barColor = switch (type) {
            case SUCCESS -> "#00D4AA";
            case ERROR   -> "#FF6B6B";
            case INFO    -> "#4499FF";
            case WARNING -> "#FFB347";
        };
        progress.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 2;");
        progress.setPrefHeight(3);
        progress.setPrefWidth(320);

        content.getChildren().addAll(titleRow, msgLabel, progress);

        // Create popup stage
        Stage popup = new Stage();
        popup.initOwner(ownerStage);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.setAlwaysOnTop(true);

        StackPane root = new StackPane(content);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(
            NotificationService.class.getResource("/styles/dark-theme.css").toExternalForm());
        popup.setScene(scene);

        // Position: bottom-right
        double screenW = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        double screenH = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
        popup.setX(screenW - 380);
        popup.setY(screenH - 140);

        popup.show();

        // Slide in animation
        content.setTranslateX(400);
        content.setOpacity(0);
        Timeline slideIn = new Timeline(
            new KeyFrame(Duration.millis(350),
                new KeyValue(content.translateXProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(content.opacityProperty(), 1, Interpolator.EASE_OUT))
        );

        // Progress bar shrink
        Timeline progressAnim = new Timeline(
            new KeyFrame(Duration.millis(3500),
                new KeyValue(progress.prefWidthProperty(), 0, Interpolator.LINEAR))
        );

        // Slide out and close
        Timeline slideOut = new Timeline(
            new KeyFrame(Duration.millis(300),
                new KeyValue(content.translateXProperty(), 400, Interpolator.EASE_IN),
                new KeyValue(content.opacityProperty(), 0, Interpolator.EASE_IN))
        );
        slideOut.setOnFinished(e -> popup.close());

        slideIn.play();
        progressAnim.play();

        PauseTransition wait = new PauseTransition(Duration.millis(3500));
        wait.setOnFinished(e -> slideOut.play());
        wait.play();

        // Click to dismiss
        content.setOnMouseClicked(e -> {
            wait.stop();
            progressAnim.stop();
            slideOut.play();
        });
    }

    public static void success(String title, String message) { show(title, message, Type.SUCCESS); }
    public static void error(String title, String message)   { show(title, message, Type.ERROR);   }
    public static void info(String title, String message)    { show(title, message, Type.INFO);    }
    public static void warning(String title, String message) { show(title, message, Type.WARNING); }
}
