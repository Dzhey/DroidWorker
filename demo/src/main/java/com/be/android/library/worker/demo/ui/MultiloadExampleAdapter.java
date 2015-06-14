package com.be.android.library.worker.demo.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.be.android.library.worker.demo.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiloadExampleAdapter extends BaseAdapter {

    public interface ListEntryDataRequestListener {
        void onDataRequested(int itemId);
    }

    public static class Item {
        private final int mId;
        private String mData;
        private boolean mIsLoaded;

        public Item(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        public boolean isLoaded() {
            return mIsLoaded;
        }

        public void setIsLoaded(boolean isLoaded) {
            mIsLoaded = isLoaded;
        }

        public String getData() {
            return mData;
        }

        public void setData(String data) {
            mData = data;
        }
    }

    private static class ViewHolder {
        View mView;
        ProgressBar mProgressBar;
        TextView mTextView;
    }

    private final List<Item> mItems;
    private final ListEntryDataRequestListener mListEntryDataRequestListener;

    public MultiloadExampleAdapter(ListEntryDataRequestListener listEntryDataRequestListener) {
        mListEntryDataRequestListener = listEntryDataRequestListener;
        mItems = new ArrayList<Item>();
    }

    public void setItems(Collection<Item> items) {
        mItems.clear();
        mItems.addAll(items);

        for (Item item : items) {
            if (!item.mIsLoaded) {
                mListEntryDataRequestListener.onDataRequested(item.mId);
            }
        }

        notifyDataSetChanged();
    }

    public void setItemData(int itemId, String data) {
        if (data == null) {
            throw new IllegalArgumentException("data == null");
        }

        Item item = findItemForId(itemId);

        if (item != null) {
            item.setData(data);
            item.setIsLoaded(true);
            notifyDataSetChanged();
        }
    }

    public Item findItemForId(int itemId) {
        for (Item item : mItems) {
            if (item.getId() == itemId) {
                return item;
            }
        }

        return null;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Item getItem(int pos) {
        return mItems.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return mItems.get(pos).mId;
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent) {
        final ViewHolder h;
        if (view == null) {
            view = LayoutInflater.from(parent.getContext())
                                 .inflate(R.layout.view_multiload_demo_item, parent, false);
            h = new ViewHolder();
            h.mView = view;
            h.mProgressBar = (ProgressBar) view.findViewById(android.R.id.progress);
            h.mTextView = (TextView) view.findViewById(android.R.id.text1);
            view.setTag(h);

        } else {
            h = (ViewHolder) view.getTag();
        }

        final Item item = getItem(pos);
        if (item.mIsLoaded) {
            h.mTextView.setText(String.format("%d. %s", item.mId, item.getData()));
            h.mProgressBar.setVisibility(View.INVISIBLE);
        } else {
            h.mTextView.setText(String.format("%d. Loading...", item.mId));
            h.mProgressBar.setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
