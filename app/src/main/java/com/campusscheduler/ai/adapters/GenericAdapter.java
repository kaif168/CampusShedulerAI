package com.campusscheduler.ai.adapters;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;

import java.util.List;

public class GenericAdapter<T> extends RecyclerView.Adapter<GenericAdapter.GenericViewHolder> {

    public interface OnBindViewHolderListener<T> {
        void onBind(T item, GenericViewHolder holder);
    }

    private final List<T> itemList;
    private final OnBindViewHolderListener<T> binder;
    private final int layoutId;

    public GenericAdapter(List<T> itemList, OnBindViewHolderListener<T> binder) {
        this(itemList, binder, R.layout.item_generic);
    }

    public GenericAdapter(List<T> itemList, OnBindViewHolderListener<T> binder, int layoutId) {
        this.itemList = itemList;
        this.binder = binder;
        this.layoutId = layoutId;
    }

    @NonNull
    @Override
    public GenericViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new GenericViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenericViewHolder holder, int position) {
        if (itemList != null && position < itemList.size()) {
            T item = itemList.get(position);
            if (binder != null) {
                binder.onBind(item, holder);
            }
        }
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    public static class GenericViewHolder extends RecyclerView.ViewHolder {

        private final SparseArray<View> views = new SparseArray<>();

        public GenericViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @SuppressWarnings("unchecked")
        public <V extends View> V getView(int viewId) {
            View view = views.get(viewId);
            if (view == null) {
                view = itemView.findViewById(viewId);
                views.put(viewId, view);
            }
            return (V) view;
        }

        public void setText(int viewId, String text) {
            TextView tv = getView(viewId);
            if (tv != null) {
                tv.setText(text != null ? text : "");
            }
        }
    }
}
