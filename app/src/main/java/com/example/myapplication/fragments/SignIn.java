package com.example.myapplication.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.utils.PasswordResetHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class SignIn extends Fragment {
    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private ProgressBar progressBar;
    private AuthListener authListener;
    private TextView welcomeText;
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_FIRST_RUN = "first_run";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        mAuth = FirebaseAuth.getInstance();
        emailEditText = view.findViewById(R.id.email);
        passwordEditText = view.findViewById(R.id.password);
        progressBar = view.findViewById(R.id.progressBar);
        welcomeText = view.findViewById(R.id.welcome_text);
        Button loginButton = view.findViewById(R.id.login_button);
        Button continueAsGuestButton = view.findViewById(R.id.continue_as_guest_button);
        TextView signupRedirect = view.findViewById(R.id.signup_redirect);
        TextView forgotPassword = view.findViewById(R.id.forgot_password);

        updateWelcomeText();

        loginButton.setOnClickListener(v -> attemptLogin());
        continueAsGuestButton.setOnClickListener(v -> continueAsGuest());
        signupRedirect.setOnClickListener(v -> navigateToSignUp());
        forgotPassword.setOnClickListener(v -> showResetPasswordDialog());

        return view;
    }

    private void updateWelcomeText() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean(KEY_FIRST_RUN, true);
        
        if (isFirstRun) {
            welcomeText.setText(R.string.welcome);
            // Mark that it's no longer the first run
            prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
        } else {
            welcomeText.setText(R.string.welcome_back);
        }
    }

    private void navigateToSignUp() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SignUp())
                .addToBackStack(null)
                .commit();
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (validateInputs(email, password)) {
            progressBar.setVisibility(View.VISIBLE);
            signInUser(email, password);
        }
    }

    private boolean validateInputs(String email, String password) {
        if (email.isEmpty()) {
            emailEditText.setError("Email required");
            return false;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password required");
            return false;
        }
        return true;
    }

    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        checkEmailVerification();
                    } else {
                        handleError(task.getException());
                    }
                });
    }

    private void checkEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.isEmailVerified()) {
            navigateToMain();
        } else {
            Toast.makeText(getContext(),
                    "Please verify your email first", Toast.LENGTH_SHORT).show();
            mAuth.signOut();
        }
    }

    private void handleError(Exception exception) {
        String error = "Login failed";
        if (exception instanceof FirebaseAuthInvalidUserException) {
            error = "Account not found";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            error = "Invalid credentials";
        } else if (exception instanceof FirebaseAuthEmailException) {
            error = "Email issue";
        }
        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
    }

    private void continueAsGuest() {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInAnonymously()
            .addOnCompleteListener(requireActivity(), task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    // Guest sign in successful
                    navigateToMain();
                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(getContext(), "Guest mode failed: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showResetPasswordDialog() {
        new PasswordResetHelper(requireContext()).showResetPasswordDialog();
    }

    public interface AuthListener {
        void onAuthSuccess();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AuthListener) {
            authListener = (AuthListener) context;
        } else {
            throw new RuntimeException(context + " must implement AuthListener");
        }
    }

    private void navigateToMain() {
        if (authListener != null) {
            authListener.onAuthSuccess();
        }
    }
}