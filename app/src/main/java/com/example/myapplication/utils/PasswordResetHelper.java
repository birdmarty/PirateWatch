package com.example.myapplication.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class PasswordResetHelper {
    private final Context context;
    private final FirebaseAuth mAuth;
    private AlertDialog dialog;

    public PasswordResetHelper(Context context) {
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
    }

    public void showResetPasswordDialog() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reset_password, null);
        TextInputLayout emailContainer = dialogView.findViewById(R.id.email_container);
        EditText emailInput = emailContainer.findViewById(R.id.email);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView statusMessage = dialogView.findViewById(R.id.status_message);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("Reset", null) // Set to null initially
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        dialog = builder.create();
        dialog.show();

        // Override the positive button click to prevent dialog from dismissing
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (validateEmail(email, emailContainer)) {
                progressBar.setVisibility(View.VISIBLE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                emailInput.setEnabled(false);

                sendPasswordResetEmail(email, progressBar, statusMessage);
            }
        });
    }

    private boolean validateEmail(String email, TextInputLayout emailContainer) {
        if (email.isEmpty()) {
            emailContainer.setError("Email required");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailContainer.setError("Invalid email format");
            return false;
        }
        emailContainer.setError(null);
        return true;
    }

    private void sendPasswordResetEmail(String email, ProgressBar progressBar, TextView statusMessage) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        statusMessage.setText("Password reset email sent. Please check your inbox.");
                        statusMessage.setVisibility(View.VISIBLE);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Close");
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> dialog.dismiss());
                    } else {
                        String error = "Failed to send reset email";
                        if (task.getException() != null) {
                            error = task.getException().getMessage();
                        }
                        statusMessage.setText(error);
                        statusMessage.setVisibility(View.VISIBLE);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                    }
                });
    }
} 