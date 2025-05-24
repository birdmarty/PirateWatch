package com.example.myapplication.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.utils.DownloadLocationManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment implements FullScreenLoginFragment.AuthListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "ProfileFragment";
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_THEME = "theme_mode";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView currentEmailView;
    private TextView currentDownloadLocationView;
    private ProgressBar progressBar;
    private SwitchMaterial themeSwitch;
    private FirebaseAuth mAuth;
    private File currentDownloadLocation;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MyProfile.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_profile, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        currentEmailView = view.findViewById(R.id.current_email);
        currentDownloadLocationView = view.findViewById(R.id.current_download_location);
        progressBar = view.findViewById(R.id.progressBar);
        themeSwitch = view.findViewById(R.id.theme_switch);
        MaterialButton changePasswordButton = view.findViewById(R.id.btn_change_password);
        MaterialButton changeEmailButton = view.findViewById(R.id.btn_change_email);
        MaterialButton logoutButton = view.findViewById(R.id.btn_logout);
        MaterialButton changeDownloadLocationButton = view.findViewById(R.id.btn_change_download_location);

        // Set up theme switch
        setupThemeSwitch();

        // Set up click listeners
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        changeEmailButton.setOnClickListener(v -> showChangeEmailDialog());
        logoutButton.setOnClickListener(v -> handleLogout());
        changeDownloadLocationButton.setOnClickListener(v -> handleChangeDownloadLocation());

        // Initialize download location
        // Update UI after view is created
        updateUI(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize download location from manager
        currentDownloadLocation = DownloadLocationManager.getInstance(requireContext()).getCurrentLocation();
        updateDownloadLocationText();
    }

    private void setupThemeSwitch() {
        // Get current theme mode from preferences
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, 0);
        boolean isDarkMode = prefs.getBoolean(KEY_THEME, false);
        
        // Set initial switch state
        themeSwitch.setChecked(isDarkMode);
        
        // Set initial theme
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
            AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        
        // Set up switch listener
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_THEME, isChecked);
            editor.apply();
            
            // Apply theme
            AppCompatDelegate.setDefaultNightMode(isChecked ? 
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            
            // Restart activity to apply theme changes
            requireActivity().recreate();
        });
    }

    private void updateUI(View view) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.isAnonymous()) {
                // Guest user
                currentEmailView.setText("Guest Mode");
                MaterialButton logoutButton = view.findViewById(R.id.btn_logout);
                logoutButton.setText("Log In");
                view.findViewById(R.id.btn_change_password).setVisibility(View.GONE);
                view.findViewById(R.id.btn_change_email).setVisibility(View.GONE);
            } else {
                // Regular user
            currentEmailView.setText("Current Email: " + user.getEmail());
                MaterialButton logoutButton = view.findViewById(R.id.btn_logout);
                logoutButton.setText("Log Out");
                view.findViewById(R.id.btn_change_password).setVisibility(View.VISIBLE);
                view.findViewById(R.id.btn_change_email).setVisibility(View.VISIBLE);
            }
        }
        if (currentDownloadLocation != null){
        currentDownloadLocationView.setText("Current Location: " + currentDownloadLocation.getAbsolutePath());
        }
        else {
            currentDownloadLocationView.setText("Current Location: Not set");
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordInput = dialogView.findViewById(R.id.current_password);
        EditText newPasswordInput = dialogView.findViewById(R.id.new_password);
        EditText confirmPasswordInput = dialogView.findViewById(R.id.confirm_password);

        new AlertDialog.Builder(getContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Change", (dialog, which) -> {
                    String currentPassword = currentPasswordInput.getText().toString();
                    String newPassword = newPasswordInput.getText().toString();
                    String confirmPassword = confirmPasswordInput.getText().toString();

                    if (validatePasswordInputs(currentPassword, newPassword, confirmPassword)) {
                        changePassword(currentPassword, newPassword);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean validatePasswordInputs(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "New passwords don't match", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void changePassword(String currentPassword, String newPassword) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(task2 -> {
                                        progressBar.setVisibility(View.GONE);
                                        if (task2.isSuccessful()) {
                                            Toast.makeText(getContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getContext(), "Failed to update password", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showChangeEmailDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_email, null);
        EditText passwordInput = dialogView.findViewById(R.id.password);
        EditText newEmailInput = dialogView.findViewById(R.id.new_email);

        new AlertDialog.Builder(getContext())
                .setTitle("Change Email")
                .setView(dialogView)
                .setPositiveButton("Change", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    String newEmail = newEmailInput.getText().toString();

                    if (validateEmailInputs(password, newEmail)) {
                        changeEmail(password, newEmail);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean validateEmailInputs(String password, String newEmail) {
        if (password.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(getContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void changeEmail(String password, String newEmail) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.updateEmail(newEmail)
                                    .addOnCompleteListener(task2 -> {
                                        progressBar.setVisibility(View.GONE);
                                        if (task2.isSuccessful()) {
                                            Toast.makeText(getContext(), "Email updated successfully", Toast.LENGTH_SHORT).show();
                                            updateUI(requireView());
                                            // Send verification email
                                            user.sendEmailVerification();
                                        } else {
                                            Toast.makeText(getContext(), "Failed to update email", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Password is incorrect", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void handleLogout() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.isAnonymous()) {
            // For guest users, show full screen login
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new FullScreenLoginFragment())
                    .addToBackStack(null)
                    .commit();
        } else {
            // For regular users, show confirmation dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new SignUp())
                            .commit();
                })
                .setNegativeButton("No", null)
                .show();
        }
    }

    @Override
    public void onAuthSuccess() {
        // User successfully logged in
        requireActivity().getSupportFragmentManager().popBackStack();
        updateUI(requireView());
    }

    @Override
    public void onAuthCancel() {
        // User cancelled login, go back to profile
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void handleChangeDownloadLocation() {
        // Create an intent to open the system's file picker
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && data != null) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                // Update the download location using the URI
                DownloadLocationManager.getInstance(requireContext()).setCurrentLocationFromUri(treeUri);
                currentDownloadLocation = DownloadLocationManager.getInstance(requireContext()).getCurrentLocation();
                    // Update the UI
                updateUI(requireView());
                    Toast.makeText(getContext(), "Download location updated", Toast.LENGTH_SHORT).show();
                }
            }
    }

    private void updateDownloadLocation(File newLocation) {
        if (newLocation != null) {
            DownloadLocationManager.getInstance(requireContext()).setCurrentLocation(newLocation);
            currentDownloadLocation = newLocation;
            updateDownloadLocationText();
        }
    }

    private void updateDownloadLocationText() {
        if (currentDownloadLocation != null) {
            currentDownloadLocationView.setText("Current Location: " + currentDownloadLocation.getAbsolutePath());
        }
    }
}