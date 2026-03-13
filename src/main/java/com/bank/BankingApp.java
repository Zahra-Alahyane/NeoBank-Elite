package com.bank;

import com.bank.service.NotificationService;
import com.bank.ui.LoginController;
import javafx.application.Application;
import javafx.stage.Stage;

public class BankingApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        NotificationService.setOwnerStage(primaryStage);

        LoginController login = new LoginController(primaryStage);

        primaryStage.setScene(login.buildScene());
        primaryStage.setTitle("🏦 NeoBank Elite — Connexion");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(750);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
