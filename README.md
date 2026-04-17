# SmartSpend (财务管家) 💰

A modern personal finance management Android app built with Kotlin and Jetpack Compose.

> **SEHH3140 Group Project**

---

## 📸 Screenshots

<!-- Add screenshots after taking them from the app -->
<!-- Suggested shots: Home page, Record page, Analysis page, AI report sheet, Dark mode -->

| Home | Record | Analysis |
|------|--------|----------|
| ![Home](docs/screenshots/home.png) | ![Record](docs/screenshots/record.png) | ![Analysis](docs/screenshots/analysis.png) |

| AI Report | Dark Mode | Saving Goals |
|-----------|-----------|--------------|
| ![AI](docs/screenshots/ai_report.png) | ![Dark](docs/screenshots/dark_mode.png) | ![Saving](docs/screenshots/saving_goals.png) |

---

## ✨ Features

### Core
- **Expense Tracking** — Add, edit, and delete expense records with category, date, amount, and notes
- **Quick Templates** — Save frequent expenses as templates; pin up to 3 for one-tap recording
- **Natural Language Input** — Describe expenses in plain text (e.g. "lunch 45 bucks") and let AI fill in the form
- **Calculator Keyboard** — iOS-style number pad for fast amount entry

### Analysis
- **Monthly Overview** — Large hero card showing total monthly spend, today, and this week
- **Budget Tracking** — Set a monthly budget; progress bar and donut chart show usage in real time
- **7-Day Trend Chart** — Bar chart of daily spending over the past week
- **Category Breakdown** — Ranked list of spending by category with percentage bars
- **AI Deep Analysis** — GLM-5 powered monthly report with insights and saving suggestions

### Asset Management
- **Debt Tracker** — Log money lent or borrowed, set due dates, mark as repaid
- **Saving Goals** — Set named goals with target amounts and deadlines, deposit progress manually
- **Stock Portfolio** — Track HK / US / A-share holdings with live price refresh

### Account & Sync
- **Firebase Auth** — Email/password and Google Sign-In; email verification flow
- **Guest Mode** — Use all core features without an account; AI features require login
- **Cloud Sync** — Firestore-backed sync across devices (Last Write Wins, soft delete)

### UX & Customisation
- **Multi-language** — Simplified Chinese, Traditional Chinese, and English; switch in Settings or on first launch
- **Swipe Gestures** — Swipe left to delete, right to edit on expense items (with optional confirm dialog)
- **Dark Mode** — Full light / dark / system-follow support
- **Persistent Notification** — Status bar widget showing live budget progress
- **Budget Alerts** — Push notification at 80% and 100% budget usage
- **Data Export** — Export expense records to CSV (UTF-8, Excel compatible)
- **Category Management** — Add custom categories with icons or photos from your gallery
- **New User Onboarding** — Animated highlight overlay guiding first-time users

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM (ViewModel + Repository) |
| Local DB | Room (SQLite) v9 |
| Preferences | DataStore |
| Charts | Vico Chart Library 2.0.0 |
| Image Loading | Coil |
| Auth | Firebase Authentication |
| Cloud DB | Firestore (asia-east2) |
| Backend | FastAPI (Python), deployed on Azure |
| AI Model | GLM-5 via DashScope |
| Stock Data | yfinance |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |

---

## 🚀 Getting Started

### Option A — Install the APK (Recommended for testers)

1. Download the latest APK from the [Releases](../../releases) page
2. On your Android device, go to **Settings → Security → Install unknown apps** and allow installation from your browser or file manager
3. Open the downloaded `.apk` file and tap **Install**
4. Launch **SmartSpend** from your home screen

> ⚠️ **AI features (natural language input & AI analysis) require a logged-in account.** Guest mode disables AI features. All other features work fully offline.

### Option B — Build from source

**Requirements:**
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK with API 26–34

```bash
# Clone the repository
git clone https://github.com/pure-white189/financial-app.git
cd financial-app

# Open in Android Studio, let Gradle sync, then run on a device or emulator
```

---

## ☁️ Backend

The backend is deployed on an Azure VM and runs 24/7. AI features (natural language parsing and monthly analysis) and stock price fetching all go through this server.

**Base URL:** `http://20.199.169.108`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Health check |
| `/parse-expense?text=...&lang=...` | GET | Parse natural language expense |
| `/analyze-expenses` | POST | Generate monthly AI analysis report |
| `/stock-prices?symbols=...` | GET | Fetch live stock prices |

### Rate Limits

| User type | Natural language parsing | Monthly analysis |
|-----------|--------------------------|-----------------|
| Guest / not logged in | ✗ | ✗ |
| Free | 10 / day | 2 / month |
| Pro | Unlimited | Unlimited |

---

## 📁 Project Structure

```
app/src/main/java/com/example/myapplication/
├── data/
│   ├── AppDatabase.kt           # Room database (v9), all migrations
│   ├── Category.kt / CategoryDao.kt
│   ├── Expense.kt / ExpenseDao.kt
│   ├── ExpenseTemplate.kt / ExpenseTemplateDao.kt
│   ├── Loan.kt / LoanDao.kt
│   ├── SavingGoal.kt / SavingGoalDao.kt
│   ├── Stock.kt / StockDao.kt
│   ├── ExpenseRepository.kt
│   ├── SyncRepository.kt        # Firestore cloud sync
│   ├── FirestoreMappers.kt
│   ├── ThemePreferences.kt      # DataStore settings
│   ├── NotificationHelper.kt
│   └── AiExpenseParser.kt       # Backend API client
│
├── ui/
│   ├── HomePage.kt
│   ├── RecordPage.kt
│   ├── AnalysisPage.kt
│   ├── DebtPage.kt
│   ├── SavingGoalPage.kt
│   ├── StockPage.kt
│   ├── SettingsPage.kt
│   ├── AccountPage.kt
│   ├── EditExpensePage.kt
│   ├── CategoryManagementPage.kt
│   ├── ExportPage.kt
│   ├── LanguageSelectionPage.kt # First-launch language picker
│   ├── SyncViewModel.kt
│   └── components/
│       └── FeatureHighlightOverlay.kt
│
├── utils/
│   ├── LanguageManager.kt       # Language persistence & switching
│   ├── CategoryDisplayName.kt   # Localised category name extension
│   ├── TemplateDisplayName.kt   # Localised template name extension
│   └── CsvExportHelper.kt
│
├── ExpenseViewModel.kt
├── AuthViewModel.kt
├── MainActivity.kt              # AppCompatActivity (required for locale API)
├── FinanceApplication.kt
├── BottomNavItem.kt
└── DateUtils.kt
```

---

## 📦 Database Schema

| Table | Version Added | Description |
|-------|--------------|-------------|
| `categories` | v1 | Expense categories (default + custom) |
| `expenses` | v1 | Individual expense records |
| `expense_templates` | v2 | Quick-record templates |
| `loans` | v4 | Debt / lending records |
| `saving_goals` | v5 | Savings targets with progress |
| `stocks` | v6 | Stock portfolio holdings |

Migrations are handled with explicit `Migration` objects — no destructive migration on upgrade.

| Migration | Changes |
|-----------|---------|
| v6 → v7 | Cloud sync fields: `firestoreId`, `updatedAt`, `isDeleted` (all four tables) |
| v7 → v8 | Category key column for multi-language support |
| v8 → v9 | Default template names localised |

---

## ⚠️ Known Limitations

- **Backend uses HTTP** — No HTTPS configured yet (no custom domain)
- **Stock price refresh** — A-share symbols should be entered without suffix (e.g. `600519` not `600519.SS`)
- **Rate limit counters reset on backend restart** — In-memory only, not persisted

---

## 🗺️ Roadmap

- [x] Azure VM backend deployment
- [x] Firebase Authentication (email + Google)
- [x] Cloud data sync (Firestore)
- [x] Proper Room database migrations
- [x] Multi-language support (Simplified Chinese / Traditional Chinese / English)
- [ ] Income / salary tracking with monthly reset
- [ ] Achievements & daily check-in system
- [ ] Subscription model for AI features
- [ ] Home screen widget

---

## 🤖 Development Approach

This project uses AI-assisted development (Claude, GitHub Copilot):
- I designed all features, architecture, and user experience
- AI tools generated most of the code based on my requirements
- I handled integration, testing, and iteration

This approach allowed rapid prototyping while learning modern Android development patterns.

---

## 📄 License

This project is for academic purposes only.
