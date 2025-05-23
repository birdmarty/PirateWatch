package com.example.myapplication.fragments;

import android.content.Context;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class SignUp extends Fragment {
    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private ProgressBar progressBar;
    private AuthListener authListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        mAuth = FirebaseAuth.getInstance();
        emailEditText = view.findViewById(R.id.email);
        passwordEditText = view.findViewById(R.id.password);
        confirmPasswordEditText = view.findViewById(R.id.confirm_password);
        progressBar = view.findViewById(R.id.progressBar);
        Button signUpButton = view.findViewById(R.id.signup_button);
        Button continueAsGuestButton = view.findViewById(R.id.continue_as_guest_button);
        TextView loginRedirect = view.findViewById(R.id.login_redirect);

        signUpButton.setOnClickListener(v -> attemptSignUp());
        continueAsGuestButton.setOnClickListener(v -> continueAsGuest());
        loginRedirect.setOnClickListener(v -> navigateToSignIn());

        return view;
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
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords don't match");
            return false;
        }
        return true;
    }

    private void createUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Send verification email and sign out
                        sendEmailVerification();
                        mAuth.signOut(); // Sign out until email is verified
                        navigateToSignIn(); // Navigate to sign in instead of main
                    } else {
                        handleError(task.getException());
                    }
                });
    }

    private void sendEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(),
                                        "Verification email sent. Please verify your email before signing in.",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(),
                                        "Failed to send verification email: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void handleError(Exception exception) {
        String error = "Signup failed";
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            error = "Password too weak";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            error = "Invalid email format";
        } else if (exception instanceof FirebaseAuthUserCollisionException) {
            error = "Account already exists";
        }
        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
    }

    private void navigateToSignIn() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SignIn())
                .addToBackStack(null)
                .commit();
    }

    private void continueAsGuest() {
        // Sign in anonymously
        mAuth.signInAnonymously()
            .addOnCompleteListener(requireActivity(), task -> {
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