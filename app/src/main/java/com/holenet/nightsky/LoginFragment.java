package com.holenet.nightsky;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class LoginFragment extends Fragment {
    Context context;
    UserActivity activity;

    private UserLoginTask loginTask = null;
    private UserRegisterTask registerTask = null;

    final static int MODE_LOGIN = 201;
    final static int MODE_REGISTER = 202;
    int mode = MODE_LOGIN;

    // UI references.
    private EditText eTusername;
    private EditText eTpassword;
    private EditText eTpassword2;
    private TextInputLayout tILpassword;
    private TextInputLayout tILpassword2;
    private CheckBox cBautoLogin;
    private CheckBox cBsaveUsername;
    private CheckBox cBsavePassword;
    private Button bTlogin;
    private Button bTregister;

    SharedPreferences pref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        activity = (UserActivity) getActivity();

        View v = inflater.inflate(R.layout.fragment_login, container, false);

        pref = context.getSharedPreferences("settings_login", 0);

        eTusername = (EditText) v.findViewById(R.id.eTuserName);
        if(pref.getBoolean(getString(R.string.pref_key_save_username), false))
            eTusername.setText(pref.getString(getString(R.string.pref_key_username), ""));
        eTpassword = (EditText) v.findViewById(R.id.eTPassword);
        if(pref.getBoolean(getString(R.string.pref_key_save_password), false))
            eTpassword.setText(pref.getString(getString(R.string.pref_key_password), ""));
        eTpassword2 = (EditText) v.findViewById(R.id.eTPassword2);

        tILpassword = (TextInputLayout) v.findViewById(R.id.tILpassword);
        tILpassword2 = (TextInputLayout) v.findViewById(R.id.tILpassword2);

        bTlogin = (Button) v.findViewById(R.id.bTlogin);
        bTlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        bTregister = (Button) v.findViewById(R.id.bTregister);
        bTregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mode==MODE_REGISTER)
                    attemptRegister();
                else
                    changeMode(true);
            }
        });

        cBautoLogin = (CheckBox) v.findViewById(R.id.cBautoLogin);
        cBsaveUsername = (CheckBox) v.findViewById(R.id.cBuserName);
        cBsavePassword = (CheckBox) v.findViewById(R.id.cBpassword);

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

        if(pref.getBoolean(getString(R.string.pref_key_auto_login), false))
            attemptLogin();

        return v;
    }

    private void attemptLogin() {
        if (loginTask != null) {
            return;
        }

        eTusername.setError(null);
        eTpassword.setError(null);

        String userName = eTusername.getText().toString();
        String password = eTpassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if(password.length()==0) {
            eTpassword.setError(getString(R.string.error_field_required));
            focusView = eTpassword;
            cancel = true;
        }

        if(userName.length()==0) {
            eTusername.setError(getString(R.string.error_field_required));
            focusView = eTusername;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            activity.showProgress(true);
            loginTask = new UserLoginTask(userName, password);
            loginTask.execute((Void) null);
        }
    }

    private void attemptRegister() {
        if(registerTask != null) {
            return;
        }

        eTusername.setError(null);
        eTpassword.setError(null);
        eTpassword2.setError(null);

        String userName = eTusername.getText().toString();
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
            eTusername.setError(getString(R.string.error_field_required));
            focusView = eTusername;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            activity.showProgress(true);
            registerTask = new UserRegisterTask(userName, password, password2);
            registerTask.execute((Void) null);
        }
    }



    ValueAnimator anim;
    protected void changeMode(final boolean register) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        mode = register ? MODE_REGISTER : MODE_LOGIN;
        activity.getSupportActionBar().setTitle(register ? "Register" : "Login");

        eTusername.setError(null);
        eTpassword.setError(null);
        eTpassword2.setError(null);

        if(anim!=null && anim.isRunning())
            anim.cancel();
        final int maxHeight = tILpassword.getMeasuredHeight();
        final float weight = ((LinearLayout.LayoutParams)bTlogin.getLayoutParams()).weight;
        ValueAnimator.setFrameDelay(24);
        anim = ValueAnimator.ofFloat(1-weight, register ? 1 : 0);
        anim.setDuration((long)(shortAnimTime*(register ? weight : 1-weight)));
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(!register) {
                    eTpassword2.setText("");
                }
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (float) valueAnimator.getAnimatedValue();
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) tILpassword2.getLayoutParams();
                layoutParams.height = (int)(val*maxHeight);
                tILpassword2.setLayoutParams(layoutParams);
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
            if(eTusername.getText().length()==0) {
                eTusername.requestFocus();
            } else if(eTpassword.getText().length()==0) {
                eTpassword.requestFocus();
            } else {
                eTpassword2.requestFocus();
            }
        }
    }

    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        private final String username;
        private final String password;

        UserLoginTask(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return NetworkManager.login(username, password);
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            loginTask = null;
            activity.showProgress(false);

            if(!result) {
                Toast.makeText(context, "Incorrect username or password!!", Toast.LENGTH_SHORT).show();
                eTusername.requestFocus();
                return;
            }

            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(getString(R.string.pref_key_auto_login), cBautoLogin.isChecked());
            editor.putBoolean(getString(R.string.pref_key_save_username), cBsaveUsername.isChecked());
            editor.putBoolean(getString(R.string.pref_key_save_password), cBsavePassword.isChecked());
            editor.putString(getString(R.string.pref_key_username), username);
            editor.putString(getString(R.string.pref_key_password), password);
            editor.apply();

            activity.onLogged(true, username);
        }

        @Override
        protected void onCancelled() {
            loginTask = null;
            activity.showProgress(false);
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
            Log.e("username", userName);
            Log.e("password", password1);
            Log.e("password2", password2);
            Log.e("data", data.toString());
            return NetworkManager.register(data);
        }

        @Override
        protected void onPostExecute(final String result) {
            registerTask = null;
            activity.showProgress(false);

            if(result==null) {
                Toast.makeText(context, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }

            boolean registerSuccess;
            String viewName = Parser.getMetaDataHTML(result, "view_name");
            Log.e("view_name", String.valueOf(viewName));
            registerSuccess = "post_list".equals(viewName);
            if(registerSuccess) {
                changeMode(false);
                attemptLogin();
            } else {
                List<String> errors = Parser.getErrorListHTML(result);
                if(errors.size()==0)
                    Toast.makeText(context, "Unknown error... Pleas try again.", Toast.LENGTH_SHORT).show();
                View focusView = null;
                for(String error: errors) {
                    if(error.contains("two password")) {
                        eTpassword2.setError(error);
                        focusView = eTpassword2;
                    } else if(error.contains("password")) {
                        eTpassword.setError(error);
                        focusView = eTpassword;
                    } else if(error.contains("username")) {
                        eTusername.setError(error);
                        focusView = eTusername;
                    }
                }
                if(focusView!=null)
                    focusView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            registerTask = null;
            activity.showProgress(false);
        }
    }
}
