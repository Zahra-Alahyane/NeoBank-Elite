# 🏦 NeoBank Elite

Une application bancaire de bureau développée en Java 17 et JavaFX, simulant
les principales fonctionnalités d'un système bancaire moderne.

---

## 📋 Description

NeoBank Elite est une application de gestion bancaire complète permettant de
gérer les clients, les comptes, les transactions et les exportations de données.
Elle offre une interface graphique ergonomique avec un thème sombre, construite
avec JavaFX et connectée à une base de données MySQL.

---

## ✨ Fonctionnalités

- 🔐 **Authentification** — Connexion sécurisée avec gestion de sessions
- 👤 **Gestion des clients** — Opérations CRUD, recherche dynamique, tiers de fidélité
- 🏦 **Gestion des comptes** — Comptes courants, épargne et investissement
- 💸 **Transactions** — Dépôts, retraits, virements avec historique complet
- 📊 **Export** — Génération de rapports PDF et Excel
- 🎨 **Thème sombre** — Interface moderne et ergonomique

---

## 🛠️ Technologies Utilisées

| Technologie | Version |
|---|---|
| Java | 17 (LTS) |
| JavaFX | 21.0.1 |
| Apache Maven | 3.8+ |
| MySQL (XAMPP) | mysql-connector-j 8.3.0 |
| iText (PDF) | 5.5.13.3 |
| Apache POI (Excel) | 5.2.3 |

---

## ⚙️ Installation & Lancement

### Prérequis
- Java 17+
- Maven 3.8+
- XAMPP (MySQL activé)
- IntelliJ IDEA (recommandé)

### Étapes

**1. Cloner le projet**
```bash
git clone https://github.com/Zahra-Alahyane/NeoBank-Elite.git
```

**2. Démarrer MySQL via XAMPP**
Lancez XAMPP et démarrez le module **MySQL**.

**3. Configurer la base de données**
La base de données est initialisée automatiquement au premier lancement
via `DatabaseInitializer`.

**4. Lancer l'application**
```bash
mvn clean javafx:run
```
Ou directement depuis IntelliJ en exécutant `BankingApp.java`.

---

## 📁 Structure du Projet
```
src/
├── main/
│   ├── java/
│   │   ├── BankingApp.java
│   │   ├── com.bank.model/
│   │   ├── com.bank.service/
│   │   ├── com.bank.database/
│   │   └── com.bank.ui/
│   └── resources/
│       └── styles/
│           └── dark-theme.css
```

---

## 👩‍💻 Auteur

Développé par **Zahra Alahyane et Asmae El Fakir**  
Encadré par **M. Kamal El Hattab**

---

## 📄 Licence

Ce projet est réalisé dans le cadre d'un projet académique.
