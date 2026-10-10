package com.example.digitalwellbeing.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.entity.AppRule;
import com.example.digitalwellbeing.rules.InterventionType;

/** Per-app rule editor: limits, window, intervention strength, bypass. */
public class AppRuleActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE = "package";
    public static final String EXTRA_LABEL = "label";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String packageName;
    private AppRule rule;

    private EditText dailyLimit;
    private EditText sessionLimit;
    private CheckBox blocklisted;
    private CheckBox bypassAllowed;
    private Spinner interventionSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_rule);

        packageName = getIntent().getStringExtra(EXTRA_PACKAGE);
        String label = getIntent().getStringExtra(EXTRA_LABEL);
        if (packageName == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.rule_app_name))
                .setText(label != null ? label : packageName);

        dailyLimit = findViewById(R.id.daily_limit);
        sessionLimit = findViewById(R.id.session_limit);
        blocklisted = findViewById(R.id.blocklisted);
        bypassAllowed = findViewById(R.id.bypass_allowed);

        interventionSpinner = findViewById(R.id.intervention_type);
        interventionSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        InterventionType.label(InterventionType.NONE),
                        InterventionType.label(InterventionType.BREATHING),
                        InterventionType.label(InterventionType.CONFIRM),
                        InterventionType.label(InterventionType.BLOCK)
                }));

        ((Button) findViewById(R.id.save_rule)).setOnClickListener(v -> save());

        load();
    }

    private void load() {
        AppDatabase.IO.execute(() -> {
            AppRule existing = AppDatabase.get(this).ruleDao().byPackage(packageName);
            if (existing == null) {
                existing = new AppRule();
                existing.packageName = packageName;
            }
            AppRule loaded = existing;
            mainHandler.post(() -> bind(loaded));
        });
    }

    private void bind(AppRule loaded) {
        rule = loaded;
        dailyLimit.setText(loaded.dailyLimitMinutes > 0
                ? String.valueOf(loaded.dailyLimitMinutes) : "");
        sessionLimit.setText(loaded.sessionLimitMinutes > 0
                ? String.valueOf(loaded.sessionLimitMinutes) : "");
        blocklisted.setChecked(loaded.blocklisted);
        bypassAllowed.setChecked(loaded.bypassAllowed);
        interventionSpinner.setSelection(loaded.interventionType);
    }

    private void save() {
        if (rule == null) {
            return;
        }
        rule.dailyLimitMinutes = parseMinutes(dailyLimit.getText().toString());
        rule.sessionLimitMinutes = parseMinutes(sessionLimit.getText().toString());
        rule.blocklisted = blocklisted.isChecked();
        rule.bypassAllowed = bypassAllowed.isChecked();
        rule.interventionType = interventionSpinner.getSelectedItemPosition();
        rule.enabled = true;

        AppRule toSave = rule;
        AppDatabase.IO.execute(() -> {
            AppDatabase.get(this).ruleDao().upsert(toSave);
            mainHandler.post(() -> {
                Toast.makeText(this, R.string.rule_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    /** Empty or non-numeric input means "no limit" rather than a crash. */
    private int parseMinutes(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(value, 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
