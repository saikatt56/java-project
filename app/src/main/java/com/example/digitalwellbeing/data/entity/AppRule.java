package com.example.digitalwellbeing.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * User-defined restriction for a single application. One rule per package
 * keeps the rule engine simple; focus modes are layered on top at evaluation
 * time rather than duplicated here.
 */
@Entity(tableName = "app_rule")
public class AppRule {

    @PrimaryKey
    @NonNull
    public String packageName = "";

    public boolean enabled = true;

    /** Blocked outright, regardless of time spent. */
    public boolean blocklisted;

    /** 0 = no limit. */
    public int dailyLimitMinutes;

    /** 0 = no limit. Applies to a single continuous session. */
    public int sessionLimitMinutes;

    /** Minutes from midnight. When start == end the window is ignored. */
    public int allowedStartMinute;
    public int allowedEndMinute;

    /** Value of {@link com.example.digitalwellbeing.rules.InterventionType}. */
    public int interventionType = 1; // BREATHING

    public boolean bypassAllowed = true;

    public AppRule() {
    }

    public boolean hasTimeWindow() {
        return allowedStartMinute != allowedEndMinute;
    }
}
