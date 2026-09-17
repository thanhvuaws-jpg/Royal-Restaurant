package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả của `chat_gui.php`.
 *
 * Máy chủ trả về CẢ HAI tin vừa sinh ra — tin của khách và câu đáp của bot —
 * để màn hình không phải gọi thêm một lượt nữa chỉ để thấy câu trả lời.
 *
 * `botTraLoi` cho biết bot trả lời THẬT (khớp câu hỏi thường gặp) hay chỉ
 * hứa chuyển tiếp cho nhân viên. Hai việc rất khác nhau và giao diện nên
 * phân biệt được.
 */
public class ChatGuiResponse {

    @SerializedName("status")      private String        status;
    @SerializedName("message")     private String        message;
    @SerializedName("MAHOITHOAI")  private int           maHoiThoai;
    @SerializedName("tin_moi")     private List<TinNhan> tinMoi;
    @SerializedName("bot_tra_loi") private boolean       botTraLoi;

    public String        getStatus()     { return status; }
    public String        getMessage()    { return message; }
    public int           getMaHoiThoai() { return maHoiThoai; }
    public List<TinNhan> getTinMoi()     { return tinMoi != null ? tinMoi : new ArrayList<>(); }
    public boolean       isBotTraLoi()   { return botTraLoi; }

    public boolean thanhCong() { return "success".equals(status); }
}
