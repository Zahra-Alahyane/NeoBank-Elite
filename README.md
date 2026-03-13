# 🏦 NeoBank Elite — Système de Gestion Bancaire JavaFX

Un système bancaire complet et élégant développé avec JavaFX, démontrant les principes fondamentaux de la POO.

---

## 🎯 Concepts POO Démontés

| Concept | Implémentation |
|---------|---------------|
| **Interface** | `Bankable` (dépôt/retrait/intérêts) + `Notifiable` (notifications) |
| **Classe Abstraite** | `BankAccount` — logique commune + méthodes abstraites |
| **Héritage** | `CheckingAccount`, `SavingsAccount`, `InvestmentAccount` étendent `BankAccount` |
| **Polymorphisme** | `calculateInterest()` se comporte différemment selon le type de compte |
| **Encapsulation** | Tous les champs privés/protégés avec getters/setters |

---

## ✨ Fonctionnalités

### Comptes Bancaires
- **Compte Courant** (CheckingAccount) — Découvert autorisé configurable, intérêts 0.1%
- **Compte Épargne** (SavingsAccount) — APY 3.5%, limite de 6 retraits/mois
- **Compte Investissement** (InvestmentAccount) — Niveau de risque 1-10, rendement variable avec volatilité simulée

### Interface Graphique (5 écrans)
1. **📊 Tableau de Bord** — KPIs, graphiques en temps réel, aperçu clients
2. **👥 Clients** — CRUD complet, recherche, détails, tiers de fidélité
3. **💳 Comptes** — Gestion des comptes, dépôt/retrait/gel en un clic
4. **↕ Opérations** — Historique transactions, virements entre comptes
5. **📈 Analytique** — Graphiques évolution, répartition, comparatifs

### Fonctionnalités Avancées
- Système de **tiers client** (Standard → Silver → Gold → Platinum) selon le solde
- **Notifications** automatiques à chaque opération
- **Virements** entre comptes avec validation
- **Simulation de marché** pour les comptes investissement
- **Gel/dégel** de comptes
- **Données de démonstration** pré-chargées (3 clients, 7 comptes)

---

## 🚀 Installation & Démarrage

### Prérequis
- Java 17+ (JDK)
- Maven 3.8+

### Lancer avec Maven
```bash
cd BankingSystem
mvn javafx:run
```

### Compiler un JAR exécutable
```bash
mvn package
java -jar target/banking-system-2.0.0.jar
```

### Sans Maven (avec JavaFX SDK)
```bash
# Télécharger JavaFX SDK depuis https://gluonhq.com/products/javafx/
javac --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -r src/
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml com.bank.BankingApp
```

---

## 🎨 Design

- Thème sombre premium (couleurs #0A0E1A + accent #00D4AA)
- Animations de transition fluides (280ms ease-out)
- Tableaux de données réactifs avec tri et sélection
- Graphiques interactifs (PieChart, BarChart, LineChart)
- Responsive et redimensionnable (min 1100×700)

---

## 📁 Structure du Projet

```
BankingSystem/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── com/bank/
    │       ├── BankingApp.java          ← Point d'entrée JavaFX
    │       ├── model/
    │       │   ├── Bankable.java        ← Interface principale
    │       │   ├── Notifiable.java      ← Interface notifications
    │       │   ├── BankAccount.java     ← Classe abstraite
    │       │   ├── CheckingAccount.java ← Compte courant
    │       │   ├── SavingsAccount.java  ← Compte épargne
    │       │   ├── InvestmentAccount.java ← Compte investissement
    │       │   ├── Client.java          ← Modèle client
    │       │   └── Transaction.java     ← Modèle transaction
    │       ├── service/
    │       │   └── BankService.java     ← Singleton service métier
    │       └── ui/
    │           └── MainController.java  ← Interface JavaFX complète
    └── resources/
        └── styles/
            └── dark-theme.css          ← Thème CSS sombre
```

---

## 💡 Exemples d'Utilisation

```java
// Créer un client
Client client = bankService.createClient("Jean", "Dupont", "jean@email.com", 
    "+33 6 12 34 56 78", LocalDate.of(1990, 5, 15), "Paris");

// Ouvrir des comptes
CheckingAccount checking = bankService.openCheckingAccount(client.getClientId(), 2000, 500);
SavingsAccount savings = bankService.openSavingsAccount(client.getClientId(), 10000);
InvestmentAccount invest = bankService.openInvestmentAccount(client.getClientId(), 50000, 7);

// Opérations (polymorphisme)
checking.deposit(500);
savings.withdraw(200);
invest.calculateInterest(); // comportement unique à chaque type

// Virement
bankService.transfer(checking.getAccountId(), savings.getAccountId(), 300);

// Appliquer les intérêts à tous les comptes (polymorphisme)
bankService.applyMonthlyInterestAll();
```
