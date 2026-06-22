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

public class DisplayTableFragment extends Fragment {

    private static final String TAG = "DisplayTableFragment";

    RecyclerView rvDisplayTable;
    TabLayout tabLayoutTable;
    List<BanAnDTO> banAnDTOList = new ArrayList<>();
    List<BanAnDTO> filteredList = new ArrayList<>();
    AdapterDisplayTable adapterDisplayTable;
    View view;
    private TableViewModel tableViewModel;

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

    private boolean isRecreated = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.displaytable_layout, container, false);
        setHasOptionsMenu(true);
        ((HomeActivity) getActivity()).getSupportActionBar().setTitle("Quản lý bàn");

        if (savedInstanceState != null) {
            isRecreated = true;
        }

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
        // Lưới 2 cột như GridView cũ
        rvDisplayTable.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        adapterDisplayTable = new AdapterDisplayTable(getActivity(), filteredList);
        adapterDisplayTable.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        rvDisplayTable.setAdapter(adapterDisplayTable);

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            HienThiDSBan(swipeRefreshLayout, true);
        });

        view.findViewById(R.id.fab_add_table).setOnClickListener(v ->
                resultLauncherAdd.launch(new Intent(getActivity(), AddTableActivity.class)));

        tableViewModel = new ViewModelProvider(this).get(TableViewModel.class);
        tableViewModel.getTables().observe(getViewLifecycleOwner(), list -> {
            banAnDTOList.clear();
            banAnDTOList.addAll(list);
            filterTables(tabLayoutTable != null ? tabLayoutTable.getSelectedTabPosition() : 0);
        });
        tableViewModel.getReservedTables().observe(getViewLifecycleOwner(), reservedList -> {
            adapterDisplayTable.setReservedTables(reservedList);
        });

        return view;
    }

    private io.socket.client.Socket mSocket;
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

    private void filterTables(int tabIndex) {
        filteredList.clear();
        for (BanAnDTO ban : banAnDTOList) {
            String nameLower = ban.getTenBan().toLowerCase();
            boolean isTakeaway = nameLower.contains("mang đi")
                    || nameLower.contains("mang di")
                    || nameLower.contains("takeaway")
                    || nameLower.contains("take away");
            if (tabIndex == 0) {
                if (!isTakeaway) {
                    filteredList.add(ban);
                }
            } else {
                if (isTakeaway) {
                    filteredList.add(ban);
                }
            }
        }
        adapterDisplayTable.notifyDataSetChanged();
        capNhatTrangThai();
    }

    private void HienThiDSBan() {
        HienThiDSBan(true);
    }

    private void HienThiDSBan(boolean fetchApi) {
        HienThiDSBan(null, fetchApi);
    }

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
