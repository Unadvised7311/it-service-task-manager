# 🛠️ IT-Service Task Manager

Ein modernes, rollenbasiertes Task-Management-System für IT-Service-Teams. Dieses Projekt wurde als Full-Stack-Webanwendung entwickelt und kombiniert ein robustes Java-Backend mit einem modernen React-Dashboard.

## ✨ Features

* **Rollenbasierte Zugriffskontrolle (RBAC):** Unterschiedliche Ansichten und Rechte für Administratoren, Projektleiter und Mitarbeiter.
* **Projektverwaltung:** Erstellen, Bearbeiten und Archivieren von IT-Projekten.
* **Kanban-Task-Board:** Interaktives Board für Aufgaben (To Do, In Progress, Done) innerhalb eines Projekts.
* **Echtzeit-Fortschritt:** Automatische Berechnung des Projektfortschritts basierend auf abgeschlossenen Tasks.
* **Modernes UI/UX:** Responsives "Glassmorphism"-Design für optimale Bedienbarkeit.

## 💻 Tech Stack

**Backend:**
* Java 17
* Spring Boot 3
* Spring Web / RESTful APIs
* Spring Data JPA
* H2 In-Memory Database (für schnelles Prototyping)

**Frontend:**
* React.js
* JavaScript (ES6+)
* CSS-in-JS Styling
* Fetch API für Backend-Kommunikation

## 🚀 Lokale Installation & Start

### Voraussetzungen
* Node.js (v16 oder neuer)
* Java JDK 17
* Maven

### 1. Backend starten
Navigiere im Terminal in den Backend-Ordner und starte die Spring Boot Anwendung:
```bash
cd backend
mvn clean spring-boot:run

Das Backend läuft nun auf http://localhost:3000.
(Die Datenbank wird bei jedem Start automatisch mit Testdaten gefüllt).
2. Frontend starten

Öffne ein neues Terminal, navigiere in den Frontend-Ordner und starte React:
Bash

cd frontend
npm install
npm start

Das Frontend öffnet sich automatisch im Browser unter http://localhost:3000.
🔐 Test-Accounts (Demo)

Für die Evaluation des Systems sind folgende Test-Benutzer in der Datenbank hinterlegt:
Rolle	Vorführ-Name	Rechte & Ansichten
ADMIN	Admin	Vollzugriff, Admin-Panel, Benutzerverwaltung
MANAGER	Manager-Bob	Projekte erstellen, Projekte archivieren, Tasks verwalten
EMPLOYEE	Mitarbeiter-Charlie	Nur zugewiesene Projekte sehen, Tasks bearbeiten
📚 Kontext

Dieses Projekt entstand im Rahmen einer Fallstudie zur Entwicklung eines sicheren und effizienten IT-Service-Tools.
