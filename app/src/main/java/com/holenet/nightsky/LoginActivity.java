package com.holenet.nightsky;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
/*

    private UserLoginTask loginTask = null;
    private UserRegisterTask registerTask = null;

*/
    final static int MODE_LOGIN = 201;
    final static int MODE_REGISTER = 202;
    int mode = MODE_LOGIN;

    // UI references.
    private EditText eTuserName;
    private EditText eTpassword;
    private EditText eTpassword2;
    private View pBloading;
    private View lLcontent;
    private CheckBox cBuserName;
    private CheckBox cBpassword;
    private Button bTlogin;
    private Button bTregister;

    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        pref = getSharedPreferences("setting_login", 0);

        eTuserName = (EditText) findViewById(R.id.eTuserName);
        if(pref.getBoolean("save_user_name", false))
            eTuserName.setText(pref.getString("user_name", ""));
        eTpassword = (EditText) findViewById(R.id.eTPassword);
        if(pref.getBoolean("save_password", false))
            eTpassword.setText(pref.getString("password", ""));
        eTpassword2 = (EditText) findViewById(R.id.eTPassword2);

        bTlogin = (Button) findViewById(R.id.bTlogin);
/*
        bTlogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        bTregister = (Button) findViewById(R.id.bTregister);
        bTregister.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mode==MODE_REGISTER)
                    attemptRegister();
                else
                    changeMode(true);
            }
        });
*/

        lLcontent = findViewById(R.id.lLcontent);
        pBloading = findViewById(R.id.pBloading);

        cBuserName = (CheckBox) findViewById(R.id.cBuserName);
        cBuserName.setChecked(pref.getBoolean("save_user_name", false));
        cBpassword = (CheckBox) findViewById(R.id.cBpassword);
        cBpassword.setChecked(pref.getBoolean("save_password", false));
    }

/*    private void attemptLogin() {
        if (loginTask != null) {
            return;
        }

        eTuserName.setError(null);
        eTpassword.setError(null);

        String userName = eTuserName.getText().toString();
        String password = eTpassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if(password.length()==0) {
            eTpassword.setError(getString(R.string.error_field_required));
            focusView = eTpassword;
            cancel = true;
        }

        if(userName.length()==0) {
            eTuserName.setError(getString(R.string.error_field_required));
            focusView = eTuserName;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            loginTask = new UserLoginTask(userName, password);
            loginTask.execute((Void) null);
        }
    }

    private void attemptRegister() {
        if(registerTask != null) {
            return;
        }

        eTuserName.setError(null);
        eTpassword.setError(null);
        eTpassword2.setError(null);

        String userName = eTuserName.getText().toString();
        String password = eTpassword.getText().toString();
        String password2 = eTpassword2.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if(password2.length()==0) {
            eTpassword2.setError(getString(R.string.error_field_required));
            focusView = eTpassword2;
            cancel = true;
        }

        if(password.length()==0) {
            eTpassword.setError(getString(R.string.error_field_required));
            focusView = eTpassword;
            cancel = true;
        }

        if(userName.length()==0) {
            eTuserName.setError(getString(R.string.error_field_required));
            focusView = eTuserName;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            registerTask = new UserRegisterTask(userName, password, password2);
            registerTask.execute((Void) null);
        }
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        float alpha = lLcontent.getAlpha();
        lLcontent.clearAnimation();
        lLcontent.setAlpha(alpha);
        lLcontent.setVisibility(View.VISIBLE);
        lLcontent.animate().setDuration((long)(shortAnimTime*(show ? alpha : 1-alpha))).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                lLcontent.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        alpha = pBloading.getAlpha();
        pBloading.clearAnimation();
        pBloading.setAlpha(alpha);
        pBloading.setVisibility(View.VISIBLE);
        pBloading.animate().setDuration((long)(shortAnimTime*(show ? 1-alpha : alpha))).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                pBloading.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    ValueAnimator anim;
    private void changeMode(final boolean register) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        mode = register ? MODE_REGISTER : MODE_LOGIN;
        getSupportActionBar().setTitle(register ? "Register" : "Login");

        eTuserName.setError(null);
        eTpassword.setError(null);
        eTpassword2.setError(null);

        if(anim!=null && anim.isRunning())
            anim.cancel();
        final int maxHeight = eTpassword.getMeasuredHeight();
        final float weight = ((LinearLayout.LayoutParams)bTlogin.getLayoutParams()).weight;
        ValueAnimator.setFrameDelay(24);
        anim = ValueAnimator.ofFloat(1-weight, register ? 1 : 0);
        anim.setDuration((long)(shortAnimTime*(register ? weight : 1-weight)));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (float) valueAnimator.getAnimatedValue();
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) eTpassword2.getLayoutParams();
                layoutParams.height = (int)(val*maxHeight);
                eTpassword2.setLayoutParams(layoutParams);
                layoutParams = (LinearLayout.LayoutParams) bTregister.getLayoutParams();
                layoutParams.weight = 1+val;
                bTregister.setLayoutParams(layoutParams);
                layoutParams = (LinearLayout.LayoutParams) bTlogin.getLayoutParams();
                layoutParams.weight = 1-val;
                bTlogin.setLayoutParams(layoutParams);
            }
        });
        anim.start();

        if(register) {
            if(eTuserName.getText().length()==0) {
                eTuserName.requestFocus();
            } else if(eTpassword.getText().length()==0) {
                eTpassword.requestFocus();
            } else {
                eTpassword2.requestFocus();
            }
        } else {
            eTpassword2.setText("");
        }
    }

    @Override
    public void onBackPressed() {
        if(mode==MODE_REGISTER)
            changeMode(false);
        else
            super.onBackPressed();
    }

    public class UserLoginTask extends AsyncTask<Void, Void, String> {

        private final String userName;
        private final String password;

        UserLoginTask(String userName, String password) {
            this.userName = userName;
            this.password = password;
        }

        @Override
        protected String doInBackground(Void... params) {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("username", userName);
            data.put("password", password);
            data.put("next", "blog/");
            return NetworkManager.post(NetworkManager.MAIN_DOMAIN+"accounts/login/", data);
        }

        @Override
        protected void onPostExecute(final String result) {
            loginTask = null;
            showProgress(false);

            if(result==null) {
                Toast.makeText(LoginActivity.this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }

            boolean loginSuccess;
            String viewName = Parser.getMetaDataHTML(result, "view_name");
            Log.e("view_name", String.valueOf(viewName));
            loginSuccess = !"login".equals(viewName);
            if (loginSuccess) {
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("save_user_name", cBuserName.isChecked());
                editor.putBoolean("save_password", cBpassword.isChecked());
                if(cBuserName.isChecked())
                    editor.putString("user_name", userName);
                if(cBpassword.isChecked())
                    editor.putString("password", password);
                editor.apply();
                finish();
            } else {
//                eTuserName.setError(getString(R.string.error_incorrect_user_name));
//                eTpassword.setError("or "+getString(R.string.error_incorrect_password));
                Toast.makeText(LoginActivity.this, "No match user name and password!!", Toast.LENGTH_SHORT).show();
                eTuserName.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            loginTask = null;
            showProgress(false);
        }
    }

    public class UserRegisterTask extends AsyncTask<Void, Void, String> {

        private final String userName;
        private final String password1;
        private final String password2;

        UserRegisterTask(String userName, String password1, String password2) {
            this.userName = userName;
            this.password1 = password1;
            this.password2 = password2;
        }

        @Override
        protected String doInBackground(Void... params) {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("username", userName);
            data.put("password1", password1);
            data.put("password2", password2);
            Log.e("userName", userName);
            Log.e("password", password1);
            Log.e("password2", password2);
            Log.e("data", data.toString());
//            data.put("next", "blog/");
            return NetworkManager.post(NetworkManager.MAIN_DOMAIN+"accounts/register/", data);
        }

        @Override
        protected void onPostExecute(final String result) {
            registerTask = null;
            showProgress(false);

            if(result==null) {
                Toast.makeText(LoginActivity.this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }

            boolean registerSuccess;
            String viewName = Parser.getMetaDataHTML(result, "view_name");
            Log.e("view_name", String.valueOf(viewName));
            registerSuccess = !"register".equals(viewName);
            if(registerSuccess) {
                changeMode(false);
                attemptLogin();
            } else {
                List<String> errors = Parser.getErrorListHTML(result);
                if(errors.size()==0)
                    Toast.makeText(LoginActivity.this, "Unknown error... Pleas try again.", Toast.LENGTH_SHORT).show();
                View focusView = null;
                for(String error: errors) {
                    if(error.contains("two password")) {
                        eTpassword2.setError(error);
                        focusView = eTpassword2;
                    } else if(error.contains("password")) {
                        eTpassword.setError(error);
                        focusView = eTpassword;
                    } else if(error.contains("username")) {
                        eTuserName.setError(error);
                        focusView = eTuserName;
                    }
                }
                if(focusView!=null)
                    focusView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            registerTask = null;
            showProgress(false);
        }
    }*/
}

