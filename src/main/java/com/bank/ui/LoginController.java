package com.bank.ui;

import com.bank.database.DatabaseConnection;
import com.bank.database.UserDAO;
import com.bank.model.AppUser;
import com.bank.model.SessionManager;
import com.bank.service.BankService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginController {

    private Stage stage;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button loginBtn;

    public LoginController(Stage stage) {
        this.stage = stage;
    }

    public Scene buildScene() {
        StackPane root = new StackPane();
        root.getStyleClass().add("login-root");

        // Animated background circles
        root.getChildren().add(buildBackgroundDecor());

        // Center card
        VBox card = new VBox(20);
        card.getStyleClass().add("login-card");
        card.setPadding(new Insets(48, 48, 48, 48));
        card.setMaxWidth(420);
        card.setMinWidth(380);
        card.setAlignment(Pos.CENTER);

        // Logo
        HBox logoRow = new HBox(4);
        logoRow.setAlignment(Pos.CENTER);
        Text neo = new Text("Neo");
        neo.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-fill: #E8F2FF;");
        Text bank = new Text("Bank");
        bank.getStyleClass().add("login-accent");
        bank.setStyle("-fx-font-size: 32px; -fx-font-weight: 900;");
        Text elite = new Text(" Elite");
        elite.setStyle("-fx-font-size: 32px; -fx-font-weight: 300; -fx-fill: #4A6A88;");
        logoRow.getChildren().addAll(neo, bank, elite);

        Label subtitle = new Label("Système de Gestion Bancaire");
        subtitle.getStyleClass().add("login-subtitle");

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1E2D44; -fx-opacity: 0.5;");

        // Username
        VBox usernameBox = new VBox(8);
        Label userLabel = new Label("IDENTIFIANT");
        userLabel.getStyleClass().add("login-label");
        usernameField = new TextField();
        usernameField.getStyleClass().add("login-field");
        usernameField.setPromptText("Entrez votre identifiant");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameBox.getChildren().addAll(userLabel, usernameField);

        // Password
        VBox passwordBox = new VBox(8);
        Label passLabel = new Label("MOT DE PASSE");
        passLabel.getStyleClass().add("login-label");
        passwordField = new PasswordField();
        passwordField.getStyleClass().add("login-field");
        passwordField.setPromptText("Entrez votre mot de passe");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordBox.getChildren().addAll(passLabel, passwordField);

        // Error label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // Login button
        loginBtn = new Button("Se Connecter");
        loginBtn.getStyleClass().add("login-btn");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Demo hint
        Label demoHint = new Label("💡  Démo : admin / admin123");
        demoHint.setStyle("-fx-text-fill: #2A4060; -fx-font-size: 11px;");

        card.getChildren().addAll(
            logoRow, subtitle, sep,
            usernameBox, passwordBox,
            errorLabel, loginBtn, demoHint
        );

        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(card);

        // Entrance animation
        card.setOpacity(0);
        card.setTranslateY(30);
        Timeline enter = new Timeline(
            new KeyFrame(Duration.millis(400),
                new KeyValue(card.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(card.translateYProperty(), 0, Interpolator.EASE_OUT))
        );
        enter.play();

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
        return scene;
    }

    private StackPane buildBackgroundDecor() {
        StackPane decor = new StackPane();
        decor.setPickOnBounds(false);

        // Glowing circles
        Circle c1 = new Circle(300);
        c1.setFill(Color.web("#00D4AA", 0.03));
        c1.setStroke(Color.web("#00D4AA", 0.06));
        c1.setStrokeWidth(1);
        c1.setTranslateX(-400);
        c1.setTranslateY(-200);

        Circle c2 = new Circle(200);
        c2.setFill(Color.web("#4499FF", 0.03));
        c2.setStroke(Color.web("#4499FF", 0.05));
        c2.setStrokeWidth(1);
        c2.setTranslateX(400);
        c2.setTranslateY(250);

        Circle c3 = new Circle(100);
        c3.setFill(Color.web("#AA66FF", 0.04));
        c3.setTranslateX(200);
        c3.setTranslateY(-300);

        // Slow pulse animation
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(4), c1);
        pulse.setFromX(1.0); pulse.setToX(1.1);
        pulse.setFromY(1.0); pulse.setToY(1.1);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        decor.getChildren().addAll(c1, c2, c3);
        return decor;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("⚠️  Veuillez remplir tous les champs");
            shakeField(username.isEmpty() ? usernameField : passwordField);
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Connexion...");

        // Initialize DB first (needed on first run)
        new Thread(() -> {
            try {
                BankService.getInstance(); // triggers DB init + seed
                AppUser user = UserDAO.authenticate(username, password);
                Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    loginBtn.setText("Se Connecter");
                    if (user != null) {
                        SessionManager.setCurrentUser(user);
                        openMainApp();
                    } else {
                        showError("❌  Identifiant ou mot de passe incorrect");
                        shakeField(passwordField);
                        passwordField.clear();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    loginBtn.setText("Se Connecter");
                    showError("❌  Erreur de connexion à la base de données.\nVérifiez que XAMPP MySQL est démarré.");
                });
            }
        }).start();
    }

    private void openMainApp() {
        MainController main = new MainController(stage);
        Scene mainScene = main.buildScene();

        // Transition
        stage.setScene(mainScene);
        stage.setTitle("🏦 NeoBank Elite — " + SessionManager.getCurrentUser().getFullName());
        stage.setMaximized(true);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), errorLabel);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    private void shakeField(javafx.scene.Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), node);
        shake.setFromX(0); shake.setByX(10);
        shake.setCycleCount(6); shake.setAutoReverse(true);
        shake.play();
    }
}
