package com.sinhvien.orderdrinkapp.Fragments;

import android.app.Activity;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sinhvien.orderdrinkapp.Activities.AddCategoryActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayCategory;
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.LoaiMonResponse;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DisplayCategoryFragment extends Fragment {

    GridView gv_category_CategoryList;
    List<LoaiMonDTO> loaiMonDTOList;
    AdapterDisplayCategory adapter;
    FragmentManager fragmentManager;
    int maban;
    View view;

    ActivityResultLauncher<Intent> resultLauncherCategory = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        if (intent != null) {
                            boolean ktra = intent.getBooleanExtra("ktra", false);
                            String chucnang = intent.getStringExtra("chucnang");
                            if ("themloai".equals(chucnang)) {
                                if (ktra) {
                                    HienThiDSLoai();
                                    Toast.makeText(getActivity(), R.string.add_sucessful, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(), R.string.add_failed, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                if (ktra) {
                                    HienThiDSLoai();
                                    Toast.makeText(getActivity(), R.string.edit_sucessful, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(), "Sửa thất bại", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
            });


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.displaycategory_layout, container, false);
        setHasOptionsMenu(true);
        gv_category_CategoryList = (GridView) view.findViewById(R.id.gv_category_CategoryList);
        view.findViewById(R.id.img_category_Back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });
        fragmentManager = getActivity().getSupportFragmentManager();
        HienThiDSLoai();

        Bundle bDataCategory = getArguments();
        if (bDataCategory != null) {
            maban = bDataCategory.getInt("maban");
        }

        gv_category_CategoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int maloai = loaiMonDTOList.get(position).getMaLoai();
                String tenloai = loaiMonDTOList.get(position).getTenLoai();
                DisplayMenuFragment displayMenuFragment = new DisplayMenuFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("maloai", maloai);
                bundle.putString("tenloai", tenloai);
                bundle.putInt("maban", maban);
                displayMenuFragment.setArguments(bundle);

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.contentView, displayMenuFragment).addToBackStack("hienthiloai");
                transaction.commit();
            }
        });

        registerForContextMenu(gv_category_CategoryList);

        // Kết nối nút Thêm loại món nổi (FAB)
        view.findViewById(R.id.fab_add_category).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddCategoryActivity.class);
                resultLauncherCategory.launch(intent);
            }
        });

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (SessionManager.isAdmin(getActivity())) {
            getActivity().getMenuInflater().inflate(R.menu.edit_context_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int vitri = menuInfo.position;
        int maloai = loaiMonDTOList.get(vitri).getMaLoai();

        if (id == R.id.itEdit) {
            Intent iEdit = new Intent(getActivity(), AddCategoryActivity.class);
            iEdit.putExtra("maloai", maloai);
            resultLauncherCategory.launch(iEdit);
            return true;
        } else if (id == R.id.itDelete) {
            new android.app.AlertDialog.Builder(getActivity())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa loại món này?")
                .setPositiveButton("Xóa", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getActivity());
                        progressDialog.setMessage("Đang xóa...");
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        ApiService apiService = ApiClient.getClient().create(ApiService.class);
                        apiService.manageCategory("delete", maloai, "", "").enqueue(new Callback<OrderResponse>() {
                            @Override
                            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                                if (progressDialog.isShowing()) progressDialog.dismiss();
                                if (!isAdded() || getActivity() == null) return;
                                if (response.isSuccessful()) {
                                    HienThiDSLoai();
                                    Toast.makeText(getActivity(), R.string.delete_sucessful, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(), R.string.delete_failed, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<OrderResponse> call, Throwable t) {
                                if (progressDialog.isShowing()) progressDialog.dismiss();
                                if (isAdded() && getActivity() != null) {
                                    Toast.makeText(getActivity(), "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (SessionManager.isAdmin(getActivity())) {
            MenuItem itAddCategory = menu.add(1, R.id.itAddCategory, 1, R.string.addCategory);
            itAddCategory.setIcon(R.drawable.ic_baseline_add_24);
            itAddCategory.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.itAddCategory) {
            Intent intent = new Intent(getActivity(), AddCategoryActivity.class);
            resultLauncherCategory.launch(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void HienThiDSLoai() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCategories().enqueue(new Callback<List<LoaiMonResponse>>() {
            @Override
            public void onResponse(Call<List<LoaiMonResponse>> call, Response<List<LoaiMonResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    loaiMonDTOList = new ArrayList<>();
                    // Chuyển đổi từ dữ liệu Server sang DTO của App
                    for (LoaiMonResponse res : response.body()) {
                        LoaiMonDTO dto = new LoaiMonDTO();
                        dto.setMaLoai(res.getMaLoai());
                        dto.setTenLoai(res.getTenLoai());
                        dto.setHinhAnhPath(res.getHinhAnh());
                        loaiMonDTOList.add(dto);
                    }
                    capNhatGiaoDien();
                }
            }

            @Override
            public void onFailure(Call<List<LoaiMonResponse>> call, Throwable t) {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void capNhatGiaoDien() {
        View layout_empty_state = view.findViewById(R.id.layout_empty_state);
        if (loaiMonDTOList != null && loaiMonDTOList.size() > 0) {
            adapter = new AdapterDisplayCategory(getActivity(), R.layout.custom_layout_displaycategory, loaiMonDTOList);
            gv_category_CategoryList.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            if (layout_empty_state != null) layout_empty_state.setVisibility(View.GONE);
            gv_category_CategoryList.setVisibility(View.VISIBLE);
        } else {
            gv_category_CategoryList.setVisibility(View.GONE);
            if (layout_empty_state != null) {
                layout_empty_state.setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.txt_empty_StateTitle)).setText(R.string.empty_list_title);
                ((TextView) view.findViewById(R.id.txt_empty_StateDesc)).setText(R.string.empty_list_desc);
            }
        }
    }
}
