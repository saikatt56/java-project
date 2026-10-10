package com.example.digitalwellbeing.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Intervention telemetry (proposal 3.4). Every hard block, soft prompt and
 * bypass is recorded together with what the user did next, because a drop in
 * screen time on its own does not say which mechanism produced it.
 */
@Entity(tableName = "intervention_event")
public class InterventionEvent {

    public static final int OUTCOME_SHOWN = 0;
    public static final int OUTCOME_CANCELLED = 1;   // user backed out
    public static final int OUTCOME_CONTINUED = 2;   // user went on to the app
    public static final int OUTCOME_BYPASSED = 3;    // emergency bypass used

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;

    @NonNull
    public String packageName = "";

    /** Value of {@link com.example.digitalwellbeing.rules.InterventionType}. */
    public int interventionType;

    /** Machine-readable reason from the rule engine, e.g. DAILY_LIMIT. */
    public String reason;

    public int outcome = OUTCOME_SHOWN;

    /** Milliseconds the intervention screen was on display. */
    public long dwellMs;

    public String focusModeName;

    public InterventionEvent() {
    }
}
