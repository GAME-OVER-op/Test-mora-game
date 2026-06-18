package com.mora.gamedock;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TileAdapter extends RecyclerView.Adapter<TileAdapter.VH> {

    private final List<Tile> items;

    public TileAdapter(List<Tile> items) {
        this.items = items;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView icon;
        final TextView label;
        final View iconBox;
        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.tileIcon);
            label = v.findViewById(R.id.tileLabel);
            iconBox = v.findViewById(R.id.tileIconBox);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tile_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Tile t = items.get(position);
        h.icon.setText(t.icon);
        h.label.setText(t.name);
        int color = t.active ? 0xFFE93363 : 0xFF2A2A2A;
        h.iconBox.setBackgroundTintList(ColorStateList.valueOf(color));
        h.label.setTextColor(t.active ? 0xFFFFFFFF : 0xFFB9B8B8);
        h.itemView.setOnClickListener(v -> {
            t.active = !t.active;
            notifyItemChanged(h.getAdapterPosition());
            // TODO mora: здесь можно дёрнуть root-команду под конкретный тумблер.
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
