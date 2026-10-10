package com.example.digitalwellbeing.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.digitalwellbeing.data.entity.FocusMode;
import com.example.digitalwellbeing.data.entity.FocusSession;

import java.util.List;

@Dao
public interface FocusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsertMode(FocusMode mode);

    @Query("SELECT * FROM focus_mode ORDER BY id ASC")
    List<FocusMode> allModes();

    @Query("SELECT * FROM focus_mode WHERE enabled = 1")
    List<FocusMode> enabledModes();

    @Query("SELECT COUNT(*) FROM focus_mode")
    int modeCount();

    @Insert
    long startSession(FocusSession session);

    @Update
    void updateSession(FocusSession session);

    @Query("SELECT * FROM focus_session WHERE endedAt = 0 LIMIT 1")
    FocusSession runningSession();

    @Query("SELECT * FROM focus_session ORDER BY startedAt DESC")
    List<FocusSession> allSessions();
}
