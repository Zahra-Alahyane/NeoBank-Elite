package com.bank.database;

import com.bank.model.Client;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientDAO {

    // ── CREATE ──────────────────────────────────────────────
    public static Client save(Client client) throws SQLException {
        if (client.getClientId() == null || findById(client.getClientId()) == null) {
            return insert(client);
        } else {
            return update(client);
        }
    }

    private static Client insert(Client client) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO clients (id, first_name, last_name, email, phone,
                                 date_of_birth, address, tier, member_since)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.setString(2, client.getFirstName());
            ps.setString(3, client.getLastName());
            ps.setString(4, client.getEmail());
            ps.setString(5, client.getPhone());
            ps.setObject(6, client.getDateOfBirth());
            ps.setString(7, client.getAddress());
            ps.setString(8, client.getTier().name());
            ps.executeUpdate();
        }
        return findById(id);
    }

    public static Client update(Client client) throws SQLException {
        String sql = """
            UPDATE clients SET first_name=?, last_name=?, email=?, phone=?,
                               date_of_birth=?, address=?, tier=?
            WHERE id=?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, client.getFirstName());
            ps.setString(2, client.getLastName());
            ps.setString(3, client.getEmail());
            ps.setString(4, client.getPhone());
            ps.setObject(5, client.getDateOfBirth());
            ps.setString(6, client.getAddress());
            ps.setString(7, client.getTier().name());
            ps.setString(8, client.getClientId());
            ps.executeUpdate();
        }
        return findById(client.getClientId());
    }

    // ── READ ─────────────────────────────────────────────────
    public static List<Client> findAll() throws SQLException {
        List<Client> list = new ArrayList<>();
        String sql = "SELECT * FROM clients ORDER BY member_since DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public static Client findById(String id) throws SQLException {
        String sql = "SELECT * FROM clients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public static boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM clients WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ── DELETE ───────────────────────────────────────────────
    public static void delete(String id) throws SQLException {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    // ── STATS ────────────────────────────────────────────────
    public static long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM clients";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    // ── MAPPING ──────────────────────────────────────────────
    private static Client mapRow(ResultSet rs) throws SQLException {
        Date dob = rs.getDate("date_of_birth");
        Client c = new Client(
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("phone"),
            dob != null ? dob.toLocalDate() : null,
            rs.getString("address")
        );
        // Inject the persisted ID via reflection-free setter
        c.setClientId(rs.getString("id"));
        c.setTierFromString(rs.getString("tier"));
        return c;
    }
}
