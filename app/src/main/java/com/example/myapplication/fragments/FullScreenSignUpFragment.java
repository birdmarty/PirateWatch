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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class FullScreenSignUpFragment extends Fragment {
    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private ProgressBar progressBar;
    private AuthListener authListener;

    public interface AuthListener {
        void onAuthSuccess();
        void onAuthCancel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fullscreen_signup, container, false);

        mAuth = FirebaseAuth.getInstance();
        emailEditText = view.findViewById(R.id.email);
        passwordEditText = view.findViewById(R.id.password);
        confirmPasswordEditText = view.findViewById(R.id.confirm_password);
        progressBar = view.findViewById(R.id.progressBar);
        Button signupButton = view.findViewById(R.id.signup_button);
        TextView loginRedirect = view.findViewById(R.id.login_redirect);
        ImageButton closeButton = view.findViewById(R.id.close_button);

        signupButton.setOnClickListener(v -> attemptSignUp());
        loginRedirect.setOnClickListener(v -> navigateToLogin());
        closeButton.setOnClickListener(v -> {
            if (authListener != null) {
                authListener.onAuthCancel();
            }
        });

        return view;
    }

    private void navigateToLogin() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new FullScreenLoginFragment())
                .addToBackStack(null)
                .commit();
    }

    private void attemptSignUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (validateInputs(email, password, confirmPassword)) {
            progressBar.setVisibility(View.VISIBLE);
            createUser(email, password);
        }
    }

    private boolean validateInputs(String email, String password, String confirmPassword) {
        if (email.isEmpty()) {
            emailEditText.setError("Email required");
            return false;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password required");
            return false;
        }
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.setError("Please confirm your password");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords don't match");
            return false;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    private void createUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(task2 -> {
                                        if (task2.isSuccessful()) {
                                            Toast.makeText(getContext(),
                                                    "Verification email sent. Please check your email.",
                                                    Toast.LENGTH_LONG).show();
                                            if (authListener != null) {
                                                authListener.onAuthSuccess();
                                            }
                                        } else {
                                            Toast.makeText(getContext(),
                                                    "Failed to send verification email.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        handleError(task.getException());
                    }
                });
    }

    private void handleError(Exception exception) {
        String error = "Sign up failed";
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            error = "Password is too weak";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            error = "Invalid email format";
        } else if (exception instanceof FirebaseAuthUserCollisionException) {
            error = "Email already in use";
        }
        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
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