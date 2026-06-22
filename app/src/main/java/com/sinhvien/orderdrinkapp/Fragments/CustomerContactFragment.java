package com.sinhvien.orderdrinkapp.Fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.sinhvien.orderdrinkapp.R;

public class CustomerContactFragment extends Fragment {

    private final String HOTLINE_NUMBER = "0856761038";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_contact, container, false);

        CardView cardContactHotline = view.findViewById(R.id.card_contact_hotline);

        cardContactHotline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactOptionsDialog();
            }
        });

        return view;
    }

    private void showContactOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chọn phương thức liên hệ");
        
        String[] options = {"Nhắn tin qua Zalo", "Gọi điện thoại trực tiếp"};
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // Mở Zalo
                    openZalo();
                } else if (which == 1) {
                    // Gọi điện thoại
                    makePhoneCall();
                }
            }
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void openZalo() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/" + HOTLINE_NUMBER));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makePhoneCall() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + HOTLINE_NUMBER));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
