# AccessibilityService: scope and limits

## What the service does here

`ForegroundAppWatcherService` subscribes to `typeWindowStateChanged` and reads
exactly one thing from each event: `event.getPackageName()`. That package name
is looked up against the user's own rules, and if a rule matches, an
intervention screen is shown.

The service config sets `canRetrieveWindowContent="false"`. The app cannot read
text on screen, form fields, passwords or message contents, and does not try to.

## Why the service is needed at all

`UsageStatsManager` reports usage *after the fact*. Interrupting an app as it
opens needs to know the foreground package within a second or so.
`UsageEvents` polling is the alternative (`UsageStatsCollector.currentForegroundPackage()`
implements it) but polling costs battery and lags the launch by seconds, which
defeats the point of a friction intervention.

## Policy caution

Google Play restricts `AccessibilityService` use to apps that genuinely assist
users with disabilities, and apps that use it for other purposes have been
rejected or removed. For this project that means:

- the tracking, analytics, limits, notifications and export features must keep
  working with the service **disabled** — they do;
- blocking and friction degrade to nothing rather than crashing when the user
  turns the service off;
- before any public distribution, check the current Play Developer Program
  policy and consider whether a `UsageEvents` poll from a foreground service is
  an acceptable substitute.

## Device variation

OEM battery managers on some devices kill background services aggressively. When
testing, record which manufacturer and Android version each result came from,
and expect the watcher to be killed on at least one of them. The mitigation
already in the code is that limits and alerts run through WorkManager, which
survives process death and reboot.
