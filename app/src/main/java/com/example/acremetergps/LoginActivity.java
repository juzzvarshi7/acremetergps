package com.example.acremetergps;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.acremetergps.R;

import java.lang.ref.WeakReference;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginBtn, signupBtn;
    private TextView forgotPasswordBtn;

    AppDatabase db;
    UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.acremetergps.R.layout.activity_login);

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        loginBtn = findViewById(R.id.buttonLogin);
        signupBtn = findViewById(R.id.buttonSignup);
        forgotPasswordBtn = findViewById(R.id.textForgotPassword);

        db = AppDatabase.getInstance(this);
        userDao = db.userDao();

        loginBtn.setOnClickListener(v -> signIn());
        signupBtn.setOnClickListener(v -> signUp());
        forgotPasswordBtn.setOnClickListener(v -> forgotPassword());
    }

    private void signUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateEmailAndPassword(email, password)) return;

        new SignUpTask(this, email, password).execute();
    }

    private void signIn() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateEmailAndPassword(email, password)) return;

        new SignInTask(this, email, password).execute();
    }

    private void forgotPassword() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter email to recover password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Password recovery not implemented. Please contact support.", Toast.LENGTH_LONG).show();
    }

    private boolean validateEmailAndPassword(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Static AsyncTask to avoid leaks
    private static class SignUpTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<LoginActivity> activityRef;
        private String email, password;

        SignUpTask(LoginActivity activity, String email, String password) {
            activityRef = new WeakReference<>(activity);
            this.email = email;
            this.password = password;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            LoginActivity activity = activityRef.get();
            if (activity == null) return false;

            UserDao userDao = activity.userDao;
            User existingUser = userDao.getUserByEmail(email);
            if (existingUser == null) {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setPassword(password); // Ideally hash password
                userDao.insert(newUser);
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            LoginActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing()) return;

            if (success) {
                Toast.makeText(activity, "Signup successful", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "User already exists", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Static AsyncTask for sign-in
    private static class SignInTask extends AsyncTask<Void, Void, User> {
        private WeakReference<LoginActivity> activityRef;
        private String email, password;

        SignInTask(LoginActivity activity, String email, String password) {
            activityRef = new WeakReference<>(activity);
            this.email = email;
            this.password = password;
        }

        @Override
        protected User doInBackground(Void... voids) {
            LoginActivity activity = activityRef.get();
            if (activity == null) return null;

            UserDao userDao = activity.userDao;
            return userDao.login(email, password);
        }

        @Override
        protected void onPostExecute(User user) {
            LoginActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing()) return;

            if (user != null) {
                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);
                activity.finish();
            } else {
                Toast.makeText(activity, "Invalid email or password", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
