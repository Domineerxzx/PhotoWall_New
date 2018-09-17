package com.triplebro.photowall_new.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.triplebro.photowall_new.views.SquareImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoHorizontalScrollView extends HorizontalScrollView implements View.OnClickListener {

    private int position;
    private final LinearLayout.LayoutParams lpChildImage = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    private final LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
    private Context context;
    private List<String> imagePath;
    private PhotoHorizontalScrollView.Callback mCallback;
    private List<ImageView> iPicture = new ArrayList<>();
    private LinearLayout ll_photo_list_s;
    public int imageSize;

    public PhotoHorizontalScrollView(Context context) {
        super(context);
    }

    public PhotoHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public PhotoHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setImageData(LinearLayout ll_photo_list_s, List<String> imagePath, int paddingWidth, int position) {
        iPicture.clear();
        ll_photo_list_s.removeAllViews();
        this.ll_photo_list_s = ll_photo_list_s;
        this.imagePath = imagePath;
        this.position = position;
        DisplayMetrics mDisplayMetrics = context.getResources().getDisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int screenWidth = windowManager.getDefaultDisplay().getWidth();
        int mSpace = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, mDisplayMetrics) + 0.5f);
        if (imagePath.size() <= 2) {
            imageSize = (int) ((screenWidth * 1f - mSpace - paddingWidth) / 2);
            lpChildImage.width = imageSize;
            lpChildImage.height = imageSize;
        } else {
            imageSize = (int) ((screenWidth * 1f - mSpace * 2 - paddingWidth) / 2.4f);
            System.out.println("图片大小:" + imageSize);
            lpChildImage.width = imageSize;
            lpChildImage.height = imageSize;
        }
        for (String path : imagePath) {
            SquareImageView squareImageView = new SquareImageView(context);
            if (imagePath.size() != 1) {
                squareImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                squareImageView.setScaleType(ImageView.ScaleType.FIT_START);
            }
            squareImageView.setLayoutParams(lpChildImage);
            View view = new View(context);
            if (imagePath.size() <= 2) {
                viewParams.width = mSpace;
                view.setLayoutParams(viewParams);
            } else {
                viewParams.width = mSpace;
                view.setLayoutParams(viewParams);
            }
            Glide.with(context).load(new File(path)).into(squareImageView);
            squareImageView.setOnClickListener(this);
            this.ll_photo_list_s.addView(squareImageView);
            this.ll_photo_list_s.addView(view);
            iPicture.add(squareImageView);
            /*if (iPicture.size() < imagePath.size()) {
                iPicture.add(squareImageView);
            } else {
                iPicture.remove(imagePath.indexOf(path));
                iPicture.add(imagePath.indexOf(path), squareImageView);
            }*/
        }
    }

    @Override
    public void onClick(View v) {
        if (mCallback != null) {
            mCallback.onThumbPictureClick((ImageView) v, iPicture, imagePath, position, iPicture.indexOf(v));
        }
    }

    public interface Callback {
        void onThumbPictureClick(ImageView i, List<ImageView> imageGroupList, List<String> imagePath, int position, int index);
    }

    public void setCallback(PhotoHorizontalScrollView.Callback callback) {
        mCallback = callback;
    }
}
