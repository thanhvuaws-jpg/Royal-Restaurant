package com.sinhvien.orderdrinkapp.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;

import com.sinhvien.orderdrinkapp.Activities.AddStaffActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.Api.StaffResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayStaff;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;
import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;
import com.sinhvien.orderdrinkapp.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DisplayStaffFragment extends Fragment {

    RecyclerView rvStaff;
    TabLayout tabLayoutStaff;
    List<NhanVienDTO> nhanVienDTOS = new ArrayList<>();
    List<NhanVienDTO> allStaffList = new ArrayList<>();
    AdapterDisplayStaff adapterDisplayStaff;
    View view;

    ActivityResultLauncher<Intent> resultLauncherAdd = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        long ktra = result.getData().getLongExtra("ketquaktra", 0);
                        String chucnang = result.getData().getStringExtra("chucnang");
                        if (ktra != 0) {
                            HienThiDSNV();
                            Toast.makeText(getActivity(),
                                    "themnv".equals(chucnang) ? "Thêm thành công" : "Sửa thành công",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(),
                                    "themnv".equals(chucnang) ? "Thêm thất bại" : "Sửa thất bại",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.displaystaff_layout, container, false);
        ((HomeActivity) getActivity()).getSupportActionBar().setTitle("Quản lý nhân viên");
        setHasOptionsMenu(true);

        rvStaff = view.findViewById(R.id.rvStaff);
        rvStaff.setLayoutManager(new LinearLayoutManager(getActivity()));

        tabLayoutStaff = view.findViewById(R.id.tabLayoutStaff);
        if (tabLayoutStaff.getTabCount() == 0) {
            tabLayoutStaff.addTab(tabLayoutStaff.newTab().setText("Nhân viên"));
            tabLayoutStaff.addTab(tabLayoutStaff.newTab().setText("Khách hàng"));
        }
        tabLayoutStaff.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        adapterDisplayStaff = new AdapterDisplayStaff(getActivity(), nhanVienDTOS);
        adapterDisplayStaff.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        rvStaff.setAdapter(adapterDisplayStaff);

        // Xử lý click (chọn nhân viên) và long-click (Sửa/Xóa)
        adapterDisplayStaff.setOnItemClickListener(new AdapterDisplayStaff.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Click thường: mở màn hình sửa
                Intent iEdit = new Intent(getActivity(), AddStaffActivity.class);
                iEdit.putExtra("manv", nhanVienDTOS.get(position).getMANV());
                resultLauncherAdd.launch(iEdit);
            }

            @Override
            public void onItemLongClick(int position) {
                // Giữ lâu: hiện hộp thoại Sửa/Xóa
                int manv = nhanVienDTOS.get(position).getMANV();
                String tenNV = nhanVienDTOS.get(position).getHOTENNV();
                hienThiMenuTuyChon(manv, tenNV, position);
            }
        });

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            HienThiDSNV(swipeRefreshLayout, true);
        });

        HienThiDSNV(savedInstanceState == null);
        return view;
    }

    // Hiện hộp thoại tùy chọn Sửa/Xóa
    private void hienThiMenuTuyChon(int manv, String tenNV, int position) {
        String[] options = {"✏️ Sửa thông tin", "🗑️ Xóa nhân viên"};
        new AlertDialog.Builder(getActivity())
                .setTitle(tenNV)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Sửa
                        Intent iEdit = new Intent(getActivity(), AddStaffActivity.class);
                        iEdit.putExtra("manv", manv);
                        resultLauncherAdd.launch(iEdit);
                    } else {
                        // Xóa
                        xoaNhanVien(manv, position);
                    }
                })
                .show();
    }

    private void xoaNhanVien(int manv, int position) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Xóa nhân viên")
                .setMessage("Bạn có chắc chắn muốn xóa nhân viên này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(getActivity(), "Đang xóa...");
                    progressDialog.show();

                    ApiService apiService = ApiClient.getClient().create(ApiService.class);
                    apiService.manageStaff("delete", manv, "", "", "", "", "", "", "", 0).enqueue(new Callback<OrderResponse>() {
                        @Override
                        public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                            if (progressDialog.isShowing()) progressDialog.dismiss();
                            if (!isAdded() || getActivity() == null) return;
                            if (response.isSuccessful()) {
                                NhanVienDTO toRemove = null;
                                for (NhanVienDTO nv : nhanVienDTOS) {
                                    if (nv.getMANV() == manv) { toRemove = nv; break; }
                                }
                                if (toRemove != null) {
                                    int currentPos = nhanVienDTOS.indexOf(toRemove);
                                    allStaffList.remove(toRemove);
                                    nhanVienDTOS.remove(toRemove);
                                    adapterDisplayStaff.notifyItemRemoved(currentPos);
                                    adapterDisplayStaff.notifyItemRangeChanged(currentPos, nhanVienDTOS.size());
                                }
                                Toast.makeText(getActivity(), R.string.delete_sucessful, Toast.LENGTH_SHORT).show();
                                capNhatTrangThai();
                            }
                        }

                        @Override
                        public void onFailure(Call<OrderResponse> call, Throwable t) {
                            if (progressDialog.isShowing()) progressDialog.dismiss();
                            if (isAdded() && getActivity() != null) {
                                Toast.makeText(getActivity(), "Lỗi xóa Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem itAddStaff = menu.add(1, R.id.itAddStaff, 1, "Thêm nhân viên");
        itAddStaff.setIcon(R.drawable.ic_baseline_add_24);
        itAddStaff.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.itAddStaff) {
            resultLauncherAdd.launch(new Intent(getActivity(), AddStaffActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void HienThiDSNV() {
        HienThiDSNV(true);
    }

    private void HienThiDSNV(boolean fetchApi) {
        HienThiDSNV(null, fetchApi);
    }

    private void HienThiDSNV(SwipeRefreshLayout swipeRefresh, boolean fetchApi) {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        // 1. Tải và hiển thị dữ liệu từ SQLite trước
        LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(getActivity());
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<NhanVienDTO> cachedList = dbHelper.getStaff();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                allStaffList.clear();
                allStaffList.addAll(cachedList);
                applyFilter();
            });
        });

        if (!fetchApi) {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            return;
        }

        // 2. Gọi API đồng bộ ở chế độ nền
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getStaff().enqueue(new Callback<List<StaffResponse>>() {
            @Override
            public void onResponse(Call<List<StaffResponse>> call, Response<List<StaffResponse>> response) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    // Cập nhật dữ liệu mới vào SQLite dưới nền
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncStaff(response.body());
                        List<NhanVienDTO> updatedList = dbHelper.getStaff();
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            allStaffList.clear();
                            allStaffList.addAll(updatedList);
                            applyFilter();
                        });
                    });
                }
            }

            @Override
            public void onFailure(Call<List<StaffResponse>> call, Throwable t) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi đồng bộ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFilter() {
        nhanVienDTOS.clear();
        if (tabLayoutStaff == null) {
            // Trường hợp chưa init tabLayoutStaff
            capNhatTrangThai();
            return;
        }
        int selectedTab = tabLayoutStaff.getSelectedTabPosition();
        for (NhanVienDTO nv : allStaffList) {
            if (selectedTab == 0) {
                // Nhân viên (Quyền 1, 2, 3)
                if (nv.getMAQUYEN() != 4) {
                    nhanVienDTOS.add(nv);
                }
            } else {
                // Khách hàng (Quyền 4)
                if (nv.getMAQUYEN() == 4) {
                    nhanVienDTOS.add(nv);
                }
            }
        }
        adapterDisplayStaff.notifyDataSetChanged();
        capNhatTrangThai();
    }

    private void capNhatTrangThai() {
        View layout_empty_state = view.findViewById(R.id.layout_empty_state);
        int selectedTab = tabLayoutStaff != null ? tabLayoutStaff.getSelectedTabPosition() : 0;
        String title = selectedTab == 0 ? "Chưa có nhân viên" : "Chưa có khách hàng";
        String desc = selectedTab == 0 ? "Hãy nhấn nút + để thêm nhân viên mới." : "Danh sách khách hàng hiện đang trống.";

        if (nhanVienDTOS != null && nhanVienDTOS.size() > 0) {
            rvStaff.setVisibility(View.VISIBLE);
            if (layout_empty_state != null) layout_empty_state.setVisibility(View.GONE);
        } else {
            rvStaff.setVisibility(View.GONE);
            if (layout_empty_state != null) {
                layout_empty_state.setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.txt_empty_StateTitle)).setText(title);
                ((TextView) view.findViewById(R.id.txt_empty_StateDesc)).setText(desc);
            }
        }
    }
}
