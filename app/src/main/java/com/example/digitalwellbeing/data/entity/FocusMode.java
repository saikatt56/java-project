package com.example.digitalwellbeing.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Study / Work / Sleep or a user-defined schedule (proposal 3.4). */
@Entity(tableName = "focus_mode")
public class FocusMode {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    /** Comma-separated package names blocked while this mode is active. */
    public String blockedPackages = "";

    public int startMinute;
    public int endMinute;

    /** Bit 0 = Monday ... bit 6 = Sunday. 0x7F = every day. */
    public int daysMask = 0x7F;

    public boolean enabled;

    /** Strict mode: the user cannot disable the mode while it is running. */
    public boolean strict;

    public FocusMode() {
    }
}
