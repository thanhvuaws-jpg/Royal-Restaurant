package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DishPageResponse - Mô hình dữ liệu phân trang danh sách món ăn từ API.
 * Hỗ trợ hiển thị danh sách cuộn vô hạn (Infinite Scroll) hoặc phân trang trên giao diện.
 */
public class DishPageResponse {

    // Trạng thái yêu cầu API (ví dụ: "success")
    @SerializedName("status")
    private String status;

    // Danh sách các đối tượng MonResponse (món ăn) nằm trong trang hiện tại
    @SerializedName("data")
    private List<MonResponse> data;

    // Số thứ tự trang hiện tại (1-indexed)
    @SerializedName("page")
    private int page;

    // Giới hạn số lượng món ăn tối đa trong một trang
    @SerializedName("limit")
    private int limit;

    // Tổng số lượng món ăn khớp với điều kiện tìm kiếm/lọc trên server
    @SerializedName("total")
    private int total;

    // Cờ báo hiệu xem có còn dữ liệu ở các trang tiếp theo hay không
    @SerializedName("has_more")
    private boolean hasMore;

    public String getStatus()        { return status; }
    public List<MonResponse> getData() { return data; }
    public int getPage()             { return page; }
    public int getLimit()            { return limit; }
    public int getTotal()            { return total; }
    public boolean isHasMore()       { return hasMore; }
}
