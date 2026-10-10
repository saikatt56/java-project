package com.example.digitalwellbeing.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.service.ForegroundAppWatcherService;
import com.example.digitalwellbeing.util.ParticipantId;
import com.example.digitalwellbeing.util.PermissionUtils;

/**
 * Consent and permission flow (proposal 3.4). Permissions are requested only
 * after the user has read what is collected and why -- usage access and
 * accessibility are both opened by hand in system Settings, so an unexplained
 * jump into Settings would be confusing as well as poor practice.
 */
public class OnboardingActivity extends AppCompatActivity {

    private Button usageButton;
    private Button accessibilityButton;
    private Button continueButton;
    private CheckBox researchConsent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        TextView participant = findViewById(R.id.participant_id);
        participant.setText(getString(R.string.participant_id_fmt, ParticipantId.get(this)));

        researchConsent = findViewById(R.id.research_consent);
        researchConsent.setChecked(ParticipantId.hasResearchConsent(this));
        researchConsent.setOnCheckedChangeListener(
                (view, checked) -> ParticipantId.setResearchConsent(this, checked));

        usageButton = findViewById(R.id.grant_usage);
        usageButton.setOnClickListener(v -> PermissionUtils.openUsageAccessSettings(this));

        accessibilityButton = findViewById(R.id.grant_accessibility);
        accessibilityButton.setOnClickListener(v -> PermissionUtils.openAccessibilitySettings(this));

        continueButton = findViewById(R.id.continue_button);
        continueButton.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }

    /** Re-checked on every resume because the user grants these outside the app. */
    private void refreshState() {
        boolean usage = PermissionUtils.hasUsageAccess(this);
        boolean accessibility = PermissionUtils.isAccessibilityServiceEnabled(
                this, ForegroundAppWatcherService.class);

        usageButton.setEnabled(!usage);
        usageButton.setText(usage ? R.string.usage_granted : R.string.grant_usage_access);

        accessibilityButton.setEnabled(!accessibility);
        accessibilityButton.setText(accessibility
                ? R.string.accessibility_enabled
                : R.string.enable_accessibility);

        // Usage access alone is enough for tracking; blocking needs the service.
        continueButton.setEnabled(usage);
    }
}
