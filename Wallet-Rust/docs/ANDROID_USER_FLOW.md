# Android Wallet App - User Flow Documentation

**Document Version:** 1.0  
**Date:** February 25, 2026  
**Purpose:** Android App Screens Development Guide

---

## Executive Summary

This document outlines the complete user journey for the multi-chain wallet Android application. It describes all major screens, user interactions, and transaction flows from initial onboarding through daily wallet operations.

---

## Table of Contents

1. [Phase 1: Authentication & Onboarding](#phase-1-authentication--onboarding)
2. [Phase 2: Main Dashboard](#phase-2-main-dashboard)
3. [Phase 3: Core Features](#phase-3-core-features)
4. [Phase 4: Settings & Security](#phase-4-settings--security)
5. [Phase 5: Transaction Flow](#phase-5-transaction-flow)
6. [Android Screens Checklist](#android-screens-checklist)
7. [Key Features by Screen](#key-features-by-screen)

---

## Phase 1: Authentication & Onboarding

### 1.1 Welcome Screen
**Purpose:** First screen users see upon opening the app

**Elements:**
- App logo/branding
- Title: "Welcome to Earth Wallet"
- Three main action buttons
- Subtitle about multi-chain support

**User Actions:**
1. **"Create New Wallet"** - For first-time users
2. **"Import Wallet"** - For users with existing wallets
3. **"Quick Login (Google/Apple)"** - OAuth sign-in

---

### 1.2 Create Wallet Flow

**Screen 1: Generate Recovery Phrase**
- System generates 12-word mnemonic
- Display words with copy/write-down option
- Warning: "Never share your recovery phrase"
- User confirms they've written it down

**Screen 2: Verify Recovery Phrase**
- Show random words from the phrase
- User must select correct words in order
- Ensures user understands phrase importance

**Screen 3: Security Setup**
- Set 6-digit PIN
- Confirm PIN
- Enable biometrics (Fingerprint/Face ID)
- Option: "Enable Biometrics"

**Result:** Wallet successfully created

---

### 1.3 Import Wallet Flow

**Option A: Cloud Recovery**
- Click "Import from Cloud"
- Authenticate with Google/Apple
- System fetches encrypted wallet blob
- Enter PIN to decrypt
- Wallet restored

**Option B: Manual Import**
- Enter 12-word recovery phrase manually
- Select word count (12/24 words)
- Verify each word
- Create PIN & setup biometrics

---

### 1.4 OAuth Authentication (Google/Apple)

**Google Sign-In Flow:**
1. User clicks "Continue with Google"
2. Google login popup appears
3. User selects Google account
4. App receives authentication token
5. Routes to Create or Import wallet (if cloud backup exists)

**Apple Sign-In Flow:**
1. User clicks "Continue with Apple"
2. Apple authentication popup appears
3. User confirms with Face ID/Touch ID
4. App receives authentication token
5. Routes to Create or Import wallet (if cloud backup exists)

---

## Phase 2: Main Dashboard

### 2.1 Home Screen (After Successful Login)

**Top Section:**
- User greeting message
- Wallet address badge
- Network indicator (Mainnet/Testnet)

**Balance Card:**
- Total portfolio value in USD
- Price change indicator (↑/↓ with percentage)
- Balance in crypto (BTC, ETH, SOL)

**Quick Actions (Horizontal Scroll):**
- 🔄 Swap - Exchange tokens
- 📤 Send - Transfer funds
- 💰 Receive - Show QR code
- 📈 Earn - Staking/Mining
- 🔍 Discover - Browse dApps

**Recent Transactions (List):**
- Last 5 transactions
- Transaction type (Send/Receive/Swap)
- Amount & timestamp
- Status indicator (Pending/Success/Failed)

**Bottom Navigation (5 Tabs):**
1. Home (Dashboard) - Selected by default
2. Portfolio (Assets list)
3. Send (Transfer screen)
4. Swap (Exchange screen)
5. Settings (Configuration)

---

## Phase 3: Core Features

### 3.1 Portfolio Screen

**Header:**
- "My Assets" title
- Total portfolio value
- Filter/Sort options

**Asset List View:**
- Token icon
- Token name & symbol
- Balance in both crypto and USD
- Percentage of portfolio
- Price change 24h

**Per-Token Interactions:**
- **Tap Token Card:**
  - Navigate to Asset Details
  - See full transaction history
  - View token analytics

**Action Buttons (Bottom):**
- "Add Token" button
- "Import Custom Token" button

---

### 3.2 Asset Details Screen

**Token Information Card:**
- Token icon (large)
- Token name & symbol
- Current price
- Market cap
- 24h change
- Trading volume

**Balance Section:**
- Your balance
- USD equivalent
- Network/blockchain indicator

**Quick Actions:**
- 📤 Send button
- 🔄 Swap button
- 📊 View Chart

**Transaction History:**
- Complete list of all token movements
- Filter by type (Send/Receive/Swap)
- Date indicators
- Amount & status

**Add Custom Token (Modal):**
- Contract address input field
- Token name auto-fill
- Decimal places
- Add to wallet confirmation

---

### 3.3 Send/Transfer Screen

**Step 1: Enter Recipient**
- "Recipient Address" input field
- Address validation
- QR scan button (camera icon)
- "Use Recent" - Recent addresses dropdown
- "Add to Contacts" option

**Step 2: Select Token & Amount**
- Token dropdown/selector
- Available balance displayed
- Amount input field
- "Send Max" button
- Fee display

**Step 3: Network Selection**
- Current network highlighted
- Select which blockchain:
  - Ethereum
  - Solana
  - Bitcoin
  - Binance Smart Chain
  - Others

**Step 4: Review Transaction**
- Recipient address (truncated)
- Token & amount
- Network fee (estimated gas)
- Total cost
- Slippage warning (if applicable)

**Step 5: Biometric/PIN Authentication**
- Fingerprint or Face ID prompt
- OR Enter 6-digit PIN
- Success confirmation

**Result Screen:**
- ✅ "Transaction Sent Successfully"
- Transaction hash (clickable to explorer)
- "View in Explorer" button
- "Done" - Return to home
- "Send Another" - New transaction

---

### 3.4 Swap/Exchange Screen

**Step 1: Select "From" Token**
- Token list with balances
- Search functionality
- Recently used tokens
- All available tokens

**Step 2: Select "To" Token**
- Token list filtered by compatible chains
- Direct swap vs. bridged swap option
- Different DEX options if multiple available

**Step 3: Enter Amount**
- "From Amount" input
- Real-time "To Amount" calculation
- Price impact indicator
- Slippage tolerance (default 0.5%)
- Advanced options

**Step 4: Get Quote**
- Show best route
- Fee breakdown
- Price comparison

**Step 5: Review Swap**
- From token & amount
- To token & estimated amount
- Price per token
- Total fees
- Exchange rate
- Min received (with slippage)

**Step 6: Biometric/PIN Authentication**
- Same as send flow
- Confirmation
- if the user enable from setting (biometric)

**Result:**
- ✅ "Swap Completed"
- Transaction hash
- From/To amounts confirmed
- "Done" or "Swap Another"

---

### 3.5 Staking/Mining/Earn Screen

**Available Earn Options:**
- Staking opportunities
  - Platform name
  - APY/APR
  - Min/Max stake
  - Lock-up period
  - Available liquidity

- Mining programs
  - Pool name
  - Hash rate requirement
  - Expected rewards
  - Difficulty level

**Select Earn Type:**
- Card view of opportunities
- Tap to see details

**Earnings Detail Screen:**
- Token to stake/mine
- Amount input
- Lock duration
- Estimated rewards
- Risk factors

**Confirm & Execute:**
- Review details
- Authenticate (PIN/Biometric)
- Execute transaction

**Active Earnings View:**
- Current stakes
- Accumulated rewards
- Time remaining
- Claim rewards button
- Unstake options

---

### 3.6 Ecosystem/Discover Screen

**Browse dApps & Protocols:**
**Protocol Card:**
- Protocol icon
- Name & description
- TVL (Total Value Locked)
- APY where applicable
- "Explore" button

**Tap Protocol:**
- Open in-app browser or link
- Connect wallet (one-click)
- Show transaction preview
- Execute transaction flow

---

## Phase 4: Settings & Security

### 4.1 Settings Home Screen

**Account Section:**
- User email (if OAuth connected)
- Wallet address
- Edit account button

**Security Section:**
- 🔐 Security & Privacy
- 🔑 Biometrics Management
- 🔒 Cloud Backup & Recovery
- 📋 Daily Spending Limits

**App Section:**
- 🌐 Language & Region
- 🎨 Theme (Light/Dark)
- 🔔 Notifications
- ⚙️ Advanced Settings

**Support Section:**
- 📖 Help & Documentation
- 🐛 Report Issue
- 📞 Contact Support
- 📝 View Audit Logs

---

### 4.2 Security & Privacy Screen

**Password/PIN Management:**
- Change PIN button
- Current PIN verification required
- New PIN creation
- Confirm PIN

**Biometrics Setup:**
- Toggle Biometric auth on/off
- Enrolled biometrics list
- Add/remove biometrics
- Test biometric authentication

**Session Management:**
- Current session info
- Auto-lock timeout (5/10/15/30 min)
- Log out button

**Security Image:**
- Set security image for visual confirmation
- Choose from gallery or predefined images
- Shows before PIN entry

---

### 4.3 Cloud Recovery & Backup

**Cloud Backup Status:**
- Last backup timestamp
- Backup provider (Google/Apple)
- Backup size
- Status indicator

**Manual Backup:**
- "Backup Now" button
- Initiates cloud backup
- Requires PIN confirmation

**Recovery Options:**
- "Connect to Cloud" - Link existing account
- "View Recovery Status" - Check backup health
- "Manual Recovery" - Enter recovery phrase
- "Delete Backup" - Remove cloud backup (warning)

**Cloud Recovery Settings:**
- Choose encryption mode:
  - Server-held KEK (easier)
  - Client-passphrase (more secure)
- Cloud provider selection

---

### 4.4 Daily Spending Limits

**Set Daily Limit:**
- Current limit display
- Edit button
- Input amount (in USD or crypto)
- Daily reset time selector
- Enable/disable toggle

**Current Usage:**
- Amount spent today
- Remaining amount
- Percentage bar
- Reset countdown timer

**Transaction Restrictions:**
- Limits apply to Send transactions
- High-value transactions require additional auth
- Warnings at 75%, 90%, 100%

---

### 4.5 Activity & Audit Logs

**Transaction History:**
- All transactions (Send/Receive/Swap/Stake)
- Filter by coin name
- Search by address/hash

**Audit Logs:**
- All account activities
- Login attempts
- Settings changes
- PIN changes
- Device additions
- Biometric changes

**Log Entry Details:**
- Timestamp
- Action type
- IP address/device
- Success/failure status
- Details modal

---

## Phase 5: Transaction Flow

### 5.1 Generic Transaction Authorization Flow

```
User Action (Send/Swap/Stake)
        ↓
Enter Details
        ↓
Review Transaction
        ↓
Authentication Required
        ├─ Option 1: Biometric (Face ID/Fingerprint)
        ├─ Option 2: PIN (6 digits)
        └─ Option 3: Both (for high-value)
        ↓
Transaction Broadcast
        ↓
Success/Error Response
        ↓
Show Result Screen
```

### 5.2 High-Value Transaction Protection

**Triggers:**
- Amount > Daily limit
- Amount > $5,000 USD
- Unusual recipient address
- Multi-signature transaction

**Additional Security:**
- Require both Biometric AND PIN
- Security image confirmation
- Email confirmation (if available)
- Delay transaction (optional)

### 5.3 Error Handling

**Network Errors:**
- "No internet connection" message
- Offline mode indicator
- Retry button
- Queue transaction locally

**Insufficient Balance:**
- Display available balance
- Show required amount
- Suggest reducing amount
- Option to add funds

**Invalid Address:**
- Highlight address field
- Show validation error
- Suggest similar addresses (if found)
- QR scan option

**Transaction Failed:**
- Show error message
- Display error code/reason
- "Retry" button
- "Contact Support" option

---

## Android Screens Checklist

### Authentication & Onboarding
- [ ] Welcome Screen
- [ ] Create Wallet Screen
- [ ] Verify Recovery Phrase Screen
- [ ] Security Setup Screen (PIN + Biometric)
- [ ] Import Wallet Screen
- [ ] OAuth Callback Handler
- [ ] Wallet Creation Success Screen

### Main Dashboard
- [ ] Home/Dashboard Screen
- [ ] Portfolio/Assets List Screen
- [ ] Recent Transactions Widget
- [ ] Bottom Navigation Bar
- [ ] Quick Actions Bar

### Portfolio Management
- [ ] Asset Details Screen
- [ ] Asset Chart & Analytics
- [ ] Add Custom Token Screen
- [ ] Token Search Screen
- [ ] Transaction History (per token)

### Transactions
- [ ] Send Address Input Screen
- [ ] Send Amount & Fee Screen
- [ ] Send Review Screen
- [ ] Send Success Screen
- [ ] Swap From Token Selection
- [ ] Swap To Token Selection
- [ ] Swap Amount Input Screen
- [ ] Swap Review Screen
- [ ] Swap Success Screen

### Earning
- [ ] Staking/Mining Browse Screen
- [ ] Staking Details Screen
- [ ] Active Stakes Dashboard
- [ ] Rewards Claim Screen
- [ ] Unstaking Screen

### Ecosystem
- [ ] dApp Discovery Screen
- [ ] Protocol Details Screen
- [ ] In-app Browser
- [ ] Transaction Preview Modal

### Settings
- [ ] Settings Home Screen
- [ ] Security Management Screen
- [ ] PIN Change Screen
- [ ] Biometrics Setup Screen
- [ ] Cloud Recovery Screen
- [ ] Backup Status Screen
- [ ] Daily Limits Configuration
- [ ] Activity Log Screen
- [ ] Audit Log Screen
- [ ] App Settings Screen
- [ ] Support & Help Screen

### Modals & Dialogs
- [ ] PIN Entry Modal
- [ ] Biometric Prompt Dialog
- [ ] QR Code Scanner
- [ ] Address Book Modal
- [ ] Confirmation Dialog
- [ ] Error Dialog
- [ ] Loading Spinner
- [ ] Toast Notifications

---

## Key Features by Screen

### Home Dashboard
**Priority:** CRITICAL
**User Goals:**
- Quick overview of portfolio
- Initiate common transactions
- Monitor recent activity
- Check wallet balance

**Key Interactions:**
- Tap to navigate to other screens
- Swipe for quick actions
- Pull-to-refresh for updates
- Long-press for more options

---

### Portfolio Screen
**Priority:** CRITICAL
**User Goals:**
- See all assets
- Track individual tokens
- View holdings over time
- Manage token list

**Key Interactions:**
- Search tokens
- Filter by network
- Sort by value/change
- Tap to see details

---

### Send Screen
**Priority:** CRITICAL
**User Goals:**
- Transfer funds securely
- Choose correct recipient
- Monitor fees
- Confirm before sending

**Key Interactions:**
- Address input/paste/scan
- Fee estimation
- Max button

---

### Swap Screen
**Priority:** HIGH
**User Goals:**
- Exchange tokens efficiently
- Compare rates
- Check price impact
- Execute swap securely

**Key Interactions:**
- Token selection
- Amount input
- Slippage adjustment

---

### Settings Screen
**Priority:** HIGH
**User Goals:**
- Secure wallet
- Backup wallet
- Manage preferences
- View activity

**Key Interactions:**
- Enter PIN for sensitive actions
- Toggle settings
- View history
- Download data

---

## User Personas

### 1. Casual Investor
- Uses primarily for holding assets
- Prefers simple interface
- Lower transaction frequency

### 2. Active Trader
- Frequent swaps
- High transaction volume
- Advanced features important
- Watches real-time prices

### 3. DeFi Enthusiast
- Staking activities
- Multi-chain interactions
- Uses ecosystem

### 4. Security-First User
- Prioritizes privacy
- Cloud recovery backup
- Audit logs review
- Custom daily limits

---

## Navigation Architecture

```
Welcome Screen
├─ Create Wallet
│  ├─ Recovery Phrase
│  ├─ Verify Phrase
│  └─ Security Setup
├─ Import Wallet
│  ├─ Cloud Recovery
│  ├─ Manual Import
│  └─ Security Setup
└─ OAuth Login
   ├─ Google
   └─ Apple

Home (Authenticated)
├─ Dashboard
├─ Portfolio
│  └─ Asset Details
│     └─ Add Token
├─ Send
│  └─ Transaction Review
├─ Swap
│  └─ Swap Review
├─ Earn
│  └─ Staking Details
├─ Discover
│  └─ dApp Details
└─ Settings
   ├─ Security
   ├─ Biometrics
   ├─ Cloud Backup
   ├─ Daily Limits
   ├─ Activity Log
   └─ Profile
```

---

## Data Flow & Synchronization

### Real-time Updates
- Price feeds (every 10-30 seconds)
- Transaction status (on-chain polling)
- Balance updates (network events)
- Notification alerts

### Offline Support
- Queue transactions locally
- Sync when connection restored

### Privacy
- No personal data sent to backend (unless OAuth)
- Private keys never leave device
- Encrypted cloud backup

---

## Security Considerations

### PIN/Biometric Policy
- Required for all sensitive operations:
  - Send/Swap transactions
  - Settings changes
  - PIN changes
  - Audit log access
  - Cloud backup management

---

---

## Testing Scenarios

### Happy Path
- New user → Create wallet → Send → Success
- Import wallet → Verify → Dashboard
- OAuth login → Import backed-up wallet
- Swap tokens → Verify → Complete

### Error Cases
- Invalid address → Show error → Retry
- Insufficient balance → Suggest max amount
- Network timeout → Offline mode → Sync
- Failed transaction → Show reason → Retry option

### Edge Cases
- Network switching mid-transaction
- Biometric fails → PIN fallback
- Device locked during signing
- Cloud backup expired
- Recovery phrase verification failures

---


