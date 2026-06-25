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

/**
 * CustomerContactFragment - Màn hình Liên hệ của Khách hàng.
 * Hỗ trợ khách hàng liên hệ trực tiếp với nhà hàng thông qua:
 * - Gọi điện thoại hoặc nhắn tin Zalo (Hotline).
 * - Mở bản đồ định vị Google Maps dẫn đường đến nhà hàng.
 * - Gửi Email góp ý đính kèm ảnh (chụp từ Camera hoặc chọn từ Thư viện).
 */
public class CustomerContactFragment extends Fragment {

    // Số điện thoại hotline hỗ trợ
    private final String HOTLINE_NUMBER = "0856761038";
    // Địa chỉ Email tiếp nhận góp ý chất lượng dịch vụ
    private final String RESTAURANT_EMAIL = "2431540219@vaa.edu.vn";
    // Tọa độ vĩ độ của nhà hàng
    private final String RESTAURANT_LAT = "10.7952"; 
    // Tọa độ kinh độ của nhà hàng
    private final String RESTAURANT_LNG = "106.7218";
    // Tên hiển thị trên bản đồ
    private final String RESTAURANT_NAME = "Royal Restaurant";

    // Trình phóng Camera để chụp ảnh góp ý
    private ActivityResultLauncher<Intent> cameraLauncher;
    // Trình phóng Thư viện ảnh chọn ảnh góp ý
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Bỏ qua chính sách kiểm soát URI tệp tin (FileUriExposedException) để dễ dàng chia sẻ File đính kèm qua intent
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        // Đăng ký Callback xử lý kết quả trả về khi chụp ảnh từ camera
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

        // Đăng ký Callback xử lý kết quả khi chọn ảnh từ thư viện
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

        // Thiết lập sự kiện nhấn vào các thẻ liên hệ
        cardContactHotline.setOnClickListener(v -> showContactOptionsDialog());
        cardContactLocation.setOnClickListener(v -> openMap());
        cardContactEmail.setOnClickListener(v -> showEmailOptionsDialog());

        return view;
    }

    /**
     * Mở Google Maps hoặc các ứng dụng bản đồ khác định vị tọa độ nhà hàng.
     * Hỗ trợ tự động chuyển hướng sang Trình duyệt Web nếu thiết bị không cài app bản đồ.
     */
    private void openMap() {
        try {
            // Thiết lập chuỗi địa chỉ định vị tọa độ địa lý (geo URI) kèm nhãn tên nhà hàng
            String uri = "geo:" + RESTAURANT_LAT + "," + RESTAURANT_LNG + "?q=" + RESTAURANT_LAT + "," + RESTAURANT_LNG + "(" + Uri.encode(RESTAURANT_NAME) + ")";
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps"); // Ưu tiên hàng đầu mở bằng ứng dụng Google Maps
            
            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Thử mở bằng ứng dụng bản đồ hệ thống thay thế
                Intent defaultMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                if (defaultMapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivity(defaultMapIntent);
                } else {
                    // Nếu thiết bị không có bất kỳ app bản đồ nào, mở thông tin định vị qua đường link Web trên browser
                    String webUri = "https://www.google.com/maps/search/?api=1&query=" + RESTAURANT_LAT + "," + RESTAURANT_LNG;
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
                    startActivity(webIntent);
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Không thể mở Bản đồ!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Hiển thị hộp thoại chọn hình thức gửi mail góp ý (có đính kèm ảnh hoặc không).
     */
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

    /**
     * Lưu ảnh vừa chụp tạm thời vào vùng nhớ cache của ứng dụng, sau đó chuyển hướng gửi email.
     */
    private void sendEmailWithBitmap(Bitmap bitmap) {
        try {
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

    /**
     * Thực hiện tạo ý định (Intent) gửi email đính kèm tệp tin đến hòm thư hỗ trợ của nhà hàng.
     */
    private void sendEmailWithUri(Uri attachmentUri) {
        try {
            // Bước 1: Tìm ứng dụng xử lý mail mặc định bằng ACTION_SENDTO mailto:
            Intent emailFilter = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
            java.util.List<android.content.pm.ResolveInfo> resolveInfos = requireContext().getPackageManager().queryIntentActivities(emailFilter, 0);

            if (resolveInfos != null && !resolveInfos.isEmpty()) {
                // Lấy ứng dụng Mail đầu tiên tìm thấy (thường là Gmail hoặc mail mặc định)
                String packageName = resolveInfos.get(0).activityInfo.packageName;

                // Bước 2: Tạo Intent ACTION_SEND để có thể đính kèm ảnh
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822"); // MIME type chuẩn của email
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{RESTAURANT_EMAIL});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Góp ý chất lượng nhà hàng");
                
                String defaultText = "Chào ban quản lý nhà hàng,\n\nTôi muốn góp ý về vấn đề:\n";
                intent.putExtra(Intent.EXTRA_TEXT, defaultText);

                if (attachmentUri != null) {
                    intent.putExtra(Intent.EXTRA_STREAM, attachmentUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                // Ép Android mở bằng chính ứng dụng Mail tìm được, bỏ qua bảng Share chung!
                intent.setPackage(packageName);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Không tìm thấy ứng dụng Email nào trên máy!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Có lỗi xảy ra khi mở Email!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Hiển thị hộp thoại lựa chọn liên hệ trực tiếp qua số Hotline.
     */
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

    /**
     * Mở ứng dụng Zalo để gửi tin nhắn đến số điện thoại Hotline.
     */
    private void openZalo() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/" + HOTLINE_NUMBER));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Mở trình gọi điện của hệ thống nạp sẵn số điện thoại Hotline của nhà hàng.
     */
    private void makePhoneCall() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + HOTLINE_NUMBER));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
