package com.holenet.nightsky;

import android.content.SharedPreferences;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class SettingsActivity extends AppCompatActivity {
    CheckBox cBautoLogin, cBsaveUsername, cBsavePassword;

    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);

        pref = getSharedPreferences("settings_login", 0);

        cBautoLogin = (CheckBox) findViewById(R.id.cBautoLogin);
        cBsaveUsername = (CheckBox) findViewById(R.id.cBsaveUsername);
        cBsavePassword = (CheckBox) findViewById(R.id.cBsavePassword);

        cBautoLogin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    cBsaveUsername.setChecked(true);
                    cBsavePassword.setChecked(true);
                }
                cBsaveUsername.setEnabled(!b);
                cBsavePassword.setEnabled(!b);
            }
        });

        cBsaveUsername.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!b) {
                    cBsavePassword.setChecked(false);
                }
                cBsavePassword.setEnabled(b);
            }
        });

        cBsavePassword.setChecked(pref.getBoolean(getString(R.string.pref_key_save_password), false));
        cBsaveUsername.setChecked(pref.getBoolean(getString(R.string.pref_key_save_username), false));
        cBautoLogin.setChecked(pref.getBoolean(getString(R.string.pref_key_auto_login), false));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed(); return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void pad(View v) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(getString(R.string.pref_key_auto_login), cBautoLogin.isChecked());
        editor.putBoolean(getString(R.string.pref_key_save_username), cBsaveUsername.isChecked());
        editor.putBoolean(getString(R.string.pref_key_save_password), cBsavePassword.isChecked());
        editor.apply();
        finish();
    }
}
