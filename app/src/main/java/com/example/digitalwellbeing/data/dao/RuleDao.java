package com.example.digitalwellbeing.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.digitalwellbeing.data.entity.AppRule;

import java.util.List;

@Dao
public interface RuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(AppRule rule);

    @Delete
    void delete(AppRule rule);

    @Query("SELECT * FROM app_rule WHERE packageName = :packageName LIMIT 1")
    AppRule byPackage(String packageName);

    @Query("SELECT * FROM app_rule WHERE enabled = 1")
    List<AppRule> active();

    @Query("SELECT * FROM app_rule ORDER BY packageName ASC")
    List<AppRule> all();

    @Query("SELECT packageName FROM app_rule WHERE enabled = 1 AND dailyLimitMinutes > 0")
    List<String> packagesWithDailyLimit();
}
