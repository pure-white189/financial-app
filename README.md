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

### Core Expense Tracking
- **Expense CRUD** — Add, edit, delete records with category, date, amount, and notes
- **Quick Templates** — Save frequent expenses as templates; pin up to 3 for one-tap recording; up to 2 shown as notification shortcuts
- **Natural Language Input** — Describe expenses in plain text (e.g. "lunch 45 bucks yesterday") — AI parses amount, category, date, time, and currency automatically
- **Calculator Keyboard** — iOS-style number pad for fast amount entry
- **Multi-currency Entry** — Select HKD / CNY / USD per record; live exchange rate fetched automatically and locked at record time
- **Large Amount Confirmation** — Optional second confirmation dialog for big purchases

### Analysis & Budget
- **Monthly Overview** — Hero card showing total spend, today, and this week
- **Budget Tracking** — Set a monthly budget; donut chart and progress bar update in real time
- **7-Day Trend Chart** — Bar chart of daily spending over the past week (k-format for large values)
- **Category Breakdown** — Ranked spending by category with percentage bars
- **AI Deep Analysis** — GLM-5 powered monthly report with multi-language insights and saving suggestions, displayed in an immersive bottom sheet
- **Report History** — Every AI report is saved locally; browse past months anytime without regenerating

### Income & Balance
- **Monthly Income Recording** — Set a monthly income total with notes; view full history
- **Balance Summary** — Home gallery card showing income / expenses / net balance; surplus shown in green, deficit in red
- **Transfer to Savings** — One-tap transfer of monthly surplus into a saving goal

### Asset Management
- **Debt Tracker** — Log money lent or borrowed; set due dates; overdue items highlighted; mark as repaid
- **Saving Goals** — Named goals with target amounts and deadlines; manual deposit with progress bar
- **Stock Portfolio** — Track HK / US / A-share holdings with live price refresh; A-share codes entered without suffix (auto-completed); total portfolio value converted to primary currency

### AI Features
- **Natural Language Parsing** — Auto-fills record form from plain text input; supports relative dates ("yesterday"), currency detection, and time extraction
- **Monthly Analysis Report** — Comprehensive AI report saved locally, browsable by month
- **Usage Quota** — Free users: 10 parses/day, 2 analyses/month; Pro users: unlimited
- **Token System** — Earn tokens via daily check-in and achievements; redeem to extend AI quota

### Account & Sync
- **Firebase Auth** — Email/password and Google Sign-In; email verification flow; forgot password
- **Guest Mode** — Full offline access to core features; AI requires login
- **Cloud Sync** — Bidirectional Firestore sync across all 11 business tables (Last Write Wins, soft delete propagation)
- **Pro Subscription** — Activation code system (no Google Play required); 90-day Pro access, stackable

### Check-in & Achievements
- **Daily Check-in** — +1 token per day; milestone bonuses at 7 / 30 / 90 / 365 / 730 consecutive days
- **Achievement System** — 21 achievements across three categories: behavioural (first expense, first sync, etc.), check-in streaks, and budget consistency (1–24 months under budget)
- **Token Wallet** — Balance managed server-side; history displayed locally

### Personalised Recommendations
- **Local Rule Engine** — Analyses spending patterns and financial state; triggers recommendations based on 10 tag types (over_budget, food_heavy, investor, streak_user, etc.)
- **Three Display Positions** — AI report bottom sheet, home Today Insight card (dismissable), saving goal completion screen
- **Content Library** — JSON-based, three languages, hosted on backend; app fetches and caches with version comparison

### UX & Customisation
- **Multi-language** — Simplified Chinese, Traditional Chinese, English; switch in Settings or on first launch
- **Dark Mode** — Full light / dark / system-follow support
- **Font Size** — Three sizes (small / medium / large); no Activity restart required
- **Primary Currency** — HKD / CNY / USD; all amounts and notifications follow this setting
- **Swipe Gestures** — Swipe left to delete, right to edit (configurable confirm dialog; 40% threshold to prevent accidental triggers)
- **Persistent Notification** — Status bar widget showing live budget progress
- **Budget Alerts** — Push notification at 80% and 100% usage; once per day maximum
- **New User Onboarding** — 7-step animated highlight overlay guiding first-time users
- **Data Export** — CSV export (UTF-8 BOM, Excel compatible)
- **Category Management** — Custom categories with icons or gallery photos; restore defaults

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin (Coroutines + Flow) |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM + UDF (ViewModel + Repository) |
| Local DB | Room (SQLite) v16 |
| Preferences | DataStore |
| Charts | Vico Chart Library 2.0.0 |
| Image Loading | Coil |
| Auth | Firebase Authentication |
| Cloud DB | Firestore (asia-east2) |
| Backend | FastAPI (Python), deployed on Azure VM |
| AI Model | GLM-5 via Alibaba Cloud DashScope |
| Stock & FX Data | yfinance |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 16 (API 36) |

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
- Android SDK with API 24–36

```bash
# Clone the repository
git clone https://github.com/pure-white189/financial-app.git
cd financial-app

# Open in Android Studio, let Gradle sync, then run on a device or emulator
```

> **Note:** AI and sync features require backend connectivity and a Firebase project. The production backend is already running — no extra setup needed for testers.

---

## ☁️ Backend

The backend is deployed on an Azure VM (Switzerland North) and runs 24/7. AI features, stock prices, and exchange rates all go through this server.

**Base URL:** `http://20.199.169.108`

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/` | GET | — | Health check |
| `/parse-expense?text=...&lang=...` | GET | Bearer | Natural language expense parsing |
| `/analyze-expenses` | POST | Bearer | Generate monthly AI analysis report |
| `/stock-prices?symbols=...` | GET | — | Fetch live stock prices (batch) |
| `/exchange-rate?from_currency=...&to_currency=...` | GET | — | Live exchange rate (yfinance) |
| `/recommendations` | GET | — | Personalised recommendation content library |
| `/redeem-code` | POST | Bearer | Redeem Pro activation code |
| `/subscription-status` | GET | Bearer | Query subscription plan and expiry |
| `/usage-status` | GET | Bearer | Query AI usage counters |
| `/redeem-tokens` | POST | Bearer | Exchange tokens for AI quota |
| `/token-balance` | GET | Bearer | Current token balance |
| `/check-in` | POST | Bearer | Daily check-in (server-side dedup + token award) |
| `/unlock-achievement` | POST | Bearer | Unlock achievement (server-side dedup + token award) |
| `/check-in-status` | GET | Bearer | Check-in state, streak, and balance |

### AI Rate Limits

| User type | Natural language parsing | Monthly analysis |
|-----------|--------------------------|-----------------|
| Guest / not logged in | ✗ | ✗ |
| Free | 10 / day | 2 / month |
| Pro | Unlimited | Unlimited |

All users are capped at 10 requests/minute. Users can redeem tokens to extend their quota beyond the free limit.

---

## 📁 Project Structure

```
app/src/main/java/com/example/myapplication/
├── data/
│   ├── AppDatabase.kt                # Room v16, all 15 migrations
│   ├── Category.kt / CategoryDao.kt
│   ├── Expense.kt / ExpenseDao.kt
│   ├── ExpenseTemplate.kt / ExpenseTemplateDao.kt
│   ├── Loan.kt / LoanDao.kt
│   ├── SavingGoal.kt / SavingGoalDao.kt
│   ├── Stock.kt / StockDao.kt
│   ├── MonthlyIncome.kt / MonthlyIncomeDao.kt
│   ├── CheckIn.kt / CheckInDao.kt
│   ├── Achievement.kt / AchievementDao.kt
│   ├── TokenTransaction.kt / TokenTransactionDao.kt
│   ├── AiReport.kt / AiReportDao.kt
│   ├── ExpenseRepository.kt
│   ├── CheckInRepository.kt
│   ├── AchievementRepository.kt
│   ├── SyncRepository.kt             # Firestore bidirectional sync
│   ├── FirestoreMappers.kt
│   ├── ThemePreferences.kt           # DataStore settings
│   ├── NotificationHelper.kt
│   └── AiExpenseParser.kt            # Backend API client
│
├── ui/
│   ├── HomePage.kt
│   ├── RecordPage.kt
│   ├── EditExpensePage.kt
│   ├── AnalysisPage.kt
│   ├── DebtPage.kt
│   ├── SavingGoalPage.kt
│   ├── StockPage.kt
│   ├── IncomePage.kt
│   ├── CheckInPage.kt
│   ├── AiReportHistoryPage.kt
│   ├── SettingsPage.kt
│   ├── AccountPage.kt
│   ├── CategoryManagementPage.kt
│   ├── ExportPage.kt
│   ├── LanguageSelectionPage.kt      # First-launch language picker
│   ├── SyncViewModel.kt
│   ├── CheckInViewModel.kt
│   └── components/
│       ├── FeatureHighlightOverlay.kt
│       └── RecommendationCard.kt
│
├── utils/
│   ├── LanguageManager.kt            # Language persistence & switching
│   ├── CategoryDisplayName.kt
│   ├── TemplateDisplayName.kt
│   ├── RecommendationEngine.kt       # Local rule engine for recommendations
│   └── CsvExportHelper.kt
│
├── ExpenseViewModel.kt
├── AuthViewModel.kt
├── MainActivity.kt                   # AppCompatActivity (required for locale API)
├── FinanceApplication.kt
├── BottomNavItem.kt
└── DateUtils.kt
```

---

## 📦 Database Schema

| Table | Added | Description |
|-------|-------|-------------|
| `categories` | v1 | Expense categories (default + custom); cloud sync from v16 |
| `expenses` | v1 | Individual expense records; multi-currency fields from v14 |
| `expense_templates` | v2 | Quick-record templates; cloud sync from v16 |
| `loans` | v4 | Debt / lending records; multi-currency fields from v14 |
| `saving_goals` | v5 | Savings targets with progress |
| `stocks` | v6 | Stock portfolio holdings; currency field from v15 |
| `monthly_income` | v10 | Monthly income records |
| `check_ins` | v11 | Daily check-in records |
| `achievements` | v11 | Achievement unlock state |
| `token_transactions` | v11 | Token transaction history (display only; balance held server-side) |
| `ai_reports` | v12 | AI monthly analysis report cache |

All migrations use explicit `Migration` objects — no destructive migration on upgrade.

| Migration | Changes |
|-----------|---------|
| v6 → v7 | Cloud sync fields on expenses / loans / saving_goals / stocks |
| v7 → v8 | Category key column for multi-language support |
| v8 → v9 | Default template names localised to English keys |
| v9 → v10 | Add `monthly_income` table |
| v10 → v11 | Add `check_ins`, `achievements`, `token_transactions` tables |
| v11 → v12 | Add `ai_reports` table |
| v12 → v13 | Refactor `ai_reports`: auto-increment PK, support multiple records per month |
| v13 → v14 | Add `originalAmount`, `originalCurrency`, `exchangeRate` to expenses / loans |
| v14 → v15 | Add `currency` field to stocks |
| v15 → v16 | Add cloud sync fields to categories and expense_templates |

---

## ⚠️ Known Limitations

- **Backend uses HTTP** — No HTTPS / custom domain configured (out of scope for this project)
- **A-share symbols** — Enter without suffix (e.g. `600519`, not `600519.SS`); suffix is auto-appended
- **Per-minute rate limit counter** — Stored in memory; resets on backend restart (expected behaviour; daily/monthly quotas persist in SQLite)

---

## 🗺️ Roadmap

- [x] Azure VM backend deployment
- [x] Firebase Authentication (email + Google Sign-In + Guest mode)
- [x] Cloud data sync (Firestore, bidirectional LWW, soft delete)
- [x] Proper Room database migrations (v1 → v16, zero destructive upgrades)
- [x] Multi-language support (Simplified Chinese / Traditional Chinese / English)
- [x] Income & balance tracking with savings transfer
- [x] Achievements & daily check-in system with token rewards
- [x] Pro subscription via activation codes (no Google Play required)
- [x] AI natural language expense parsing with date / time / currency extraction
- [x] AI monthly analysis reports with local persistence and history browser
- [x] Personalised recommendation engine (local rules + backend content library)
- [x] Multi-currency support with live exchange rates and locked-in conversion
- [x] Font size customisation (3 levels, no Activity restart)
- [x] 7-step new user onboarding overlay
- [ ] Home screen widget
- [ ] HTTPS / custom domain

---

## 🤖 Development Approach

This project uses an AI-assisted development pipeline:

- **Claude** — Architecture decisions, system design, database schema planning, debugging root-cause analysis
- **GitHub Copilot Agent** — Code generation and UI implementation based on Claude-authored prompts

All feature requirements, UX design, integration, testing, and iteration were driven by the developer. The two AI tools handle different layers of the stack, with a deliberate separation of concerns that significantly reduces the risk of broad unintended changes.

---

## 📄 License

This project is for academic purposes only.
