package com.triplebro.photowall_new.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.triplebro.photowall_new.R;
import com.triplebro.photowall_new.views.SquareImageView;

import java.util.ArrayList;
import java.util.List;

public class PhotoListAdapter extends BaseAdapter implements OnClickListener {
    private int screenWidth;
    private int mSpace;
    private int paddingWidth;
    private int position;
    private int imageSize;
    private final LinearLayout.LayoutParams lpChildImage = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    private final LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
    private Context context;
    private List<String> imagePath;
    private PhotoListAdapter.Callback mCallback;
    private List<ImageView> iPicture = new ArrayList<>();

    public PhotoListAdapter(Context context, List<String> imagePath, int paddingWidth, int position) {
        this.context = context;
        this.imagePath = imagePath;
        this.paddingWidth = paddingWidth;
        this.position = position;
        DisplayMetrics mDisplayMetrics = context.getResources().getDisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        screenWidth = windowManager.getDefaultDisplay().getWidth();
        mSpace = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, mDisplayMetrics) + 0.5f);
        if (imagePath.size() == 1) {
            imageSize = (int) ((screenWidth * 1f - mSpace - paddingWidth) / 2);
            lpChildImage.width = imageSize;
            lpChildImage.height = imageSize;
        }else if (imagePath.size() <= 2) {
            imageSize = (int) ((screenWidth * 1f - mSpace - paddingWidth) / 2);
            lpChildImage.width = imageSize;
            lpChildImage.height = lpChildImage.width;
        }
        if (imagePath.size() > 2) {
            imageSize = (int) ((screenWidth * 1f - mSpace * 2 - paddingWidth) / 2.4f);
            System.out.println("图片大小:" + imageSize);
            lpChildImage.width = imageSize;
            lpChildImage.height = lpChildImage.width;
        }
    }

    @Override
    public int getCount() {
        return imagePath.size();
    }

    @Override
    public Object getItem(int position) {
        return imagePath.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        viewHolder = new ViewHolder();
        convertView = View.inflate(context, R.layout.item_photo_list, null);
        viewHolder.siv_photo_list = convertView.findViewById(R.id.siv_photo_list);
        convertView.setTag(viewHolder);
        viewHolder.siv_photo_list.setLayoutParams(lpChildImage);
        if(imagePath.size()!=1){
            viewHolder.siv_photo_list.setScaleType(ImageView.ScaleType.FIT_XY);
        }else{
            viewHolder.siv_photo_list.setScaleType(ImageView.ScaleType.FIT_START);
        }
        viewHolder.v_view = convertView.findViewById(R.id.v_view);
        if(imagePath.size()<=2){
            viewParams.width = mSpace;
            viewHolder.v_view.setLayoutParams(viewParams);
        }else {
            viewParams.width = mSpace;
            viewHolder.v_view.setLayoutParams(viewParams);

        }
        Glide.with(context).load(imagePath.get(position)).into(viewHolder.siv_photo_list);

        if(iPicture.size() <= position||iPicture.get(position) == null){
            iPicture.add(position,viewHolder.siv_photo_list);
        }else{
            iPicture.remove(position);
            iPicture.add(position,viewHolder.siv_photo_list);
        }
        viewHolder.siv_photo_list.setOnClickListener(this);
        return convertView;
    }

    private class ViewHolder {
        private SquareImageView siv_photo_list;
        private View v_view;
    }

    @Override
    public void onClick(View v) {
        if (mCallback != null) {
            System.out.println(iPicture.size() + "---------------------------------");
            mCallback.onThumbPictureClick((ImageView) v, iPicture, imagePath, position);
        }
    }

    public interface Callback {
        void onThumbPictureClick(ImageView i, List<ImageView> imageGroupList, List<String> imagePath, int position);
    }

    public void setCallback(PhotoListAdapter.Callback callback) {
        mCallback = callback;
    }
}
