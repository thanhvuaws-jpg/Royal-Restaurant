package com.sinhvien.orderdrinkapp.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.sinhvien.orderdrinkapp.R;

import java.io.File;
import java.io.FileOutputStream;

public class CustomerContactFragment extends Fragment {

    private final String HOTLINE_NUMBER = "0856761038";
    private final String RESTAURANT_EMAIL = "2431540219@vaa.edu.vn";
    private final String RESTAURANT_LAT = "10.7952"; // Landmark 81, HCM
    private final String RESTAURANT_LNG = "106.7218";
    private final String RESTAURANT_NAME = "Royal Restaurant";

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Bỏ qua lỗi FileUriExposedException để gửi file đính kèm dễ dàng
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Bundle extras = result.getData().getExtras();
                            if (extras != null && extras.get("data") != null) {
                                Bitmap imageBitmap = (Bitmap) extras.get("data");
                                sendEmailWithBitmap(imageBitmap);
                            }
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                sendEmailWithUri(selectedImageUri);
                            }
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_contact, container, false);

        CardView cardContactHotline = view.findViewById(R.id.card_contact_hotline);
        CardView cardContactLocation = view.findViewById(R.id.card_contact_location);
        CardView cardContactEmail = view.findViewById(R.id.card_contact_email);

        cardContactHotline.setOnClickListener(v -> showContactOptionsDialog());
        cardContactLocation.setOnClickListener(v -> openMap());
        cardContactEmail.setOnClickListener(v -> showEmailOptionsDialog());

        return view;
    }

    private void openMap() {
        try {
            // geo:lat,lng?q=lat,lng(Label)
            String uri = "geo:" + RESTAURANT_LAT + "," + RESTAURANT_LNG + "?q=" + RESTAURANT_LAT + "," + RESTAURANT_LNG + "(" + Uri.encode(RESTAURANT_NAME) + ")";
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps"); // Ưu tiên mở bằng Google Maps
            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Nếu không có Google Maps thì mở web hoặc map mặc định
                Intent defaultMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(defaultMapIntent);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Không tìm thấy ứng dụng Bản đồ trên máy!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showEmailOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Góp ý & Phản hồi qua Email");
        
        String[] options = {"Chụp ảnh đính kèm", "Chọn ảnh từ Thư viện", "Chỉ gửi chữ (Không đính kèm)"};
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    Intent iCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraLauncher.launch(iCamera);
                } else if (which == 1) {
                    Intent iGallery = new Intent(Intent.ACTION_GET_CONTENT);
                    iGallery.setType("image/*");
                    galleryLauncher.launch(Intent.createChooser(iGallery, "Chọn ảnh góp ý"));
                } else if (which == 2) {
                    sendEmailWithUri(null);
                }
            }
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void sendEmailWithBitmap(Bitmap bitmap) {
        try {
            // Lưu bitmap ra file tạm
            File cachePath = new File(requireContext().getExternalCacheDir(), "feedback_images");
            cachePath.mkdirs();
            File file = new File(cachePath, "feedback_image.jpg");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
            
            Uri uri = Uri.fromFile(file);
            sendEmailWithUri(uri);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khi xử lý ảnh!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmailWithUri(Uri attachmentUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO); // Chỉ định gửi email
            intent.setData(Uri.parse("mailto:")); // chỉ mở các app gửi mail
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{RESTAURANT_EMAIL});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Góp ý chất lượng nhà hàng");
            
            // Text mặc định
            String defaultText = "Chào ban quản lý nhà hàng,\n\nTôi muốn góp ý về vấn đề:\n";
            intent.putExtra(Intent.EXTRA_TEXT, defaultText);

            if (attachmentUri != null) {
                // Phải đổi sang ACTION_SEND để đính kèm được
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("message/rfc822"); // format để ép mở email client thay vì app message
                intent.putExtra(Intent.EXTRA_STREAM, attachmentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(Intent.createChooser(intent, "Gửi góp ý qua Email..."));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Không tìm thấy ứng dụng Email nào trên máy!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showContactOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chọn phương thức liên hệ");
        
        String[] options = {"Nhắn tin qua Zalo", "Gọi điện thoại trực tiếp"};
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    openZalo();
                } else if (which == 1) {
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
