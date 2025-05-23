package com.example.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.utils.PasswordResetHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class FullScreenLoginFragment extends Fragment {
    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private ProgressBar progressBar;
    private AuthListener authListener;

    public interface AuthListener {
        void onAuthSuccess();
        void onAuthCancel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fullscreen_login, container, false);

        mAuth = FirebaseAuth.getInstance();
        emailEditText = view.findViewById(R.id.email);
        passwordEditText = view.findViewById(R.id.password);
        progressBar = view.findViewById(R.id.progressBar);
        Button loginButton = view.findViewById(R.id.login_button);
        TextView signupRedirect = view.findViewById(R.id.signup_redirect);
        TextView forgotPassword = view.findViewById(R.id.forgot_password);
        ImageButton closeButton = view.findViewById(R.id.close_button);

        loginButton.setOnClickListener(v -> attemptLogin());
        signupRedirect.setOnClickListener(v -> navigateToSignUp());
        forgotPassword.setOnClickListener(v -> showResetPasswordDialog());
        closeButton.setOnClickListener(v -> {
            if (authListener != null) {
                authListener.onAuthCancel();
            }
        });

        return view;
    }

    private void navigateToSignUp() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new FullScreenSignUpFragment())
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
            if (authListener != null) {
                authListener.onAuthSuccess();
            }
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

    private void showResetPasswordDialog() {
        new PasswordResetHelper(requireContext()).showResetPasswordDialog();
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof AuthListener) {
            authListener = (AuthListener) context;
        } else {
            throw new RuntimeException(context + " must implement AuthListener");
        }
    }
} 