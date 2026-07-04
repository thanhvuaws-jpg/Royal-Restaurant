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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sinhvien.orderdrinkapp.Activities.AddTableActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.TableResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayTable;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;
import com.sinhvien.orderdrinkapp.DTO.BanAnDTO;
import com.sinhvien.orderdrinkapp.R;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;
import androidx.lifecycle.ViewModelProvider;
import com.sinhvien.orderdrinkapp.ViewModel.TableViewModel;

/**
 * DisplayTableFragment - Màn hình Quản lý Danh sách Bàn ăn (Table Dashboard).
 * - Sử dụng TabLayout phân tách danh sách:
 *   1. Ngồi tại bàn: Hiển thị các bàn ăn vật lý bố trí tại nhà hàng.
 *   2. Mang đi: Hiển thị các hóa đơn mang đi (Takeaway/Mang đi).
 * - Sử dụng RecyclerView với GridLayoutManager (lưới 2 cột) hiển thị các ô bàn ăn kèm trạng thái:
 *   + Bàn trống (Màu xanh/Xám).
 *   + Đang có khách/Đang gọi món (Màu đỏ/Vàng).
 *   + Đã được đặt trước (Reserved).
 * - Vuốt làm mới (SwipeRefreshLayout) để đồng bộ lại dữ liệu bàn ăn và đơn đặt từ Server VPS.
 * - Tự động đồng bộ bằng kết nối thời gian thực qua Socket.io khi có nhân viên khác đổi trạng thái bàn/gọi món.
 * - Cho phép thêm bàn mới (AddTableActivity) dành riêng cho người dùng quản lý.
 */
public class DisplayTableFragment extends Fragment {

    private static final String TAG = "DisplayTableFragment";

    // RecyclerView hiển thị danh sách các ô bàn
    RecyclerView rvDisplayTable;
    // TabLayout phân loại Ngồi tại bàn vs Mang đi
    TabLayout tabLayoutTable;
    // Danh sách gốc chứa toàn bộ thông tin bàn ăn tải về
    List<BanAnDTO> banAnDTOList = new ArrayList<>();
    // Danh sách đã qua bộ lọc loại hình (Ngồi tại bàn / Mang đi)
    List<BanAnDTO> filteredList = new ArrayList<>();
    // Adapter phục vụ hiển thị bàn ăn
    AdapterDisplayTable adapterDisplayTable;
    View view;
    // ViewModel quản lý bàn ăn
    private TableViewModel tableViewModel;

    // Đón nhận kết quả trả về khi thêm bàn ăn thành công
    ActivityResultLauncher<Intent> resultLauncherAdd = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        HienThiDSBan();
                    }
                }
            });

    // Cờ đánh dấu fragment được tái tạo lại
    private boolean isRecreated = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.displaytable_layout, container, false);
        setHasOptionsMenu(true);
        ((HomeActivity) getActivity()).getSupportActionBar().setTitle("Quản lý bàn");

        if (savedInstanceState != null) {
            isRecreated = true;
        }

        // Thiết lập hai tab phân loại hình thức gọi món
        tabLayoutTable = view.findViewById(R.id.tabLayoutTable);
        tabLayoutTable.addTab(tabLayoutTable.newTab().setText("Ngồi tại bàn"));
        tabLayoutTable.addTab(tabLayoutTable.newTab().setText("Mang đi"));

        tabLayoutTable.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterTables(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        rvDisplayTable = view.findViewById(R.id.rvDisplayTable);
        // Bố trí dạng lưới 2 cột
        rvDisplayTable.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        adapterDisplayTable = new AdapterDisplayTable(getActivity(), filteredList);
        adapterDisplayTable.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        rvDisplayTable.setAdapter(adapterDisplayTable);

        // Vuốt làm mới đồng bộ dữ liệu
        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            HienThiDSBan(swipeRefreshLayout, true);
        });

        // FAB thêm bàn mới dành cho Admin/Nhân viên
        view.findViewById(R.id.fab_add_table).setOnClickListener(v ->
                resultLauncherAdd.launch(new Intent(getActivity(), AddTableActivity.class)));

        // Khởi tạo ViewModel bàn ăn và đăng ký lắng nghe thay đổi
        tableViewModel = new ViewModelProvider(this).get(TableViewModel.class);
        tableViewModel.getTables().observe(getViewLifecycleOwner(), list -> {
            banAnDTOList.clear();
            banAnDTOList.addAll(list);
            filterTables(tabLayoutTable != null ? tabLayoutTable.getSelectedTabPosition() : 0);
        });
        // Đăng ký quan sát danh sách bàn đã được đặt trước (booking)
        tableViewModel.getReservedTables().observe(getViewLifecycleOwner(), reservedList -> {
            adapterDisplayTable.setReservedTables(reservedList);
        });

        return view;
    }

    private io.socket.client.Socket mSocket;
    
    // Tự động làm mới danh sách bàn khi có bất kỳ thay đổi nào từ Socket.io
    private io.socket.emitter.Emitter.Listener onRefreshOrders = new io.socket.emitter.Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HienThiDSBan();
                    }
                });
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (isRecreated) {
            HienThiDSBan(false);
            isRecreated = false;
        } else {
            HienThiDSBan(true);
        }
        
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

    /**
     * Khởi tạo Actionbar Menu Thêm bàn ăn.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem itAddTable = menu.add(1, R.id.itAddTable, 1, R.string.addTable);
        itAddTable.setIcon(R.drawable.ic_baseline_add_24);
        itAddTable.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.itAddTable) {
            resultLauncherAdd.launch(new Intent(getActivity(), AddTableActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Lọc danh sách bàn ăn dựa trên Tab được chọn.
     * - Tab 0: Ngồi tại bàn (Không chứa các cụm từ "mang đi", "takeaway").
     * - Tab 1: Mang đi (Có chứa các cụm từ "mang đi", "takeaway").
     */
    private void filterTables(int tabIndex) {
        List<BanAnDTO> newFilteredList = new ArrayList<>();
        for (BanAnDTO ban : banAnDTOList) {
            String nameLower = ban.getTenBan().toLowerCase();
            boolean isTakeaway = nameLower.contains("mang đi")
                    || nameLower.contains("mang di")
                    || nameLower.contains("takeaway")
                    || nameLower.contains("take away");
            if (tabIndex == 0) {
                if (!isTakeaway) {
                    newFilteredList.add(ban);
                }
            } else {
                if (isTakeaway) {
                    newFilteredList.add(ban);
                }
            }
        }
        adapterDisplayTable.updateData(newFilteredList);
        filteredList.clear();
        filteredList.addAll(newFilteredList);
        capNhatTrangThai();
    }

    private void HienThiDSBan() {
        HienThiDSBan(true);
    }

    private void HienThiDSBan(boolean fetchApi) {
        HienThiDSBan(null, fetchApi);
    }

    /**
     * Thực hiện tải dữ liệu bàn ăn từ SQLite và gọi API đồng bộ hóa từ máy chủ VPS.
     */
    private void HienThiDSBan(SwipeRefreshLayout swipeRefresh, boolean fetchApi) {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        
        tableViewModel.loadTablesFromLocal();

        if (!fetchApi) {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            return;
        }

        tableViewModel.syncTablesFromServer(true, new TableViewModel.OnSyncCallback() {
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
                Log.e(TAG, "Lỗi đồng bộ danh sách bàn: " + errorMsg);
                if (isAdded() && getActivity() != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getActivity(), "Lỗi đồng bộ: " + errorMsg, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void fetchReservedTables() {
        tableViewModel.fetchReservedTables();
    }

    /**
     * Hiển thị thông báo trạng thái rỗng khi danh sách bàn lọc ra bị trống.
     */
    private void capNhatTrangThai() {
        View layout_empty_state = view.findViewById(R.id.layout_empty_state);
        if (!filteredList.isEmpty()) {
            rvDisplayTable.setVisibility(View.VISIBLE);
            if (layout_empty_state != null) layout_empty_state.setVisibility(View.GONE);
        } else {
            rvDisplayTable.setVisibility(View.GONE);
            if (layout_empty_state != null) {
                layout_empty_state.setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.txt_empty_StateTitle)).setText("Chưa có bàn");
                ((TextView) view.findViewById(R.id.txt_empty_StateDesc)).setText("Hãy nhấn nút + để thêm bàn mới.");
            }
        }
    }
}
