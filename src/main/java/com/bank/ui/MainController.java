package com.bank.ui;

import com.bank.model.*;
import com.bank.service.BankService;
import com.bank.service.ExportService;
import com.bank.service.NotificationService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

public class MainController {

    private Stage stage;
    private BorderPane root;
    private BankService bankService;
    private StackPane contentArea;
    private Label statusLabel;
    private VBox sidebar;
    private Button activeSidebarBtn;

    public MainController(Stage stage) {
        this.stage = stage;
        bankService = BankService.getInstance();
    }

    public Scene buildScene() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #0B0F1A;");
        root.setTop(buildHeader());
        sidebar = buildSidebar();
        root.setLeft(sidebar);

        contentArea = new StackPane();
        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("main-scroll");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        root.setCenter(scrollPane);
        root.setBottom(buildStatusBar());

        showDashboard();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
        return scene;
    }

    // ===== HEADER =====
    private HBox buildHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 24, 0, 24));
        header.setSpacing(16);

        // Logo
        HBox logoBox = new HBox(12);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        Circle logoCircle = new Circle(18);
        logoCircle.setFill(Color.web("#00D4AA"));
        Text logoText = new Text("N");
        logoText.setFont(Font.font("System", FontWeight.BLACK, 18));
        logoText.setFill(Color.web("#0A0E1A"));
        StackPane logoIcon = new StackPane(logoCircle, logoText);
        Text bankName = new Text("NeoBank");
        bankName.getStyleClass().add("header-logo-text");
        Text elite = new Text(" ELITE");
        elite.getStyleClass().add("header-logo-accent");
        TextFlow logoFlow = new TextFlow(bankName, elite);
        logoBox.getChildren().addAll(logoIcon, logoFlow);

        // Global search bar
        TextField globalSearch = new TextField();
        globalSearch.setPromptText("🔍  Recherche globale — clients, comptes...");
        globalSearch.getStyleClass().add("search-field");
        globalSearch.setPrefWidth(300);
        globalSearch.textProperty().addListener((obs, old, val) -> {
            if (val.length() >= 2) showGlobalSearch(val);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Stats
        HBox statsBox = new HBox(28);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.getChildren().addAll(
            buildHeaderStat("ACTIFS TOTAUX", String.format("€%.0f", bankService.getTotalAssets())),
            buildHeaderStat("CLIENTS", String.valueOf(bankService.getAllClients().size())),
            buildHeaderStat("TRANSACTIONS", String.valueOf(bankService.getTotalTransactions()))
        );

        // User badge
        AppUser user = SessionManager.getCurrentUser();
        String roleIcon = (user != null && "ADMIN".equals(user.getRole())) ? "👑 " : "👤 ";
        String displayName = user != null ? user.getFullName() : "Utilisateur";
        Label userBadge = new Label(roleIcon + displayName);
        userBadge.getStyleClass().add("user-badge");

        // Logout button
        Button logoutBtn = new Button("⏻");
        logoutBtn.setStyle("-fx-background-color: #1A1020; -fx-text-fill: #FF6B6B; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #FF6B6B33; -fx-border-width: 1; -fx-font-size: 14px; -fx-padding: 6 10; -fx-cursor: hand;");
        logoutBtn.setTooltip(new Tooltip("Se déconnecter"));
        logoutBtn.setOnAction(e -> handleLogout());

        header.getChildren().addAll(logoBox, globalSearch, spacer, statsBox,
                new Separator(Orientation.VERTICAL), userBadge, logoutBtn);
        return header;
    }

    private void handleLogout() {
        SessionManager.logout();
        LoginController login = new LoginController(stage);
        stage.setScene(login.buildScene());
        stage.setTitle("🏦 NeoBank Elite — Connexion");
        stage.setMaximized(false);
        stage.setWidth(1200);
        stage.setHeight(750);
        stage.centerOnScreen();
    }

    private void showGlobalSearch(String query) {
        VBox page = new VBox(20);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(32));

        Label title = new Label("🔍  Résultats pour : \"" + query + "\"");
        title.getStyleClass().add("page-title");

        // Search clients
        List<Client> matchedClients = bankService.getAllClients().stream()
            .filter(c -> c.getFullName().toLowerCase().contains(query.toLowerCase())
                      || c.getEmail().toLowerCase().contains(query.toLowerCase()))
            .toList();

        // Search accounts
        List<BankAccount> matchedAccounts = bankService.getAllAccounts().stream()
            .filter(a -> a.getAccountNumber().toLowerCase().contains(query.toLowerCase())
                      || a.getOwner().getFullName().toLowerCase().contains(query.toLowerCase())
                      || a.getAccountType().toLowerCase().contains(query.toLowerCase()))
            .toList();

        VBox results = new VBox(24);

        if (!matchedClients.isEmpty()) {
            Label cLabel = new Label("👥  Clients (" + matchedClients.size() + ")");
            cLabel.getStyleClass().add("section-title");
            TableView<Client> ct = buildClientTable(matchedClients);
            ct.setPrefHeight(Math.min(matchedClients.size() * 46 + 48, 280));
            ct.setMinHeight(180);
            results.getChildren().addAll(cLabel, ct);
        }

        if (!matchedAccounts.isEmpty()) {
            Label aLabel = new Label("💳  Comptes (" + matchedAccounts.size() + ")");
            aLabel.getStyleClass().add("section-title");
            TableView<BankAccount> at = buildAccountTable(matchedAccounts);
            at.setPrefHeight(Math.min(matchedAccounts.size() * 46 + 48, 280));
            at.setMinHeight(180);
            results.getChildren().addAll(aLabel, at);
        }

        if (matchedClients.isEmpty() && matchedAccounts.isEmpty()) {
            Label noResult = new Label("Aucun résultat pour \"" + query + "\"");
            noResult.setStyle("-fx-text-fill: #3A5570; -fx-font-size: 16px;");
            results.getChildren().add(noResult);
        }

        page.getChildren().addAll(title, results);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: #0B0F1A; -fx-background: #0B0F1A;");
        animateIn(page);
        contentArea.getChildren().setAll(scroll);
    }

    private VBox buildHeaderStat(String label, String value) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("header-stat-label");
        Label val = new Label(value);
        val.getStyleClass().add("header-stat-value");
        box.getChildren().addAll(val, lbl);
        return box;
    }

    // ===== SIDEBAR =====
    private VBox buildSidebar() {
        VBox sb = new VBox(4);
        sb.getStyleClass().add("sidebar");
        sb.setPadding(new Insets(24, 0, 24, 0));

        Label navLabel = new Label("NAVIGATION");
        navLabel.getStyleClass().add("sidebar-section-label");
        navLabel.setPadding(new Insets(0, 16, 8, 16));

        Button dashBtn         = createSidebarButton("📊", "Tableau de Bord");
        Button clientsBtn      = createSidebarButton("👥", "Clients");
        Button accountsBtn     = createSidebarButton("💳", "Comptes");
        Button transactionsBtn = createSidebarButton("↕", "Opérations");
        Button analyticsBtn    = createSidebarButton("📈", "Analytique");

        dashBtn.setOnAction(e -> { setActiveSidebarBtn(dashBtn); showDashboard(); });
        clientsBtn.setOnAction(e -> { setActiveSidebarBtn(clientsBtn); showClients(); });
        accountsBtn.setOnAction(e -> { setActiveSidebarBtn(accountsBtn); showAccounts(); });
        transactionsBtn.setOnAction(e -> { setActiveSidebarBtn(transactionsBtn); showOperations(); });
        analyticsBtn.setOnAction(e -> { setActiveSidebarBtn(analyticsBtn); showAnalytics(); });

        Label toolsLabel = new Label("OUTILS");
        toolsLabel.getStyleClass().add("sidebar-section-label");
        toolsLabel.setPadding(new Insets(16, 16, 8, 16));

        Button interestBtn  = createSidebarButton("💹", "Calc. Intérêts");
        Button newClientBtn = createSidebarButton("➕", "Nouveau Client");
        Button reportBtn    = createSidebarButton("📄", "Rapport Mensuel");
        Button exportBtn    = createSidebarButton("📤", "Export Excel/PDF");

        interestBtn.setOnAction(e -> { setActiveSidebarBtn(interestBtn); showInterestCalculator(); });
        newClientBtn.setOnAction(e -> { setActiveSidebarBtn(newClientBtn); showNewClientForm(); });
        reportBtn.setOnAction(e -> generateMonthlyReport());
        exportBtn.setOnAction(e -> { setActiveSidebarBtn(exportBtn); showExportPage(); });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label versionLabel = new Label("v3.0 — NeoBank Elite");
        versionLabel.setStyle("-fx-text-fill: #1A2D44; -fx-font-size: 10px;");
        versionLabel.setPadding(new Insets(0, 16, 0, 16));

        sb.getChildren().addAll(navLabel, dashBtn, clientsBtn, accountsBtn, transactionsBtn, analyticsBtn,
                toolsLabel, interestBtn, newClientBtn, reportBtn, exportBtn, spacer, versionLabel);

        setActiveSidebarBtn(dashBtn);
        return sb;
    }

    private Button createSidebarButton(String icon, String text) {
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("sidebar-icon");
        Label textLbl = new Label(text);
        textLbl.getStyleClass().add("sidebar-text");
        content.getChildren().addAll(iconLbl, textLbl);

        Button btn = new Button();
        btn.setGraphic(content);
        btn.getStyleClass().add("sidebar-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(10, 16, 10, 16));
        return btn;
    }

    private void setActiveSidebarBtn(Button btn) {
        if (activeSidebarBtn != null) activeSidebarBtn.getStyleClass().remove("sidebar-btn-active");
        activeSidebarBtn = btn;
        btn.getStyleClass().add("sidebar-btn-active");
    }

    // ===== STATUS BAR =====
    private HBox buildStatusBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("status-bar");
        bar.setPadding(new Insets(6, 16, 6, 16));
        bar.setAlignment(Pos.CENTER_LEFT);
        statusLabel = new Label("✅ Système opérationnel — Bienvenue sur NeoBank Elite");
        statusLabel.getStyleClass().add("status-label");
        bar.getChildren().add(statusLabel);
        return bar;
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(5), e ->
                statusLabel.setText("✅ Système opérationnel")));
        tl.play();
    }

    private void showStatusSuccess(String title, String message) {
        showStatus("✅ " + message);
        NotificationService.success(title, message);
    }

    private void showStatusError(String title, String message) {
        showStatus("❌ " + message);
        NotificationService.error(title, message);
    }

    // ===== EXPORT PAGE =====
    private void showExportPage() {
        VBox page = new VBox(24);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(32));

        Label title = new Label("📤  Export des Données");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Exportez vos transactions en PDF ou Excel");
        subtitle.getStyleClass().add("page-subtitle");

        HBox cardsRow = new HBox(20);

        // PDF Card
        VBox pdfCard = buildExportCard(
            "📄", "Export PDF",
            "Génère un relevé de transactions formaté en PDF\nprofessionnel avec en-tête NeoBank Elite.",
            "#FF6B6B", "Exporter en PDF",
            () -> exportAllTransactions("PDF")
        );

        // Excel Card
        VBox xlsCard = buildExportCard(
            "📊", "Export Excel",
            "Génère un fichier Excel (.xlsx) avec toutes\nles transactions, calculs automatiques inclus.",
            "#4499FF", "Exporter en Excel",
            () -> exportAllTransactions("EXCEL")
        );

        HBox.setHgrow(pdfCard, Priority.ALWAYS);
        HBox.setHgrow(xlsCard, Priority.ALWAYS);
        cardsRow.getChildren().addAll(pdfCard, xlsCard);

        // Account selector for targeted export
        VBox selectorCard = new VBox(16);
        selectorCard.getStyleClass().add("form-card");
        selectorCard.setPadding(new Insets(24));

        Label selTitle = new Label("Export par Compte Spécifique");
        selTitle.getStyleClass().add("section-title");

        ComboBox<BankAccount> accountCombo = new ComboBox<>();
        accountCombo.getStyleClass().add("combo-field");
        accountCombo.setMaxWidth(Double.MAX_VALUE);
        accountCombo.setPromptText("Sélectionner un compte...");
        accountCombo.setItems(FXCollections.observableArrayList(bankService.getAllAccounts()));
        accountCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(BankAccount a) {
                return a == null ? "" : a.getOwner().getFullName() + " — " + a.getAccountType() + " (" + a.getAccountNumber().substring(0, 10) + "...)";
            }
            @Override public BankAccount fromString(String s) { return null; }
        });

        HBox exportBtns = new HBox(12);
        Button pdfBtn2 = new Button("📄 Exporter PDF");
        pdfBtn2.getStyleClass().add("btn-export");
        Button xlsBtn2 = new Button("📊 Exporter Excel");
        xlsBtn2.getStyleClass().add("btn-export");

        pdfBtn2.setOnAction(e -> {
            BankAccount selected = accountCombo.getValue();
            if (selected == null) { showStatusError("Export", "Sélectionnez un compte d'abord"); return; }
            exportAccountTransactions(selected, "PDF");
        });
        xlsBtn2.setOnAction(e -> {
            BankAccount selected = accountCombo.getValue();
            if (selected == null) { showStatusError("Export", "Sélectionnez un compte d'abord"); return; }
            exportAccountTransactions(selected, "EXCEL");
        });

        exportBtns.getChildren().addAll(pdfBtn2, xlsBtn2);
        selectorCard.getChildren().addAll(selTitle, accountCombo, exportBtns);

        page.getChildren().addAll(new VBox(4, title, subtitle), cardsRow, selectorCard);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: #0B0F1A; -fx-background: #0B0F1A;");
        animateIn(page);
        contentArea.getChildren().setAll(scroll);
    }

    private VBox buildExportCard(String icon, String title, String desc,
                                  String color, String btnLabel, Runnable action) {
        VBox card = new VBox(16);
        card.getStyleClass().add("form-card");
        card.setPadding(new Insets(24));

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 36px;");

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #E0EEFF; -fx-font-size: 18px; -fx-font-weight: 700;");

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-text-fill: #4A6A88; -fx-font-size: 13px;");
        descLbl.setWrapText(true);

        Button btn = new Button(btnLabel);
        btn.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color +
                "; -fx-font-weight: 700; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: " +
                color + "44; -fx-border-width: 1; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-size: 13px;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> action.run());

        card.getChildren().addAll(ico, lbl, descLbl, btn);
        return card;
    }

    private void exportAllTransactions(String format) {
        List<Transaction> txs = bankService.getAllAccounts().stream()
            .flatMap(a -> a.getTransactions().stream())
            .toList();
        if (txs.isEmpty()) { showStatusError("Export", "Aucune transaction à exporter"); return; }
        doExport(txs, "Tous les comptes", format);
    }

    private void exportAccountTransactions(BankAccount account, String format) {
        List<Transaction> txs = account.getTransactions();
        if (txs.isEmpty()) { showStatusError("Export", "Aucune transaction sur ce compte"); return; }
        doExport(txs, account.getOwner().getFullName() + " — " + account.getAccountType(), format);
    }

    private void doExport(List<Transaction> txs, String info, String format) {
        new Thread(() -> {
            try {
                File file = format.equals("PDF")
                    ? ExportService.exportTransactionsPDF(txs, info)
                    : ExportService.exportTransactionsExcel(txs, info);
                javafx.application.Platform.runLater(() ->
                    showStatusSuccess("Export réussi ✅",
                        "Fichier sauvegardé sur le Bureau : " + file.getName()));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                    showStatusError("Erreur Export", ex.getMessage()));
            }
        }).start();
    }

    private void generateMonthlyReport() {
        new Thread(() -> {
            try {
                List<Client> clients = bankService.getAllClients();
                List<BankAccount> accounts = bankService.getAllAccounts();
                List<Transaction> txs = accounts.stream()
                    .flatMap(a -> a.getTransactions().stream()).toList();
                File file = ExportService.exportMonthlyReport(
                    clients, accounts, txs, bankService.getTotalAssets());
                javafx.application.Platform.runLater(() ->
                    showStatusSuccess("Rapport généré ✅",
                        "Rapport mensuel sauvegardé : " + file.getName()));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                    showStatusError("Erreur Rapport", ex.getMessage()));
            }
        }).start();
        NotificationService.info("Rapport en cours...", "Génération du rapport mensuel PDF");
    }

    // ===== DASHBOARD =====
    private void showDashboard() {
        VBox dashboard = new VBox(24);
        dashboard.getStyleClass().add("page");
        dashboard.setPadding(new Insets(32));

        Label title = new Label("Tableau de Bord");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vue d'ensemble de la banque");
        subtitle.getStyleClass().add("page-subtitle");

        // KPI Cards
        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
            buildKpiCard("💰", "Actifs Totaux", String.format("€%.2f", bankService.getTotalAssets()), "+2.3%", "kpi-green"),
            buildKpiCard("👥", "Clients Actifs", String.valueOf(bankService.getAllClients().size()), "Tous actifs", "kpi-blue"),
            buildKpiCard("💳", "Comptes Ouverts", String.valueOf(bankService.getAllAccounts().size()), "3 types", "kpi-purple"),
            buildKpiCard("🔄", "Transactions", String.valueOf(bankService.getTotalTransactions()), "Ce mois", "kpi-orange")
        );

        // Client Summary Table
        Label clientTitle = new Label("Aperçu Clients");
        clientTitle.getStyleClass().add("section-title");

        TableView<Client> table = buildClientTable(bankService.getAllClients());
        table.setPrefHeight(220);
        table.setMinHeight(220);
        table.setMaxHeight(220);

        // Account distribution chart
        HBox chartsRow = new HBox(20);
        chartsRow.setPrefHeight(340);
        chartsRow.setMinHeight(320);
        Node pieChart = buildAccountTypeChart();
        Node barChart = buildBalanceBarChart();
        HBox.setHgrow(pieChart, Priority.ALWAYS);
        HBox.setHgrow(barChart, Priority.ALWAYS);
        chartsRow.getChildren().addAll(pieChart, barChart);

        dashboard.getChildren().addAll(
                new VBox(4, title, subtitle),
                kpiRow,
                clientTitle, table,
                chartsRow
        );

        animateIn(dashboard);
        contentArea.getChildren().setAll(dashboard);
    }

    private VBox buildKpiCard(String icon, String label, String value, String change, String styleClass) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("kpi-card", styleClass);
        card.setPadding(new Insets(20));
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 24px;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label changeLbl = new Label(change);
        changeLbl.getStyleClass().add("kpi-change");
        top.getChildren().addAll(iconLbl, sp, changeLbl);

        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("kpi-value");
        Label labelLbl = new Label(label);
        labelLbl.getStyleClass().add("kpi-label");

        card.getChildren().addAll(top, valueLbl, labelLbl);
        return card;
    }

    private PieChart buildAccountTypeChart() {
        long checking = bankService.getAllAccounts().stream()
                .filter(a -> a instanceof CheckingAccount).count();
        long savings = bankService.getAllAccounts().stream()
                .filter(a -> a instanceof SavingsAccount).count();
        long invest = bankService.getAllAccounts().stream()
                .filter(a -> a instanceof InvestmentAccount).count();

        PieChart chart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("Courant (" + checking + ")", checking),
                new PieChart.Data("Épargne (" + savings + ")", savings),
                new PieChart.Data("Investissement (" + invest + ")", invest)
        ));
        chart.setTitle("Répartition des Comptes");
        chart.getStyleClass().add("chart-card");
        chart.setPrefWidth(420);
        chart.setPrefHeight(320);
        chart.setMinHeight(300);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setStartAngle(90);
        return chart;
    }

    private BarChart<String, Number> buildBalanceBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Client");
        yAxis.setLabel("Solde (€)");
        xAxis.setTickLabelFont(javafx.scene.text.Font.font(13));
        yAxis.setTickLabelFont(javafx.scene.text.Font.font(12));

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Solde Total par Client");
        chart.getStyleClass().add("chart-card");
        chart.setPrefHeight(320);
        chart.setMinHeight(300);
        chart.setBarGap(6);
        chart.setCategoryGap(20);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Solde Total (€)");
        bankService.getAllClients().forEach(c ->
                series.getData().add(new XYChart.Data<>(c.getFirstName(), c.getTotalBalance())));
        chart.getData().add(series);
        chart.setAnimated(true);
        HBox.setHgrow(chart, Priority.ALWAYS);
        return chart;
    }

    // ===== CLIENTS VIEW =====
    private void showClients() {
        VBox page = new VBox(24);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(32));

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Gestion des Clients");
        title.getStyleClass().add("page-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button addBtn = new Button("+ Nouveau Client");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showNewClientForm());
        header.getChildren().addAll(title, sp, addBtn);

        // Search
        TextField search = new TextField();
        search.setPromptText("🔍  Rechercher un client...");
        search.getStyleClass().add("search-field");
        search.setMaxWidth(400);

        TableView<Client> table = buildClientTable(bankService.getAllClients());
        table.setPrefHeight(250);
        table.setMinHeight(200);

        search.textProperty().addListener((obs, old, newVal) -> {
            List<Client> filtered = bankService.getAllClients().stream()
                    .filter(c -> c.getFullName().toLowerCase().contains(newVal.toLowerCase()) ||
                            c.getEmail().toLowerCase().contains(newVal.toLowerCase()))
                    .toList();
            table.setItems(FXCollections.observableArrayList(filtered));
        });

        // Client detail panel
        Label detailTitle = new Label("Sélectionnez un client pour voir ses détails");
        detailTitle.getStyleClass().add("section-title");
        VBox detailPanel = new VBox(12);
        detailPanel.getStyleClass().add("detail-panel");
        detailPanel.setPadding(new Insets(16));
        detailPanel.getChildren().add(detailTitle);

        ScrollPane detailScroll = new ScrollPane(detailPanel);
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        detailScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        detailScroll.setPrefWidth(320);
        detailScroll.setMinWidth(280);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, client) -> {
            if (client != null) {
                detailPanel.getChildren().setAll(buildClientDetail(client));
            }
        });

        HBox mainContent = new HBox(16);
        VBox leftSide = new VBox(12, search, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(leftSide, Priority.ALWAYS);
        mainContent.getChildren().addAll(leftSide, detailScroll);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        page.getChildren().addAll(header, mainContent);
        animateIn(page);
        contentArea.getChildren().setAll(page);
    }

    private TableView<Client> buildClientTable(List<Client> clients) {
        TableView<Client> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setItems(FXCollections.observableArrayList(clients));

        TableColumn<Client, String> nameCol = new TableColumn<>("Nom");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFullName()));
        nameCol.setPrefWidth(160);

        TableColumn<Client, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEmail()));
        emailCol.setPrefWidth(200);

        TableColumn<Client, String> phoneCol = new TableColumn<>("Téléphone");
        phoneCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPhone()));
        phoneCol.setPrefWidth(150);

        TableColumn<Client, String> tierCol = new TableColumn<>("Tier");
        tierCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTier().name()));
        tierCol.setPrefWidth(100);
        tierCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String tier, boolean empty) {
                super.updateItem(tier, empty);
                if (empty || tier == null) { setText(null); setStyle(""); return; }
                setText(tier);
                setStyle("-fx-text-fill: " + switch(tier) {
                    case "PLATINUM" -> "#00D4AA";
                    case "GOLD" -> "#FFD700";
                    case "SILVER" -> "#C0C0C0";
                    default -> "#8899AA";
                } + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<Client, String> balanceCol = new TableColumn<>("Solde Total");
        balanceCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("€%.2f", c.getValue().getTotalBalance())));
        balanceCol.setPrefWidth(130);

        TableColumn<Client, String> accountsCol = new TableColumn<>("Comptes");
        accountsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(c.getValue().getAccounts().size())));
        accountsCol.setPrefWidth(80);

        table.getColumns().addAll(nameCol, emailCol, phoneCol, tierCol, balanceCol, accountsCol);
        return table;
    }

    private VBox buildClientDetail(Client client) {
        VBox box = new VBox(12);

        // Avatar + Name
        Circle avatar = new Circle(32);
        avatar.setFill(Color.web("#00D4AA33"));
        avatar.setStroke(Color.web("#00D4AA"));
        avatar.setStrokeWidth(2);
        Text initials = new Text(
                client.getFirstName().charAt(0) + "" + client.getLastName().charAt(0));
        initials.setFill(Color.web("#00D4AA"));
        initials.setFont(Font.font("System", FontWeight.BOLD, 20));
        StackPane avatarPane = new StackPane(avatar, initials);

        Label name = new Label(client.getFullName());
        name.getStyleClass().add("detail-name");
        Label tier = new Label("● " + client.getTier().name());
        tier.getStyleClass().add("detail-tier");

        HBox avatarRow = new HBox(12, avatarPane, new VBox(4, name, tier));
        avatarRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();

        // Info grid
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        addDetailRow(grid, 0, "📧 Email", client.getEmail());
        addDetailRow(grid, 1, "📱 Téléphone", client.getPhone());
        addDetailRow(grid, 2, "🎂 Date de naissance", client.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        addDetailRow(grid, 3, "📍 Adresse", client.getAddress());
        addDetailRow(grid, 4, "📅 Membre depuis", client.getMemberSince().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        addDetailRow(grid, 5, "💰 Solde total", String.format("€%.2f", client.getTotalBalance()));

        // Accounts list
        Label accTitle = new Label("Comptes (" + client.getAccounts().size() + ")");
        accTitle.getStyleClass().add("section-title-small");

        VBox accountsList = new VBox(6);
        client.getAccounts().forEach(acc -> {
            HBox row = new HBox(8);
            row.getStyleClass().add("account-mini-card");
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setAlignment(Pos.CENTER_LEFT);
            Label type = new Label(acc.getAccountType());
            type.getStyleClass().add("acc-type-label");
            Region s = new Region();
            HBox.setHgrow(s, Priority.ALWAYS);
            Label bal = new Label(String.format("€%.2f", acc.getBalance()));
            bal.getStyleClass().add("acc-balance-label");
            row.getChildren().addAll(type, s, bal);
            accountsList.getChildren().add(row);
        });

        box.getChildren().addAll(avatarRow, sep, grid, accTitle, accountsList);
        return box;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("detail-label");
        Label val = new Label(value);
        val.getStyleClass().add("detail-value");
        val.setWrapText(true);
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    // ===== ACCOUNTS VIEW =====
    private void showAccounts() {
        VBox page = new VBox(24);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(32));

        Label title = new Label("Gestion des Comptes");
        title.getStyleClass().add("page-title");

        // Filter tabs
        HBox tabs = new HBox(8);
        String[] filters = {"Tous", "Courant", "Épargne", "Investissement"};
        ToggleGroup tg = new ToggleGroup();
        List<BankAccount>[] filtered = new List[]{bankService.getAllAccounts()};

        TableView<BankAccount> table = buildAccountTable(filtered[0]);
        table.setPrefHeight(260);
        table.setMinHeight(210);
        VBox.setVgrow(table, Priority.ALWAYS);

        for (String f : filters) {
            ToggleButton tb = new ToggleButton(f);
            tb.setToggleGroup(tg);
            tb.getStyleClass().add("filter-tab");
            tb.setOnAction(e -> {
                List<BankAccount> result = switch(f) {
                    case "Courant" -> bankService.getAllAccounts().stream()
                            .filter(a -> a instanceof CheckingAccount).toList();
                    case "Épargne" -> bankService.getAllAccounts().stream()
                            .filter(a -> a instanceof SavingsAccount).toList();
                    case "Investissement" -> bankService.getAllAccounts().stream()
                            .filter(a -> a instanceof InvestmentAccount).toList();
                    default -> bankService.getAllAccounts();
                };
                table.setItems(FXCollections.observableArrayList(result));
            });
            tabs.getChildren().add(tb);
        }
        ((ToggleButton)tabs.getChildren().get(0)).setSelected(true);

        // Account action panel
        HBox actionPanel = new HBox(12);
        actionPanel.getStyleClass().add("action-panel");
        actionPanel.setPadding(new Insets(16));

        Label actionTitle = new Label("Actions sur le compte sélectionné:");
        actionTitle.getStyleClass().add("section-title-small");

        TextField amountField = new TextField();
        amountField.setPromptText("Montant (€)");
        amountField.getStyleClass().add("input-field");
        amountField.setPrefWidth(140);

        Button depositBtn = new Button("⬆ Dépôt");
        depositBtn.getStyleClass().addAll("btn-success");

        Button withdrawBtn = new Button("⬇ Retrait");
        withdrawBtn.getStyleClass().addAll("btn-danger");

        Button interestBtn = new Button("💹 Intérêts");
        interestBtn.getStyleClass().addAll("btn-info");

        Button freezeBtn = new Button("🔒 Geler");
        freezeBtn.getStyleClass().addAll("btn-warning");

        depositBtn.setOnAction(e -> {
            BankAccount acc = table.getSelectionModel().getSelectedItem();
            if (acc == null) { showStatus("⚠️ Sélectionnez un compte"); return; }
            try {
                double amount = Double.parseDouble(amountField.getText().replace(",", "."));
                acc.deposit(amount);
                table.refresh();
                showStatus("✅ Dépôt de €" + String.format("%.2f", amount) + " effectué");
                amountField.clear();
            } catch (Exception ex) { showStatus("❌ Montant invalide"); }
        });

        withdrawBtn.setOnAction(e -> {
            BankAccount acc = table.getSelectionModel().getSelectedItem();
            if (acc == null) { showStatus("⚠️ Sélectionnez un compte"); return; }
            try {
                double amount = Double.parseDouble(amountField.getText().replace(",", "."));
                boolean ok = acc.withdraw(amount);
                table.refresh();
                showStatus(ok ? "✅ Retrait de €" + String.format("%.2f", amount) + " effectué"
                             : "❌ Retrait refusé - fonds insuffisants ou limite atteinte");
                amountField.clear();
            } catch (Exception ex) { showStatus("❌ Montant invalide"); }
        });

        interestBtn.setOnAction(e -> {
            BankAccount acc = table.getSelectionModel().getSelectedItem();
            if (acc == null) { showStatus("⚠️ Sélectionnez un compte"); return; }
            acc.calculateInterest();
            table.refresh();
            showStatus("✅ Intérêts appliqués au compte " + acc.getAccountNumber());
        });

        freezeBtn.setOnAction(e -> {
            BankAccount acc = table.getSelectionModel().getSelectedItem();
            if (acc == null) { showStatus("⚠️ Sélectionnez un compte"); return; }
            if (acc.getStatus() == BankAccount.AccountStatus.ACTIVE) {
                acc.setStatus(BankAccount.AccountStatus.FROZEN);
                freezeBtn.setText("🔓 Dégeler");
                showStatus("🔒 Compte gelé: " + acc.getAccountNumber());
            } else {
                acc.setStatus(BankAccount.AccountStatus.ACTIVE);
                freezeBtn.setText("🔒 Geler");
                showStatus("🔓 Compte dégelé: " + acc.getAccountNumber());
            }
            table.refresh();
        });

        actionPanel.getChildren().addAll(actionTitle, amountField, depositBtn, withdrawBtn, interestBtn, freezeBtn);

        page.getChildren().addAll(title, tabs, table, actionPanel);
        animateIn(page);
        contentArea.getChildren().setAll(page);
    }

    private TableView<BankAccount> buildAccountTable(List<BankAccount> accounts) {
        TableView<BankAccount> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setItems(FXCollections.observableArrayList(accounts));

        TableColumn<BankAccount, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAccountType()));
        typeCol.setPrefWidth(160);

        TableColumn<BankAccount, String> numberCol = new TableColumn<>("Numéro de Compte");
        numberCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAccountNumber()));
        numberCol.setPrefWidth(200);

        TableColumn<BankAccount, String> ownerCol = new TableColumn<>("Titulaire");
        ownerCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getOwner().getFullName()));
        ownerCol.setPrefWidth(150);

        TableColumn<BankAccount, String> balanceCol = new TableColumn<>("Solde");
        balanceCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("€%.2f", c.getValue().getBalance())));
        balanceCol.setPrefWidth(130);
        balanceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                double amount = Double.parseDouble(val.replace("€", "").replace(",", "."));
                setStyle("-fx-text-fill: " + (amount >= 0 ? "#00D4AA" : "#FF6B6B") + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<BankAccount, String> rateCol = new TableColumn<>("Taux");
        rateCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.1f%%", c.getValue().getInterestRate() * 100)));
        rateCol.setPrefWidth(80);

        TableColumn<BankAccount, String> statusCol = new TableColumn<>("Statut");
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus().name()));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-text-fill: " + switch(s) {
                    case "ACTIVE" -> "#00D4AA";
                    case "FROZEN" -> "#FFB347";
                    default -> "#FF6B6B";
                } + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<BankAccount, String> txCol = new TableColumn<>("Transactions");
        txCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(c.getValue().getTransactions().size())));
        txCol.setPrefWidth(100);

        table.getColumns().addAll(typeCol, numberCol, ownerCol, balanceCol, rateCol, statusCol, txCol);
        return table;
    }

    // ===== OPERATIONS VIEW =====
    private void showOperations() {
        VBox page = new VBox(24);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(32));

        Label title = new Label("Opérations Bancaires");
        title.getStyleClass().add("page-title");

        // Transaction history
        Label histTitle = new Label("Historique des Transactions");
        histTitle.getStyleClass().add("section-title");

        // Client selector
        ComboBox<Client> clientCombo = new ComboBox<>();
        clientCombo.setItems(FXCollections.observableArrayList(bankService.getAllClients()));
        clientCombo.setPromptText("Sélectionner un client");
        clientCombo.getStyleClass().add("combo-field");
        clientCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Client c) { return c == null ? "" : c.getFullName(); }
            @Override public Client fromString(String s) { return null; }
        });

        ComboBox<BankAccount> accountCombo = new ComboBox<>();
        accountCombo.setPromptText("Sélectionner un compte");
        accountCombo.getStyleClass().add("combo-field");
        accountCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(BankAccount a) { return a == null ? "" : a.getAccountType() + " - " + a.getAccountNumber().substring(0, 8) + "..."; }
            @Override public BankAccount fromString(String s) { return null; }
        });

        TableView<Transaction> txTable = new TableView<>();
        txTable.getStyleClass().add("data-table");
        txTable.setPrefHeight(260);
        txTable.setMinHeight(210);
        VBox.setVgrow(txTable, Priority.ALWAYS);

        TableColumn<Transaction, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTransactionId()));
        idCol.setPrefWidth(90);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getType().name()));
        typeCol.setPrefWidth(120);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) { setText(null); return; }
                setText(t);
                String icon = switch(t) {
                    case "DEPOSIT" -> "⬆ ";
                    case "WITHDRAWAL" -> "⬇ ";
                    case "TRANSFER" -> "↔ ";
                    case "INTEREST" -> "💹 ";
                    default -> "• ";
                };
                setText(icon + t);
                setStyle("-fx-text-fill: " + (t.equals("DEPOSIT") || t.equals("INTEREST") ? "#00D4AA" : "#FF6B6B") + ";");
            }
        });

        TableColumn<Transaction, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDescription()));
        descCol.setPrefWidth(200);

        TableColumn<Transaction, String> amtCol = new TableColumn<>("Montant");
        amtCol.setCellValueFactory(c -> {
            Transaction tx = c.getValue();
            String prefix = tx.isCredit() ? "+" : "-";
            return new javafx.beans.property.SimpleStringProperty(prefix + String.format("€%.2f", tx.getAmount()));
        });
        amtCol.setPrefWidth(120);

        TableColumn<Transaction, String> balCol = new TableColumn<>("Solde Après");
        balCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("€%.2f", c.getValue().getBalanceAfter())));
        balCol.setPrefWidth(120);

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date/Heure");
        dateCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        dateCol.setPrefWidth(150);

        txTable.getColumns().addAll(idCol, typeCol, descCol, amtCol, balCol, dateCol);

        clientCombo.setOnAction(e -> {
            Client c = clientCombo.getValue();
            if (c != null) {
                accountCombo.setItems(FXCollections.observableArrayList(
                        bankService.getAccountsByClient(c.getClientId())));
            }
        });

        accountCombo.setOnAction(e -> {
            BankAccount acc = accountCombo.getValue();
            if (acc != null) {
                txTable.setItems(FXCollections.observableArrayList(acc.getTransactions()));
            }
        });

        // Transfer section
        Label transferTitle = new Label("Virement entre Comptes");
        transferTitle.getStyleClass().add("section-title");

        HBox transferRow = new HBox(12);
        transferRow.setAlignment(Pos.CENTER_LEFT);
        transferRow.getStyleClass().add("action-panel");
        transferRow.setPadding(new Insets(16));

        ComboBox<BankAccount> fromCombo = new ComboBox<>();
        fromCombo.setItems(FXCollections.observableArrayList(bankService.getAllAccounts()));
        fromCombo.setPromptText("Compte source");
        fromCombo.getStyleClass().add("combo-field");
        fromCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(BankAccount a) { return a == null ? "" : a.getOwner().getFirstName() + " - " + a.getAccountType(); }
            @Override public BankAccount fromString(String s) { return null; }
        });

        Label arrow = new Label("→");
        arrow.setStyle("-fx-font-size: 20px; -fx-text-fill: #00D4AA;");

        ComboBox<BankAccount> toCombo = new ComboBox<>();
        toCombo.setItems(FXCollections.observableArrayList(bankService.getAllAccounts()));
        toCombo.setPromptText("Compte destination");
        toCombo.getStyleClass().add("combo-field");
        toCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(BankAccount a) { return a == null ? "" : a.getOwner().getFirstName() + " - " + a.getAccountType(); }
            @Override public BankAccount fromString(String s) { return null; }
        });

        TextField transferAmt = new TextField();
        transferAmt.setPromptText("Montant (€)");
        transferAmt.getStyleClass().add("input-field");
        transferAmt.setPrefWidth(140);

        Button transferBtn = new Button("Effectuer le Virement");
        transferBtn.getStyleClass().add("btn-primary");
        transferBtn.setOnAction(e -> {
            if (fromCombo.getValue() == null || toCombo.getValue() == null) {
                showStatus("⚠️ Sélectionnez les deux comptes"); return;
            }
            try {
                double amount = Double.parseDouble(transferAmt.getText().replace(",", "."));
                boolean ok = bankService.transfer(
                        fromCombo.getValue().getAccountId(),
                        toCombo.getValue().getAccountId(), amount);
                showStatus(ok ? "✅ Virement de €" + String.format("%.2f", amount) + " effectué"
                             : "❌ Virement échoué - fonds insuffisants");
                transferAmt.clear();
                if (accountCombo.getValue() != null) {
                    txTable.setItems(FXCollections.observableArrayList(accountCombo.getValue().getTransactions()));
                }
            } catch (Exception ex) { showStatus("❌ Montant invalide"); }
        });

        transferRow.getChildren().addAll(fromCombo, arrow, toCombo, transferAmt, transferBtn);

        HBox selectors = new HBox(12, clientCombo, accountCombo);

        // Export buttons row
        HBox exportRow = new HBox(10);
        exportRow.setAlignment(Pos.CENTER_RIGHT);
        Button exportPdfBtn = new Button("📄 Exporter PDF");
        exportPdfBtn.getStyleClass().add("btn-export");
        Button exportXlsBtn = new Button("📊 Exporter Excel");
        exportXlsBtn.getStyleClass().add("btn-export");
        exportPdfBtn.setOnAction(e -> {
            List<Transaction> txs = txTable.getItems();
            if (txs == null || txs.isEmpty()) { showStatusError("Export", "Aucune transaction à exporter"); return; }
            BankAccount sel = accountCombo.getValue();
            String info = sel != null ? sel.getOwner().getFullName() + " — " + sel.getAccountType() : "Toutes transactions";
            doExport(new java.util.ArrayList<>(txs), info, "PDF");
        });
        exportXlsBtn.setOnAction(e -> {
            List<Transaction> txs = txTable.getItems();
            if (txs == null || txs.isEmpty()) { showStatusError("Export", "Aucune transaction à exporter"); return; }
            BankAccount sel = accountCombo.getValue();
            String info = sel != null ? sel.getOwner().getFullName() + " — " + sel.getAccountType() : "Toutes transactions";
            doExport(new java.util.ArrayList<>(txs), info, "EXCEL");
        });
        exportRow.getChildren().addAll(new Label("Exporter :") {{
            setStyle("-fx-text-fill: #3A5570; -fx-font-size: 12px;");
        }}, exportPdfBtn, exportXlsBtn);

        page.getChildren().addAll(title, histTitle, selectors, txTable, exportRow, transferTitle, transferRow);
        animateIn(page);
        contentArea.getChildren().setAll(page);
    }

    // ===== ANALYTICS VIEW =====
    private void showAnalytics() {
        VBox page = new VBox(24);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(32));

        Label title = new Label("Analytique Bancaire");
        title.getStyleClass().add("page-title");

        // Summary cards
        HBox cards = new HBox(16);
        double totalAssets = bankService.getTotalAssets();
        double avgBalance = totalAssets / bankService.getAllAccounts().size();
        Client richest = bankService.getAllClients().stream()
                .max((a, b) -> Double.compare(a.getTotalBalance(), b.getTotalBalance()))
                .orElse(null);

        cards.getChildren().addAll(
            buildKpiCard("💰", "Actifs Totaux", String.format("€%.2f", totalAssets), "Tous comptes", "kpi-green"),
            buildKpiCard("📊", "Solde Moyen", String.format("€%.2f", avgBalance), "Par compte", "kpi-blue"),
            buildKpiCard("🏆", "Client Premium", richest != null ? richest.getFirstName() : "-",
                    richest != null ? String.format("€%.0f", richest.getTotalBalance()) : "", "kpi-purple")
        );

        // Line chart - simulated balance evolution
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Évolution Simulée des Actifs (6 mois)");
        lineChart.getStyleClass().add("chart-card");

        String[] months = {"Sept", "Oct", "Nov", "Déc", "Jan", "Fév"};
        bankService.getAllClients().forEach(client -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(client.getFirstName());
            double base = client.getTotalBalance() * 0.8;
            for (String month : months) {
                base *= (1 + (Math.random() * 0.04 - 0.01));
                series.getData().add(new XYChart.Data<>(month, base));
            }
            lineChart.getData().add(series);
        });
        lineChart.setPrefHeight(300);

        // Client tier distribution
        HBox bottomRow = new HBox(16);

        // Tier pie
        long standard = bankService.getAllClients().stream().filter(c -> c.getTier() == Client.ClientTier.STANDARD).count();
        long silver = bankService.getAllClients().stream().filter(c -> c.getTier() == Client.ClientTier.SILVER).count();
        long gold = bankService.getAllClients().stream().filter(c -> c.getTier() == Client.ClientTier.GOLD).count();
        long platinum = bankService.getAllClients().stream().filter(c -> c.getTier() == Client.ClientTier.PLATINUM).count();

        PieChart tierChart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("Standard", standard),
                new PieChart.Data("Silver", silver),
                new PieChart.Data("Gold", gold),
                new PieChart.Data("Platinum", platinum)
        ));
        tierChart.setTitle("Distribution des Tiers");
        tierChart.getStyleClass().add("chart-card");
        tierChart.setPrefWidth(300);

        // Account type balance bar
        CategoryAxis bx = new CategoryAxis();
        NumberAxis by = new NumberAxis();
        BarChart<String, Number> balChart = new BarChart<>(bx, by);
        balChart.setTitle("Solde par Type de Compte");
        balChart.getStyleClass().add("chart-card");
        XYChart.Series<String, Number> balSeries = new XYChart.Series<>();
        balSeries.setName("Solde");
        double checkingTotal = bankService.getAllAccounts().stream()
                .filter(a -> a instanceof CheckingAccount).mapToDouble(BankAccount::getBalance).sum();
        double savingsTotal = bankService.getAllAccounts().stream()
                .filter(a -> a instanceof SavingsAccount).mapToDouble(BankAccount::getBalance).sum();
        double investTotal = bankService.getAllAccounts().stream()
                .filter(a -> a instanceof InvestmentAccount).mapToDouble(BankAccount::getBalance).sum();
        balSeries.getData().addAll(
                new XYChart.Data<>("Courant", checkingTotal),
                new XYChart.Data<>("Épargne", savingsTotal),
                new XYChart.Data<>("Investissement", investTotal)
        );
        balChart.getData().add(balSeries);
        HBox.setHgrow(balChart, Priority.ALWAYS);

        bottomRow.getChildren().addAll(tierChart, balChart);

        page.getChildren().addAll(title, cards, lineChart, bottomRow);
        animateIn(page);
        contentArea.getChildren().setAll(page);
    }

    // ===== NEW CLIENT FORM =====
    private void showNewClientForm() {
        VBox page = new VBox(24);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(32));

        Label title = new Label("Nouveau Client");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Créer un nouveau profil client et ouvrir des comptes");
        subtitle.getStyleClass().add("page-subtitle");

        // Form card
        VBox formCard = new VBox(20);
        formCard.getStyleClass().add("form-card");
        formCard.setPadding(new Insets(32));
        formCard.setMaxWidth(700);

        Label formTitle = new Label("Informations Personnelles");
        formTitle.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        TextField firstNameField = createFormField("Prénom");
        TextField lastNameField = createFormField("Nom de famille");
        TextField emailField = createFormField("Email");
        TextField phoneField = createFormField("Téléphone");
        DatePicker dobPicker = new DatePicker();
        dobPicker.setPromptText("Date de naissance");
        dobPicker.getStyleClass().add("input-field");
        dobPicker.setValue(LocalDate.of(1990, 1, 1));
        TextField addressField = createFormField("Adresse");
        addressField.setPrefWidth(400);

        grid.add(styledLabel("Prénom *"), 0, 0); grid.add(firstNameField, 1, 0);
        grid.add(styledLabel("Nom *"), 0, 1); grid.add(lastNameField, 1, 1);
        grid.add(styledLabel("Email"), 0, 2); grid.add(emailField, 1, 2);
        grid.add(styledLabel("Téléphone"), 0, 3); grid.add(phoneField, 1, 3);
        grid.add(styledLabel("Date de naissance"), 0, 4); grid.add(dobPicker, 1, 4);
        grid.add(styledLabel("Adresse"), 0, 5); grid.add(addressField, 1, 5);

        // Account options
        Label accTitle = new Label("Ouvrir des Comptes");
        accTitle.getStyleClass().add("section-title");

        CheckBox openChecking = new CheckBox("Compte Courant");
        openChecking.getStyleClass().add("styled-checkbox");
        openChecking.setSelected(true);
        TextField checkingDeposit = createFormField("Dépôt initial (€)");
        checkingDeposit.setText("1000");
        TextField overdraftField = createFormField("Découvert autorisé (€)");
        overdraftField.setText("500");

        CheckBox openSavings = new CheckBox("Compte Épargne (3.5% APY)");
        openSavings.getStyleClass().add("styled-checkbox");
        TextField savingsDeposit = createFormField("Dépôt initial (€)");
        savingsDeposit.setText("5000");

        CheckBox openInvest = new CheckBox("Compte Investissement");
        openInvest.getStyleClass().add("styled-checkbox");
        TextField investDeposit = createFormField("Dépôt initial (€)");
        investDeposit.setText("10000");
        Slider riskSlider = new Slider(1, 10, 5);
        riskSlider.setShowTickLabels(true);
        riskSlider.setShowTickMarks(true);
        riskSlider.setMajorTickUnit(1);
        riskSlider.setSnapToTicks(true);
        riskSlider.getStyleClass().add("risk-slider");
        Label riskLabel = new Label("Niveau de risque: 5/10");
        riskSlider.valueProperty().addListener((obs, old, val) ->
                riskLabel.setText("Niveau de risque: " + val.intValue() + "/10"));

        GridPane accGrid = new GridPane();
        accGrid.setHgap(16);
        accGrid.setVgap(12);
        accGrid.add(openChecking, 0, 0);
        accGrid.add(checkingDeposit, 1, 0);
        accGrid.add(styledLabel("Découvert:"), 2, 0);
        accGrid.add(overdraftField, 3, 0);
        accGrid.add(openSavings, 0, 1);
        accGrid.add(savingsDeposit, 1, 1);
        accGrid.add(openInvest, 0, 2);
        accGrid.add(investDeposit, 1, 2);
        accGrid.add(riskSlider, 2, 2);
        accGrid.add(riskLabel, 3, 2);

        // Submit
        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> showClients());

        Button submitBtn = new Button("✓  Créer le Client");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setStyle("-fx-font-size: 14px; -fx-padding: 12 24;");

        submitBtn.setOnAction(e -> {
            try {
                if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank()) {
                    showStatus("❌ Prénom et Nom sont obligatoires"); return;
                }
                Client newClient = bankService.createClient(
                        firstNameField.getText().trim(),
                        lastNameField.getText().trim(),
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        dobPicker.getValue(),
                        addressField.getText().trim()
                );

                if (openChecking.isSelected()) {
                    double dep = parseDouble(checkingDeposit.getText(), 1000);
                    double ovd = parseDouble(overdraftField.getText(), 500);
                    bankService.openCheckingAccount(newClient.getClientId(), dep, ovd);
                }
                if (openSavings.isSelected()) {
                    double dep = parseDouble(savingsDeposit.getText(), 5000);
                    bankService.openSavingsAccount(newClient.getClientId(), dep);
                }
                if (openInvest.isSelected()) {
                    double dep = parseDouble(investDeposit.getText(), 10000);
                    bankService.openInvestmentAccount(newClient.getClientId(), dep, riskSlider.getValue());
                }

                showStatus("✅ Client " + newClient.getFullName() + " créé avec succès !");
                showClients();
            } catch (Exception ex) {
                showStatus("❌ Erreur: " + ex.getMessage());
            }
        });

        btnRow.getChildren().addAll(cancelBtn, submitBtn);

        formCard.getChildren().addAll(formTitle, grid, new Separator(), accTitle, accGrid, new Separator(), btnRow);

        page.getChildren().addAll(new VBox(4, title, subtitle), formCard);

        ScrollPane formScroll = new ScrollPane(page);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setStyle("-fx-background-color: #0A0E1A; -fx-background: #0A0E1A;");

        animateIn(page);
        contentArea.getChildren().setAll(formScroll);
    }

    // ===== INTEREST CALCULATOR =====
    private void showInterestCalculator() {
        VBox page = new VBox(24);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(32));

        Label title = new Label("Calculateur d'Intérêts");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Simulez et appliquez les intérêts sur vos comptes");
        subtitle.getStyleClass().add("page-subtitle");

        // ── TOP ROW: Simulator + Apply All ──────────────────────────
        HBox topRow = new HBox(16);

        // === SIMULATION CARD ===
        VBox simCard = new VBox(16);
        simCard.getStyleClass().add("form-card");
        simCard.setPadding(new Insets(24));
        HBox.setHgrow(simCard, Priority.ALWAYS);

        Label simTitle = new Label("🧮  Simulateur de Capital");
        simTitle.getStyleClass().add("section-title");

        GridPane simGrid = new GridPane();
        simGrid.setHgap(16);
        simGrid.setVgap(14);

        TextField capitalField = new TextField("10000");
        capitalField.getStyleClass().add("input-field");
        capitalField.setPrefWidth(180);

        TextField tauxField = new TextField("3.5");
        tauxField.getStyleClass().add("input-field");
        tauxField.setPrefWidth(180);

        TextField dureeField = new TextField("12");
        dureeField.getStyleClass().add("input-field");
        dureeField.setPrefWidth(180);

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Intérêts Simples", "Intérêts Composés (mensuel)", "Intérêts Composés (annuel)"));
        typeCombo.setValue("Intérêts Composés (mensuel)");
        typeCombo.getStyleClass().add("combo-field");
        typeCombo.setPrefWidth(220);

        simGrid.add(styledLabel("Capital initial (€)"), 0, 0); simGrid.add(capitalField, 1, 0);
        simGrid.add(styledLabel("Taux annuel (%)"), 0, 1); simGrid.add(tauxField, 1, 1);
        simGrid.add(styledLabel("Durée (mois)"), 0, 2); simGrid.add(dureeField, 1, 2);
        simGrid.add(styledLabel("Type de calcul"), 0, 3); simGrid.add(typeCombo, 1, 3);

        // Result box
        VBox resultBox = new VBox(10);
        resultBox.getStyleClass().add("result-box");
        resultBox.setPadding(new Insets(16));
        resultBox.setVisible(false);

        Label resultCapital = new Label();
        resultCapital.getStyleClass().add("result-main");
        Label resultInterets = new Label();
        resultInterets.getStyleClass().add("result-sub");
        Label resultTotal = new Label();
        resultTotal.getStyleClass().add("result-total");
        resultBox.getChildren().addAll(resultCapital, resultInterets, new Separator(), resultTotal);

        Button calcBtn = new Button("Calculer");
        calcBtn.getStyleClass().add("btn-primary");
        calcBtn.setMaxWidth(Double.MAX_VALUE);
        calcBtn.setOnAction(e -> {
            try {
                double capital = Double.parseDouble(capitalField.getText().replace(",", "."));
                double taux = Double.parseDouble(tauxField.getText().replace(",", ".")) / 100;
                int duree = Integer.parseInt(dureeField.getText().trim());
                String type = typeCombo.getValue();

                double totalAvecInterets;
                if (type.equals("Intérêts Simples")) {
                    totalAvecInterets = capital * (1 + taux * (duree / 12.0));
                } else if (type.equals("Intérêts Composés (mensuel)")) {
                    totalAvecInterets = capital * Math.pow(1 + taux / 12.0, duree);
                } else {
                    totalAvecInterets = capital * Math.pow(1 + taux, duree / 12.0);
                }

                double interets = totalAvecInterets - capital;
                resultCapital.setText("💰  Capital initial :  €" + String.format("%.2f", capital));
                resultInterets.setText("📈  Intérêts générés :  €" + String.format("%.2f", interets)
                        + "  (" + String.format("%.2f", taux * 100) + "% / an × " + duree + " mois)");
                resultTotal.setText("🏦  Total final :  €" + String.format("%.2f", totalAvecInterets));
                resultBox.setVisible(true);
            } catch (Exception ex) {
                showStatus("❌ Valeurs invalides — vérifiez les champs");
            }
        });

        simCard.getChildren().addAll(simTitle, simGrid, calcBtn, resultBox);

        // === APPLY ALL CARD ===
        VBox applyCard = new VBox(16);
        applyCard.getStyleClass().add("form-card");
        applyCard.setPadding(new Insets(24));
        applyCard.setPrefWidth(320);
        applyCard.setMinWidth(280);

        Label applyTitle = new Label("⚡  Application Mensuelle");
        applyTitle.getStyleClass().add("section-title");

        Label applyDesc = new Label("Applique automatiquement les intérêts mensuels à TOUS les comptes selon leur type :");
        applyDesc.setWrapText(true);
        applyDesc.setStyle("-fx-text-fill: #7799BB; -fx-font-size: 13px;");

        VBox rateInfo = new VBox(10);
        rateInfo.getStyleClass().add("rate-info-box");
        rateInfo.setPadding(new Insets(14));
        rateInfo.getChildren().addAll(
            buildRateRow("💳", "Compte Courant", "0.1% / mois", "#5599FF"),
            buildRateRow("🏦", "Compte Épargne", "3.5% APY", "#00D4AA"),
            buildRateRow("📈", "Investissement", "7% ± volatilité", "#FFB347")
        );

        Label warningLabel = new Label("⚠️  Cette action est irréversible et modifie tous les soldes.");
        warningLabel.setWrapText(true);
        warningLabel.setStyle("-fx-text-fill: #FFB347; -fx-font-size: 12px;");

        Button applyAllBtn = new Button("⚡  Appliquer à Tous les Comptes");
        applyAllBtn.getStyleClass().add("btn-primary");
        applyAllBtn.setMaxWidth(Double.MAX_VALUE);
        applyAllBtn.setOnAction(e -> {
            bankService.applyMonthlyInterestAll();
            showStatus("✅ Intérêts mensuels appliqués à tous les comptes !");
            showInterestCalculator(); // refresh to show updated preview
        });

        applyCard.getChildren().addAll(applyTitle, applyDesc, rateInfo, warningLabel, applyAllBtn);

        topRow.getChildren().addAll(simCard, applyCard);

        // ── BOTTOM: Per-Account Interest Preview Table ───────────────
        Label previewTitle = new Label("Aperçu des Intérêts par Compte");
        previewTitle.getStyleClass().add("section-title");

        TableView<BankAccount> previewTable = new TableView<>();
        previewTable.getStyleClass().add("data-table");
        previewTable.setPrefHeight(250);
        previewTable.setMinHeight(210);

        TableColumn<BankAccount, String> ownerCol = new TableColumn<>("Titulaire");
        ownerCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getOwner().getFullName()));
        ownerCol.setPrefWidth(140);

        TableColumn<BankAccount, String> typeCol = new TableColumn<>("Type de Compte");
        typeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getAccountType()));
        typeCol.setPrefWidth(160);

        TableColumn<BankAccount, String> balanceCol = new TableColumn<>("Solde Actuel");
        balanceCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("€%.2f", c.getValue().getBalance())));
        balanceCol.setPrefWidth(130);

        TableColumn<BankAccount, String> rateCol = new TableColumn<>("Taux Annuel");
        rateCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.2f%%", c.getValue().getInterestRate() * 100)));
        rateCol.setPrefWidth(110);

        TableColumn<BankAccount, String> estimCol = new TableColumn<>("Intérêt Mensuel Estimé");
        estimCol.setCellValueFactory(c -> {
            BankAccount acc = c.getValue();
            double monthly = acc.getBalance() * acc.getInterestRate() / 12.0;
            String prefix = monthly >= 0 ? "+" : "";
            return new javafx.beans.property.SimpleStringProperty(
                    prefix + String.format("€%.4f", monthly));
        });
        estimCol.setPrefWidth(180);
        estimCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); return; }
                setText(val);
                setStyle("-fx-text-fill: " + (val.startsWith("+") ? "#00D4AA" : "#FF6B6B")
                        + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<BankAccount, String> afterCol = new TableColumn<>("Solde Après Intérêts");
        afterCol.setCellValueFactory(c -> {
            BankAccount acc = c.getValue();
            double after = acc.getBalance() + acc.getBalance() * acc.getInterestRate() / 12.0;
            return new javafx.beans.property.SimpleStringProperty(String.format("€%.2f", after));
        });
        afterCol.setPrefWidth(170);
        afterCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); return; }
                setText(val);
                setStyle("-fx-text-fill: #C8DCF0; -fx-font-weight: bold;");
            }
        });

        previewTable.getColumns().addAll(ownerCol, typeCol, balanceCol, rateCol, estimCol, afterCol);
        previewTable.setItems(FXCollections.observableArrayList(bankService.getAllAccounts()));

        // Total summary row
        double totalEstimatedInterest = bankService.getAllAccounts().stream()
                .mapToDouble(a -> a.getBalance() * a.getInterestRate() / 12.0).sum();
        HBox totalRow = new HBox(16);
        totalRow.getStyleClass().add("action-panel");
        totalRow.setPadding(new Insets(14, 20, 14, 20));
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalLbl = new Label("Total intérêts mensuels estimés (tous comptes) :");
        totalLbl.setStyle("-fx-text-fill: #7799BB; -fx-font-size: 13px; -fx-font-weight: 600;");
        Label totalVal = new Label(String.format("+ €%.4f", totalEstimatedInterest));
        totalVal.setStyle("-fx-text-fill: #00D4AA; -fx-font-size: 18px; -fx-font-weight: 700;");
        totalRow.getChildren().addAll(totalLbl, totalVal);

        page.getChildren().addAll(
                new VBox(4, title, subtitle),
                topRow,
                previewTitle,
                previewTable,
                totalRow
        );

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: #0A0E1A; -fx-background: #0A0E1A;");

        animateIn(page);
        contentArea.getChildren().setAll(scroll);
    }

    private HBox buildRateRow(String icon, String label, String rate, String color) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 16px;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #8BAED4; -fx-font-size: 13px;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label rateLbl = new Label(rate);
        rateLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 700; -fx-font-size: 13px;");
        row.getChildren().addAll(ico, lbl, sp, rateLbl);
        return row;
    }

    private Label styledLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #7799BB; -fx-font-size: 13px; -fx-font-weight: 600;");
        return lbl;
    }

    private TextField createFormField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("input-field");
        tf.setPrefWidth(200);
        return tf;
    }

    private double parseDouble(String s, double fallback) {
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (Exception e) { return fallback; }
    }

    // ===== ANIMATIONS =====
    private void animateIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(16);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(node.opacityProperty(), 0),
                new KeyValue(node.translateYProperty(), 16)),
            new KeyFrame(Duration.millis(280),
                new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT))
        );
        tl.play();
    }

    public BorderPane getRoot() { return root; }
}
