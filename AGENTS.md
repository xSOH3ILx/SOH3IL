# SOH3IL (سهیل) - Codebase Rules & Invariants

## Core Invariants & Strict Rules (DO NOT CHANGE)

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

4. **FLOATING ACTION BUTTON (FAB) POSITION (LOCKED)**:
   - In `CalendarScreen.kt`, the FAB position is strictly locked to `floatingActionButtonPosition = FabPosition.Start` (Right side in RTL layout).
   - Do NOT move the FAB to `FabPosition.End` or Center.

5. **TAB CHECKMARK REMOVAL (LOCKED)**:
   - In `CalendarScreen.kt`, `SegmentedButton` items for switching between "تقویم ماهانه" and "دستور کار (رویدادها)" must NOT display a checkmark.
   - Always set `icon = {}` on both `SegmentedButton` instances.

6. **HOLIDAY SELECTION COLOR (LOCKED)**:
   - When a holiday cell is selected on the calendar grid (`isSelected && isHoliday`), its background MUST be `FarsiHolidayRed` (`#EF4444`), NEVER `MaterialTheme.colorScheme.primary` (Blue).
   - Text color for selected cells must be `Color.White`.

7. **APP BRANDING & TYPOGRAPHY**:
   - App name is strictly `سهیل` (written plainly without diacritics / اعراب / حرکت).
   - All numbers presented in Persian context must use `.toPersianDigits()`.
   - Persian & Arabic numeral input in all dialogs and text fields must use `toStandardDigits()` / `toIntStandardOrNull()` to ensure resilience across Iranian and Arabic keyboards.

8. **CODEBASE MODULARITY**:
   - Pure Kotlin 100%. No Java files.
   - Maintain deep modularity. No God classes or monolithic files exceeding ~350 lines.
