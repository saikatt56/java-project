package com.example.digitalwellbeing.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.rules.AppCategory;
import com.example.digitalwellbeing.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Per-app rows on the dashboard; tapping a row opens its rule screen. */
public class UsageAdapter extends RecyclerView.Adapter<UsageAdapter.ViewHolder> {

    public interface OnAppSelected {
        void onAppSelected(UsageRecord record);
    }

    private final List<UsageRecord> items = new ArrayList<>();
    private final OnAppSelected listener;

    public UsageAdapter(OnAppSelected listener) {
        this.listener = listener;
    }

    public void submit(List<UsageRecord> records) {
        items.clear();
        items.addAll(records);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_usage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UsageRecord record = items.get(position);
        holder.name.setText(record.appLabel != null ? record.appLabel : record.packageName);
        holder.duration.setText(TimeUtils.formatDuration(record.foregroundMs));
        holder.detail.setText(String.format(Locale.US, "%s \u00b7 %d opens",
                AppCategory.label(holder.itemView.getContext(), record.category), record.launchCount));
        holder.itemView.setOnClickListener(v -> listener.onAppSelected(record));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView detail;
        final TextView duration;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.app_name);
            detail = itemView.findViewById(R.id.app_detail);
            duration = itemView.findViewById(R.id.app_duration);
        }
    }
}
