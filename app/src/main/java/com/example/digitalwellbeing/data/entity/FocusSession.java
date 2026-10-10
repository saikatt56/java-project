package com.example.digitalwellbeing.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A focus-mode run. The optional context tag ("Studying", "Boredom") is
 * user-supplied labelling for later analysis -- it is not a diagnosis.
 */
@Entity(tableName = "focus_session")
public class FocusSession {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long focusModeId;
    public long startedAt;
    public long endedAt;
    public String contextTag;

    public FocusSession() {
    }

    public boolean isRunning() {
        return endedAt == 0L;
    }
}
