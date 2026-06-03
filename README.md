# Portfolio-Grade AI-Powered Expense Tracker

A production-ready full-stack AI Expense Tracker application featuring native Jetpack Compose on Android as well as React.js + Node.js/Express + MongoDB for cross-platform visual synchronization, powered by Gemini 2.5 Flash.

The project incorporates structural compliance for local Android simulator executions, complete REST architecture CRUD configurations, high-fidelity mock thermal bill rendering for instant hands-free API evaluations, and monorepo production setup configurations for vercel.

---

## 📂 Codebase Folder Architecture

```text
/
├── app/                       # Native Android Application module
│   ├── src/main/java/         # Android Source Files
│   │   └── com/example/
│   │       ├── data/          # Database Entities (Room, DAO)
│   │       ├── network/       # Gemini REST API service client (Moshi/Retrofit)
│   │       └── ui/            # Dashboard Composable screens & ViewModels
│   └── build.gradle.kts       # Gradle module details
│
├── backend/                   # Express.js REST API service module
│   ├── config/                # Database connection configuration
│   ├── models/                # Mongoose Database Schemas
│   ├── routes/                # Express Routing maps (CRUD & upload endpoints)
│   ├── services/              # AI Gemini services integrating @google/genai SDK
│   ├── server.js              # Express Node Server entry point
│   └── package.json           # Node configuration definitions
│
├── frontend/                  # React.JS graphical interface dashboard
│   ├── src/
│   │   ├── App.js             # Main dashboard rendering including Recharts
│   │   └── index.js           # Web index entry point
│   └── package.json           # Frontend configuration definitions
│
├── vercel/                    # Production deployment configuration profiles
│   └── README.md              # Steps and tips for continuous Vercel integration
│
├── vercel.json                # Central monorepo deployment routings
└── metadata.json              # Platform descriptor configurations
```

---

## 🛠️ Installation & Setup Guide

### 1. Pre-requisites
- **Java Development Kit Development**: JDK 17+ (configured for Android Compose compiler compatibility).
- **Node.js**: v18+ for Express and React development.
- **MongoDB**: An active Atlas string or local MongoDB instance on port `27017`.

### 2. Configure Environment variables
Navigate to `/backend` and configure `.env`:
```properties
PORT=8080
MONGODB_URI=mongodb://localhost:27017/expense-ai-tracker
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```
*Note: Make sure to replace `YOUR_GEMINI_API_KEY` with a valid model-authorized API key obtained from Google AI Studio.*

### 3. Initialize Backend Server
Install library dependencies and start Node host server:
```bash
cd backend
npm install
npm run start
```
*The Express API service will host on port `8080` (avoiding ports 5000, 5001, and 5050).*

### 4. Initialize React Frontend
Open a new console terminal, install dependencies, and launch React build servers:
```bash
cd frontend
npm install
npm run start
```
*The interactive dashboard will load in your browser at `http://localhost:3000`.*

---

## 📱 Mobile Android Application Setup
1. Enter your Gemini API key in the **Secrets panel inside Google AI Studio**. The platform will inject this securely via `BuildConfig.GEMINI_API_KEY` without committing to source files.
2. Under the Streaming Android Emulator in your browser, the Android package will boot automatically.
3. If compiling manually:
   - Build using: `gradle assembleDebug` or by connecting your hardware phone details.

---

## 🔬 Core System Features

### 🔸 AI Receipt Scanner
Choose any of the 4 High-Fidelity Mock Presets (Starbucks, AWS subscription, Comcast broadband, Target furniture suite). Selecting a preset drafts a digital thermal statement directly on a virtual JPEG image and encodes it to Base64 to trigger Gemini API OCR.
The model extracts the total amount, vendor names, specific receipt descriptors, and selects a category strictly between:
- **Food**
- **Utility**
- **Subscriptions**
- **Others**

### 🔸 Expense Portfolio CRUD
Fully integrated local database persistency:
- **CREATE**: Add manually or confirm saving AI parsed scanned receipts.
- **READ**: Live allocation distributions with reactive visual donut cards.
- **UPDATE**: Correct logs inline by expanding dialog edit screens.
- **DELETE**: Wipe out incorrect transaction history segments instantly.

### 🔸 Deep Advisory AI spending Insights
A backend REST routine fetches the current database logs inside Express and passes them to Gemini 2.5 Flash, generating action-ready recommendations, budget indicators, and subscription summaries directly over the UI.
