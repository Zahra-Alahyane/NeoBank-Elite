package com.bank.database;

import com.bank.model.Transaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionDAO {

    public static void save(Transaction tx, String accountId) throws SQLException {
        String sql = """
            INSERT INTO transactions
                (id, transaction_ref, type, amount, description, balance_after, account_id, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, tx.getTransactionId());
            ps.setString(3, tx.getType().name());
            ps.setDouble(4, tx.getAmount());
            ps.setString(5, tx.getDescription());
            ps.setDouble(6, tx.getBalanceAfter());
            ps.setString(7, accountId);
            ps.setObject(8, tx.getTimestamp());
            ps.executeUpdate();
        }
    }

    public static List<Transaction> findByAccountId(String accountId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public static List<Transaction> findByClientId(String clientId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = """
            SELECT t.* FROM transactions t
            JOIN bank_accounts a ON t.account_id = a.id
            WHERE a.client_id = ?
            ORDER BY t.timestamp DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public static long countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private static Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction(
            Transaction.Type.valueOf(rs.getString("type")),
            rs.getDouble("amount"),
            rs.getString("description"),
            rs.getDouble("balance_after")
        );
        tx.setTransactionRef(rs.getString("transaction_ref"));
        return tx;
    }
}
