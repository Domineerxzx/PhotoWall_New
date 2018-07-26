package com.triplebro.photowall_new.adapters;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.triplebro.photowall_new.R;
import com.triplebro.photowall_new.beans.PhotoWallInfo;
import com.triplebro.photowall_new.widgets.HorizontalListView;
import com.triplebro.photowall_new.widgets.PhotoHorizontalScrollView;

import java.util.List;

public class PhotoWallAdapter extends BaseAdapter {

    private Context context;
    private List<PhotoWallInfo> photoWallInfoList;
    private int paddingWidth;
    private PhotoHorizontalScrollView.Callback mCallback;
    private int screenWidth;
    private int mSpace;
    private int imageSize;
    private final LinearLayout.LayoutParams lpChildImage = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

    public PhotoWallAdapter(Context context, List<PhotoWallInfo> photoWallInfoList,PhotoHorizontalScrollView.Callback mCallback) {
        this.context = context;
        this.photoWallInfoList = photoWallInfoList;
        this.mCallback = mCallback;
        DisplayMetrics mDisplayMetrics = context.getResources().getDisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        screenWidth = windowManager.getDefaultDisplay().getWidth();
        mSpace = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, mDisplayMetrics) + 0.5f);
    }

    @Override
    public int getCount() {
        return photoWallInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return photoWallInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = View.inflate(context,R.layout.item_photo_wall, null);
            viewHolder.phs_photo_list = convertView.findViewById(R.id.phs_photo_list);
            viewHolder.textView = convertView.findViewById(R.id.tv_text);
            viewHolder.ll_photo_list = convertView.findViewById(R.id.ll_photo_list);
            viewHolder.ll_photo_list_s = convertView.findViewById(R.id.ll_photo_list_s);
            int size = photoWallInfoList.get(position).getImagePath().size();
            if (size <= 2) {
                imageSize = (int) ((screenWidth * 1f - mSpace - paddingWidth) / 2);
                lpChildImage.height = imageSize;
            }
            if (size > 2) {
                imageSize = (int) ((screenWidth * 1f - mSpace * 2 - paddingWidth) / 2.4f);
                System.out.println("图片大小:" + imageSize);
                lpChildImage.height = imageSize;
            }
            viewHolder.phs_photo_list.setLayoutParams(lpChildImage);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        paddingWidth = viewHolder.ll_photo_list.getPaddingLeft() + viewHolder.ll_photo_list.getPaddingRight();
        viewHolder.phs_photo_list.setCallback(mCallback);
        viewHolder.phs_photo_list.setImageData(viewHolder.ll_photo_list_s,photoWallInfoList.get(position).getImagePath(),paddingWidth,position);
        viewHolder.textView.setText(photoWallInfoList.get(position).getText());
        return convertView;
    }

    private class ViewHolder{
        private PhotoHorizontalScrollView phs_photo_list;
        private TextView textView;
        private LinearLayout ll_photo_list;
        private LinearLayout ll_photo_list_s;
    }
}
