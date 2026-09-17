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

import android.widget.TextView;

import com.sinhvien.orderdrinkapp.Activities.ChatActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.ChatResponse;
import com.sinhvien.orderdrinkapp.Api.TinNhan;
import com.sinhvien.orderdrinkapp.R;

import java.io.File;
import java.io.FileOutputStream;

import com.google.gson.Gson;
import com.sinhvien.orderdrinkapp.Utils.SocketManager;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CustomerContactFragment - Màn hình Hỗ trợ khách hàng.
 * Bốn cách liên hệ, xếp theo thứ tự nên dùng:
 * - Chat thẳng với bộ phận chăm sóc ngay trong app (mở ChatActivity).
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
    // Huy hiệu đếm tin nhắn chưa đọc trên thẻ chat
    private TextView huyHieuChat;
    // Bộ nghe tin nhắn mới từ Socket.IO, giữ lại để gỡ khi hủy view
    private Emitter.Listener ngheTinMoi;

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

        CardView cardChat = view.findViewById(R.id.card_chat_ho_tro);
        CardView cardContactHotline = view.findViewById(R.id.card_contact_hotline);
        CardView cardContactLocation = view.findViewById(R.id.card_contact_location);
        CardView cardContactEmail = view.findViewById(R.id.card_contact_email);
        huyHieuChat = view.findViewById(R.id.huy_hieu_chat);

        // Thiết lập sự kiện nhấn vào các thẻ liên hệ
        cardChat.setOnClickListener(v -> startActivity(new Intent(getContext(), ChatActivity.class)));
        cardContactHotline.setOnClickListener(v -> showContactOptionsDialog());
        cardContactLocation.setOnClickListener(v -> openMap());
        cardContactEmail.setOnClickListener(v -> showEmailOptionsDialog());

        ngheSocket();

        return view;
    }

    /*
     * BA ĐƯỜNG CẬP NHẬT HUY HIỆU, VÌ MỘT ĐƯỜNG KHÔNG ĐỦ
     * ==================================================
     * Bản đầu chỉ đếm trong onResume(). Chạy thử trên máy ảo thì huy hiệu
     * KHÔNG BAO GIỜ hiện: nhân viên trả lời trong lúc khách đang mở sẵn màn
     * hình Hỗ trợ, mà onResume thì đã chạy xong từ trước đó.
     *
     * Ba lối vào màn hình này, mỗi lối gọi một hàm khác nhau:
     *
     *   Quay lại từ ChatActivity ............ onResume()
     *   Đổi tab trong cùng Activity ......... onHiddenChanged()
     *   Đang đứng yên, có tin mới tới ....... sự kiện Socket.IO
     *
     * Thiếu đường nào thì huy hiệu sai trong đúng tình huống đó, và sai một
     * cách im lặng — không có gì báo cho ta biết.
     */

    @Override
    public void onResume() {
        super.onResume();
        demTinChuaDoc();
    }

    /**
     * Chạy khi đổi tab.
     *
     * CustomerHomeActivity dùng hide/show chứ không tạo lại fragment (xem
     * navigateTo), nên onResume KHÔNG chạy lúc đổi tab — chỉ có hàm này chạy.
     */
    @Override
    public void onHiddenChanged(boolean an) {
        super.onHiddenChanged(an);
        if (!an) demTinChuaDoc();
    }

    /** Có tin mới của nhà hàng trong lúc màn hình đang mở. */
    private void ngheSocket() {
        Socket socket = SocketManager.getInstance().getSocket();
        if (socket == null || ngheTinMoi != null) return;

        final Gson gson = new Gson();
        ngheTinMoi = args -> {
            if (args == null || args.length == 0) return;
            try {
                TinNhan t = gson.fromJson(args[0].toString(), TinNhan.class);
                // Tin của chính khách thì bỏ qua: nó không làm tăng số chưa đọc
                // của khách, đếm lại chỉ tốn một lời gọi mạng vô ích.
                if (t == null || t.laCuaKhach()) return;
            } catch (Exception e) {
                return;
            }
            // Hỏi lại máy chủ thay vì tự cộng thêm một. Con số chưa đọc do máy
            // chủ giữ; tự cộng ở đây sẽ lệch ngay khi khách mở màn chat trên
            // một thiết bị khác.
            if (isAdded()) requireActivity().runOnUiThread(this::demTinChuaDoc);
        };
        socket.on("chat_moi", ngheTinMoi);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Socket socket = SocketManager.getInstance().getSocket();
        if (socket != null && ngheTinMoi != null) {
            socket.off("chat_moi", ngheTinMoi);
            ngheTinMoi = null;
        }
        huyHieuChat = null;
    }

    /**
     * Hỏi máy chủ xem có tin nào của nhà hàng mà khách chưa đọc không.
     *
     * Gọi với danh_dau_da_doc=0 — CỰC KỲ QUAN TRỌNG. Mặc định endpoint sẽ đánh
     * dấu đã đọc; gọi ở đây mà không tắt cờ đó thì chỉ cần khách LƯỚT QUA màn
     * hình Hỗ trợ là mọi tin bị coi như đã xem, và huy hiệu không bao giờ hiện.
     */
    private void demTinChuaDoc() {
        if (huyHieuChat == null) return;
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.demTinChuaDoc(0).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> c, @NonNull Response<ChatResponse> r) {
                if (huyHieuChat == null) return;
                int n = 0;
                if (r.isSuccessful() && r.body() != null && r.body().getHoiThoai() != null) {
                    n = r.body().getHoiThoai().getSoChuaDocKH();
                }
                if (n > 0) {
                    huyHieuChat.setText(n > 9 ? "9+" : String.valueOf(n));
                    huyHieuChat.setVisibility(View.VISIBLE);
                } else {
                    huyHieuChat.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> c, @NonNull Throwable t) {
                // Mất mạng thì cứ để huy hiệu như cũ. Ẩn nó đi sẽ nói dối rằng
                // không còn tin nào chờ đọc.
            }
        });
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
            
            // Dùng FileProvider để tạo content:// URI thay vì file:// URI nhằm tránh lỗi bảo mật Scoped Storage
            Uri uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
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
            String defaultText = "Chào ban quản lý nhà hàng,\n\nTôi muốn góp ý về vấn đề:\n";

            if (attachmentUri == null) {
                // Nếu KHÔNG CÓ ảnh đính kèm: Dùng chuẩn ACTION_SENDTO để mở màn hình soạn mail cực mượt
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{RESTAURANT_EMAIL});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Góp ý chất lượng nhà hàng");
                intent.putExtra(Intent.EXTRA_TEXT, defaultText);
                startActivity(Intent.createChooser(intent, "Gửi góp ý qua Email..."));
                return;
            }

            // Nếu CÓ ảnh đính kèm: Phải dùng ACTION_SEND nhưng trỏ đích danh vào màn hình Soạn Mail
            Intent emailFilter = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
            java.util.List<android.content.pm.ResolveInfo> resolveInfos = requireContext().getPackageManager().queryIntentActivities(emailFilter, 0);

            if (resolveInfos != null && !resolveInfos.isEmpty()) {
                String targetPackage = resolveInfos.get(0).activityInfo.packageName;
                String targetActivity = resolveInfos.get(0).activityInfo.name;
                
                for (android.content.pm.ResolveInfo info : resolveInfos) {
                    String pkgName = info.activityInfo.packageName.toLowerCase();
                    if (pkgName.contains("gmail") || pkgName.contains("email") || pkgName.contains("mail")) {
                        targetPackage = info.activityInfo.packageName;
                        targetActivity = info.activityInfo.name; // Lấy đích danh Activity soạn mail
                        break;
                    }
                }

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{RESTAURANT_EMAIL});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Góp ý chất lượng nhà hàng");
                intent.putExtra(Intent.EXTRA_TEXT, defaultText);
                intent.putExtra(Intent.EXTRA_STREAM, attachmentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Ép Android mở bằng chính MÀN HÌNH SOẠN MAIL (ComposeActivity), 
                // tránh trường hợp Gmail nhận nhầm intent đẩy sang Google Chat.
                intent.setComponent(new android.content.ComponentName(targetPackage, targetActivity));
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
