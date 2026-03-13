package com.bank.database;

import com.bank.model.AppUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.UUID;

public class UserDAO {

    public static void createTable() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_users (
                    id           VARCHAR(36)  PRIMARY KEY,
                    username     VARCHAR(100) UNIQUE NOT NULL,
                    password_hash VARCHAR(64) NOT NULL,
                    role         ENUM('ADMIN','CLIENT') DEFAULT 'CLIENT',
                    client_id    VARCHAR(36),
                    full_name    VARCHAR(200),
                    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);
        }
    }

    public static AppUser findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM app_users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public static AppUser authenticate(String username, String password) throws SQLException {
        AppUser user = findByUsername(username);
        if (user == null) return null;
        String hash = hashPassword(password);
        return hash.equals(user.getPasswordHash()) ? user : null;
    }

    public static void save(String username, String password, String role, String clientId, String fullName) throws SQLException {
        String sql = "INSERT INTO app_users (id, username, password_hash, role, client_id, full_name) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, username);
            ps.setString(3, hashPassword(password));
            ps.setString(4, role);
            ps.setString(5, clientId);
            ps.setString(6, fullName);
            ps.executeUpdate();
        }
    }

    public static boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM app_users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public static long count() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM app_users")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erreur hash", e);
        }
    }

    private static AppUser mapRow(ResultSet rs) throws SQLException {
        return new AppUser(
            rs.getString("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("role"),
            rs.getString("client_id"),
            rs.getString("full_name")
        );
    }
}
