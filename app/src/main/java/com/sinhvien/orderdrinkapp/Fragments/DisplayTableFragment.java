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
import androidx.fragment.app.Fragment;

import com.sinhvien.orderdrinkapp.Activities.AddTableActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.Api.TableResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayTable;
import com.sinhvien.orderdrinkapp.DTO.BanAnDTO;
import com.sinhvien.orderdrinkapp.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DisplayTableFragment extends Fragment {

    GridView gvDisplayTable;
    List<BanAnDTO> banAnDTOList;
    AdapterDisplayTable adapterDisplayTable;

    ActivityResultLauncher<Intent> resultLauncherAdd = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        HienThiDSBan();
                    }
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.displaytable_layout, container, false);
        setHasOptionsMenu(true);
        ((HomeActivity)getActivity()).getSupportActionBar().setTitle("Quản lý bàn");

        gvDisplayTable = view.findViewById(R.id.gvDisplayTable);
        view.findViewById(R.id.img_table_Back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });

        // HienThiDSBan() is called in onResume() instead of here

        registerForContextMenu(gvDisplayTable);

        // Kết nối nút Thêm bàn nổi (FAB)
        view.findViewById(R.id.fab_add_table).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddTableActivity.class);
                resultLauncherAdd.launch(intent);
            }
        });

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.edit_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int vitri = menuInfo.position;
        int maban = banAnDTOList.get(vitri).getMaBan();

        switch (id){
            case R.id.itEdit:
                Intent intent = new Intent(getActivity(), AddTableActivity.class);
                intent.putExtra("maban", maban);
                resultLauncherAdd.launch(intent);
                break;

            case R.id.itDelete:
                new android.app.AlertDialog.Builder(getActivity())
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa bàn này?")
                    .setPositiveButton("Xóa", new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface dialog, int which) {
                            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getActivity());
                            progressDialog.setMessage("Đang xóa...");
                            progressDialog.setCancelable(false);
                            progressDialog.show();

                            ApiService apiService = ApiClient.getClient().create(ApiService.class);
                            apiService.manageTable("delete", maban, "").enqueue(new Callback<OrderResponse>() {
                                @Override
                                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                                    if (progressDialog.isShowing()) progressDialog.dismiss();
                                    if (!isAdded() || getActivity() == null) return;
                                    if (response.isSuccessful()) {
                                        HienThiDSBan();
                                        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.delete_sucessful), Toast.LENGTH_SHORT).show();
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
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem itAddTable = menu.add(1, R.id.itAddTable, 1, R.string.addTable);
        itAddTable.setIcon(R.drawable.ic_baseline_add_24);
        itAddTable.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.itAddTable){
            Intent iAddTable = new Intent(getActivity(), AddTableActivity.class);
            resultLauncherAdd.launch(iAddTable);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        HienThiDSBan();
    }

    private void HienThiDSBan(){
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getTables().enqueue(new Callback<List<TableResponse>>() {
            @Override
            public void onResponse(Call<List<TableResponse>> call, Response<List<TableResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    banAnDTOList = new ArrayList<>();
                    for (TableResponse res : response.body()) {
                        BanAnDTO dto = new BanAnDTO();
                        dto.setMaBan(res.getMaBan());
                        dto.setTenBan(res.getTenBan());
                        dto.setTinhTrang(res.getTinhTrang());
                        banAnDTOList.add(dto);
                    }
                    adapterDisplayTable = new AdapterDisplayTable(getActivity(), R.layout.custom_layout_displaytable, banAnDTOList);
                    gvDisplayTable.setAdapter(adapterDisplayTable);
                    adapterDisplayTable.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<TableResponse>> call, Throwable t) {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
