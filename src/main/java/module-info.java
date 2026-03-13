open module com.bank {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;

    exports com.bank;
    exports com.bank.ui;
    exports com.bank.model;
    exports com.bank.service;
    exports com.bank.database;
}