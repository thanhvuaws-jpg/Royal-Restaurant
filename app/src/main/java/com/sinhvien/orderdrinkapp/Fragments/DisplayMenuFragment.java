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

public class DisplayMenuFragment extends Fragment {

    // Hằng số phân trang
    private static final int PAGE_SIZE = 10;

    int maloai, maban;
    String tenloai;

    RecyclerView rv_menu_DishList;
    ProgressBar pb_menu_LoadMore;
    List<MonDTO> monDTOList = new ArrayList<>();
    AdapterDisplayMenuRecycler adapter;
    View view;

    // Biến điều khiển phân trang và tìm kiếm
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private String currentSearch = ""; // Từ khóa tìm kiếm hiện tại

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
                                // Reset và tải lại từ đầu khi có thay đổi
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

            // Thiết lập RecyclerView dạng lưới 2 cột
            GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
            rv_menu_DishList.setLayoutManager(layoutManager);

            adapter = new AdapterDisplayMenuRecycler(getActivity(), monDTOList, maban);
            rv_menu_DishList.setAdapter(adapter);

            // Tải từ SQLite cache trước để hiển thị tức thì
            LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(getActivity());
            monDTOList.clear();
            monDTOList.addAll(dbHelper.getDishes(maloai, currentSearch));
            adapter.notifyDataSetChanged();
            capNhatTrangThai();

            // Tải trang đầu tiên từ Server để đồng bộ
            taiThemMon();

            // Lắng nghe sự kiện cuộn - Infinite Scroll
            rv_menu_DishList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy > 0) { // Chỉ kích hoạt khi cuộn xuống
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                        // Khi còn 4 item cuối thì bắt đầu tải thêm
                        if (!isLoading && hasMore) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4) {
                                taiThemMon();
                            }
                        }
                    }
                }
            });
        }

        // Nút thêm món
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

        // Thanh tìm kiếm → Gửi từ khóa lên Server
        SearchView sv = view.findViewById(R.id.sv_menu_SearchDish);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                timKiemTrenServer(query.trim());
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    // Xóa từ khóa → quay về phân trang bình thường
                    timKiemTrenServer("");
                }
                return true;
            }
        });

        // Nút Back
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

    // Tải thêm món từ Server (có phân trang)
    private void taiThemMon() {
        if (isLoading || !hasMore) return;
        isLoading = true;
        pb_menu_LoadMore.setVisibility(View.VISIBLE);

        LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(getActivity());
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getDishes(maloai, currentPage, PAGE_SIZE, currentSearch).enqueue(new Callback<DishPageResponse>() {
            @Override
            public void onResponse(Call<DishPageResponse> call, Response<DishPageResponse> response) {
                if (!isAdded() || getActivity() == null) return;
                pb_menu_LoadMore.setVisibility(View.GONE);
                isLoading = false;

                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    List<MonResponse> newItems = response.body().getData();
                    hasMore = response.body().isHasMore();

                    if (newItems != null) {
                        // Lưu dữ liệu vào SQLite
                        dbHelper.syncDishes(maloai, newItems, currentPage == 1);

                        // Load lại toàn bộ từ SQLite lên List để đồng nhất dữ liệu
                        List<MonDTO> cachedList = dbHelper.getDishes(maloai, currentSearch);
                        monDTOList.clear();
                        monDTOList.addAll(cachedList);
                        adapter.notifyDataSetChanged();
                        
                        currentPage++;
                    }
                    capNhatTrangThai();
                }
            }

            @Override
            public void onFailure(Call<DishPageResponse> call, Throwable t) {
                if (!isAdded() || getActivity() == null) return;
                pb_menu_LoadMore.setVisibility(View.GONE);
                isLoading = false;
                // Offline thì giữ nguyên dữ liệu đã có từ SQLite
            }
        });
    }

    // Reset và tải lại từ đầu (sau khi thêm/sửa/xóa)
    private void resetVaTaiLai() {
        currentPage = 1;
        hasMore = true;
        isLoading = false;
        currentSearch = "";
        
        LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(getActivity());
        monDTOList.clear();
        monDTOList.addAll(dbHelper.getDishes(maloai, currentSearch));
        adapter.notifyDataSetChanged();
        capNhatTrangThai();

        taiThemMon();
    }

    // Tìm kiếm trên Server (trả về toàn bộ kết quả khớp)
    private void timKiemTrenServer(String query) {
        currentSearch = query;
        currentPage = 1;
        hasMore = true;
        isLoading = false;
        
        LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(getActivity());
        monDTOList.clear();
        monDTOList.addAll(dbHelper.getDishes(maloai, currentSearch));
        adapter.notifyDataSetChanged();
        capNhatTrangThai();

        taiThemMon();
    }

    // Cập nhật trạng thái hiển thị danh sách hoặc empty state
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
}
