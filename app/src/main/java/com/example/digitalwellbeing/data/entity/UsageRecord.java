package com.example.digitalwellbeing.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One row per application per day. Written by the aggregation worker from
 * UsageStatsManager data, so re-running aggregation for the same day
 * overwrites rather than duplicates (see the unique index).
 */
@Entity(
        tableName = "usage_record",
        indices = {@Index(value = {"dayKey", "packageName"}, unique = true)}
)
public class UsageRecord {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Local calendar day as yyyyMMdd, e.g. 20260919. */
    public int dayKey;

    @NonNull
    public String packageName = "";

    public String appLabel;

    /** Category id from {@link com.example.digitalwellbeing.rules.AppCategory}. */
    public int category;

    public long foregroundMs;

    /** Number of times the app moved to the foreground on this day. */
    public int launchCount;

    public long updatedAt;

    public UsageRecord() {
    }

    public long foregroundMinutes() {
        return foregroundMs / 60_000L;
    }
}
