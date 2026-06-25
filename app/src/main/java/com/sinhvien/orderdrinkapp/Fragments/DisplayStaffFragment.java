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
import androidx.lifecycle.ViewModelProvider;
import com.sinhvien.orderdrinkapp.ViewModel.StaffViewModel;

/**
 * DisplayStaffFragment - Màn hình Quản lý Danh sách Nhân viên và Khách hàng (Staff & Customer Management).
 * - Sử dụng TabLayout phân tách 2 danh sách:
 *   1. Nhân viên (Quyền hạn 1, 2, 3: Quản lý, Phục vụ, Thu ngân).
 *   2. Khách hàng (Quyền hạn 4: Khách hàng thành viên đăng ký ứng dụng).
 * - Cho phép Admin (Quản trị viên) tạo mới nhân viên (AddStaffActivity), cập nhật thông tin.
 * - Cho phép giữ lâu (Long click) để mở Dialog xóa tài khoản (đồng bộ gọi API RESTful).
 * - Sử dụng StaffViewModel kết hợp LiveData phục vụ hiển thị mượt mà.
 */
public class DisplayStaffFragment extends Fragment {

    // RecyclerView hiển thị danh sách người dùng
    RecyclerView rvStaff;
    // TabLayout chuyển đổi giữa danh sách Nhân viên & Khách hàng
    TabLayout tabLayoutStaff;
    // Danh sách hiển thị hiện thời trên RecyclerView (đã qua lọc)
    List<NhanVienDTO> nhanVienDTOS = new ArrayList<>();
    // Danh sách tổng hợp toàn bộ dữ liệu tải về từ SQLite/Server
    List<NhanVienDTO> allStaffList = new ArrayList<>();
    // Adapter hiển thị thông tin nhân viên
    AdapterDisplayStaff adapterDisplayStaff;
    View view;
    // ViewModel quản lý nhân viên
    private StaffViewModel staffViewModel;

    // Trình launcher đón nhận kết quả trả về khi thêm/sửa nhân viên thành công
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

        // Thiết lập hai tab: Nhân viên & Khách hàng
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

        // Khởi tạo Adapter
        adapterDisplayStaff = new AdapterDisplayStaff(getActivity(), nhanVienDTOS);
        adapterDisplayStaff.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        rvStaff.setAdapter(adapterDisplayStaff);

        // Thiết lập sự kiện click và giữ lâu trên RecyclerView
        adapterDisplayStaff.setOnItemClickListener(new AdapterDisplayStaff.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Click thường: Mở màn hình chỉnh sửa nhân viên
                Intent iEdit = new Intent(getActivity(), AddStaffActivity.class);
                iEdit.putExtra("manv", nhanVienDTOS.get(position).getMANV());
                resultLauncherAdd.launch(iEdit);
            }

            @Override
            public void onItemLongClick(int position) {
                // Click giữ lâu: Hiển thị Menu lựa chọn Sửa hoặc Xóa nhân viên
                int manv = nhanVienDTOS.get(position).getMANV();
                String tenNV = nhanVienDTOS.get(position).getHOTENNV();
                hienThiMenuTuyChon(manv, tenNV, position);
            }
        });

        // SwipeRefreshLayout vuốt để đồng bộ lại từ server
        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            HienThiDSNV(swipeRefreshLayout, true);
        });

        // Đăng ký quan sát dữ liệu danh sách nhân viên từ ViewModel
        staffViewModel = new ViewModelProvider(this).get(StaffViewModel.class);
        staffViewModel.getStaff().observe(getViewLifecycleOwner(), list -> {
            allStaffList.clear();
            allStaffList.addAll(list);
            applyFilter();
        });

        // Tải danh sách nhân viên lúc khởi chạy màn hình
        HienThiDSNV(savedInstanceState == null);
        return view;
    }

    /**
     * Hiển thị danh mục tùy chọn (Sửa/Xóa) khi quản trị viên click giữ lâu thông tin nhân viên.
     */
    private void hienThiMenuTuyChon(int manv, String tenNV, int position) {
        String[] options = {"✏️ Sửa thông tin", "🗑️ Xóa nhân viên"};
        new AlertDialog.Builder(getActivity())
                .setTitle(tenNV)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent iEdit = new Intent(getActivity(), AddStaffActivity.class);
                        iEdit.putExtra("manv", manv);
                        resultLauncherAdd.launch(iEdit);
                    } else {
                        xoaNhanVien(manv, position);
                    }
                })
                .show();
    }

    /**
     * Thực hiện gửi yêu cầu xóa tài khoản người dùng lên Server và cập nhật cục bộ.
     */
    private void xoaNhanVien(int manv, int position) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Xóa nhân viên")
                .setMessage("Bạn có chắc chắn muốn xóa nhân viên này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(getActivity(), "Đang xóa...");
                    progressDialog.show();

                    ApiService apiService = ApiClient.getClient().create(ApiService.class);
                    // Gọi API DELETE quản lý nhân viên
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
                                    // Hiệu ứng mượt mà khi xóa dòng trong danh sách RecyclerView
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

    /**
     * Khởi tạo Menu Thêm nhân viên trên thanh ActionBar.
     */
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

    /**
     * Tải dữ liệu danh sách người dùng từ SQLite nội bộ và đồng bộ API.
     */
    private void HienThiDSNV(SwipeRefreshLayout swipeRefresh, boolean fetchApi) {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        
        staffViewModel.loadStaffFromLocal();

        if (!fetchApi) {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            return;
        }

        staffViewModel.syncStaffFromServer(new StaffViewModel.OnSyncCallback() {
            @Override
            public void onSuccess() {
                if (swipeRefresh != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        swipeRefresh.setRefreshing(false);
                    });
                }
            }

            @Override
            public void onError(String errorMsg) {
                if (swipeRefresh != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        swipeRefresh.setRefreshing(false);
                    });
                }
                if (isAdded() && getActivity() != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getActivity(), "Lỗi đồng bộ: " + errorMsg, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * Lọc danh sách nhân viên theo Tab đang được chọn.
     * - Tab 0: Nhân viên (Quyền 1, 2, 3 - Quản lý, Phục vụ, Thu ngân).
     * - Tab 1: Khách hàng (Quyền 4 - Khách hàng thành viên đăng ký app).
     */
    private void applyFilter() {
        nhanVienDTOS.clear();
        if (tabLayoutStaff == null) {
            capNhatTrangThai();
            return;
        }
        int selectedTab = tabLayoutStaff.getSelectedTabPosition();
        for (NhanVienDTO nv : allStaffList) {
            if (selectedTab == 0) {
                if (nv.getMAQUYEN() != 4) {
                    nhanVienDTOS.add(nv);
                }
            } else {
                if (nv.getMAQUYEN() == 4) {
                    nhanVienDTOS.add(nv);
                }
            }
        }
        adapterDisplayStaff.notifyDataSetChanged();
        capNhatTrangThai();
    }

    /**
     * Hiển thị trạng thái giao diện báo danh sách trống (Empty State).
     */
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
