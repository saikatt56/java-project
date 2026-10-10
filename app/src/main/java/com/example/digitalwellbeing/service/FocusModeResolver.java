package com.example.digitalwellbeing.service;

import com.example.digitalwellbeing.data.entity.FocusMode;
import com.example.digitalwellbeing.rules.RestrictionEngine;

import java.util.List;

/** Finds which enabled focus mode, if any, is active at the current minute. */
public class FocusModeResolver {

    public FocusMode activeMode(List<FocusMode> modes, int minuteOfDay, int isoDayOfWeek) {
        if (modes == null) {
            return null;
        }
        for (FocusMode mode : modes) {
            if (!mode.enabled || !RestrictionEngine.matchesDay(mode.daysMask, isoDayOfWeek)) {
                continue;
            }
            if (covers(mode, minuteOfDay)) {
                return mode;
            }
        }
        return null;
    }

    /** Sleep modes usually wrap past midnight, so that case is handled here. */
    public boolean covers(FocusMode mode, int minuteOfDay) {
        if (mode.startMinute == mode.endMinute) {
            return true;
        }
        if (mode.startMinute < mode.endMinute) {
            return minuteOfDay >= mode.startMinute && minuteOfDay < mode.endMinute;
        }
        return minuteOfDay >= mode.startMinute || minuteOfDay < mode.endMinute;
    }

    public boolean blocks(FocusMode mode, String packageName) {
        if (mode == null || mode.blockedPackages == null || mode.blockedPackages.isEmpty()) {
            return false;
        }
        for (String pkg : mode.blockedPackages.split(",")) {
            if (pkg.trim().equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
