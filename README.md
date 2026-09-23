[README.md](https://github.com/user-attachments/files/32581998/README.md)

# Plastic Rex

> \*\*On-device EMI aggregation \& safe borrowing companion for Android\*\*

Plastic Rex is an Android app that helps users understand their existing EMI commitments before taking on new debt. It reads relevant SMS messages on-device, extracts EMI information, stores the results locally, and calculates a repayment-to-income ratio against a user-defined safe threshold.

The project is designed as a lightweight MVP with a strong privacy-first approach: the core workflow does not require an internet connection, and SMS/financial data is processed locally on the device.

\---

## Features

### 📩 EMI detection from SMS

* Reads SMS messages after the user grants SMS permission.
* Filters out unrelated messages such as recharge, telecom, OTP, and payment notifications.
* Detects loan/EMI-related messages using rule-based parsing.
* Extracts useful fields such as:

  * Lender
  * EMI amount
  * Due date when available
  * Tenure / remaining months when available

### 💰 Repayment-to-income tracking

* Stores monthly income locally.
* Lets the user set a safe EMI threshold, such as 40% of monthly income.
* Calculates the current repayment-to-income ratio.
* Shows remaining EMI capacity under the selected threshold.
* Displays a risk state based on how close the user is to the limit.

### ➕ Manual EMI entry

* Add an EMI manually when it was not detected from SMS.
* Specify lender, EMI amount, and an optional due date.

### ✅ EMI management

* Mark active EMIs as completed.
* Delete EMI records.
* Deduplicate similar detected loans to reduce repeated entries from multiple SMS messages.

### 🧮 New-loan safety check

* Enter a proposed EMI amount.
* Calculates the projected repayment-to-income ratio.
* Warns when the projected ratio exceeds the user's selected threshold.

### 🔒 Local-first privacy

* Core features work without internet access.
* SMS and EMI data are stored locally using Room.
* User income and threshold preferences are stored locally using DataStore Preferences.

\---

## Tech Stack

|Technology|Purpose|
|-|-|
|**Kotlin**|Application language|
|**Jetpack Compose**|UI|
|**Material 3**|UI components and theming|
|**Android ViewModel**|UI state and business logic|
|**Kotlin Coroutines / Flow**|Asynchronous work and reactive state|
|**Room**|Local EMI database|
|**KSP**|Room code generation|
|**DataStore Preferences**|Local user settings|
|**Android SMS APIs**|Reading SMS inbox messages|

\---

## Architecture

Plastic Rex follows a simple layered structure:

```text
                    ┌─────────────────────┐
                    │   Jetpack Compose   │
                    │        UI           │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │    MainViewModel    │
                    │ State + App Logic   │
                    └───────┬───────┬─────┘
                            │       │
                ┌───────────┘       └────────────┐
                ▼                                ▼
       ┌─────────────────┐              ┌─────────────────┐
       │ SmsParser       │              │ UserPreferences │
       │ SMS → EMI data  │              │    DataStore    │
       └────────┬────────┘              └─────────────────┘
                │
                ▼
       ┌─────────────────┐
       │ LoanRepository  │
       └────────┬────────┘
                │
                ▼
       ┌─────────────────┐
       │ Room Database   │
       │   loans table   │
       └─────────────────┘
```

### Project structure

```text
app/src/main/java/com/cos/plasticrex/
├── MainActivity.kt
├── data/
│   ├── Loan.kt
│   ├── LoanDao.kt
│   ├── AppDatabase.kt
│   ├── LoanRepository.kt
│   └── UserPreferences.kt
├── parser/
│   └── SmsParser.kt
├── viewmodel/
│   └── MainViewModel.kt
└── ui/
    ├── HomeScreen.kt
    ├── SettingsScreen.kt
    ├── CheckLoanScreen.kt
    └── AddLoanScreen.kt
```

\---

## How SMS detection works

Plastic Rex currently uses a **rule-based parser**, rather than a machine-learning NLP model.

The parser roughly follows this pipeline:

```text
SMS inbox
   ↓
Read SMS messages
   ↓
Relevance / exclusion filtering
   ↓
Loan \& EMI keyword detection
   ↓
Amount / date / lender / tenure extraction
   ↓
Create Loan record
   ↓
Deduplicate
   ↓
Store locally in Room
```

The filtering layer uses strong EMI/loan signals and exclusions for common non-loan messages such as telecom recharges, OTPs, and generic payment notifications.

\---

## Requirements

* Android Studio with Kotlin support
* Android device running **API 26 / Android 8.0 or newer**
* A physical Android phone is recommended for SMS testing
* USB debugging enabled for development

The MVP was designed around a minimum SDK of **26**.

\---

## Getting Started

### 1\. Clone the repository

```bash
git clone https://github.com/cos1864/PlasticRex.git
cd PlasticRex
```

### 2\. Open in Android Studio

Open the project in Android Studio and allow Gradle to sync.

### 3\. Build and run

Connect a physical Android device with USB debugging enabled and run the `app` configuration.

### 4\. Grant SMS permission

When Plastic Rex requests access to SMS, grant the required permission.

The manifest requires:

```xml
<uses-permission android:name="android.permission.READ\_SMS" />
<uses-permission android:name="android.permission.RECEIVE\_SMS" />
```

### 5\. Configure income and threshold

Open **Settings** and enter:

* Monthly income
* Safe EMI threshold (for example, `40` for 40%)

### 6\. Import EMI messages

Tap **Refresh SMS** and the app will scan the SMS inbox for relevant EMI/loan messages.

### 7\. Test a new loan

Open **Check New Loan Safety**, enter a proposed EMI, and view the projected repayment-to-income ratio.

\---

## Testing with an SMS

For development, use a message that is delivered as a normal **SMS**, not an RCS/Chat message.

Example:

```text
Your HDFC loan EMI of INR 2250 is due on 30/09/2026.
```

Then open Plastic Rex and select **Refresh SMS**.

> \*\*Note:\*\* RCS messages are not ordinary SMS records and may not appear in the SMS inbox provider queried by the app.

\---

## Permissions

Plastic Rex currently requests SMS access because the MVP reads the device's SMS inbox.

### Required

* `READ\_SMS` — read SMS messages for EMI detection
* `RECEIVE\_SMS` — declared for SMS-related functionality

Only grant these permissions on devices where you are comfortable allowing the app to access SMS data.

\---

## Privacy

Plastic Rex is designed around a local-first data model.

* SMS parsing happens on-device.
* EMI records are stored locally using Room.
* Income and threshold preferences are stored locally using DataStore Preferences.
* The MVP does not require internet access for its core features.

The app is **not a financial advisor** and its risk/threshold calculations are informational. Users remain responsible for deciding whether to take on additional borrowing.

\---

## Current Limitations

* Rule-based SMS parsing can miss unusual lender message formats.
* RCS/Chat messages are not equivalent to ordinary SMS and are not handled by the current SMS-provider approach.
* Some modern lenders may primarily use app notifications instead of SMS.
* Due dates may be unavailable when the source SMS does not contain one.
* The current MVP uses a local database and does not provide cloud synchronization.

\---

## Future Improvements

Planned directions include:

1. **NotificationListenerService** for lenders that send notifications instead of SMS.
2. Better deduplication and loan identity tracking.
3. Automatic recognition of paid EMI confirmation messages.
4. Calendar-based EMI visualization.
5. On-device NLP/ML for more flexible SMS understanding.
6. Database encryption.
7. CSV/PDF export while keeping processing local.
8. Hindi and other Indian-language support.

\---

## Why Plastic Rex?

People often receive loan and EMI information across many messages, making it difficult to see the total repayment burden in one place.

Plastic Rex turns those scattered messages into a simple local view of:

**what you owe → when it is due → how much of your income is already committed → how a new EMI changes the picture.**

\---

## Project Status

**MVP / Prototype**

The project is currently focused on a working Android MVP for on-device EMI aggregation and safe-borrowing calculations.

\---

