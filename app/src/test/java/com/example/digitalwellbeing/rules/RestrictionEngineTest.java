package com.example.digitalwellbeing.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.digitalwellbeing.data.entity.AppRule;

import org.junit.Before;
import org.junit.Test;

/**
 * JVM tests for the rule engine. These run without an emulator because the
 * engine has no Android dependencies (proposal 3.7, unit testing).
 */
public class RestrictionEngineTest {

    private RestrictionEngine engine;

    @Before
    public void setUp() {
        engine = new RestrictionEngine();
    }

    private AppRule rule() {
        AppRule rule = new AppRule();
        rule.packageName = "com.example.social";
        rule.enabled = true;
        rule.interventionType = InterventionType.NONE;
        return rule;
    }

    private UsageSnapshot snapshot(int usedToday, int sessionMinutes, int minuteOfDay) {
        return new UsageSnapshot(usedToday, sessionMinutes, minuteOfDay, 3, null, false);
    }

    @Test
    public void unrestrictedAppIsAllowed() {
        Decision decision = engine.evaluate(null, snapshot(500, 60, 600));
        assertFalse(decision.interrupts());
    }

    @Test
    public void disabledRuleIsIgnored() {
        AppRule rule = rule();
        rule.enabled = false;
        rule.blocklisted = true;
        assertFalse(engine.evaluate(rule, snapshot(0, 0, 600)).interrupts());
    }

    @Test
    public void dailyLimitBlocksOnlyAtOrAboveTheLimit() {
        AppRule rule = rule();
        rule.dailyLimitMinutes = 30;

        assertFalse(engine.evaluate(rule, snapshot(29, 0, 600)).interrupts());

        Decision atLimit = engine.evaluate(rule, snapshot(30, 0, 600));
        assertTrue(atLimit.isHardBlock());
        assertEquals(Decision.REASON_DAILY_LIMIT, atLimit.reason);
    }

    @Test
    public void sessionLimitBlocksIndependentlyOfDailyTotal() {
        AppRule rule = rule();
        rule.sessionLimitMinutes = 10;
        Decision decision = engine.evaluate(rule, snapshot(2, 11, 600));
        assertEquals(Decision.REASON_SESSION_LIMIT, decision.reason);
    }

    @Test
    public void blocklistWinsOverEverythingElse() {
        AppRule rule = rule();
        rule.blocklisted = true;
        rule.dailyLimitMinutes = 600;
        Decision decision = engine.evaluate(rule, snapshot(0, 0, 600));
        assertEquals(Decision.REASON_BLOCKLIST, decision.reason);
    }

    @Test
    public void focusModeBlocksEvenWhenUnderLimit() {
        AppRule rule = rule();
        rule.dailyLimitMinutes = 120;
        UsageSnapshot inFocus = new UsageSnapshot(5, 1, 600, 3, "Study", true);
        assertEquals(Decision.REASON_FOCUS_MODE, engine.evaluate(rule, inFocus).reason);
    }

    @Test
    public void softInterventionAppliesWhileUnderTheLimit() {
        AppRule rule = rule();
        rule.dailyLimitMinutes = 60;
        rule.interventionType = InterventionType.BREATHING;
        Decision decision = engine.evaluate(rule, snapshot(10, 1, 600));
        assertTrue(decision.interrupts());
        assertFalse(decision.isHardBlock());
        assertEquals(InterventionType.BREATHING, decision.interventionType);
    }

    @Test
    public void windowThatWrapsMidnightIsHandled() {
        AppRule rule = rule();
        rule.allowedStartMinute = 22 * 60;  // 22:00
        rule.allowedEndMinute = 6 * 60;     // 06:00

        assertTrue(engine.withinWindow(rule, 23 * 60));   // inside, before midnight
        assertTrue(engine.withinWindow(rule, 2 * 60));    // inside, after midnight
        assertFalse(engine.withinWindow(rule, 12 * 60));  // midday, outside
    }

    @Test
    public void outsideAllowedWindowIsBlocked() {
        AppRule rule = rule();
        rule.allowedStartMinute = 9 * 60;
        rule.allowedEndMinute = 17 * 60;
        assertEquals(Decision.REASON_OUTSIDE_WINDOW,
                engine.evaluate(rule, snapshot(0, 0, 20 * 60)).reason);
    }

    @Test
    public void percentUsedIsReportedForLimitNotifications() {
        AppRule rule = rule();
        rule.dailyLimitMinutes = 60;
        assertEquals(80, engine.evaluate(rule, snapshot(48, 0, 600)).limitPercentUsed);
    }

    @Test
    public void dayMaskMatchesSelectedWeekdays() {
        int weekdays = 0b0011111; // Monday to Friday
        assertTrue(RestrictionEngine.matchesDay(weekdays, 1));   // Monday
        assertTrue(RestrictionEngine.matchesDay(weekdays, 5));   // Friday
        assertFalse(RestrictionEngine.matchesDay(weekdays, 6));  // Saturday
    }
}
