package com.triplebro.photowall_new.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.triplebro.photowall_new.R;
import com.triplebro.photowall_new.activities.MainActivity;
import com.triplebro.photowall_new.beans.PhotoWallInfo;
import com.triplebro.photowall_new.utils.ViewState;
import com.triplebro.photowall_new.widgets.PhotoHorizontalScrollView;

import java.util.List;

public class PhotoWallAdapter extends BaseAdapter {

    private Context context;
    private List<PhotoWallInfo> photoWallInfoList;
    private PhotoHorizontalScrollView.Callback mCallback;
    private ViewHolder viewHolder;

    public PhotoWallAdapter(Context context, List<PhotoWallInfo> photoWallInfoList, PhotoHorizontalScrollView.Callback mCallback) {
        this.context = context;
        this.photoWallInfoList = photoWallInfoList;
        this.mCallback = mCallback;
    }

    @Override
    public int getCount() {
        return photoWallInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = View.inflate(context, R.layout.item_photo_wall, null);
            viewHolder.phs_photo_list = convertView.findViewById(R.id.phs_photo_list);
            viewHolder.ll_photo_list = convertView.findViewById(R.id.ll_photo_list);
            viewHolder.ll_photo_list_inside = convertView.findViewById(R.id.ll_photo_list_inside);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        int paddingWidth = viewHolder.ll_photo_list.getPaddingLeft() + viewHolder.ll_photo_list.getPaddingRight();
        viewHolder.phs_photo_list.setHorizontalScrollBarEnabled(false);
        viewHolder.phs_photo_list.setCallback(mCallback);
        viewHolder.phs_photo_list.setImageData(viewHolder.ll_photo_list_inside, photoWallInfoList.get(position).getImagePath(), paddingWidth, position);
        if (photoWallInfoList.get(position).getImagePath().size() == 2) {
            viewHolder.phs_photo_list.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }
        return convertView;
    }

    private class ViewHolder {
        private PhotoHorizontalScrollView phs_photo_list;
        private LinearLayout ll_photo_list;
        private LinearLayout ll_photo_list_inside;
    }
}
