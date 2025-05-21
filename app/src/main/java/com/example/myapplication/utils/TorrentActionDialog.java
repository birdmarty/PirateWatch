package com.example.myapplication.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class TorrentActionDialog {
    private final Dialog dialog;
    private final Context context;

    public interface ActionListener {
        void onWatchClick();
        void onDownloadClick();
        void onWatchLaterClick();
    }

    public TorrentActionDialog(Context context, String title, ActionListener listener) {
        this.context = context;
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_torrent_actions);
        
        // Set transparent background and remove default dialog background
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set title
        TextView titleView = dialog.findViewById(R.id.dialog_title);
        titleView.setText(title);

        // Set up close button
        ImageButton closeButton = dialog.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Set up action buttons
        MaterialButton watchButton = dialog.findViewById(R.id.btn_watch);
        MaterialButton downloadButton = dialog.findViewById(R.id.btn_download);
        MaterialButton watchLaterButton = dialog.findViewById(R.id.btn_watch_later);

        watchButton.setOnClickListener(v -> {
            listener.onWatchClick();
            dialog.dismiss();
        });

        downloadButton.setOnClickListener(v -> {
            listener.onDownloadClick();
            dialog.dismiss();
        });

        watchLaterButton.setOnClickListener(v -> {
            Toast.makeText(context, "Watch Later feature coming soon!", Toast.LENGTH_SHORT).show();
            // Placeholder for future implementation
            listener.onWatchLaterClick();
        });
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }
} 