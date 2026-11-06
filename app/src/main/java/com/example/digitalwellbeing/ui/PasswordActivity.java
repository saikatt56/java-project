package com.example.digitalwellbeing.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.util.PasswordManager;

public class PasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        EditText passwordInput = findViewById(R.id.password_input);
        Button btnUnlock = findViewById(R.id.btn_unlock);

        btnUnlock.setOnClickListener(v -> {
            String entered = passwordInput.getText().toString();
            String saved = PasswordManager.getPassword(this);
            if (saved != null && saved.equals(entered)) {
                PasswordManager.setUnlocked(true);
                finish();
            } else {
                Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }
}
