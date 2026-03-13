package com.bank.service;

import com.bank.database.*;
import com.bank.model.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class BankService {

    private static BankService instance;

    private BankService() {
        DatabaseInitializer.initialize();
        try {
            if (ClientDAO.count() == 0) seedDemoData();
        } catch (SQLException e) {
            System.err.println("Erreur vérification données: " + e.getMessage());
        }
    }

    public static BankService getInstance() {
        if (instance == null) instance = new BankService();
        return instance;
    }

    public Client createClient(String firstName, String lastName, String email,
                                String phone, LocalDate dob, String address) {
        try {
            Client client = new Client(firstName, lastName, email, phone, dob, address);
            return ClientDAO.save(client);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur création client: " + e.getMessage(), e);
        }
    }

    public List<Client> getAllClients() {
        try {
            List<Client> clients = ClientDAO.findAll();
            for (Client c : clients) {
                List<BankAccount> accounts = AccountDAO.findByClientId(c.getClientId());
                accounts.forEach(a -> c.getAccounts().add(a));
            }
            return clients;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement clients: " + e.getMessage(), e);
        }
    }

    public Client getClient(String id) {
        try {
            Client client = ClientDAO.findById(id);
            if (client != null) {
                List<BankAccount> accounts = AccountDAO.findByClientId(id);
                accounts.forEach(a -> client.getAccounts().add(a));
            }
            return client;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement client: " + e.getMessage(), e);
        }
    }

    public CheckingAccount openCheckingAccount(String clientId, double deposit, double overdraft) {
        try {
            Client client = ClientDAO.findById(clientId);
            CheckingAccount acc = new CheckingAccount(client, deposit, overdraft);
            AccountDAO.save(acc);
            if (deposit > 0) TransactionDAO.save(acc.getTransactions().get(0), acc.getAccountId());
            return acc;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ouverture compte courant: " + e.getMessage(), e);
        }
    }

    public SavingsAccount openSavingsAccount(String clientId, double deposit) {
        try {
            Client client = ClientDAO.findById(clientId);
            SavingsAccount acc = new SavingsAccount(client, deposit);
            AccountDAO.save(acc);
            if (deposit > 0) TransactionDAO.save(acc.getTransactions().get(0), acc.getAccountId());
            return acc;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ouverture compte épargne: " + e.getMessage(), e);
        }
    }

    public InvestmentAccount openInvestmentAccount(String clientId, double deposit, double risk) {
        try {
            Client client = ClientDAO.findById(clientId);
            InvestmentAccount acc = new InvestmentAccount(client, deposit, risk);
            AccountDAO.save(acc);
            if (deposit > 0) TransactionDAO.save(acc.getTransactions().get(0), acc.getAccountId());
            return acc;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ouverture compte investissement: " + e.getMessage(), e);
        }
    }

    public List<BankAccount> getAllAccounts() {
        try {
            List<BankAccount> accounts = AccountDAO.findAll();
            for (BankAccount acc : accounts) {
                List<Transaction> txs = TransactionDAO.findByAccountId(acc.getAccountId());
                acc.getTransactions().addAll(txs);
            }
            return accounts;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement comptes: " + e.getMessage(), e);
        }
    }

    public List<BankAccount> getAccountsByClient(String clientId) {
        try {
            return AccountDAO.findByClientId(clientId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement comptes client: " + e.getMessage(), e);
        }
    }

    public BankAccount getAccount(String id) {
        try {
            return AccountDAO.findById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement compte: " + e.getMessage(), e);
        }
    }

    public void deposit(BankAccount account, double amount) {
        try {
            account.deposit(amount);
            AccountDAO.update(account);
            Transaction tx = account.getTransactions().get(account.getTransactions().size() - 1);
            TransactionDAO.save(tx, account.getAccountId());
        } catch (SQLException e) {
            throw new RuntimeException("Erreur dépôt: " + e.getMessage(), e);
        }
    }

    public boolean withdraw(BankAccount account, double amount) {
        try {
            boolean ok = account.withdraw(amount);
            if (ok) {
                AccountDAO.update(account);
                Transaction tx = account.getTransactions().get(account.getTransactions().size() - 1);
                TransactionDAO.save(tx, account.getAccountId());
            }
            return ok;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur retrait: " + e.getMessage(), e);
        }
    }

    public boolean transfer(String fromId, String toId, double amount) {
        try {
            BankAccount from = AccountDAO.findById(fromId);
            BankAccount to   = AccountDAO.findById(toId);
            if (from == null || to == null) return false;
            boolean ok = from.withdraw(amount);
            if (!ok) return false;
            to.deposit(amount);
            AccountDAO.update(from);
            AccountDAO.update(to);
            Transaction txFrom = from.getTransactions().get(from.getTransactions().size() - 1);
            txFrom.setDescription("Virement vers " + to.getAccountNumber());
            TransactionDAO.save(txFrom, fromId);
            Transaction txTo = to.getTransactions().get(to.getTransactions().size() - 1);
            txTo.setDescription("Virement de " + from.getAccountNumber());
            TransactionDAO.save(txTo, toId);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur virement: " + e.getMessage(), e);
        }
    }

    public void applyInterest(BankAccount account) {
        try {
            account.calculateInterest();
            AccountDAO.update(account);
            Transaction tx = account.getTransactions().get(account.getTransactions().size() - 1);
            TransactionDAO.save(tx, account.getAccountId());
        } catch (SQLException e) {
            throw new RuntimeException("Erreur calcul intérêts: " + e.getMessage(), e);
        }
    }

    public void applyMonthlyInterestAll() {
        getAllAccounts().forEach(this::applyInterest);
    }

    public void setAccountStatus(BankAccount account, BankAccount.AccountStatus status) {
        try {
            account.setStatus(status);
            AccountDAO.update(account);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur mise à jour statut: " + e.getMessage(), e);
        }
    }

    public double getTotalAssets() {
        try { return AccountDAO.getTotalAssets(); } catch (SQLException e) { return 0; }
    }

    public long getTotalTransactions() {
        try { return TransactionDAO.countAll(); } catch (SQLException e) { return 0; }
    }

    public String getBankName() { return "NeoBank Elite"; }

    private void seedDemoData() throws SQLException {
        System.out.println("🌱 Chargement des données de démonstration...");
        Client alice = createClient("Alice", "Martin", "alice@email.com",
                "+33 6 12 34 56 78", LocalDate.of(1990, 3, 15), "12 Rue de la Paix, Paris");
        Client bob = createClient("Bob", "Dupont", "bob@email.com",
                "+33 6 98 76 54 32", LocalDate.of(1985, 7, 22), "45 Avenue Victor Hugo, Lyon");
        Client carol = createClient("Carol", "Lefèvre", "carol@email.com",
                "+33 6 55 44 33 22", LocalDate.of(1998, 11, 8), "78 Rue du Faubourg, Marseille");
        openCheckingAccount(alice.getClientId(), 5000, 1000);
        openSavingsAccount(alice.getClientId(), 25000);
        InvestmentAccount ai = openInvestmentAccount(alice.getClientId(), 50000, 7);
        applyInterest(ai);
        openCheckingAccount(bob.getClientId(), 3000, 500);
        SavingsAccount bs = openSavingsAccount(bob.getClientId(), 8500);
        applyInterest(bs);
        openCheckingAccount(carol.getClientId(), 1200, 200);

        // Create users for login
        UserDAO.save("admin",  "admin123",  "ADMIN",  null,               "Administrateur");
        UserDAO.save("alice",  "alice123",  "CLIENT", alice.getClientId(), alice.getFullName());
        UserDAO.save("bob",    "bob123",    "CLIENT", bob.getClientId(),   bob.getFullName());
        UserDAO.save("carol",  "carol123",  "CLIENT", carol.getClientId(), carol.getFullName());

        System.out.println("✅ Données de démonstration chargées !");
        System.out.println("👤 Login: admin / admin123");
    }
}