# SOH3IL Calendar — AI Coding Agent Rules & Engineering Constitution (AGENTS.md)

**Role & Authority:** This document is the mandatory engineering constitution for any AI Coding Agent operating on the `com.soh3il.calendar` repository. Every code generation, refactoring, bug fix, or UI modification MUST strictly comply with all rules below. Never bypass these constraints for convenience.

---

## 0. Core Invariants & Strict Rules (DO NOT CHANGE)

1. **NO EMOJIS IN USER INTERFACE OR OUTPUTS**:
   - The use of emojis anywhere in the app (including UI texts, Status Bar notifications, widgets, dialogs, buttons, logs, and changelogs) is STRICTLY PROHIBITED.
   - All notifications, widgets, and dialogs must use clean, professional Persian typography with Vazirmatn font.

2. **SWIPE GESTURE MAPPING (LOCKED)**:
   - In `CalendarScreen.kt`, month swipe gesture mapping is strictly locked:
     - Dragging right (`totalDragDistance > threshold`) -> `displayedDate = displayedDate.plusMonths(1)`
     - Dragging left (`totalDragDistance < -threshold`) -> `displayedDate = displayedDate.plusMonths(-1)`
   - Do NOT alter this direction.

3. **HEADER NAVIGATION BUTTONS (LOCKED)**:
   - In `CalendarHeader.kt`:
     - Right button (`Start` in RTL) = `onPreviousMonth` (`Icons.Default.ChevronRight`)
     - Left button (`End` in RTL) = `onNextMonth` (`Icons.Default.ChevronLeft`)
   - Do NOT alter these button positions or vectors.

4. **FLOATING ACTION BUTTON (FAB) REMOVED (LOCKED)**:
   - The calendar screen intentionally has no FAB. The primary add-reminder action is the expressive button below the selected-day details.
   - Do NOT reintroduce a floating action button without the user's direct instruction.

5. **TAB CHECKMARK REMOVAL (LOCKED)**:
   - In `CalendarScreen.kt`, `SegmentedButton` items for switching between "تقویم ماهانه" and "دستور کار (رویدادها)" must NOT display a checkmark.
   - Always set `icon = {}` on both `SegmentedButton` instances.

6. **HOLIDAY SELECTION COLOR (LOCKED)**:
   - When a holiday cell is selected on the calendar grid (`isSelected && isHoliday`), its background MUST be `FarsiHolidayRed` (`#EF4444`), NEVER `MaterialTheme.colorScheme.primary` (Blue).
   - Text color for selected cells must be `Color.White`.

7. **APP BRANDING & TYPOGRAPHY**:
   - App name is strictly `SOH3IL` and may change only on the user's direct instruction.
   - All numbers presented in Persian context must use `.toPersianDigits()`.
   - Persian & Arabic numeral input in all dialogs and text fields must use `toStandardDigits()` / `toEnglishDigits()` / `toIntStandardOrNull()` to ensure resilience across Iranian and Arabic keyboards.

8. **CODEBASE MODULARITY**:
   - Pure Kotlin 100%. No Java files.
   - Maintain deep modularity. No God classes or monolithic files exceeding ~350 lines.

---

## 1. Module Architecture & Dependency Rules

The project follows a strict multi-module Clean Architecture pattern:

- `:app` — Application entry point (`Soh3ilApp`, `MainActivity`), AppWidgets (`CalendarAppWidgetProvider`), and dependency wiring.
- `:core:model` — Pure Kotlin domain models (`Models.kt`). ZERO Android framework dependencies allowed.
- `:core:jalali` — Canonical Jalali/Gregorian/Julian calendar math, `IranianHolidays`, `JalaliRecurrenceEngine`, and `IcsParser`. Pure Kotlin + JUnit tests required for every logic change.
- `:core:database` — Room database (`Soh3ilDatabase`), DAOs, Entities, Converters, and `UserPreferences`.
- `:core:designsystem` — Material 3 Expressive theme (`Soh3ilTheme`, `Color.kt`, `Type.kt`), Vazirmatn typography, `PersianUtils`, and reusable `FarsiComponents`.
- `:core:scheduler` — Background scheduling (`AlarmScheduler`, `AlarmReceiver`, `MidnightScheduler`, `MidnightReceiver`, `LiveIconHelper`, `NotificationHelper`, `PersistentNotificationHelper`, `IcsHelper`).
- `:core:updater` — Self-update engine (`UpdateChecker`).
- `:feature:calendar` | `:feature:reminder` | `:feature:settings` — UI feature modules. Feature modules MUST NOT depend on other feature modules unless explicitly routed through navigation/contracts.

---

## 2. Security & Cryptography Guardrails (Zero-Tolerance)

### Rule 2.1: Secret & Keystore Isolation
- NEVER commit `.jks`, `.keystore`, or `keystore.properties` files to version control.
- Signing configurations in `app/build.gradle.kts` must read from environment variables (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) first, then untracked `signing/keystore.properties`. Release builds must fail if that keystore is missing. Never sign a release APK with the debug certificate or a password committed in source.
- Never log user reminder titles, descriptions, or file paths in release builds.

### Rule 2.2: PendingIntent & BroadcastReceiver Hardening
- Every `PendingIntent` across the entire codebase MUST:
  - Use an explicit component intent: `Intent(context, TargetClass::class.java)`.
  - Include `PendingIntent.FLAG_IMMUTABLE` (combined with `PendingIntent.FLAG_UPDATE_CURRENT` where appropriate). NEVER use `FLAG_MUTABLE` unless required by a system API that mutates the intent.
- Every `<receiver>` in `AndroidManifest.xml` MUST explicitly declare `android:exported="true"` or `android:exported="false"`.
- Inside `BroadcastReceiver.onReceive(context, intent)`, the very first statement must validate `intent?.action` against an explicit whitelist of expected actions. Ignore and return immediately on unknown actions.

### Rule 2.3: Secure In-App APK Updater (:core:updater)
- `app/src/main/res/xml/file_paths.xml` must only expose a dedicated cache subdirectory (`<cache-path name="apk_updates" path="updates/" />`). Never expose `<root-path>` or the entire `<files-path>` / `<cache-path>`.
- All APK downloads in `UpdateChecker.kt` must stream to a temporary `.part` file, clean up old `.apk`/`.part` files prior to download, and verify HTTP 200 status + exact `Content-Length` before renaming to `.apk`.
- Before launching `Intent.ACTION_VIEW` / `PackageInstaller`, you MUST verify the downloaded APK archive using `PackageManager.getPackageArchiveInfo(..., PackageManager.GET_SIGNING_CERTIFICATES)`:
  - Assert `archiveInfo.packageName == context.packageName`.
  - Assert the downloaded APK's signing certificate SHA-256 digest matches the currently installed application's signing certificate. Delete the file and abort if verification fails.

### Rule 2.4: Defensive File Parsing (IcsParser.kt & IcsHelper.kt)
- Never read an unbounded user-supplied Uri stream into memory via `.readText()`.
- Always enforce a maximum stream size (10 MB) and a maximum parsed event count (5000 events) on `Dispatchers.IO` to prevent Out-Of-Memory (OOM) Denial-of-Service crashes.

---

## 3. Background Work, Alarms, Live Icon & Notifications

### Rule 3.1: The BroadcastReceiver goAsync() Law
- NEVER launch fire-and-forget coroutines (`CoroutineScope(Dispatchers.IO).launch { ... }`) inside `AlarmReceiver.onReceive()` or `MidnightReceiver.onReceive()` without `goAsync()`.
- Always follow this exact pattern whenever performing database I/O or preference reads in a receiver:
  ```kotlin
  val pendingResult = goAsync()
  CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
          withTimeoutOrNull(8_500L) {
              // Perform DB queries, alarm rescheduling, and notification updates here
          }
      } finally {
          pendingResult.finish()
      }
  }
  ```

### Rule 3.2: Live App Icon Invariants (LiveIconHelper.kt & AndroidManifest.xml)
- **Invariant 1 (Never Disable MainActivity):** `MainActivity` must remain `android:enabled="true"` at all times without the `LAUNCHER` category so that all internal `PendingIntents` (Widgets, Notifications, Deep Links) targeting `MainActivity::class.java` never throw `ActivityNotFoundException`.
- **Invariant 2 (Enable-First Atomic Transition):** When switching between launcher `<activity-alias>` components (`day1`..`day31` and the default launcher alias):
  - FIRST enable the target alias (`COMPONENT_ENABLED_STATE_ENABLED` with `DONT_KILL_APP`).
  - SECOND iterate over other aliases and only call `setComponentEnabledSetting(..., COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)` on aliases whose current `getComponentEnabledSetting()` is actually enabled. Never blindly loop and call `PackageManager` 32 times.
- **Invariant 3 (Startup Self-Healing):** On app startup (`Soh3ilApp.onCreate`), verify that exactly one launcher alias is enabled. If 0 or >1 are enabled, automatically repair the state.

### Rule 3.3: Exact Alarms & Android 12–15+ Compliance (AlarmScheduler.kt, MidnightScheduler.kt)
- Do not use `AlarmManager.setAlarmClock(...)` for reminders or the midnight tick. Its status-bar show intent fires the PendingIntent. Use `setExactAndAllowWhileIdle(...)` only after `canScheduleExactAlarms()` on API 31+, inside `try/catch (SecurityException)` that falls back to `setAndAllowWhileIdle(...)`.
- Always reschedule active alarms and midnight tasks upon receiving:
  - `Intent.ACTION_BOOT_COMPLETED`
  - `Intent.ACTION_MY_PACKAGE_REPLACED`
  - `Intent.ACTION_TIME_CHANGED` (`ACTION_TIME_SET`)
  - `Intent.ACTION_TIMEZONE_CHANGED`
  - `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`

### Rule 3.4: Composite Request Codes & Notification IDs (No Collisions)
- NEVER use `reminder.id.toInt()` alone as the `PendingIntent` requestCode or `NotificationManager.notify(id, ...)` ID.
- Because a single reminder can have multiple pre-notifications (`preOffsetMinutes`), always compute a deterministic composite ID:
  ```kotlin
  fun alarmRequestCode(reminderId: Long, preOffsetMinutes: Int): Int =
      Objects.hash(reminderId, preOffsetMinutes)
  ```
- When updating or deleting a reminder, cancel the `PendingIntent` for the main alarm (`preOffsetMinutes = 0`) AND every configured pre-notification offset.

### Rule 3.5: Persistent Notification & Widget Synchronization
- In `PersistentNotificationHelper.kt`:
  - Configure `.setOngoing(true)`, `.setSilent(true)`, `.setOnlyAlertOnce(true)`, and `.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)`.
  - Because Android 14+ (API 34+) allows users to swipe away ongoing notifications, attach a `deleteIntent` or ensure `MainActivity.onResume()` and `MidnightReceiver` restore the notification when enabled in `UserPreferences`.
- Whenever a reminder is added, edited, or deleted, or whenever calendar/theme preferences change, ALWAYS trigger `CalendarAppWidgetProvider.requestUpdate(context)` to refresh active home screen widgets immediately.

---

## 4. Jalali Calendar Engine & Database Correctness

### Rule 4.1: Single Source of Truth for Jalali Leap Years
- Do NOT mix multiple leap-year algorithms across `:core:jalali`.
- `JalaliDate.isLeapYear(year)`, `JalaliDate.daysInMonth(year, month)`, and `DateConverter` must all delegate to the single canonical calculation engine verified against official Iranian calendar leap years (including 5-year cycle boundaries like 1403, 1408, 1436 SH).

### Rule 4.2: Recurrence Month-End & Leap-Day Clamping (JalaliRecurrenceEngine.kt)
- Never construct `JalaliDate(targetYear, targetMonth, anchorDay)` directly without clamping.
- Always clamp the day of the month:
  ```kotlin
  val safeDay = minOf(anchorDay, JalaliDate.daysInMonth(targetYear, targetMonth))
  ```
- Always preserve the user's original `anchorDay` (e.g., 31) across recurrence iterations so that clamping to 30 in Mehr or 29 in Esfand does not permanently mutate future occurrences in 31-day months.

### Rule 4.3: Room Database Schema Safety (:core:database)
- NEVER use `.fallbackToDestructiveMigration()` in `Soh3ilDatabase.kt`. User reminders and custom events must never be wiped on app updates.
- Every change to `Entities.kt` must increment the Room `@Database(version = ...)` and provide either an explicit `Migration(startVersion, endVersion)` or `@AutoMigration` backed by exported Room schema JSONs.

### Rule 4.4: RFC 5545 ICS Import/Export Fidelity (IcsParser.kt)
- Always unfold multi-line RFC 5545 properties (`Regex("\r?\n[ \t]")` replaced with `""`) before splitting lines.
- Always unescape `\\`, `\n`, `\;`, and `\,` when importing, and escape them when exporting `.ics` files.

---

## 5. Material Design 3 Expressive, RTL & Accessibility Standards

### Rule 5.1: Mandatory RTL Layout & AutoMirrored Icons
- `Soh3ilTheme` (`Theme.kt`) MUST wrap its root content in:
  ```kotlin
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { ... }
  ```
- RemoteViews XML layouts (`widget_calendar_m3.xml`) MUST set `android:layoutDirection="rtl"` on the root view.
- NEVER use non-mirrored directional icons (`Icons.Default.ArrowBack`, `KeyboardArrowLeft`, `KeyboardArrowRight`, `Send`, etc.). Always use `Icons.AutoMirrored.Rounded.*` or `Icons.AutoMirrored.Filled.*`.
  - *(Exception: Header navigation in `CalendarHeader.kt` is strictly locked to `Icons.Default.ChevronRight` [onPreviousMonth / Start] and `Icons.Default.ChevronLeft` [onNextMonth / End] per Core Invariant 3).*

### Rule 5.2: Edge-to-Edge & WindowInsets (Android 15 / API 35 Ready)
- `MainActivity` must use `enableEdgeToEdge()` and synchronize status/navigation bar icon brightness (`isAppearanceLightStatusBars`) with the active dark/light theme.
- Never ignore `Scaffold` `innerPadding`. Pass it to screen roots or `LazyColumn(contentPadding = ...)`.
- Every `ModalBottomSheet` (e.g., `AddReminderSheet.kt`) MUST apply `.navigationBarsPadding().imePadding()` to its scrollable content so the soft keyboard (IME) never obscures text inputs or action buttons.

### Rule 5.3: M3 Tonal Surface Hierarchy & WCAG AA Holiday Contrast
- Use Material 3 Expressive motion, shape and color tokens across all screens, dialogs, notifications, widgets and settings. Do NOT use hardcoded hex colors inside feature Composables. All colors must come from `MaterialTheme.colorScheme` or a theme-aware `CompositionLocal` in `:core:designsystem`.
- Use Material 3 surface container roles (`surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`) for Cards, BottomSheets, and Dialogs instead of `surfaceVariant.copy(alpha = ...)`.
- Official holidays and Fridays in `DayCell.kt`, `SelectedDayDetails.kt`, and `AgendaView.kt` must use theme-aware semantic tokens (`colorScheme.error` / `errorContainer` / `onErrorContainer`) that maintain a minimum 4.5:1 contrast ratio in both Light and Dark themes.
  - *(Note: When a holiday cell is selected on the calendar grid [`isSelected && isHoliday`], its background MUST be `FarsiHolidayRed` [`#EF4444`] with `Color.White` text per Core Invariant 6).*
- `DayCell.kt` must visually distinguish all 4 states clearly:
  - Selected Today: Filled primary container, onPrimary text.
  - Unselected Today: 1.5.dp primary border, primary text.
  - Selected Other Day: Filled secondaryContainer, onSecondaryContainer text.
  - Unselected Holiday/Friday: error text with subtle tonal indicator.

### Rule 5.4: Touch Targets, Font Scaling & TalkBack Semantics
- Every interactive element (`IconButton`, `DayCell`, chip, row) must have a minimum touch target of 48 x 48 dp.
- `DayCell.kt` MUST include `Modifier.semantics(mergeDescendants = true)` with a complete Persian `contentDescription` announcing: Day + Month + Today status + Holiday status + Reminder count (e.g., "۱۵ فروردین، امروز، تعطیل رسمی، دارای ۲ یادآور").
- Layouts in `MonthGridView.kt` and `CalendarHeader.kt` must not clip text or event dots when the Android system font scale is set up to 150%.

### Rule 5.5: Persian/Arabic Digit Normalization & Cursor Preservation
- Whenever parsing user text input into numbers (`DateConverterDialog`, `CustomPreReminderDialog`, `CustomRecurrenceDialog`), ALWAYS normalize Persian (`\u06F0`..`\u06F9`) and Arabic-Indic (`\u0660`..`\u0669`) digits to ASCII (`0`..`9`) via `String.toEnglishDigits()` before calling `.toIntOrNull()`.
- Avoid mutating `TextField` string state in ways that change character length or reset cursor selection during composition; prefer `VisualTransformation` with `OffsetMapping.Identity` for displaying Persian numerals (۰-۹).

---

## 6. Resource Management & Pre-Commit Verification

### Rule 6.1: Zero Duplicate Resources & Vector-First Policy
- Never duplicate drawables or strings across `:app` and `:core:designsystem`. Keep shared UI assets strictly in `:core:designsystem`.
- Never place raster `.png` files in the base density-less `res/drawable/` directory. Status bar icons (`ic_stat_day_1`..`ic_stat_day_31`) and UI icons must be monochrome `VectorDrawable`s (`.xml`) or placed in explicit density buckets (`drawable-mdpi` through `drawable-xxxhdpi`).

### Rule 6.2: Mandatory Verification Before Completing Any Task
Before declaring any task complete, the AI Agent MUST run and pass:
1. `./gradlew :core:jalali:test` (All calendar conversion, holiday, recurrence, and ICS parser unit tests must pass).
2. `./gradlew testDebugUnitTest` (All module unit tests must pass).
3. `./gradlew lintDebug` (Zero new security, accessibility, or PendingIntent lint errors).
4. `./gradlew assembleDebug` (Clean compilation across all modules).
