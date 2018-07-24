package com.triplebro.photowall_new.activities;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.triplebro.photowall_new.R;
import com.triplebro.photowall_new.adapters.PhotoListAdapter;
import com.triplebro.photowall_new.adapters.PhotoWallAdapter;
import com.triplebro.photowall_new.beans.PhotoWallInfo;
import com.triplebro.photowall_new.utils.Utils;
import com.triplebro.photowall_new.widgets.ImageWatcher;
import com.triplebro.photowall_new.widgets.MyListView;

import java.util.List;


public class MainActivity extends Activity implements PhotoListAdapter.Callback, ImageWatcher.OnPictureLongPressListener {

    private ImageWatcher imageWatcher;
    private boolean isTranslucentStatus;
    private RelativeLayout rl_picture_imbtn;
    private ImageView btn_picture_magnify_delete;
    private ImageView btn_picture_magnify_share;
    private MyListView mlv_photo_wall;
    private PhotoWallAdapter photoWallAdapter;
    private PhotoWallInfo photoWallInfo;
    private List<PhotoWallInfo> photoWallInfos;
    private List<String> imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isTranslucentStatus = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            isTranslucentStatus = true;
        }
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        rl_picture_imbtn = (RelativeLayout) findViewById(R.id.rl_picture_imbtn);
        imageWatcher = (ImageWatcher) findViewById(R.id.v_image_watcher);
        btn_picture_magnify_delete = (ImageView) findViewById(R.id.btn_picture_magnify_delete);
        btn_picture_magnify_share = (ImageView) findViewById(R.id.btn_picture_magnify_share);
        mlv_photo_wall = (MyListView) findViewById(R.id.mlv_photo_wall);

        /*photoWallInfos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            photoWallInfo = new PhotoWallInfo();
            int name = i+1;
            imagePath = new ArrayList<>();
            imagePath.add("/data/data/com.triplebro.photowal_new/cache/"+name+".bmp");
            photoWallInfo.setImagePath(imagePath);
            photoWallInfo.setText(String.valueOf(i));
            photoWallInfos.add(photoWallInfo);
            photoWallInfo = null;
        }
        System.out.println(photoWallInfos.get(0).getImagePath().size());*/
        photoWallInfos = PhotoWallInfo.getPhotoList();
        photoWallAdapter = new PhotoWallAdapter(this, photoWallInfos, this);
        mlv_photo_wall.setAdapter(photoWallAdapter);


        //图片分享
        btn_picture_magnify_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "分享", Toast.LENGTH_SHORT).show();
            }
        });
        imageWatcher.setTranslucentStatus(!isTranslucentStatus ? Utils.calcStatusBarHeight(this) : 0);
        //长按
        imageWatcher.setOnPictureLongPressListener(this);
        //给放大动画后加按钮
        imageWatcher.setOnPictureButton(rl_picture_imbtn);
        //状态栏自适应
        Utils.fitsSystemWindows(isTranslucentStatus, findViewById(R.id.v_fit));
    }

    @Override
    public void onBackPressed() {
        if (!imageWatcher.handleBackPressed()) {
            super.onBackPressed();
        }
    }


    @Override
    public void onThumbPictureClick(final ImageView i, final List<ImageView> imageGroupList, List<String> imagePath, final int position,final int index) {
        imageWatcher.show(i, imageGroupList, imagePath);
        final int[] indexof = {index};
        //图片删除
        btn_picture_magnify_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "删除", Toast.LENGTH_SHORT).show();
                imageGroupList.remove(indexof[0]);
                List<String> path = photoWallInfos.get(position).getImagePath();
                path.remove(indexof[0]);
                PhotoWallAdapter photoWallAdapter = new PhotoWallAdapter(MainActivity.this, photoWallInfos, MainActivity.this);
                mlv_photo_wall.setAdapter(photoWallAdapter);
                if (path.size() == 0) {
                    photoWallInfos.remove(position);
                    imageWatcher.setVisibility(View.GONE);
                    rl_picture_imbtn.setVisibility(View.GONE);
                } else if (index == 0) {
                    imageWatcher.show(imageGroupList.get(indexof[0]), imageGroupList, path);
                } else {
                    imageWatcher.show(imageGroupList.get(indexof[0] - 1), imageGroupList, path);
                    indexof[0] = indexof[0] -1;
                }
            }
        });
    }

    //重写长按
    @Override
    public void onPictureLongPress(ImageView v, String url, int pos) {
        Toast.makeText(v.getContext().getApplicationContext(), "长按了第" + (pos + 1) + "张图片", Toast.LENGTH_SHORT).show();
    }
}
