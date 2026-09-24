package com.sinhvien.orderdrinkapp.Fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Activities.LapPhieuKhoActivity;
import com.sinhvien.orderdrinkapp.Activities.LichSuKhoActivity;
import com.sinhvien.orderdrinkapp.Api.KhoResponse;
import com.sinhvien.orderdrinkapp.Api.NguyenLieuResponse;
import com.sinhvien.orderdrinkapp.Api.NhomNguyenLieuResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterKhoNguyenLieu;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.ViewModel.KhoViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * KhoFragment - Màn hình chính quản lý Kho nguyên vật liệu trên Android.
 * - Danh sách tồn kho phân trang 20 mục / trang.
 * - RecyclerView dùng DiffUtil và setHasFixedSize(true).
 * - Tự động tải trang tiếp theo khi còn 5 mục cuối.
 * - Hủy yêu cầu mạng khi rời màn hình (onStop).
 */
public class KhoFragment extends Fragment {

    private KhoViewModel viewModel;
    private AdapterKhoNguyenLieu adapter;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rcvKho;
    private TextView txtEmptyState;
    private ProgressBar progressBar;

    private TextView txtCardAm;
    private TextView txtCardHet;
    private TextView txtCardSapHet;
    private TextView txtCardDu;
    private TextView txtCardCanhBaoHan;

    private EditText edtTimKiem;
    private Spinner spnNhomNl;
    private CheckBox chkChiCanhBao;
    private Button btnLapPhieu;
    private Button btnKiemKe;

    private final List<NhomNguyenLieuResponse.NhomItem> listNhom = new ArrayList<>();
    private ArrayAdapter<NhomNguyenLieuResponse.NhomItem> spinnerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kho, container, false);

        initViews(view);
        setupRecyclerView();
        setupViewModel();
        setupListeners();

        // Tải dữ liệu ban đầu
        viewModel.loadNhom();
        viewModel.loadFirstPage();

        return view;
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        rcvKho = view.findViewById(R.id.rcv_kho);
        txtEmptyState = view.findViewById(R.id.txt_empty_state);
        progressBar = view.findViewById(R.id.progress_loading);

        txtCardAm = view.findViewById(R.id.txt_card_am);
        txtCardHet = view.findViewById(R.id.txt_card_het);
        txtCardSapHet = view.findViewById(R.id.txt_card_sap_het);
        txtCardDu = view.findViewById(R.id.txt_card_du);
        txtCardCanhBaoHan = view.findViewById(R.id.txt_card_canh_bao_han);

        edtTimKiem = view.findViewById(R.id.edt_tim_kiem);
        spnNhomNl = view.findViewById(R.id.spn_nhom_nl);
        chkChiCanhBao = view.findViewById(R.id.chk_chi_canh_bao);
        btnLapPhieu = view.findViewById(R.id.btn_lap_phieu);
        btnKiemKe = view.findViewById(R.id.btn_kiem_ke);

        // Yêu cầu 11: chỉ Quản lý (quyền 1) lập phiếu và kiểm kê; nhân viên
        // chỉ xem tồn. Máy chủ đã chặn 403, nhưng để nút hiện thì nhân viên
        // điền hết biểu mẫu, chụp ảnh, rồi mới biết mình không có quyền.
        if (!SessionManager.isAdmin(requireContext())) {
            btnLapPhieu.setVisibility(View.GONE);
            btnKiemKe.setVisibility(View.GONE);
        }

        // Spinner nhóm nguyên liệu
        listNhom.clear();
        listNhom.add(new NhomNguyenLieuResponse.NhomItem(0, "Tất cả nhóm"));
        spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, listNhom);
        spnNhomNl.setAdapter(spinnerAdapter);
    }

    private void setupRecyclerView() {
        rcvKho.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rcvKho.setLayoutManager(layoutManager);

        adapter = new AdapterKhoNguyenLieu(getContext(), item -> {
            // Mở màn hình Sổ kho (LichSuKhoActivity)
            Intent intent = new Intent(getContext(), LichSuKhoActivity.class);
            intent.putExtra("manl", item.getMaNL());
            intent.putExtra("tennl", item.getTenNL());
            intent.putExtra("donvi", item.getDonVi());
            intent.putExtra("ton", item.getTonHienTai());
            startActivity(intent);
        });
        rcvKho.setAdapter(adapter);

        // Cuộn tới gần cuối (còn 5 mục cuối) thì tải tiếp trang mới
        rcvKho.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                    if (totalItemCount > 0 && lastVisibleItem >= totalItemCount - 5) {
                        viewModel.loadNextPage();
                    }
                }
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(KhoViewModel.class);

        // Danh sách nguyên liệu
        viewModel.getNguyenLieuList().observe(getViewLifecycleOwner(), list -> {
            adapter.updateList(list);
            if (list == null || list.isEmpty()) {
                txtEmptyState.setVisibility(View.VISIBLE);
                rcvKho.setVisibility(View.GONE);
            } else {
                txtEmptyState.setVisibility(View.GONE);
                rcvKho.setVisibility(View.VISIBLE);
            }
        });

        // Tóm tắt tồn
        viewModel.getDemTon().observe(getViewLifecycleOwner(), demTon -> {
            if (demTon != null) {
                txtCardAm.setText(String.valueOf(demTon.getAm()));
                txtCardHet.setText(String.valueOf(demTon.getHet()));
                txtCardSapHet.setText(String.valueOf(demTon.getSapHet()));
                txtCardDu.setText(String.valueOf(demTon.getDu()));
            }
        });

        // Cảnh báo hạn
        viewModel.getCanhBaoHan().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                txtCardCanhBaoHan.setText(String.valueOf(count));
            }
        });

        // Loading
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                if (adapter.getItemCount() == 0) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            } else {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }
        });

        // Nhóm nguyên liệu
        viewModel.getNhomList().observe(getViewLifecycleOwner(), nhoms -> {
            if (nhoms != null && !nhoms.isEmpty()) {
                listNhom.clear();
                listNhom.add(new NhomNguyenLieuResponse.NhomItem(0, "Tất cả nhóm"));
                listNhom.addAll(nhoms);
                spinnerAdapter.notifyDataSetChanged();
            }
        });

        // Thông báo lỗi
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty() && getContext() != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(() -> viewModel.loadFirstPage());

        // Lọc tìm kiếm
        edtTimKiem.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                triggerFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Lọc nhóm
        spnNhomNl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                triggerFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Lọc chỉ cảnh báo
        chkChiCanhBao.setOnCheckedChangeListener((buttonView, isChecked) -> triggerFilter());

        // Nút Lập phiếu kho
        btnLapPhieu.setOnClickListener(v -> showChonLoaiPhieuDialog());

        // Nút Kiểm kê
        btnKiemKe.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).navigateToSubFragment(new KiemKeFragment(), "KiemKeFragment");
            }
        });
    }

    private void triggerFilter() {
        String keyword = edtTimKiem.getText() != null ? edtTimKiem.getText().toString().trim() : "";
        NhomNguyenLieuResponse.NhomItem selectedItem = (NhomNguyenLieuResponse.NhomItem) spnNhomNl.getSelectedItem();
        Integer maNhom = (selectedItem != null && selectedItem.getMaNhom() > 0) ? selectedItem.getMaNhom() : null;
        boolean chiCanhBao = chkChiCanhBao.isChecked();

        viewModel.applyFilter(maNhom, keyword, chiCanhBao);
    }

    private void showChonLoaiPhieuDialog() {
        String[] options = {"Phiếu xuất kho", "Phiếu hủy hàng"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn loại phiếu cần lập")
                .setItems(options, (dialog, which) -> {
                    Intent intent = new Intent(getContext(), LapPhieuKhoActivity.class);
                    if (which == 0) {
                        intent.putExtra("loai", "xuat");
                    } else {
                        intent.putExtra("loai", "huy");
                    }
                    startActivity(intent);
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null && adapter.getItemCount() > 0) {
            viewModel.loadFirstPage();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Dừng các yêu cầu mạng khi rời màn hình (Requirement 12.7)
        if (viewModel != null) {
            viewModel.cancelPendingCalls();
        }
    }
}
