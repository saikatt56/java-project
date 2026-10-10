package com.example.digitalwellbeing.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.digitalwellbeing.data.entity.InterventionEvent;

import java.util.List;

@Dao
public interface InterventionDao {

    @Insert
    long insert(InterventionEvent event);

    @Query("UPDATE intervention_event SET outcome = :outcome, dwellMs = :dwellMs WHERE id = :id")
    void recordOutcome(long id, int outcome, long dwellMs);

    @Query("SELECT * FROM intervention_event ORDER BY timestamp DESC LIMIT :limit")
    List<InterventionEvent> recent(int limit);

    @Query("SELECT * FROM intervention_event WHERE timestamp BETWEEN :from AND :to "
            + "ORDER BY timestamp ASC")
    List<InterventionEvent> inRange(long from, long to);

    @Query("SELECT COUNT(*) FROM intervention_event "
            + "WHERE packageName = :packageName AND outcome = :outcome AND timestamp >= :since")
    int countOutcome(String packageName, int outcome, long since);
}
