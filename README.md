# 🚫 DOOMSCROLL DESTROYER

> *Your personal dopamine bouncer. Instagram gets 20 minutes a day. After that — it's dead to you.*

---

## WHAT THIS APP DOES

- **20 minutes per day** of Instagram. That's your budget. No exceptions.
- **Every 1 cumulative minute open** → 30-minute lockout. 15sec + 30sec + 16sec = locked.
- **When locked**, a full-screen roast overlay **blasts on top of Instagram** the moment you try to open it. Funny message. Shame. Countdown.
- **After 20 mins total** → Instagram is permanently blocked until **midnight**.
- **Survives restarts and reinstalls** (state is backed up via Android backup).
- **Resets at 12:00 AM** automatically every day.

---

## HOW TO GET THE APK (no Android Studio needed)

### Step 1 — Make a free GitHub account
Go to **https://github.com** → Sign Up → verify your email.

---

### Step 2 — Create a new repository
1. Click the **+** icon (top right) → **New repository**
2. Name it: `doomscroll-destroyer`
3. Set it to **Public** (required for free GitHub Actions builds)
4. ✅ Check **"Add a README file"**
5. Click **Create repository**

---

### Step 3 — Upload all the project files

**Option A — GitHub Web UI (easiest, no terminal needed):**

1. Open your new repository on GitHub
2. Click **"Add file"** → **"Upload files"**
3. Drag and drop the entire `DoomscrollDestroyer` folder contents
   *(everything inside it: `app/`, `.github/`, `build.gradle`, `settings.gradle`, `gradlew`, `.gitignore`)*
4. Scroll down → click **"Commit changes"**

**Option B — If you have git installed:**
```bash
cd DoomscrollDestroyer
git init
git add .
git commit -m "initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/doomscroll-destroyer.git
git push -u origin main
```

---

### Step 4 — Watch it build automatically

1. Go to your repository on GitHub
2. Click the **"Actions"** tab at the top
3. You'll see **"Build APK"** workflow running (yellow spinning circle)
4. Wait ~3–5 minutes for it to finish (turns green ✅)

If it fails (red ✗), see the **Troubleshooting** section below.

---

### Step 5 — Download your APK

1. Click the completed workflow run (the green ✅ row)
2. Scroll to the bottom of the page
3. Under **"Artifacts"**, click **"DoomscrollDestroyer-debug"**
4. A ZIP downloads → open it → inside is **`app-debug.apk`**

---

### Step 6 — Install on your Android phone

1. **Send the APK to your phone** (WhatsApp, Google Drive, email — any way)
2. On your phone, open the APK file
3. Android will say *"Install from unknown sources"* → tap **Settings** → enable it for your file manager/browser → go back → tap **Install**
4. Open **Doomscroll Destroyer**
5. Follow the **3-step setup screen** inside the app:
   - Enable Accessibility Service
   - Allow Display Over Other Apps
   - Grant Usage Stats Access

That's it. Instagram is now on a leash. 🔒

---

## FIRST TIME APP SETUP (inside the app)

When you open the app for the first time, it shows a setup screen with 3 buttons:

| Step | What it does | How long it takes |
|------|-------------|-------------------|
| 1. Accessibility Service | Lets app detect when Instagram opens | 30 seconds |
| 2. Display Over Other Apps | Lets the roast screen appear on top | 10 seconds |
| 3. Usage Stats Access | Backup detection method | 10 seconds |

**All 3 are one-time. You never have to do this again.**

After granting all 3, the app takes you to the home screen and starts working immediately.

---

## HOW THE BLOCKING ACTUALLY WORKS

```
You open Instagram
        ↓
Is it blocked? (daily limit hit OR active lockout)
   YES → Roast overlay appears instantly on top of Instagram
         Instagram gets minimised to home screen
         You cannot use it
   NO  → Timer starts counting...
        ↓
        1 cumulative minute of use reached?
   YES → 30-minute lockout begins
         Roast overlay appears
         Every time you try to open Instagram for 30 mins → roast overlay again
   NO  → Continue using...
        ↓
        20 total minutes used today?
   YES → PERMANENT BLOCK until midnight
         Every single open attempt → roast overlay
         No matter what you do
   NO  → Keep going (you have budget left)
        ↓
       12:00 AM
        ↓
      RESET — 20 minutes restored, all lockouts cleared
```

---

## SURVIVING REINSTALL

The app backs up your daily state (time used, lockout status) via Android's auto-backup system. This means:

- **Uninstall + reinstall** → state is restored from backup → still blocked if you were blocked
- **Restart phone** → app auto-restarts via BootReceiver
- **Force-stop the app** → Accessibility Service continues running independently
- **Only escape**: Factory reset (but honestly, if you factory-reset your phone to check Instagram, you've already lost 😂)

---

## TROUBLESHOOTING

### Build fails with "SDK not found"
This shouldn't happen — GitHub Actions provides the Android SDK automatically. If it does, check the Actions log for the exact error and open a GitHub issue.

### "Access denied" when uploading files
Make sure your repository is **Public**. Private repos require a paid plan for GitHub Actions minutes beyond a small free quota.

### APK installs but blocker doesn't work
Make sure you completed all 3 permission steps in the app's setup screen. The **Accessibility Service** is the most important one — without it, the app can't detect Instagram.

### Instagram opens for a split second before the overlay appears
This is normal. The Accessibility Service fires on window state change, which takes ~100-200ms. The overlay appears very quickly after Instagram loads. This delay is unavoidable on Android.

### "Unknown sources" install blocked
On some phones (Samsung, Xiaomi, etc.) you need to enable installation from unknown sources *for the specific app you're using to open the APK* (e.g. Chrome, Files, WhatsApp). Go to Settings → Apps → [your file manager] → Install unknown apps → Allow.

---

## RE-TRIGGERING THE BUILD

If you want to rebuild (e.g. after making changes):
1. Go to **Actions** tab on GitHub
2. Click **"Build APK"** in the left sidebar
3. Click **"Run workflow"** button (top right of the workflow list)
4. Click the green **"Run workflow"** button in the popup
5. Wait ~3-5 mins → download new APK

---

## PROJECT STRUCTURE (for the curious)

```
DoomscrollDestroyer/
├── .github/workflows/build.yml          ← GitHub Actions build script
├── app/src/main/
│   ├── java/com/doomscroll/destroyer/
│   │   ├── MainActivity.kt              ← Home screen, live timers
│   │   ├── PermissionSetupActivity.kt   ← First-launch setup wizard
│   │   ├── service/
│   │   │   ├── InstagramWatcherService  ← Accessibility service (core detector)
│   │   │   └── BlockerForegroundService ← 1-second ticker, triggers lockouts
│   │   ├── overlay/
│   │   │   └── OverlayManager.kt        ← Draws roast screen OVER Instagram
│   │   ├── receiver/
│   │   │   └── Receivers.kt             ← Boot restart + midnight reset alarm
│   │   └── utils/
│   │       ├── BlockerState.kt          ← All timer logic, SharedPreferences
│   │       └── RoastContent.kt          ← 15+ roast messages, daily variants
│   ├── res/
│   │   ├── layout/                      ← All screen XML layouts
│   │   ├── font/                        ← Anton, Caveat, VT323, Yellowtail, Playfair
│   │   ├── drawable/                    ← Stickers, icons, progress bar styles
│   │   └── values/                      ← Colors, strings, themes
│   └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
└── gradlew
```

---

## WANT TO CUSTOMISE?

Open these files before uploading to GitHub:

**Change the daily limit (default 20 mins):**
→ `utils/BlockerState.kt` → line with `getInt(KEY_DAILY_LIMIT, 1200)` → change `1200` (seconds)

**Change the trip wire (default 1 min):**
→ `utils/BlockerState.kt` → line with `getInt(KEY_TRIP_SECONDS, 60)` → change `60`

**Change the lockout duration (default 30 mins):**
→ `utils/BlockerState.kt` → line with `getInt(KEY_LOCKOUT_DURATION, 1800)` → change `1800`

**Add/change roast messages:**
→ `utils/RoastContent.kt` → edit the `lockoutRoasts` or `dailyExhaustRoasts` lists

**Add TikTok blocking:**
→ `service/InstagramWatcherService.kt` → uncomment the TikTok package lines

---

*Built with ✊ and a genuine hatred of doomscrolling.*
