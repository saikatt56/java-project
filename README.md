# Digital Wellbeing & App Intervention System

An Android application, written entirely in **Java**, that measures per-app
smartphone usage, lets the user restrict selected apps, interrupts habitual
opening with a short breathing pause, and records what the user did next so the
effect of each intervention can be examined later.

Implementation of the project proposal *"Java-Based Android Digital Wellbeing
and App Intervention System"*. The core cycle the MVP demonstrates:

```
collect usage -> store -> analyse -> display -> intervene -> record outcome -> export
```

## Why the intervention log matters

A screen-time counter can show a problem without helping anyone change it. This
app therefore separates **measurement** from **intervention**, and makes the
strength of the intervention configurable per app: log-only, a breathing pause,
a confirmation prompt, or a hard block. Every interruption is written to
`intervention_event` with its outcome — cancelled, continued, or bypassed —
because a drop in screen time alone does not say which mechanism produced it.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Java 17 (no Kotlin in app sources) |
| Min SDK | 26 (Android 8.0) / target 34 |
| Usage data | `UsageStatsManager`, `UsageEvents` |
| Foreground detection | `AccessibilityService` (package name only) |
| Persistence | Room 2.6 |
| Background work | WorkManager 2.9 |
| Charts | MPAndroidChart 3.1 |
| Widget | `AppWidgetProvider` |
| Export | CSV via `java.io` |

## Project layout

```
app/src/main/java/com/example/digitalwellbeing/
├── WellbeingApp.java              # channels + periodic work registration
├── data/
│   ├── AppDatabase.java           # Room database + shared IO executor
│   ├── dao/                       # UsageDao, RuleDao, FocusDao, InterventionDao
│   └── entity/                    # UsageRecord, AppRule, FocusMode,
│                                  # FocusSession, InterventionEvent
├── usage/
│   ├── UsageStatsCollector.java   # foreground time + launch counts
│   └── SessionTracker.java        # current continuous session
├── rules/
│   ├── RestrictionEngine.java     # pure Java, unit-testable
│   ├── Decision.java              # what to do + why
│   ├── UsageSnapshot.java         # clock/usage state passed in
│   ├── InterventionType.java
│   └── AppCategory.java
├── service/
│   ├── ForegroundAppWatcherService.java
│   └── FocusModeResolver.java     # Study / Work / Sleep schedules
├── work/                          # aggregation, limit alerts, daily summary
├── ui/                            # onboarding, dashboard, rule editor,
│                                  # block screen, breathing screen
├── export/CsvExporter.java
├── widget/UsageWidgetProvider.java
└── util/                          # TimeUtils, ParticipantId, PermissionUtils

app/src/test/java/...              # JVM tests for the engine and scheduler
```

## Build and run

```bash
git clone <your-fork-url>
cd DigitalWellbeing
./gradlew assembleDebug
./gradlew test            # JVM unit tests, no emulator needed
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open in Android Studio (Hedgehog or newer) and let Gradle sync; the
MPAndroidChart dependency comes from JitPack, which is already declared in
`settings.gradle`.

### Permissions to grant by hand

Both are *special access* permissions — they cannot be requested with
`requestPermissions()` and the app checks their state on every resume:

1. **Usage access** — Settings → Apps → Special app access → Usage access.
2. **Accessibility** — Settings → Accessibility → Wellbeing (only needed for
   blocking and friction; tracking works without it).

Test on a **physical device**. Usage statistics, background execution and
accessibility behaviour all differ on an emulator.

## Data model

| Table | Holds |
| --- | --- |
| `usage_record` | one row per app per day: foreground ms, launch count, category |
| `app_rule` | per-app daily limit, session limit, allowed window, intervention type, bypass flag |
| `focus_mode` | Study / Work / Sleep / custom schedules with a day mask |
| `focus_session` | a focus run, with an optional user context tag |
| `intervention_event` | every interruption, its reason, outcome and dwell time |

## Rule evaluation order

`RestrictionEngine.evaluate()` checks strongest first and returns at the first
match, so a blocklisted app stays blocked even inside its allowed hours:

1. rule missing or disabled → allow
2. blocklisted → hard block
3. blocked by the active focus mode → hard block
4. outside the allowed time window (wrap past midnight supported) → hard block
5. daily limit reached → hard block
6. session limit reached → hard block
7. otherwise → the configured soft intervention, or allow

The engine has no Android imports, which is what makes
`app/src/test/.../RestrictionEngineTest.java` runnable on the JVM.

## Research export

`CsvExporter` writes two files into the app's external files directory:

```
usage_<day>.csv          participant_uuid,day,package_name,category,foreground_minutes,launch_count
interventions_<day>.csv  participant_uuid,timestamp_ms,day,package_name,intervention_type,
                         reason,outcome,dwell_ms,focus_mode
```

Exports carry a locally generated **participant UUID** only — no name, email,
phone number, account or advertising identifier. App labels are left out of the
intervention file because a custom-renamed app can identify a person.

## Implementation status

| Feature | Status |
| --- | --- |
| Per-app usage, history, launch counts | implemented |
| Category breakdown and charts | implemented |
| Daily limits, session limits, blocklist, time windows | implemented |
| Focus modes with day masks | engine implemented; editor UI pending |
| Breathing pause and confirmation prompt | breathing implemented |
| Hard block with logged emergency bypass | implemented |
| Intervention telemetry | implemented |
| Overuse alerts, daily summary, streaks | implemented |
| CSV export with participant UUID | implemented |
| Home-screen widget | implemented |
| Escalating friction, research sync | not started (Phase 3) |
| Shorts / notification-redirect prototypes | not started (stretch) |

## A note on AccessibilityService

Android documents `AccessibilityService` as a facility for assisting users with
disabilities, not as general-purpose automation. This project uses it narrowly:
`canRetrieveWindowContent="false"`, a single event type, and no reading of
screen text. That is defensible for a coursework prototype on your own device,
but **review current Play policy before distributing anything built on it**. See
[`docs/ACCESSIBILITY.md`](docs/ACCESSIBILITY.md).

## Licence

MIT — see [LICENSE](LICENSE).
