package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Phản hồi của ban_anh_mau.php — ảnh mẫu có sẵn để chọn cho bàn. */
public class AnhMauBanResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("ANH")
    private List<AnhMau> anh;

    public String getStatus() { return status; }
    public List<AnhMau> getAnh() { return anh != null ? anh : new ArrayList<>(); }

    public static class AnhMau {
        /** Đường dẫn gửi lại nguyên văn trong trường `hinhanh` khi lưu bàn. */
        @SerializedName("DUONG_DAN")
        private String duongDan;

        @SerializedName("URL_ANH_NHO")
        private String urlAnhNho;

        public String getDuongDan() { return duongDan; }
        public String getUrlAnhNho() { return urlAnhNho; }
    }
}
