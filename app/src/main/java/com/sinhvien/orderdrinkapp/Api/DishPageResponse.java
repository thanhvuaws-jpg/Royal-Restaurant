package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DishPageResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private List<MonResponse> data;

    @SerializedName("page")
    private int page;

    @SerializedName("limit")
    private int limit;

    @SerializedName("total")
    private int total;

    @SerializedName("has_more")
    private boolean hasMore;

    public String getStatus()        { return status; }
    public List<MonResponse> getData() { return data; }
    public int getPage()             { return page; }
    public int getLimit()            { return limit; }
    public int getTotal()            { return total; }
    public boolean isHasMore()       { return hasMore; }
}
