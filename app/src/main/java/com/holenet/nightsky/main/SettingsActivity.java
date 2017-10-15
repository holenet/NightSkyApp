package com.holenet.nightsky.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.holenet.nightsky.R;

public class SettingsActivity extends AppCompatActivity {
    LinearLayout lLtouchArea;

    CheckBox cBautoLogin, cBsaveUsername, cBsavePassword;
    CheckBox cBnotice;

    SharedPreferences prefLogin, prefNotice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefLogin = getSharedPreferences("settings_login", 0);
        prefNotice = getSharedPreferences("settings_notice", 0);

        lLtouchArea = (LinearLayout) findViewById(R.id.lLtouchArea);
        lLtouchArea.setOnTouchListener(new View.OnTouchListener() {
            final boolean[] passcode = new boolean[] {
                    true,
                    false,
                    false,
                    true,
                    false,
                    true,
                    false,
                    false,
                    false,
            }; // true: left scroll
            float lastX;
            int currPos = -1;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if(action==MotionEvent.ACTION_DOWN) {
                    Log.e("action", "down");
                    lastX = motionEvent.getX();
                } else if(action==MotionEvent.ACTION_MOVE) {
//                    Log.e("action", "move");
                } else if(action==MotionEvent.ACTION_UP) {
                    Log.e("action", "up");
                    currPos++;
                    if(passcode[currPos]==motionEvent.getX()<lastX) {
                        if(currPos==passcode.length-1) {
                            SharedPreferences pref = getSharedPreferences("secret", 0);
                            boolean activated = pref.getBoolean("activated", false);
                            Toast.makeText(SettingsActivity.this, (activated?"Dea":"A")+"ctivate Secret Mode!!!", Toast.LENGTH_SHORT).show();
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putBoolean("activated", !activated);
                            editor.apply();
                            currPos = -1;
                        }
                    } else {
                        currPos = -1;
                    }
                    Log.e("pos", currPos+"");
                } else {
                    Log.e("action", "etc");
                }
                return true;
            }
        });

        cBautoLogin = (CheckBox) findViewById(R.id.cBautoLogin);
        cBsaveUsername = (CheckBox) findViewById(R.id.cBsaveUsername);
        cBsavePassword = (CheckBox) findViewById(R.id.cBsavePassword);
        cBnotice = (CheckBox) findViewById(R.id.cBnotice);

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

        cBsavePassword.setChecked(prefLogin.getBoolean(getString(R.string.pref_key_save_password), false));
        cBsaveUsername.setChecked(prefLogin.getBoolean(getString(R.string.pref_key_save_username), false));
        cBautoLogin.setChecked(prefLogin.getBoolean(getString(R.string.pref_key_auto_login), false));
        cBnotice.setChecked(prefNotice.getBoolean(getString(R.string.pref_key_notice), false));
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
        SharedPreferences.Editor editor = prefLogin.edit();
        editor.putBoolean(getString(R.string.pref_key_auto_login), cBautoLogin.isChecked());
        editor.putBoolean(getString(R.string.pref_key_save_username), cBsaveUsername.isChecked());
        editor.putBoolean(getString(R.string.pref_key_save_password), cBsavePassword.isChecked());
        editor.apply();
        editor = prefNotice.edit();
        editor.putBoolean(getString(R.string.pref_key_notice), cBnotice.isChecked());
        editor.apply();
        finish();
    }
}
