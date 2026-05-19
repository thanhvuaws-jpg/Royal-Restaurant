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

import com.sinhvien.orderdrinkapp.Activities.AddStaffActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.Api.StaffResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayStaff;
import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;
import com.sinhvien.orderdrinkapp.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DisplayStaffFragment extends Fragment {

    RecyclerView rvStaff;
    List<NhanVienDTO> nhanVienDTOS = new ArrayList<>();
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

        adapterDisplayStaff = new AdapterDisplayStaff(getActivity(), nhanVienDTOS);
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

        HienThiDSNV();
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
                    android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getActivity());
                    progressDialog.setMessage("Đang xóa...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    ApiService apiService = ApiClient.getClient().create(ApiService.class);
                    apiService.manageStaff("delete", manv, "", "", "", "", "", "", "", 0).enqueue(new Callback<OrderResponse>() {
                        @Override
                        public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                            if (progressDialog.isShowing()) progressDialog.dismiss();
                            if (!isAdded() || getActivity() == null) return;
                            if (response.isSuccessful()) {
                                nhanVienDTOS.remove(position);
                                adapterDisplayStaff.notifyItemRemoved(position);
                                adapterDisplayStaff.notifyItemRangeChanged(position, nhanVienDTOS.size());
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
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getStaff().enqueue(new Callback<List<StaffResponse>>() {
            @Override
            public void onResponse(Call<List<StaffResponse>> call, Response<List<StaffResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    nhanVienDTOS.clear();
                    for (StaffResponse res : response.body()) {
                        NhanVienDTO dto = new NhanVienDTO();
                        dto.setMANV(res.getMaNV());
                        dto.setHOTENNV(res.getHoTenNV());
                        dto.setTENDN(res.getTenDN());
                        dto.setEMAIL(res.getEmail());
                        dto.setSDT(res.getSdt());
                        dto.setGIOITINH(res.getGioiTinh());
                        dto.setNGAYSINH(res.getNgaySinh());
                        dto.setMAQUYEN(res.getMaQuyen());
                        nhanVienDTOS.add(dto);
                    }
                    adapterDisplayStaff.notifyDataSetChanged();
                    capNhatTrangThai();
                }
            }

            @Override
            public void onFailure(Call<List<StaffResponse>> call, Throwable t) {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void capNhatTrangThai() {
        View layout_empty_state = view.findViewById(R.id.layout_empty_state);
        if (nhanVienDTOS != null && nhanVienDTOS.size() > 0) {
            rvStaff.setVisibility(View.VISIBLE);
            if (layout_empty_state != null) layout_empty_state.setVisibility(View.GONE);
        } else {
            rvStaff.setVisibility(View.GONE);
            if (layout_empty_state != null) {
                layout_empty_state.setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.txt_empty_StateTitle)).setText("Chưa có nhân viên");
                ((TextView) view.findViewById(R.id.txt_empty_StateDesc)).setText("Hãy nhấn nút + để thêm nhân viên mới.");
            }
        }
    }
}
