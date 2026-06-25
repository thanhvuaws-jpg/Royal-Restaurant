package com.sinhvien.orderdrinkapp.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import com.sinhvien.orderdrinkapp.Activities.AddMenuActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.DishPageResponse;
import com.sinhvien.orderdrinkapp.Api.MonResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayMenuRecycler;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;
import com.sinhvien.orderdrinkapp.DTO.MonDTO;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.lifecycle.ViewModelProvider;
import com.sinhvien.orderdrinkapp.ViewModel.MenuViewModel;

/**
 * DisplayMenuFragment - Màn hình danh sách Món ăn/Nước uống (Menu).
 * Hiển thị toàn bộ các món ăn thuộc một loại danh mục món ăn (maloai).
 * - Sử dụng RecyclerView dạng lưới 2 cột để trình bày các món ăn (bao gồm hình ảnh, tên món, giá bán).
 * - Hỗ trợ cơ chế vô hạn cuộn (Infinite Scroll) kết hợp phân trang tải thêm (Pagination) từ API Server.
 * - Hỗ trợ tìm kiếm theo tên món ăn thông qua SearchView với cơ chế Debounce (trễ 300ms) giảm tải yêu cầu API.
 * - Cho phép vuốt làm mới danh sách món ăn đồng bộ từ Server về SQLite (SwipeRefreshLayout).
 * - Tích hợp lắng nghe sự thay đổi menu từ Socket.io ("menu_changed") để tự động cập nhật lại danh sách tức thời.
 * - Nút thêm món (FAB) hiển thị riêng biệt đối với người dùng là Admin.
 */
public class DisplayMenuFragment extends Fragment {

    // Kích thước tối đa tải dữ liệu trong một trang (Pagination size)
    private static final int PAGE_SIZE = 1000;

    // Lưu mã loại món, mã bàn ăn, tên loại món
    int maloai, maban;
    String tenloai;

    // RecyclerView và thanh tiến trình xoay khi tải trang mới
    RecyclerView rv_menu_DishList;
    ProgressBar pb_menu_LoadMore;
    List<MonDTO> monDTOList = new ArrayList<>();
    AdapterDisplayMenuRecycler adapter;
    View view;

    // Bộ lọc từ khóa tìm kiếm món ăn
    private String currentSearch = ""; 
    // ViewModel phụ trách xử lý nghiệp vụ danh mục món ăn
    private MenuViewModel menuViewModel;

    // Bộ trì hoãn thời gian xử lý tìm kiếm (Debounce Search)
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

    private Socket mSocket;
    
    // Đồng bộ menu khi có tín hiệu làm mới đơn hàng từ Socket.io
    private final Emitter.Listener onRefreshOrders = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> refreshMenuInPlace());
            }
        }
    };

    // Đồng bộ menu khi thực đơn món ăn thay đổi từ Socket.io
    private final Emitter.Listener onMenuChanged = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> refreshMenuInPlace());
            }
        }
    };

    // Nhận kết quả thêm/sửa món ăn trả về từ màn hình AddMenuActivity
    ActivityResultLauncher<Intent> resultLauncherMenu = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        if (intent != null) {
                            boolean ktra = intent.getBooleanExtra("ktra", false);
                            String chucnang = intent.getStringExtra("chucnang");
                            if (ktra) {
                                resetVaTaiLai();
                                Toast.makeText(getActivity(),
                                        "themmon".equals(chucnang) ? R.string.add_sucessful : R.string.edit_sucessful,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(),
                                        "themmon".equals(chucnang) ? getString(R.string.add_failed) : "Sửa thất bại",
                                        Toast.LENGTH_SHORT).show();
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

        rv_menu_DishList = view.findViewById(R.id.rv_menu_DishList);
        pb_menu_LoadMore = view.findViewById(R.id.pb_menu_LoadMore);

        Bundle bundle = getArguments();
        if (bundle != null) {
            maloai = bundle.getInt("maloai");
            tenloai = bundle.getString("tenloai");
            maban = bundle.getInt("maban");

            menuViewModel = new ViewModelProvider(this).get(MenuViewModel.class);

            // Khôi phục lại bộ nhớ tìm kiếm và phân trang
            if (savedInstanceState != null) {
                currentSearch = savedInstanceState.getString("current_search", "");
                menuViewModel.setCurrentPage(savedInstanceState.getInt("current_page", 1));
                menuViewModel.setHasMore(savedInstanceState.getBoolean("has_more", true));
            }

            // Thiết lập RecyclerView dạng lưới 2 cột hiển thị các card món ăn
            GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
            rv_menu_DishList.setLayoutManager(layoutManager);

            adapter = new AdapterDisplayMenuRecycler(getActivity(), monDTOList, maban);
            adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
            rv_menu_DishList.setAdapter(adapter);

            // Đăng ký quan sát dữ liệu LiveData từ MenuViewModel
            menuViewModel.getDishes(maloai, currentSearch).observe(getViewLifecycleOwner(), dishes -> {
                // Sử dụng DiffUtil so sánh khác biệt dữ liệu giúp RecyclerView hoạt động mượt mà
                androidx.recyclerview.widget.DiffUtil.DiffResult diffResult =
                        androidx.recyclerview.widget.DiffUtil.calculateDiff(new MenuDiffCallback(monDTOList, dishes));
                monDTOList.clear();
                monDTOList.addAll(dishes);
                diffResult.dispatchUpdatesTo(adapter);
                capNhatTrangThai();
            });

            // Vuốt làm mới danh sách
            SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                menuViewModel.setCurrentPage(1);
                menuViewModel.setHasMore(true);
                menuViewModel.setLoading(false);
                taiThemMon(swipeRefreshLayout);
            });

            // Tải trang đầu tiên từ Server để đồng bộ
            if (savedInstanceState == null) {
                taiThemMon();
            }

            // Lắng nghe sự kiện cuộn màn hình để tự động tải trang kế tiếp (Lazy Loading/Infinite Scroll)
            rv_menu_DishList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy > 0) { 
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                        // Nếu người dùng cuộn đến 4 phần tử cuối, gọi phân trang tải thêm
                        if (!menuViewModel.isLoading() && menuViewModel.isHasMore()) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4) {
                                taiThemMon();
                            }
                        }
                    }
                }
            });
        }

        // Nút nổi thêm món ăn chỉ hiển thị đối với Admin
        View fabAddDish = view.findViewById(R.id.fab_add_dish);
        if (SessionManager.isAdmin(getActivity())) {
            fabAddDish.setVisibility(View.VISIBLE);
            fabAddDish.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AddMenuActivity.class);
                intent.putExtra("maloai", maloai);
                intent.putExtra("tenloai", tenloai);
                resultLauncherMenu.launch(intent);
            });
        } else {
            fabAddDish.setVisibility(View.GONE);
        }

        // Cài đặt SearchView tìm kiếm món ăn
        SearchView sv = view.findViewById(R.id.sv_menu_SearchDish);
        if (currentSearch != null && !currentSearch.isEmpty()) {
            sv.setQuery(currentSearch, false);
            sv.clearFocus();
        }
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                timKiemTrenServer(query.trim());
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                // Áp dụng Debounce 300ms trước khi bắt đầu truy vấn
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = new Runnable() {
                    @Override
                    public void run() {
                        timKiemTrenServer(newText.trim());
                    }
                };
                searchHandler.postDelayed(searchRunnable, 300);
                return true;
            }
        });

        // Xử lý sự kiện nhấn nút Back vật lý trên điện thoại: Quay về danh mục loại món ăn
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                getParentFragmentManager().popBackStack("hienthiloai", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                return true;
            }
            return false;
        });

        setHasOptionsMenu(true);
        return view;
    }

    /**
     * Tải thêm các món ăn ở trang tiếp theo.
     */
    private void taiThemMon() {
        taiThemMon(null);
    }

    private void taiThemMon(SwipeRefreshLayout swipeRefresh) {
        if (menuViewModel.isLoading() || (!menuViewModel.isHasMore() && swipeRefresh == null)) {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            return;
        }

        if (swipeRefresh == null) {
            pb_menu_LoadMore.setVisibility(View.VISIBLE);
        }

        menuViewModel.loadMoreDishes(maloai, currentSearch, swipeRefresh != null, success -> {
            if (swipeRefresh != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    swipeRefresh.setRefreshing(false);
                });
            }
            if (getActivity() != null && isAdded()) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    pb_menu_LoadMore.setVisibility(View.GONE);
                });
            }
        });
    }

    /**
     * Đặt lại các thông số và đồng bộ lại danh sách món ăn từ đầu.
     */
    private void resetVaTaiLai() {
        currentSearch = "";
        menuViewModel.setCurrentPage(1);
        menuViewModel.setHasMore(true);
        menuViewModel.setLoading(false);
        menuViewModel.loadDishesFromLocal(maloai, currentSearch);
        taiThemMon();
    }

    /**
     * Thực hiện tìm kiếm món ăn gửi từ khóa lên API.
     */
    private void timKiemTrenServer(String query) {
        currentSearch = query;
        menuViewModel.setCurrentPage(1);
        menuViewModel.setHasMore(true);
        menuViewModel.setLoading(false);
        menuViewModel.loadDishesFromLocal(maloai, currentSearch);
        taiThemMon();
    }

    /**
     * Hiển thị layout báo trống khi danh sách rỗng.
     */
    private void capNhatTrangThai() {
        View emptyState = view.findViewById(R.id.layout_empty_state);
        if (monDTOList.isEmpty()) {
            rv_menu_DishList.setVisibility(View.GONE);
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.txt_empty_StateTitle)).setText("Chưa có món ăn");
                ((TextView) view.findViewById(R.id.txt_empty_StateDesc)).setText("Hãy nhấn nút + để thêm món mới.");
            }
        } else {
            rv_menu_DishList.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    /**
     * Thêm Actionbar Menu thêm món ăn dành cho Admin.
     */
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
        if (item.getItemId() == R.id.itAddMenu) {
            Intent intent = new Intent(getActivity(), AddMenuActivity.class);
            intent.putExtra("maloai", maloai);
            intent.putExtra("tenloai", tenloai);
            resultLauncherMenu.launch(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Tự động làm mới dữ liệu tại chỗ (không hiển thị loading dialog).
     */
    private void refreshMenuInPlace() {
        menuViewModel.loadMoreDishes(maloai, currentSearch, true, null);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("current_search", currentSearch);
        if (menuViewModel != null) {
            outState.putInt("current_page", menuViewModel.getCurrentPage());
            outState.putBoolean("has_more", menuViewModel.isHasMore());
        }
    }

    /**
     * Lớp MenuDiffCallback giúp tối ưu cập nhật danh sách món ăn trên RecyclerView.
     */
    private static class MenuDiffCallback extends androidx.recyclerview.widget.DiffUtil.Callback {
        private final List<MonDTO> oldList;
        private final List<MonDTO> newList;

        public MenuDiffCallback(List<MonDTO> oldList, List<MonDTO> newList) {
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
            return oldList.get(oldItemPosition).getMaMon() == newList.get(newItemPosition).getMaMon();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            MonDTO oldItem = oldList.get(oldItemPosition);
            MonDTO newItem = newList.get(newItemPosition);
            return equalsOrNull(oldItem.getTenMon(), newItem.getTenMon())
                    && equalsOrNull(oldItem.getGiaTien(), newItem.getGiaTien())
                    && equalsOrNull(oldItem.getTinhTrang(), newItem.getTinhTrang())
                    && equalsOrNull(oldItem.getHinhAnhUrl(), newItem.getHinhAnhUrl());
        }

        private boolean equalsOrNull(Object a, Object b) {
            return a == b || (a != null && a.equals(b));
        }
    }
}
