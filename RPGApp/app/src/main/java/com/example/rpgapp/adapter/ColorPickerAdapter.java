package com.example.rpgapp.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import java.util.List;

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder> {

    private List<Integer> colors;
    private int selectedColor;
    private OnColorSelectedListener listener;

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    public ColorPickerAdapter(List<Integer> colors, int selectedColor, OnColorSelectedListener listener) {
        this.colors = colors;
        this.selectedColor = selectedColor;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        int color = colors.get(position);
        holder.bind(color, color == selectedColor, listener);
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        private View colorView;
        private View selectedIndicator;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.colorView);
            selectedIndicator = itemView.findViewById(R.id.selectedIndicator);
        }

        public void bind(int color, boolean isSelected, OnColorSelectedListener listener) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            colorView.setBackground(drawable);

            selectedIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> listener.onColorSelected(color));
        }
    }
}

