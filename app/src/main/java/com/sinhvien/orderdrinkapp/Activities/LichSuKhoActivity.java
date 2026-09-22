package com.sinhvien.orderdrinkapp.Activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.NguyenLieuResponse;
import com.sinhvien.orderdrinkapp.Api.SoKhoResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDongSoKho;
import com.sinhvien.orderdrinkapp.R;

import java.text.DecimalFormat;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LichSuKhoActivity - Xem chi tiết Sổ kho của một nguyên liệu.
 * Hiển thị các biến động tăng giảm theo thứ tự mới nhất trước.
 */
public class LichSuKhoActivity extends AppCompatActivity {

    private int maNL;
    private String tenNL = "";
    private String donVi = "";
    private double ton = 0.0;

    private TextView txtHeaderTenNL;
    private TextView txtHeaderMaNL;
    private TextView txtHeaderTonHienTai;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rcvSoKho;
    private TextView txtEmpty;
    private ProgressBar progressBar;

    private AdapterDongSoKho adapter;
    private Call<SoKhoResponse> call;
    private final DecimalFormat df = new DecimalFormat("#,##0.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lich_su_kho);

        // Lấy thông tin từ Intent
        maNL = getIntent().getIntExtra("manl", 0);
        tenNL = getIntent().getStringExtra("tennl");
        if (tenNL == null) tenNL = "";
        donVi = getIntent().getStringExtra("donvi");
        if (donVi == null) donVi = "";
        ton = getIntent().getDoubleExtra("ton", 0.0);

        initViews();
        loadData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_lich_su);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sổ kho: " + tenNL);
        }

        txtHeaderTenNL = findViewById(R.id.txt_header_ten_nl);
        txtHeaderMaNL = findViewById(R.id.txt_header_ma_nl);
        txtHeaderTonHienTai = findViewById(R.id.txt_header_ton_hientai);

        txtHeaderTenNL.setText(tenNL);
        txtHeaderMaNL.setText("Mã NL: #" + maNL);
        txtHeaderTonHienTai.setText("Tồn hiện tại: " + df.format(ton) + " " + donVi);

        swipeRefresh = findViewById(R.id.swipe_refresh_lich_su);
        rcvSoKho = findViewById(R.id.rcv_so_kho);
        txtEmpty = findViewById(R.id.txt_empty_so_kho);
        progressBar = findViewById(R.id.progress_loading_so_kho);

        rcvSoKho.setLayoutManager(new LinearLayoutManager(this));
        rcvSoKho.setHasFixedSize(true);
        adapter = new AdapterDongSoKho(this, donVi);
        rcvSoKho.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void loadData() {
        if (maNL <= 0) {
            Toast.makeText(this, "Mã nguyên liệu không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (call != null && !call.isCanceled()) {
            call.cancel();
        }

        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        call = api.getLichSuKho(maNL, 100);

        call.enqueue(new Callback<SoKhoResponse>() {
            @Override
            public void onResponse(Call<SoKhoResponse> c, Response<SoKhoResponse> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    SoKhoResponse res = response.body();
                    NguyenLieuResponse nl = res.getNguyenLieu();
                    if (nl != null) {
                        tenNL = nl.getTenNL();
                        donVi = nl.getDonVi();
                        ton = nl.getTonHienTai();

                        txtHeaderTenNL.setText(tenNL);
                        txtHeaderTonHienTai.setText("Tồn hiện tại: " + df.format(ton) + " " + donVi);
                    }

                    List<SoKhoResponse.DongSoKho> lichSu = res.getLichSu();
                    if (lichSu == null || lichSu.isEmpty()) {
                        txtEmpty.setVisibility(View.VISIBLE);
                        rcvSoKho.setVisibility(View.GONE);
                    } else {
                        txtEmpty.setVisibility(View.GONE);
                        rcvSoKho.setVisibility(View.VISIBLE);
                        adapter.setData(lichSu);
                    }
                } else {
                    Toast.makeText(LichSuKhoActivity.this, "Không thể tải sổ kho (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SoKhoResponse> c, Throwable t) {
                if (c.isCanceled()) return;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(LichSuKhoActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
