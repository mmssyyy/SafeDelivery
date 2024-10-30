package com.example.safedelivery;

import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
    private List<AddressItem> addresses = new ArrayList<>();
    private OnAddressSelectedListener listener;

    public interface OnAddressSelectedListener {
        void onAddressSelected(AddressItem address);
    }

    public AddressAdapter(OnAddressSelectedListener listener) {
        this.listener = listener;
    }

    public void setAddresses(List<AddressItem> addresses) {
        this.addresses = addresses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView textView = new TextView(parent.getContext());
        textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setPadding(32, 16, 32, 16);
        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AddressItem item = addresses.get(position);
        holder.textView.setText(item.address);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddressSelected(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull TextView textView) {
            super(textView);
            this.textView = textView;
        }
    }
}