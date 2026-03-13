package com.bank.database;

import com.bank.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountDAO {

    // ── SAVE (insert or update) ──────────────────────────────
    public static BankAccount save(BankAccount account) throws SQLException {
        String checkSql = "SELECT id FROM bank_accounts WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, account.getAccountId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return update(account);
                else           return insert(account);
            }
        }
    }

    private static BankAccount insert(BankAccount account) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO bank_accounts
                (id, account_number, account_type, balance, interest_rate, status,
                 notifications_enabled, overdraft_limit, withdrawals_this_month,
                 risk_level, client_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            fillStatement(ps, id, account);
            ps.executeUpdate();
        }
        account.setAccountId(id);
        return account;
    }

    public static BankAccount update(BankAccount account) throws SQLException {
        String sql = """
            UPDATE bank_accounts SET balance=?, status=?, notifications_enabled=?,
                overdraft_limit=?, withdrawals_this_month=?, tier_update=NULL
            WHERE id=?
        """;
        // Simple update — just sync balance and status
        String sql2 = """
            UPDATE bank_accounts SET balance=?, status=?, notifications_enabled=?
            WHERE id=?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql2)) {
            ps.setDouble(1, account.getBalance());
            ps.setString(2, account.getStatus().name());
            ps.setBoolean(3, account.isNotificationsEnabled());
            ps.setString(4, account.getAccountId());
            ps.executeUpdate();
        }
        return account;
    }

    // ── READ ──────────────────────────────────────────────────
    public static List<BankAccount> findAll() throws SQLException {
        List<BankAccount> list = new ArrayList<>();
        String sql = "SELECT a.*, c.first_name, c.last_name, c.email, c.phone, c.address, c.date_of_birth, c.tier, c.member_since " +
                     "FROM bank_accounts a JOIN clients c ON a.client_id = c.id ORDER BY a.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public static List<BankAccount> findByClientId(String clientId) throws SQLException {
        List<BankAccount> list = new ArrayList<>();
        String sql = "SELECT a.*, c.first_name, c.last_name, c.email, c.phone, c.address, c.date_of_birth, c.tier, c.member_since " +
                     "FROM bank_accounts a JOIN clients c ON a.client_id = c.id WHERE a.client_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public static BankAccount findById(String id) throws SQLException {
        String sql = "SELECT a.*, c.first_name, c.last_name, c.email, c.phone, c.address, c.date_of_birth, c.tier, c.member_since " +
                     "FROM bank_accounts a JOIN clients c ON a.client_id = c.id WHERE a.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ── STATS ─────────────────────────────────────────────────
    public static double getTotalAssets() throws SQLException {
        String sql = "SELECT COALESCE(SUM(balance), 0) FROM bank_accounts WHERE status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public static long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM bank_accounts";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    // ── DELETE ────────────────────────────────────────────────
    public static void delete(String id) throws SQLException {
        String sql = "DELETE FROM bank_accounts WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    // ── HELPERS ───────────────────────────────────────────────
    private static void fillStatement(PreparedStatement ps, String id, BankAccount account) throws SQLException {
        String type = account instanceof CheckingAccount  ? "CHECKING"
                    : account instanceof SavingsAccount   ? "SAVINGS"
                    : "INVESTMENT";

        ps.setString(1, id);
        ps.setString(2, account.getAccountNumber());
        ps.setString(3, type);
        ps.setDouble(4, account.getBalance());
        ps.setDouble(5, account.getInterestRate());
        ps.setString(6, account.getStatus().name());
        ps.setBoolean(7, account.isNotificationsEnabled());
        ps.setDouble(8, account instanceof CheckingAccount ca ? ca.getOverdraftLimit() : 0);
        ps.setInt(9, account instanceof SavingsAccount sa ? sa.getWithdrawalsThisMonth() : 0);
        ps.setDouble(10, account instanceof InvestmentAccount ia ? ia.getRiskLevel() : 5);
        ps.setString(11, account.getOwner().getClientId());
    }

    private static BankAccount mapRow(ResultSet rs) throws SQLException {
        // Build the owner client
        Client owner = new Client(
            rs.getString("first_name"), rs.getString("last_name"),
            rs.getString("email"),      rs.getString("phone"),
            rs.getDate("date_of_birth") != null ? rs.getDate("date_of_birth").toLocalDate() : null,
            rs.getString("address")
        );
        owner.setClientId(rs.getString("client_id"));
        owner.setTierFromString(rs.getString("tier"));

        String type    = rs.getString("account_type");
        double balance = rs.getDouble("balance");

        BankAccount account = switch (type) {
            case "CHECKING"   -> new CheckingAccount(owner, 0, rs.getDouble("overdraft_limit"));
            case "SAVINGS"    -> new SavingsAccount(owner, 0);
            case "INVESTMENT" -> new InvestmentAccount(owner, 0, rs.getDouble("risk_level"));
            default -> throw new SQLException("Type inconnu: " + type);
        };

        account.setAccountId(rs.getString("id"));
        account.setBalance(balance);
        account.setStatus(BankAccount.AccountStatus.valueOf(rs.getString("status")));
        account.setNotificationsEnabled(rs.getBoolean("notifications_enabled"));

        if (account instanceof SavingsAccount sa)
            sa.setWithdrawalsThisMonth(rs.getInt("withdrawals_this_month"));

        return account;
    }
}
