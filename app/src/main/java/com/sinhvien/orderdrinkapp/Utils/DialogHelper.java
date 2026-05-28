package com.sinhvien.orderdrinkapp.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.sinhvien.orderdrinkapp.R;

public class DialogHelper {
    public static AlertDialog getLoadingDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);
        TextView txtMessage = dialogView.findViewById(R.id.txt_loading_message);
        if (message != null && !message.isEmpty()) {
            txtMessage.setText(message);
        } else {
            txtMessage.setText("Vui lòng chờ...");
        }
        builder.setView(dialogView);
        builder.setCancelable(false);
        return builder.create();
    }
}
