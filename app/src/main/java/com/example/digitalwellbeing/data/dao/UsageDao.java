package com.example.digitalwellbeing.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.digitalwellbeing.data.entity.UsageRecord;

import java.util.List;

@Dao
public interface UsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<UsageRecord> records);

    @Query("SELECT * FROM usage_record WHERE dayKey = :dayKey ORDER BY foregroundMs DESC")
    List<UsageRecord> forDay(int dayKey);

    @Query("SELECT * FROM usage_record WHERE dayKey = :dayKey AND packageName = :packageName LIMIT 1")
    UsageRecord recordForApp(int dayKey, String packageName);

    @Query("SELECT * FROM usage_record WHERE dayKey BETWEEN :from AND :to ORDER BY dayKey ASC")
    List<UsageRecord> forRange(int from, int to);

    @Query("SELECT * FROM usage_record ORDER BY dayKey ASC")
    List<UsageRecord> all();

    @Query("SELECT COALESCE(SUM(foregroundMs), 0) FROM usage_record WHERE dayKey = :dayKey")
    long totalForegroundMs(int dayKey);

    @Query("SELECT COALESCE(SUM(foregroundMs), 0) FROM usage_record "
            + "WHERE dayKey = :dayKey AND packageName = :packageName")
    long foregroundMsForApp(int dayKey, String packageName);

    @Query("SELECT category AS category, SUM(foregroundMs) AS totalMs FROM usage_record "
            + "WHERE dayKey = :dayKey GROUP BY category ORDER BY totalMs DESC")
    List<CategoryTotal> categoryTotals(int dayKey);

    @Query("SELECT dayKey AS dayKey, SUM(foregroundMs) AS totalMs FROM usage_record "
            + "WHERE dayKey BETWEEN :from AND :to GROUP BY dayKey ORDER BY dayKey ASC")
    List<DayTotal> dailyTotals(int from, int to);

    @Query("SELECT packageName AS packageName, COALESCE(appLabel, packageName) AS appLabel, SUM(foregroundMs) AS totalMs "
            + "FROM usage_record WHERE dayKey BETWEEN :from AND :to GROUP BY packageName HAVING totalMs > 0 ORDER BY totalMs DESC")
    List<AppTotal> appTotalsForRange(int from, int to);

    @Query("DELETE FROM usage_record WHERE dayKey < :dayKey")
    int deleteBefore(int dayKey);

    /** Projection for the category pie/bar chart. */
    class CategoryTotal {
        public int category;
        public long totalMs;
    }

    /** Projection for the weekly trend chart. */
    class DayTotal {
        public int dayKey;
        public long totalMs;
    }

    /** Projection for per-app totals over a range. */
    class AppTotal {
        public String packageName;
        public String appLabel;
        public long totalMs;
    }
}
