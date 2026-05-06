package com.sinhvien.orderdrinkapp.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.sinhvien.orderdrinkapp.Activities.AddCategoryActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterRecycleViewCategory;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterRecycleViewStatistic;
import com.sinhvien.orderdrinkapp.DTO.DonDatDTO;
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.LoaiMonResponse;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DisplayHomeFragment extends Fragment implements View.OnClickListener {

    RecyclerView rcv_display_HomeCategoryList, rcv_display_HomeOrderToday;
    MaterialCardView layout_display_HomeStatistic, layout_display_HomeViewTable, layout_display_HomeViewMenu, layout_display_HomeViewStaff;
    TextView txt_display_HomeViewAllCategory, txt_display_HomeViewAllStatistic;
    List<LoaiMonDTO> loaiMonDTOList;
    List<DonDatDTO> donDatDTOS;
    AdapterRecycleViewCategory adapterRecycleViewCategory;
    AdapterRecycleViewStatistic adapterRecycleViewStatistic;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.displayhome_layout, container, false);
        if (getActivity() != null && ((HomeActivity) getActivity()).getSupportActionBar() != null) {
            ((HomeActivity) getActivity()).getSupportActionBar().setTitle(R.string.app_name);
        }
        setHasOptionsMenu(true);

        //region Bind views
        rcv_display_HomeCategoryList = view.findViewById(R.id.rcv_display_HomeCategoryList);
        rcv_display_HomeOrderToday = view.findViewById(R.id.rcv_display_HomeOrderToday);
        layout_display_HomeStatistic = view.findViewById(R.id.layout_display_HomeStatistic);
        layout_display_HomeViewTable = view.findViewById(R.id.layout_display_HomeViewTable);
        layout_display_HomeViewMenu = view.findViewById(R.id.layout_display_HomeViewMenu);
        layout_display_HomeViewStaff = view.findViewById(R.id.layout_display_HomeViewStaff);
        txt_display_HomeViewAllCategory = view.findViewById(R.id.txt_display_HomeViewAllCategory);
        txt_display_HomeViewAllStatistic = view.findViewById(R.id.txt_display_HomeViewAllStatistic);
        
        TextView txt_welcome_UserName = view.findViewById(R.id.txt_welcome_UserName);
        //endregion

        // Hiển thị tên người dùng từ Session
        String fullName = SessionManager.getFullName(getActivity());
        if (fullName != null && !fullName.isEmpty()) {
            txt_welcome_UserName.setText(fullName);
        }

        // Phân quyền và căn chỉnh Grid cho Nhân viên
        if (!SessionManager.isAdmin(getActivity())) {
            layout_display_HomeViewStaff.setVisibility(View.GONE);
            layout_display_HomeStatistic.setVisibility(View.GONE);
            txt_display_HomeViewAllStatistic.setVisibility(View.GONE);
            
            // Chuyển sang 1 cột để 2 nút to ra và lấp đầy khoảng trống
            android.widget.GridLayout gridLayout = view.findViewById(R.id.grid_display_HomeActions);
            gridLayout.setColumnCount(1);

            // Chuyển 130dp sang pixel để set height
            int heightPx = (int) (130 * getResources().getDisplayMetrics().density);

            ViewGroup.LayoutParams paramsTable = layout_display_HomeViewTable.getLayoutParams();
            paramsTable.height = heightPx; 
            layout_display_HomeViewTable.setLayoutParams(paramsTable);

            ViewGroup.LayoutParams paramsMenu = layout_display_HomeViewMenu.getLayoutParams();
            paramsMenu.height = heightPx;
            layout_display_HomeViewMenu.setLayoutParams(paramsMenu);
        }

        HienThiDSLoai();
        HienThiDonTrongNgay();

        layout_display_HomeStatistic.setOnClickListener(this);
        layout_display_HomeViewTable.setOnClickListener(this);
        layout_display_HomeViewMenu.setOnClickListener(this);
        layout_display_HomeViewStaff.setOnClickListener(this);
        txt_display_HomeViewAllCategory.setOnClickListener(this);
        txt_display_HomeViewAllStatistic.setOnClickListener(this);

        return view;
    }

    private void HienThiDSLoai() {
        rcv_display_HomeCategoryList.setHasFixedSize(true);
        rcv_display_HomeCategoryList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        loaiMonDTOList = new ArrayList<>();
        adapterRecycleViewCategory = new AdapterRecycleViewCategory(getActivity(), R.layout.custom_layout_displaycategory, loaiMonDTOList);
        rcv_display_HomeCategoryList.setAdapter(adapterRecycleViewCategory);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCategories().enqueue(new retrofit2.Callback<List<LoaiMonResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<LoaiMonResponse>> call, retrofit2.Response<List<LoaiMonResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    loaiMonDTOList.clear();
                    for (LoaiMonResponse res : response.body()) {
                        LoaiMonDTO dto = new LoaiMonDTO();
                        dto.setMaLoai(res.getMaLoai());
                        dto.setTenLoai(res.getTenLoai());
                        dto.setHinhAnhPath(res.getHinhAnh()); // Use URL instead of blob
                        loaiMonDTOList.add(dto);
                    }
                    adapterRecycleViewCategory.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<LoaiMonResponse>> call, Throwable t) {
                if (isAdded() && getActivity() != null) {
                    android.widget.Toast.makeText(getActivity(), "Lỗi tải loại món: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void HienThiDonTrongNgay() {
        rcv_display_HomeOrderToday.setHasFixedSize(true);
        rcv_display_HomeOrderToday.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        donDatDTOS = new ArrayList<>();
        adapterRecycleViewStatistic = new AdapterRecycleViewStatistic(getActivity(), R.layout.custom_layout_displaystatistic, donDatDTOS);
        rcv_display_HomeOrderToday.setAdapter(adapterRecycleViewStatistic);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String ngaydat = dateFormat.format(calendar.getTime());

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getPaidOrders().enqueue(new retrofit2.Callback<List<OrderResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<OrderResponse>> call, retrofit2.Response<List<OrderResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    donDatDTOS.clear();
                    SimpleDateFormat cloudFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                    for (OrderResponse res : response.body()) {
                        String orderDateStr = res.getNgayDat();
                        try {
                            java.util.Date d = cloudFormat.parse(res.getNgayDat());
                            orderDateStr = dateFormat.format(d);
                        } catch (Exception ignored) {}

                        // Lọc các đơn của ngày hôm nay
                        if (ngaydat.equals(orderDateStr)) {
                            DonDatDTO dto = new DonDatDTO();
                            dto.setMaDonDat(res.getMaDonDat());
                            dto.setMaNV(res.getMaNV());
                            dto.setMaBan(res.getMaBan());
                            dto.setTongTien(String.valueOf(res.getTongTien()));
                            dto.setTinhTrang("true");
                            dto.setNgayDat(orderDateStr);
                            dto.setTenNV(res.getHoTenNV());
                            dto.setTenBan(res.getTenBan());
                            donDatDTOS.add(dto);
                        }
                    }
                    adapterRecycleViewStatistic.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<OrderResponse>> call, Throwable t) {
                if (isAdded() && getActivity() != null) {
                    android.widget.Toast.makeText(getActivity(), "Lỗi tải đơn hàng: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.navigation_view_trangchu);

        if (id == R.id.layout_display_HomeStatistic || id == R.id.txt_display_HomeViewAllStatistic) {
            FragmentTransaction tranDisplayStatistic = getActivity().getSupportFragmentManager().beginTransaction();
            tranDisplayStatistic.replace(R.id.contentView, new DisplayStatisticFragment());
            tranDisplayStatistic.addToBackStack(null);
            tranDisplayStatistic.commit();
            if (navigationView != null) navigationView.setCheckedItem(R.id.nav_statistic);

        } else if (id == R.id.layout_display_HomeViewTable) {
            FragmentTransaction tranDisplayTable = getActivity().getSupportFragmentManager().beginTransaction();
            tranDisplayTable.replace(R.id.contentView, new DisplayTableFragment());
            tranDisplayTable.addToBackStack(null);
            tranDisplayTable.commit();
            if (navigationView != null) navigationView.setCheckedItem(R.id.nav_table);

        } else if (id == R.id.layout_display_HomeViewMenu) {
            Intent iAddCategory = new Intent(getActivity(), AddCategoryActivity.class);
            startActivity(iAddCategory);
            if (navigationView != null) navigationView.setCheckedItem(R.id.nav_category);

        } else if (id == R.id.layout_display_HomeViewStaff) {
            FragmentTransaction tranDisplayStaff = getActivity().getSupportFragmentManager().beginTransaction();
            tranDisplayStaff.replace(R.id.contentView, new DisplayStaffFragment());
            tranDisplayStaff.addToBackStack(null);
            tranDisplayStaff.commit();
            if (navigationView != null) navigationView.setCheckedItem(R.id.nav_staff);

        } else if (id == R.id.txt_display_HomeViewAllCategory) {
            FragmentTransaction tranDisplayCategory = getActivity().getSupportFragmentManager().beginTransaction();
            tranDisplayCategory.replace(R.id.contentView, new DisplayCategoryFragment());
            tranDisplayCategory.addToBackStack(null);
            tranDisplayCategory.commit();
            if (navigationView != null) navigationView.setCheckedItem(R.id.nav_category);
        }
    }
}
