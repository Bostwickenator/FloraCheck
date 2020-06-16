package org.bostwickenator.floracheck;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import static org.bostwickenator.floracheck.SettingsManager.PREFS_NAME;
import static org.bostwickenator.floracheck.SettingsManager.SETTING_METRIC_UNITS;
import static org.bostwickenator.floracheck.SettingsManager.getHumidity;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupCheckbox(R.id.editText, SETTING_METRIC_UNITS);

    }

    private void setupCheckbox(int id, final String setting) {
        EditText e = (EditText) findViewById(id);
        e.setText("" + getHumidity(this));

        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    mSharedPreferences.edit().putInt(setting, Integer.parseInt(editable.toString())).apply();
                } catch (Exception e) {
                    Toast.makeText(SettingsActivity.this, "Invalid value", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}