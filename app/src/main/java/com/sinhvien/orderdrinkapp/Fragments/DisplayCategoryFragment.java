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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sinhvien.orderdrinkapp.Activities.AddCategoryActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.LoaiMonResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayCategory;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DisplayCategoryFragment extends Fragment {

    RecyclerView rv_category_CategoryList;
    List<LoaiMonDTO> loaiMonDTOList = new ArrayList<>();
    AdapterDisplayCategory adapter;
    FragmentManager fragmentManager;
    int maban;
    View view;

    private Socket mSocket;
    private final Emitter.Listener onRefreshOrders = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> refreshCategoriesInPlace());
            }
        }
    };

    ActivityResultLauncher<Intent> resultLauncherCategory = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        boolean ktra = result.getData().getBooleanExtra("ktra", false);
                        String chucnang = result.getData().getStringExtra("chucnang");
                        if (ktra) {
                            HienThiDSLoai();
                            Toast.makeText(getActivity(),
                                    "themloai".equals(chucnang) ? R.string.add_sucessful : R.string.edit_sucessful,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(),
                                    "themloai".equals(chucnang) ? getString(R.string.add_failed) : "Sửa thất bại",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.displaycategory_layout, container, false);
        setHasOptionsMenu(true);

        rv_category_CategoryList = view.findViewById(R.id.rv_category_CategoryList);
        rv_category_CategoryList.setLayoutManager(new LinearLayoutManager(getActivity()));

        fragmentManager = getActivity().getSupportFragmentManager();

        Bundle bDataCategory = getArguments();
        if (bDataCategory != null) {
            maban = bDataCategory.getInt("maban");
        }

        adapter = new AdapterDisplayCategory(getActivity(), loaiMonDTOList);
        rv_category_CategoryList.setAdapter(adapter);

        // Click item → chuyển sang màn hình món ăn
        adapter.setOnItemClickListener(position -> {
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
        });

        // FAB thêm loại
        view.findViewById(R.id.fab_add_category).setOnClickListener(v -> {
            resultLauncherCategory.launch(new Intent(getActivity(), AddCategoryActivity.class));
        });

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            HienThiDSLoai(swipeRefreshLayout);
        });

        HienThiDSLoai();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (SessionManager.isAdmin(getActivity())) {
            MenuItem itAddCategory = menu.add(1, R.id.itAddCategory, 1, R.string.addCategory);
            itAddCategory.setIcon(R.drawable.ic_baseline_add_24);
            itAddCategory.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.itAddCategory) {
            resultLauncherCategory.launch(new Intent(getActivity(), AddCategoryActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void HienThiDSLoai() {
        HienThiDSLoai(null);
    }

    private void HienThiDSLoai(SwipeRefreshLayout swipeRefresh) {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        // 1. Tải và hiển thị dữ liệu từ SQLite trước
        LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(getActivity());
        List<LoaiMonDTO> cachedList = dbHelper.getCategories();
        loaiMonDTOList.clear();
        loaiMonDTOList.addAll(cachedList);
        adapter.notifyDataSetChanged();
        capNhatTrangThai();

        // 2. Gọi API đồng bộ ở chế độ nền
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCategories().enqueue(new Callback<List<LoaiMonResponse>>() {
            @Override
            public void onResponse(Call<List<LoaiMonResponse>> call, Response<List<LoaiMonResponse>> response) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    // Cập nhật dữ liệu mới vào SQLite
                    dbHelper.syncCategories(response.body());

                    // Đọc lại từ SQLite ra giao diện để đồng nhất
                    loaiMonDTOList.clear();
                    loaiMonDTOList.addAll(dbHelper.getCategories());
                    adapter.notifyDataSetChanged();
                    capNhatTrangThai();
                }
            }

            @Override
            public void onFailure(Call<List<LoaiMonResponse>> call, Throwable t) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                // Khi không có mạng, vẫn giữ nguyên dữ liệu từ SQLite đã hiển thị trước đó
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi đồng bộ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void capNhatTrangThai() {
        View layout_empty_state = view.findViewById(R.id.layout_empty_state);
        if (!loaiMonDTOList.isEmpty()) {
            rv_category_CategoryList.setVisibility(View.VISIBLE);
            if (layout_empty_state != null) layout_empty_state.setVisibility(View.GONE);
        } else {
            rv_category_CategoryList.setVisibility(View.GONE);
            if (layout_empty_state != null) {
                layout_empty_state.setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.txt_empty_StateTitle)).setText(R.string.empty_list_title);
                ((TextView) view.findViewById(R.id.txt_empty_StateDesc)).setText(R.string.empty_list_desc);
            }
        }
    }
    private void refreshCategoriesInPlace() {
        LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(getActivity());
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCategories().enqueue(new Callback<List<LoaiMonResponse>>() {
            @Override
            public void onResponse(Call<List<LoaiMonResponse>> call, Response<List<LoaiMonResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    dbHelper.syncCategories(response.body());
                    List<LoaiMonDTO> updatedList = dbHelper.getCategories();

                    // Remove items not in updatedList
                    for (int i = loaiMonDTOList.size() - 1; i >= 0; i--) {
                        LoaiMonDTO oldItem = loaiMonDTOList.get(i);
                        boolean found = false;
                        for (LoaiMonDTO newItem : updatedList) {
                            if (oldItem.getMaLoai() == newItem.getMaLoai()) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            loaiMonDTOList.remove(i);
                            adapter.notifyItemRemoved(i);
                        }
                    }

                    // Update existing items and add new items
                    for (int i = 0; i < updatedList.size(); i++) {
                        LoaiMonDTO newItem = updatedList.get(i);
                        boolean found = false;
                        for (int j = 0; j < loaiMonDTOList.size(); j++) {
                            LoaiMonDTO oldItem = loaiMonDTOList.get(j);
                            if (oldItem.getMaLoai() == newItem.getMaLoai()) {
                                found = true;
                                oldItem.setTenLoai(newItem.getTenLoai());
                                oldItem.setHinhAnh(newItem.getHinhAnh());
                                adapter.notifyItemChanged(j);
                                break;
                            }
                        }
                        if (!found) {
                            loaiMonDTOList.add(newItem);
                            adapter.notifyItemInserted(loaiMonDTOList.size() - 1);
                        }
                    }
                    capNhatTrangThai();
                }
            }

            @Override
            public void onFailure(Call<List<LoaiMonResponse>> call, Throwable t) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (mSocket != null) {
            mSocket.on("refresh_orders", onRefreshOrders);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSocket != null) {
            mSocket.off("refresh_orders", onRefreshOrders);
        }
    }
}
