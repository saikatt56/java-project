package com.example.digitalwellbeing.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.digitalwellbeing.data.dao.FocusDao;
import com.example.digitalwellbeing.data.dao.InterventionDao;
import com.example.digitalwellbeing.data.dao.RuleDao;
import com.example.digitalwellbeing.data.dao.UsageDao;
import com.example.digitalwellbeing.data.entity.AppRule;
import com.example.digitalwellbeing.data.entity.FocusMode;
import com.example.digitalwellbeing.data.entity.FocusSession;
import com.example.digitalwellbeing.data.entity.InterventionEvent;
import com.example.digitalwellbeing.data.entity.UsageRecord;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                UsageRecord.class,
                AppRule.class,
                FocusMode.class,
                FocusSession.class,
                InterventionEvent.class
        },
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    /** Single background executor for all database writes off the main thread. */
    public static final ExecutorService IO = Executors.newFixedThreadPool(2);

    public abstract UsageDao usageDao();

    public abstract RuleDao ruleDao();

    public abstract FocusDao focusDao();

    public abstract InterventionDao interventionDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "wellbeing.db")
                            // Migrations are added here as the schema version grows.
                            // fallbackToDestructiveMigration() is deliberately not used:
                            // losing a participant's intervention log would invalidate
                            // their study data.
                            .build();
                }
            }
        }
        return instance;
    }
}
