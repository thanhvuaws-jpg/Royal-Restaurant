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
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;
import com.sinhvien.orderdrinkapp.ViewModel.CategoryViewModel;

/**
 * DisplayCategoryFragment - Màn hình danh mục Nhóm món ăn (Category).
 * Hiển thị danh sách các phân loại món ăn (Ví dụ: Khai vị, Món chính, Tráng miệng, Nước uống).
 * - Sử dụng SwipeRefreshLayout để vuốt làm mới danh mục đồng bộ từ VPS.
 * - Sử dụng CategoryViewModel kết hợp LiveData + DiffUtil giúp cập nhật danh sách mượt mà.
 * - Lắng nghe thay đổi thực đơn từ Socket.io ("menu_changed") để tự động cập nhật danh mục của các thiết bị khác.
 * - Hỗ trợ nút thêm danh mục (Floating Action Button / Options Menu) dành riêng cho quản trị viên (Admin).
 */
public class DisplayCategoryFragment extends Fragment {

    private static final String TAG = "DisplayCategoryFragment";

    // RecyclerView hiển thị các phân loại món ăn
    RecyclerView rv_category_CategoryList;
    // Danh sách lưu trữ các loại món
    List<LoaiMonDTO> loaiMonDTOList = new ArrayList<>();
    // Adapter phục vụ hiển thị danh mục
    AdapterDisplayCategory adapter;
    FragmentManager fragmentManager;
    // Lưu trữ mã bàn ăn đang thao tác chọn món
    int maban;
    View view;
    // ViewModel quản lý dữ liệu danh mục món ăn
    CategoryViewModel categoryViewModel;

    private Socket mSocket;
    
    // Callback cập nhật danh sách khi nhận được tín hiệu làm mới đơn hàng từ Socket
    private final Emitter.Listener onRefreshOrders = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> refreshCategoriesInPlace());
            }
        }
    };

    // Callback cập nhật danh sách khi menu thực đơn thay đổi (thêm/sửa/xóa món ăn hoặc loại món từ Admin)
    private final Emitter.Listener onMenuChanged = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> refreshCategoriesInPlace());
            }
        }
    };

    // Launcher nhận kết quả trả về sau khi thêm mới hoặc sửa đổi danh mục
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

        // Nhận mã bàn ăn truyền qua từ màn hình bàn ăn
        Bundle bDataCategory = getArguments();
        if (bDataCategory != null) {
            maban = bDataCategory.getInt("maban");
        }

        // Thiết lập Adapter và chính sách phục hồi trạng thái cuộn
        adapter = new AdapterDisplayCategory(getActivity(), loaiMonDTOList);
        adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        rv_category_CategoryList.setAdapter(adapter);

        // Khởi tạo ViewModel và quan sát LiveData thay đổi danh mục món ăn
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            // Áp dụng DiffUtil tối ưu hóa hiệu năng cập nhật danh sách
            androidx.recyclerview.widget.DiffUtil.DiffResult diffResult =
                    androidx.recyclerview.widget.DiffUtil.calculateDiff(new CategoryDiffCallback(loaiMonDTOList, categories));
            loaiMonDTOList.clear();
            loaiMonDTOList.addAll(categories);
            diffResult.dispatchUpdatesTo(adapter);
            capNhatTrangThai();
        });

        // Xử lý sự kiện click chọn danh mục để xem danh sách món ăn thuộc danh mục đó
        adapter.setOnItemClickListener(position -> {
            int maloai = loaiMonDTOList.get(position).getMaLoai();
            String tenloai = loaiMonDTOList.get(position).getTenLoai();

            DisplayMenuFragment displayMenuFragment = new DisplayMenuFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("maloai", maloai);
            bundle.putString("tenloai", tenloai);
            bundle.putInt("maban", maban);
            displayMenuFragment.setArguments(bundle);

            ((HomeActivity) getActivity()).navigateToSubFragment(displayMenuFragment, "hienthiloai");
        });

        // Floating Action Button cho phép Admin mở màn hình thêm danh mục
        view.findViewById(R.id.fab_add_category).setOnClickListener(v -> {
            resultLauncherCategory.launch(new Intent(getActivity(), AddCategoryActivity.class));
        });

        // Cài đặt SwipeRefreshLayout để vuốt xuống làm mới danh sách
        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            HienThiDSLoai(swipeRefreshLayout, true);
        });

        // Tải danh sách danh mục lần đầu tiên
        HienThiDSLoai(savedInstanceState == null);
        return view;
    }

    /**
     * Khởi tạo trình chọn menu trên ActionBar.
     * Chỉ hiển thị nút Thêm Danh Mục nếu tài khoản đăng nhập là Admin.
     */
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
        HienThiDSLoai(true);
    }

    private void HienThiDSLoai(boolean fetchApi) {
        HienThiDSLoai(null, fetchApi);
    }

    /**
     * Tải danh mục loại món ăn từ SQLite và gọi API đồng bộ.
     */
    private void HienThiDSLoai(SwipeRefreshLayout swipeRefresh, boolean fetchApi) {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        
        categoryViewModel.loadCategoriesFromLocal();

        if (!fetchApi) {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            return;
        }

        categoryViewModel.syncCategoriesFromServer(new CategoryViewModel.OnSyncCallback() {
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
     * Cập nhật hiển thị giao diện trạng thái trống (Empty State) khi danh mục rỗng.
     */
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

    /**
     * Lấy lại dữ liệu danh mục thầm lặng (không hiển thị loading dialog/progressBar).
     */
    private void refreshCategoriesInPlace() {
        categoryViewModel.syncCategoriesFromServer(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (mSocket != null) {
            mSocket.on("refresh_orders", onRefreshOrders);
            mSocket.on("menu_changed", onMenuChanged);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSocket != null) {
            mSocket.off("refresh_orders", onRefreshOrders);
            mSocket.off("menu_changed", onMenuChanged);
        }
    }

    /**
     * Lớp CategoryDiffCallback hỗ trợ so sánh khác biệt danh mục món ăn giúp tối ưu làm mới RecyclerView.
     */
    private static class CategoryDiffCallback extends androidx.recyclerview.widget.DiffUtil.Callback {
        private final List<LoaiMonDTO> oldList;
        private final List<LoaiMonDTO> newList;

        public CategoryDiffCallback(List<LoaiMonDTO> oldList, List<LoaiMonDTO> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getMaLoai() == newList.get(newItemPosition).getMaLoai();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            LoaiMonDTO oldItem = oldList.get(oldItemPosition);
            LoaiMonDTO newItem = newList.get(newItemPosition);
            return equalsOrNull(oldItem.getTenLoai(), newItem.getTenLoai())
                    && equalsOrNull(oldItem.getHinhAnh(), newItem.getHinhAnh());
        }

        private boolean equalsOrNull(Object a, Object b) {
            return a == b || (a != null && a.equals(b));
        }
    }
}
