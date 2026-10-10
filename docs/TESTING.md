# Testing plan

Four levels, matching section 3.7 of the proposal.

## 1. Unit (JVM, `./gradlew test`)

Covered today:

- `RestrictionEngineTest` — limit boundaries, precedence order, windows that
  wrap past midnight, percent-of-limit, day masks.
- `FocusModeResolverTest` — which mode is active, day filtering, package match.

Still to write: streak counting, category assignment overrides.

## 2. Database (instrumented, Room in-memory)

- insert/update/read for each DAO
- the unique `(dayKey, packageName)` index really does replace rather than
  duplicate when aggregation re-runs
- migrations, once schema version 2 exists

## 3. UI (Espresso)

Onboarding gating, dashboard rendering with an empty database, rule save and
reload, export button feedback.

## 4. Device

Run these by hand on a physical phone:

| Case | Expected |
| --- | --- |
| Usage access never granted | dashboard empty, no crash, aggregation retries |
| Accessibility turned off mid-session | tracking continues, no blocking |
| Reboot | WorkManager jobs resume |
| Clock crosses midnight during a session | totals roll to the new day key |
| Restricted app opened ten times in a row | one event logged per open, debounced |
| Airplane mode / no network | everything works; the app is offline-first |
| Battery saver on | aggregation may be delayed, not lost |

## Known measurement caveat

`UsageStatsManager` totals will not match the system Digital Wellbeing screen
exactly. Bucket boundaries, excluded system packages and the launcher filter all
shift the number. Compare trends, not absolute minutes.
