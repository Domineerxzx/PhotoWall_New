package com.triplebro.photowall_new.beans;

import java.util.ArrayList;
import java.util.List;

public class PhotoWallInfo {
    private String text;
    private List<String> imagePath;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getImagePath() {
        return imagePath;
    }

    public void setImagePath(List<String> imagePath) {
        this.imagePath = imagePath;
    }

    public static List<PhotoWallInfo> getPhotoList() {
        List<PhotoWallInfo> photoWallInfos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            PhotoWallInfo photoWallInfo = new PhotoWallInfo();
            List<String> imagePath = new ArrayList<>();
            switch (i) {
                case 0:
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i+1)+".jpg");
                    break;
                case 1:
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i+1)+".jpg");
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i+2)+".jpg");
                    break;
                case 2:
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i-1)+".jpg");
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i)+".jpg");
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i+1)+".jpg");
                    break;
                case 3:
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i-2)+".jpg");
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i-1)+".jpg");
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i)+".jpg");
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i+1)+".jpg");
                    break;
                case 4:
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i-3)+".jpg");
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i-2)+".jpg");
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i-1)+".jpg");
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i)+".jpg");
                    imagePath.add("/data/data/com.triplebro.photowall_new/cache/"+(i+1)+".jpg");
                    break;
            }
            photoWallInfo.setImagePath(imagePath);
            photoWallInfo.setText(String.valueOf(i+1));
            photoWallInfos.add(photoWallInfo);
        }
        return photoWallInfos;
    }
}
