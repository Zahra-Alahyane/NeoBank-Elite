package com.bank.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // ── TABLE : clients ─────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS clients (
                    id           VARCHAR(36)  PRIMARY KEY,
                    first_name   VARCHAR(100) NOT NULL,
                    last_name    VARCHAR(100) NOT NULL,
                    email        VARCHAR(200) UNIQUE NOT NULL,
                    phone        VARCHAR(50),
                    date_of_birth DATE,
                    address      TEXT,
                    tier         ENUM('STANDARD','SILVER','GOLD','PLATINUM') DEFAULT 'STANDARD',
                    member_since DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // ── TABLE : bank_accounts ───────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bank_accounts (
                    id                     VARCHAR(36)  PRIMARY KEY,
                    account_number         VARCHAR(50)  UNIQUE NOT NULL,
                    account_type           ENUM('CHECKING','SAVINGS','INVESTMENT') NOT NULL,
                    balance                DOUBLE       NOT NULL DEFAULT 0,
                    interest_rate          DOUBLE       NOT NULL DEFAULT 0,
                    status                 ENUM('ACTIVE','FROZEN','CLOSED') DEFAULT 'ACTIVE',
                    notifications_enabled  TINYINT(1)   DEFAULT 1,
                    overdraft_limit        DOUBLE       DEFAULT 0,
                    withdrawals_this_month INT          DEFAULT 0,
                    risk_level             DOUBLE       DEFAULT 5,
                    client_id              VARCHAR(36)  NOT NULL,
                    created_at             DATETIME     DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // ── TABLE : transactions ────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id              VARCHAR(36)  PRIMARY KEY,
                    transaction_ref VARCHAR(20)  NOT NULL,
                    type            ENUM('DEPOSIT','WITHDRAWAL','TRANSFER','INTEREST','FEE') NOT NULL,
                    amount          DOUBLE       NOT NULL,
                    description     VARCHAR(255),
                    balance_after   DOUBLE       NOT NULL,
                    account_id      VARCHAR(36)  NOT NULL,
                    timestamp       DATETIME     DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (account_id) REFERENCES bank_accounts(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            System.out.println("✅ Tables MySQL créées / vérifiées avec succès !");

        } catch (SQLException e) {
            System.err.println("❌ Erreur initialisation BD : " + e.getMessage());
            throw new RuntimeException("Impossible d'initialiser la base de données", e);
        }

        // Create users table
        try {
            UserDAO.createTable();
        } catch (SQLException e) {
            System.err.println("❌ Erreur table users : " + e.getMessage());
        }
    }
}
