package com.sinhvien.orderdrinkapp.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sinhvien.orderdrinkapp.Activities.AddStaffActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Activities.RegisterActivity;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayStaff;
import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;
import com.sinhvien.orderdrinkapp.R;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.Api.StaffResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DisplayStaffFragment extends Fragment {

    GridView gvStaff;
    List<NhanVienDTO> nhanVienDTOS;
    AdapterDisplayStaff adapterDisplayStaff;
    View view;

    ActivityResultLauncher<Intent> resultLauncherAdd = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent intent = result.getData();
                        long ktra = intent.getLongExtra("ketquaktra",0);
                        String chucnang = intent.getStringExtra("chucnang");
                        if(chucnang.equals("themnv"))
                        {
                            if(ktra != 0){
                                HienThiDSNV();
                                Toast.makeText(getActivity(),"Thêm thành công",Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(getActivity(),"Thêm thất bại",Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            if(ktra != 0){
                                HienThiDSNV();
                                Toast.makeText(getActivity(),"Sửa thành công",Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(getActivity(),"Sửa thất bại",Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.displaystaff_layout,container,false);
        ((HomeActivity)getActivity()).getSupportActionBar().setTitle("Quản lý nhân viên");
        setHasOptionsMenu(true);

        gvStaff = (GridView)view.findViewById(R.id.gvStaff) ;
        

        HienThiDSNV();

        registerForContextMenu(gvStaff);

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,View v,ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.edit_context_menu,menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int vitri = menuInfo.position;
        int manv = nhanVienDTOS.get(vitri).getMANV();

        switch (id){
            case R.id.itEdit:
                Intent iEdit = new Intent(getActivity(),AddStaffActivity.class);
                iEdit.putExtra("manv",manv);
                resultLauncherAdd.launch(iEdit);
                break;

            case R.id.itDelete:
                new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                    .setTitle("Xóa nhân viên")
                    .setMessage("Bạn có chắc chắn muốn xóa nhân viên này không?")
                    .setPositiveButton("Xóa", new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface dialog, int which) {
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
                                        HienThiDSNV();
                                        Toast.makeText(getActivity(), R.string.delete_sucessful, Toast.LENGTH_SHORT).show();
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
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
                return true;
        }

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem itAddStaff = menu.add(1,R.id.itAddStaff,1,"Thêm nhân viên");
        itAddStaff.setIcon(R.drawable.ic_baseline_add_24);
        itAddStaff.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.itAddStaff:
                Intent iDangky = new Intent(getActivity(), AddStaffActivity.class);
                resultLauncherAdd.launch(iDangky);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void HienThiDSNV(){
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getStaff().enqueue(new Callback<List<StaffResponse>>() {
            @Override
            public void onResponse(Call<List<StaffResponse>> call, Response<List<StaffResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    nhanVienDTOS = new ArrayList<>();
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
                    capNhatGiaoDien();
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

    private void capNhatGiaoDien() {
        View layout_empty_state = view.findViewById(R.id.layout_empty_state);
        if(nhanVienDTOS != null && nhanVienDTOS.size() > 0){
            adapterDisplayStaff = new AdapterDisplayStaff(getActivity(),R.layout.custom_layout_displaystaff,nhanVienDTOS);
            gvStaff.setAdapter(adapterDisplayStaff);
            adapterDisplayStaff.notifyDataSetChanged();
            if(layout_empty_state != null) layout_empty_state.setVisibility(View.GONE);
            gvStaff.setVisibility(View.VISIBLE);
        } else {
            gvStaff.setVisibility(View.GONE);
            if(layout_empty_state != null) {
                layout_empty_state.setVisibility(View.VISIBLE);
                ((android.widget.TextView)view.findViewById(R.id.txt_empty_StateTitle)).setText("Chưa có nhân viên");
                ((android.widget.TextView)view.findViewById(R.id.txt_empty_StateDesc)).setText("Hãy nhấn nút + để thêm nhân viên mới.");
            }
        }
    }
}
