package com.example.digitalwellbeing.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.dao.UsageDao;
import com.example.digitalwellbeing.rules.AppCategory;
import com.example.digitalwellbeing.service.ForegroundAppWatcherService;
import com.example.digitalwellbeing.util.ParticipantId;
import com.example.digitalwellbeing.util.PasswordManager;
import com.example.digitalwellbeing.util.PermissionUtils;
import com.example.digitalwellbeing.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private TextView usagePermissionText;
    private TextView accessibilityPermissionText;
    private TextView participantIdText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        usagePermissionText = findViewById(R.id.usage_permission_text);
        accessibilityPermissionText = findViewById(R.id.accessibility_permission_text);
        participantIdText = findViewById(R.id.participant_id_text);
        CheckBox consentCheckbox = findViewById(R.id.consent_checkbox);

        Button btnUsage = findViewById(R.id.btn_usage_permission);
        btnUsage.setOnClickListener(v -> PermissionUtils.openUsageAccessSettings(this));

        Button btnAccessibility = findViewById(R.id.btn_accessibility_permission);
        btnAccessibility.setOnClickListener(v -> PermissionUtils.openAccessibilitySettings(this));

        consentCheckbox.setChecked(ParticipantId.hasResearchConsent(this));
        consentCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                ParticipantId.setResearchConsent(this, isChecked));

        Button btnRegenerate = findViewById(R.id.btn_regenerate_id);
        btnRegenerate.setOnClickListener(v -> {
            ParticipantId.regenerate(this);
            updateParticipantId();
            Toast.makeText(this, R.string.id_regenerated, Toast.LENGTH_SHORT).show();
        });

        Button btnChangePassword = findViewById(R.id.btn_change_password);
        updatePasswordButtonText(btnChangePassword);
        btnChangePassword.setOnClickListener(v -> showPasswordDialog(btnChangePassword));

        Button btnManageCategories = findViewById(R.id.btn_manage_categories);
        btnManageCategories.setOnClickListener(v -> showManageCategoriesDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatuses();
        updateParticipantId();
    }

    private void updatePermissionStatuses() {
        boolean usageGranted = PermissionUtils.hasUsageAccess(this);
        usagePermissionText.setText(getString(R.string.grant_usage_access) + ": "
                + getString(usageGranted ? R.string.granted : R.string.not_granted));

        boolean accessGranted = PermissionUtils.isAccessibilityServiceEnabled(this, ForegroundAppWatcherService.class);
        accessibilityPermissionText.setText(getString(R.string.enable_accessibility) + ": "
                + getString(accessGranted ? R.string.granted : R.string.not_granted));
    }

    private void updateParticipantId() {
        participantIdText.setText(getString(R.string.participant_id_fmt, ParticipantId.get(this)));
    }

    private void updatePasswordButtonText(Button btn) {
        boolean hasPwd = PasswordManager.hasPassword(this);
        btn.setText(hasPwd ? R.string.change_password : R.string.set_password);
    }

    private void showPasswordDialog(Button btn) {
        boolean hasPwd = PasswordManager.hasPassword(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);

        EditText inputCurrent = null;
        if (hasPwd) {
            inputCurrent = new EditText(this);
            inputCurrent.setHint(R.string.current_password_hint);
            inputCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            layout.addView(inputCurrent);
        }

        EditText inputNew = new EditText(this);
        inputNew.setHint(R.string.new_password_hint);
        inputNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputNew);

        EditText finalInputCurrent = inputCurrent;
        new AlertDialog.Builder(this)
                .setTitle(hasPwd ? R.string.change_password : R.string.set_password)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newPwd = inputNew.getText().toString();
                    if (newPwd.isEmpty()) {
                        Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (hasPwd && finalInputCurrent != null) {
                        String currentPwd = finalInputCurrent.getText().toString();
                        if (!currentPwd.equals(PasswordManager.getPassword(this))) {
                            Toast.makeText(this, R.string.password_incorrect, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    PasswordManager.setPassword(this, newPwd);
                    Toast.makeText(this, R.string.password_changed, Toast.LENGTH_SHORT).show();
                    updatePasswordButtonText(btn);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showManageCategoriesDialog() {
        List<AppCategory.CategoryItem> categories = AppCategory.getAllCategories(this);
        CharSequence[] items = new CharSequence[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            items[i] = categories.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.manage_categories)
                .setItems(items, (dialog, which) -> {
                    AppCategory.CategoryItem selected = categories.get(which);
                    showCategoryDetailsDialog(selected);
                })
                .setPositiveButton(R.string.create_category, (dialog, which) -> showCreateCategoryDialog())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCreateCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.category_name_hint);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(32, 24, 32, 24);
        input.setLayoutParams(lp);

        new AlertDialog.Builder(this)
                .setTitle(R.string.create_category)
                .setView(input)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = input.getText().toString();
                    if (!name.trim().isEmpty()) {
                        AppCategory.createCategory(this, name);
                        Toast.makeText(this, R.string.category_created, Toast.LENGTH_SHORT).show();
                        showManageCategoriesDialog();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCategoryDetailsDialog(AppCategory.CategoryItem category) {
        List<String> assignedApps = AppCategory.getAppsForCategory(this, category.id);

        List<CharSequence> appDisplayList = new ArrayList<>();
        for (String pkg : assignedApps) {
            String label = labelOf(pkg);
            appDisplayList.add(label + " (" + pkg + ")");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Category: " + category.name);

        if (assignedApps.isEmpty()) {
            builder.setMessage("No apps assigned to this category.");
        } else {
            builder.setItems(appDisplayList.toArray(new CharSequence[0]), (dialog, which) -> {
                String pkg = assignedApps.get(which);
                new AlertDialog.Builder(this)
                        .setTitle("Remove app?")
                        .setMessage("Remove " + labelOf(pkg) + " from " + category.name + "?")
                        .setPositiveButton(R.string.remove, (d, w) -> {
                            AppCategory.unassignApp(this, pkg, category.id);
                            Toast.makeText(this, R.string.app_removed, Toast.LENGTH_SHORT).show();
                            showCategoryDetailsDialog(category);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        }

        builder.setPositiveButton(R.string.add_app, (dialog, which) -> showAddAppDialog(category));
        if (category.id != AppCategory.OTHER) {
            builder.setNeutralButton(R.string.delete_category, (dialog, which) -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete category?")
                        .setMessage("Delete " + category.name + "?")
                        .setPositiveButton(R.string.delete_category, (d, w) -> {
                            AppCategory.deleteCategory(this, category.id);
                            Toast.makeText(this, R.string.category_deleted, Toast.LENGTH_SHORT).show();
                            showManageCategoriesDialog();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        }
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showAddAppDialog(AppCategory.CategoryItem category) {
        AppDatabase.IO.execute(() -> {
            int today = TimeUtils.today();
            int start30 = TimeUtils.dayKeyOffset(today, -29);
            List<UsageDao.AppTotal> appTotals = AppDatabase.get(this).usageDao().appTotalsForRange(start30, today);

            List<String> packages = new ArrayList<>();
            List<CharSequence> labels = new ArrayList<>();
            for (UsageDao.AppTotal t : appTotals) {
                if (!packages.contains(t.packageName)) {
                    packages.add(t.packageName);
                    labels.add((t.appLabel != null && !t.appLabel.isEmpty() ? t.appLabel : t.packageName) + " (" + t.packageName + ")");
                }
            }

            runOnUiThread(() -> {
                if (packages.isEmpty()) {
                    Toast.makeText(this, "No apps found in recent history", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("Select app to add to " + category.name)
                        .setItems(labels.toArray(new CharSequence[0]), (dialog, which) -> {
                            String pkg = packages.get(which);
                            AppCategory.assignApp(this, pkg, category.id);
                            Toast.makeText(this, R.string.app_added, Toast.LENGTH_SHORT).show();
                            showCategoryDetailsDialog(category);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        });
    }

    private String labelOf(String packageName) {
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }
}
