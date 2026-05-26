package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.R;

import java.util.List;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.ViewHolder> {

    private Context context;
    private List<BookingResponse> bookingList;

    public BookingHistoryAdapter(Context context, List<BookingResponse> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_active_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingResponse booking = bookingList.get(position);

        holder.txt_booking_table.setText(booking.getTenBan() != null ? booking.getTenBan() : "Bàn #" + booking.getMaBan());
        holder.txt_booking_time.setText("Giờ hẹn: " + booking.getThoigianhen());

        String status = booking.getTinhtrang();
        if ("pending".equalsIgnoreCase(status)) {
            holder.txt_booking_status.setText("Chờ nhận bàn");
            holder.txt_booking_status.setTextColor(Color.parseColor("#FFAB40")); // Orange
        } else if ("checked_in".equalsIgnoreCase(status)) {
            holder.txt_booking_status.setText("Đã nhận bàn");
            holder.txt_booking_status.setTextColor(Color.parseColor("#43A047")); // Green
        } else if ("overdue".equalsIgnoreCase(status)) {
            holder.txt_booking_status.setText("Quá giờ hẹn");
            holder.txt_booking_status.setTextColor(Color.parseColor("#E53935")); // Red
        } else if ("cancelled".equalsIgnoreCase(status)) {
            holder.txt_booking_status.setText("Đã hủy");
            holder.txt_booking_status.setTextColor(Color.parseColor("#9E9E9E")); // Grey
        } else {
            holder.txt_booking_status.setText(status);
            holder.txt_booking_status.setTextColor(Color.parseColor("#9E9E9E"));
        }

        if (booking.getTongTien() != null && !booking.getTongTien().isEmpty() && !"0".equals(booking.getTongTien())) {
            holder.txt_booking_dishes.setText("Món đặt trước: " + booking.getTongTien() + " đ");
        } else {
            holder.txt_booking_dishes.setText("Món đặt trước: Không có");
        }
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt_booking_table, txt_booking_status, txt_booking_time, txt_booking_dishes;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txt_booking_table = itemView.findViewById(R.id.txt_booking_table);
            txt_booking_status = itemView.findViewById(R.id.txt_booking_status);
            txt_booking_time = itemView.findViewById(R.id.txt_booking_time);
            txt_booking_dishes = itemView.findViewById(R.id.txt_booking_dishes);
        }
    }
}
