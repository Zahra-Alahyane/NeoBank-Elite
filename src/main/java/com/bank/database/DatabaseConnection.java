package com.bank.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // ── Modifie ces valeurs si besoin ──────────────────────
    private static final String URL      = "jdbc:mysql://localhost:3306/neobank"
                                         + "?createDatabaseIfNotExist=true"
                                         + "&useSSL=false"
                                         + "&allowPublicKeyRetrieval=true"
                                         + "&serverTimezone=UTC"
                                         + "&characterEncoding=UTF-8";
    private static final String USER     = "root";
    private static final String PASSWORD = "";          // vide par défaut dans XAMPP
    // ────────────────────────────────────────────────────────

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion MySQL réussie !");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL introuvable : " + e.getMessage());
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔌 Connexion MySQL fermée.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur fermeture connexion : " + e.getMessage());
        }
    }

    public static boolean testConnection() {
        try {
            getConnection();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Connexion MySQL échouée : " + e.getMessage());
            return false;
        }
    }
}
