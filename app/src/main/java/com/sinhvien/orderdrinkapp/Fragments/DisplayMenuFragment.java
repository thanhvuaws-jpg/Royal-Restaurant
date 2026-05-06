package com.sinhvien.orderdrinkapp.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
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
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.sinhvien.orderdrinkapp.Activities.AddMenuActivity;
import com.sinhvien.orderdrinkapp.Activities.AmountMenuActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayMenu;
import com.sinhvien.orderdrinkapp.DTO.MonDTO;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.MonResponse;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DisplayMenuFragment extends Fragment {

    int maloai, maban;
    String tenloai, tinhtrang;
    GridView gv_menu_DishList;
    List<MonDTO> monDTOList;
    AdapterDisplayMenu adapterDisplayMenu;
    View view;

    ActivityResultLauncher<Intent> resultLauncherMenu = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        if (intent != null) {
                            boolean ktra = intent.getBooleanExtra("ktra", false);
                            String chucnang = intent.getStringExtra("chucnang");
                            if ("themmon".equals(chucnang)) {
                                if (ktra) {
                                    HienThiDSMon();
                                    Toast.makeText(getActivity(), R.string.add_sucessful, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(), R.string.add_failed, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                if (ktra) {
                                    HienThiDSMon();
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
        view = inflater.inflate(R.layout.displaymenu_layout, container, false);
        if (getActivity() != null && ((HomeActivity) getActivity()).getSupportActionBar() != null) {
            ((HomeActivity) getActivity()).getSupportActionBar().setTitle(R.string.nav_menu);
        }
        gv_menu_DishList = (GridView) view.findViewById(R.id.gv_menu_DishList);
        view.findViewById(R.id.img_menu_Back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });
        
        Bundle bundle = getArguments();
        if (bundle != null) {
            maloai = bundle.getInt("maloai");
            tenloai = bundle.getString("tenloai");
            maban = bundle.getInt("maban");
            HienThiDSMon();

            gv_menu_DishList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    tinhtrang = monDTOList.get(position).getTinhTrang();
                    if (maban != 0) {
                        if ("true".equals(tinhtrang)) {
                            Intent iAmount = new Intent(getActivity(), AmountMenuActivity.class);
                            iAmount.putExtra("maban", maban);
                            iAmount.putExtra("mamon", monDTOList.get(position).getMaMon());
                            startActivity(iAmount);
                        } else {
                            Toast.makeText(getActivity(),
                                    R.string.dish_out_of_stock_msg,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
        setHasOptionsMenu(true);
        registerForContextMenu(gv_menu_DishList);

        // Kết nối nút Thêm món nổi (FAB)
        view.findViewById(R.id.fab_add_dish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddMenuActivity.class);
                intent.putExtra("maloai", maloai);
                intent.putExtra("tenloai", tenloai);
                resultLauncherMenu.launch(intent);
            }
        });

        SearchView sv_menu_SearchDish = (SearchView) view.findViewById(R.id.sv_menu_SearchDish);
        sv_menu_SearchDish.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapterDisplayMenu != null) {
                    adapterDisplayMenu.getFilter().filter(newText);
                }
                return true;
            }
        });

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                    getParentFragmentManager().popBackStack("hienthiloai", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    return true;
                }
                return false;
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
        int mamon = monDTOList.get(vitri).getMaMon();

        if (id == R.id.itEdit) {
            Intent iEdit = new Intent(getActivity(), AddMenuActivity.class);
            iEdit.putExtra("mamon", mamon);
            iEdit.putExtra("maloai", maloai);
            iEdit.putExtra("tenloai", tenloai);
            resultLauncherMenu.launch(iEdit);
            return true;
        } else if (id == R.id.itDelete) {
            new android.app.AlertDialog.Builder(getActivity())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa món này?")
                .setPositiveButton("Xóa", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getActivity());
                        progressDialog.setMessage("Đang xóa...");
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        ApiService apiService = ApiClient.getClient().create(ApiService.class);
                        apiService.manageDish("delete", mamon, "", "", 0, "", "").enqueue(new Callback<OrderResponse>() {
                            @Override
                            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                                if (progressDialog.isShowing()) progressDialog.dismiss();
                                if (!isAdded() || getActivity() == null) return;
                                if (response.isSuccessful() && response.body() != null) {
                                    OrderResponse res = response.body();
                                    if ("success".equals(res.getStatus())) {
                                        HienThiDSMon();
                                        Toast.makeText(getActivity(), R.string.delete_sucessful, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getActivity(), "Lỗi: " + res.getMessage(), Toast.LENGTH_LONG).show();
                                    }
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
            MenuItem itAddMenu = menu.add(1, R.id.itAddMenu, 1, R.string.addMenu);
            itAddMenu.setIcon(R.drawable.ic_baseline_add_24);
            itAddMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.itAddMenu) {
            Intent intent = new Intent(getActivity(), AddMenuActivity.class);
            intent.putExtra("maloai", maloai);
            intent.putExtra("tenloai", tenloai);
            resultLauncherMenu.launch(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void HienThiDSMon() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getDishes(maloai).enqueue(new Callback<List<MonResponse>>() {
            @Override
            public void onResponse(Call<List<MonResponse>> call, Response<List<MonResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    monDTOList = new ArrayList<>();
                    for (MonResponse res : response.body()) {
                        MonDTO dto = new MonDTO();
                        dto.setMaMon(res.getMaMon());
                        dto.setTenMon(res.getTenMon());
                        dto.setGiaTien(res.getGiaTien());
                        dto.setHinhAnhUrl(res.getHinhAnh());
                        dto.setMaLoai(res.getMaLoai());
                        dto.setTinhTrang(res.getTinhTrang());
                        monDTOList.add(dto);
                    }
                    capNhatGiaoDien();
                }
            }

            @Override
            public void onFailure(Call<List<MonResponse>> call, Throwable t) {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void capNhatGiaoDien() {
        View layout_empty_state = view.findViewById(R.id.layout_empty_state);
        if (monDTOList != null && monDTOList.size() > 0) {
            adapterDisplayMenu = new AdapterDisplayMenu(getActivity(), R.layout.custom_layout_displaymenu, monDTOList, maban);
            gv_menu_DishList.setAdapter(adapterDisplayMenu);
            adapterDisplayMenu.notifyDataSetChanged();
            if (layout_empty_state != null) layout_empty_state.setVisibility(View.GONE);
            gv_menu_DishList.setVisibility(View.VISIBLE);
        } else {
            gv_menu_DishList.setVisibility(View.GONE);
            if (layout_empty_state != null) {
                layout_empty_state.setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.txt_empty_StateTitle)).setText("Chưa có món ăn");
                ((TextView) view.findViewById(R.id.txt_empty_StateDesc)).setText("Hãy nhấn nút + để thêm món mới vào thực đơn này.");
            }
        }
    }
}
