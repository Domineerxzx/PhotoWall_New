package com.triplebro.photowall_new.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.triplebro.photowall_new.R;
import com.triplebro.photowall_new.utils.ViewState;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

public class ImageWatcher extends FrameLayout implements GestureDetector.OnGestureListener, ViewPager.OnPageChangeListener {
    private static final int SINGLE_TAP_UP_CONFIRMED = 1;
    private final Handler mHandler;

    static final float MIN_SCALE = 0.5f;
    static final float MAX_SCALE = 3.8f;
    static int MAX_TRANSLATE_X;
    static int MAX_TRANSLATE_Y;

    private static final int TOUCH_MODE_NONE = 0; // 无状态
    private static final int TOUCH_MODE_DOWN = 1; // 按下
    private static final int TOUCH_MODE_DRAG = 2; // 单点拖拽
    private static final int TOUCH_MODE_EXIT = 3; // 退出动作
    private static final int TOUCH_MODE_SLIDE = 4; // 页面滑动
    private static final int TOUCH_MODE_SCALE_ROTATE = 5; // 缩放旋转
    private static final int TOUCH_MODE_LOCK = 6; // 缩放旋转锁定
    private static final int TOUCH_MODE_AUTO_FLING = 7; // 动画中


    private ImageView iSource;   //大图
    private ImageView iOrigin;  //缩略图

    private int mStatusBarHeight;
    private int mWidth, mHeight;
    private int mBackgroundColor = 0x00000000;
    private int mTouchMode = TOUCH_MODE_NONE;
    private float mTouchSlop;//在被判定为滚动之前用户手指可以移动的最大值

    private float mFingersDistance;
    private double mFingersAngle; // 相对于[东] point0作为起点;point1作为终点
    private float mFingersCenterX;
    private float mFingersCenterY;
    private float mExitScalingRef; // 触摸退出进度

    private ValueAnimator animBackground;
    private ValueAnimator animImageTransform;
    private boolean isInTransformAnimation;
    private final GestureDetector mGestureDetector;
    private OnPictureLongPressListener mPictureLongPressListener;
    private ImagePagerAdapter adapter;
    private ViewPager vPager;
    private List<ImageView> mImageGroupList;
    private List<String> mUrlList;
    private int initPosition;
    private int mPagerPositionOffsetPixels;
    private RelativeLayout frameLayout;
    private ViewState.ValueAnimatorBuilder lastValueAnimatorBuilder;
    private ImageView originRef;

    public ImageWatcher(final Context context, AttributeSet attrs) {
        super(context, attrs);
        mHandler = new GestureHandler(this);
        mGestureDetector = new GestureDetector(context, this);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        addView(vPager = new ViewPager(getContext()));
        vPager.addOnPageChangeListener(this);
        //vPager.setPageTransformer(false,new DepthPageTransformer());
        setVisibility(View.INVISIBLE);
    }

    //获取当前页面的position
    public int getCurrentItem() {
        return vPager.getCurrentItem();
    }

    public ImageView getISource() {
        return iSource;
    }

    /*查看大图后在大图上删除图片*/
    public void delete() {
        View view = vPager.findViewWithTag(vPager.getCurrentItem());
        int currentItem = vPager.getCurrentItem();
        ImageView remove = mImageGroupList.remove(currentItem);
        mUrlList.remove(currentItem);
        PagerAdapter adapter = vPager.getAdapter();
        adapter.notifyDataSetChanged();
        vPager.setCurrentItem(currentItem, false);

    }

    /**
     * @param i              被点击的ImageView
     * @param imageGroupList 被点击的ImageView的所在列表，加载图片时会提前展示列表中已经下载完成的thumb图片
     * @param urlList        被加载的图片url列表，数量必须大于等于 imageGroupList.size。 且顺序应当和imageGroupList保持一致
     */

    public void show(ImageView i, List<ImageView> imageGroupList, final List<String> urlList) {
        if (i == null || imageGroupList == null || urlList == null || imageGroupList.size() < 1 ||
                urlList.size() < imageGroupList.size()) {
            String info = "i[" + i + "]";
            info += "#imageGroupList " + (imageGroupList == null ? "null" : "size : " + imageGroupList.size());
            info += "#urlList " + (urlList == null ? "null" : "size :" + urlList.size());
            throw new IllegalArgumentException("error params \n" + info);
        }

        //查找i图片在什么位置，返回所在的位置数
        initPosition = imageGroupList.indexOf(i);
        if (initPosition < 0) {
            throw new IllegalArgumentException("error params initPosition " + initPosition);
        }

        if (i.getDrawable() == null) return;

        if (animImageTransform != null) animImageTransform.cancel();
        animImageTransform = null;

        mImageGroupList = imageGroupList;
        mUrlList = urlList;

        iOrigin = null;
        iSource = null;

        ImageWatcher.this.setVisibility(View.VISIBLE);
        vPager.setAdapter(adapter = new ImagePagerAdapter());
        vPager.setCurrentItem(initPosition);//设置当前页面

        if (frameLayout != null) {
            frameLayout.setVisibility(VISIBLE);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (iSource == null) return true;
        if (isInTransformAnimation) return true;

        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);//高清图初始

        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_UP:
                onUp(event);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (vsDefault != null && (mTouchMode != TOUCH_MODE_SLIDE) || mPagerPositionOffsetPixels == 0) {
                    if (mTouchMode != TOUCH_MODE_SCALE_ROTATE) {
                        mFingersDistance = 0;
                        mFingersAngle = 0;
                        mFingersCenterX = 0;
                        mFingersCenterY = 0;
                        ViewState.write(iSource, ViewState.STATE_TOUCH_SCALE_ROTATE);
                    }
                    mTouchMode = TOUCH_MODE_SCALE_ROTATE;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (vsDefault != null && mTouchMode != TOUCH_MODE_SLIDE) {
                    if (event.getPointerCount() - 1 < 1 + 1) {
                        mTouchMode = TOUCH_MODE_LOCK;
                    }
                }
                break;
        }
        return mGestureDetector.onTouchEvent(event);
    }

    //手指刚接触屏的一刹那,若取消，会导致放大图状态无法滑动拖动,可点
    @Override
    public boolean onDown(MotionEvent e) {
        mTouchMode = TOUCH_MODE_DOWN;
        ViewState.write(iSource, ViewState.STATE_TOUCH_DOWN);
        vPager.onTouchEvent(e);
        return true;
    }

    //手指离开屏幕的一瞬间,主要负责旋转下拉动画的退出
    public void onUp(MotionEvent e) {
        if (mTouchMode == TOUCH_MODE_EXIT) {
            handleExitTouchResult();
        } else if (mTouchMode == TOUCH_MODE_SCALE_ROTATE
                || mTouchMode == TOUCH_MODE_LOCK) {
            handleScaleRotateTouchResult();

        } else if (mTouchMode == TOUCH_MODE_DRAG) {
            handleDragTouchResult();
        }
        vPager.onTouchEvent(e);
    }

    //滑动
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        final float moveX = e2.getX() - e1.getX();
        final float moveY = e2.getY() - e1.getY();
        if (mTouchMode == TOUCH_MODE_DOWN) {
            if (Math.abs(moveX) > mTouchSlop || Math.abs(moveY) > mTouchSlop) {
                ViewState vsCurrent = ViewState.write(iSource, ViewState.STATE_CURRENT);
                ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);

                if (vsDefault == null) {

                    // 没有vsDefault标志的View说明图标正在下载中。转化为Slide手势，可以进行viewpager的翻页滑动
                    mTouchMode = TOUCH_MODE_SLIDE;

                } else if (vsCurrent.scaleY > vsDefault.scaleY || vsCurrent.scaleX > vsDefault.scaleX) {

                    // 图片当前为放大状态。宽或高超出了屏幕尺寸
                    if (mTouchMode != TOUCH_MODE_DRAG) {

                        ViewState.write(iSource, ViewState.STATE_DRAG);

                    }


                    // 转化为Drag手势，可以对图片进行拖拽操作
                    mTouchMode = TOUCH_MODE_DRAG;
                    String imageOrientation = (String) iSource.getTag(R.id.image_orientation);
                    if ("horizontal".equals(imageOrientation)) {
                        float translateXEdge = vsDefault.width * (vsCurrent.scaleX - 1) / 2;
                        if (vsCurrent.translationX >= translateXEdge && moveX > 0) {
                            // 图片位于边界，且仍然尝试向边界外拽动。。转化为Slide手势，可以进行viewpager的翻页滑动
                            mTouchMode = TOUCH_MODE_SLIDE;
                        } else if (vsCurrent.translationX <= -translateXEdge && moveX < 0) {
                            // 同上
                            mTouchMode = TOUCH_MODE_SLIDE;
                        }
                    } else if ("vertical".equals(imageOrientation)) {
                        if (vsDefault.width * vsCurrent.scaleX <= mWidth) {
                            // 同上
                            mTouchMode = TOUCH_MODE_SLIDE;
                        } else {
                            float translateXRightEdge = vsDefault.width * vsCurrent.scaleX / 2 - vsDefault.width / 2;
                            float translateXLeftEdge = mWidth - vsDefault.width * vsCurrent.scaleX / 2 - vsDefault.width / 2;
                            if (vsCurrent.translationX >= translateXRightEdge && moveX > 0) {
                                // 同上
                                mTouchMode = TOUCH_MODE_SLIDE;
                            } else if (vsCurrent.translationX <= translateXLeftEdge && moveX < 0) {
                                // 同上
                                mTouchMode = TOUCH_MODE_SLIDE;
                            }
                        }
                    }
                } else if (Math.abs(moveX) < mTouchSlop && moveY > mTouchSlop * 3 || moveY > mTouchSlop * 3 && Math.abs(moveX) > mTouchSlop * 1.5) {
                    // 单手垂直下拉。转化为Exit手势，可以在下拉过程中看到原始界面;
                    mTouchMode = TOUCH_MODE_EXIT;
                } else if (Math.abs(moveX) > mTouchSlop * 2) {
                    // 左右滑动。转化为Slide手势，可以进行viewpager的翻页滑动
                    mTouchMode = TOUCH_MODE_SLIDE;
                }
            }
        }

        if (mTouchMode == TOUCH_MODE_SLIDE) {
            vPager.onTouchEvent(e2);
        } else if (mTouchMode == TOUCH_MODE_SCALE_ROTATE) {
            handleScaleRotateGesture(e2);
        } else if (mTouchMode == TOUCH_MODE_EXIT) {
            handleExitGesture(e2, e1);
        } else if (mTouchMode == TOUCH_MODE_DRAG) {
            handleDragGesture(e2, e1);
        }
        return false;
    }

    private void handleScaleRotateGesture(MotionEvent e2) {

    }


    /**
     * 处理结束缩放旋转模式的手指事件，进行恢复到零旋转角度和大小收缩到正常范围以内的收尾动画<br>
     * 如果是从{@link ImageWatcher#TOUCH_MODE_EXIT}半路转化过来的事件 还需要还原背景色
     */
    private void handleScaleRotateTouchResult() {
        if (iSource == null) return;
        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
        if (vsDefault == null) return;
        ViewState vsCurrent = ViewState.write(iSource, ViewState.STATE_CURRENT);

        final float endScaleX, endScaleY;
        Log.e("TTT", "AAA  vsCurrent.scaleX :" + vsCurrent.scaleX + "###  vsDefault.scaleX:" + vsDefault.scaleX);
        endScaleX = vsCurrent.scaleX < vsDefault.scaleX ? vsDefault.scaleX : vsCurrent.scaleX;
        endScaleY = vsCurrent.scaleY < vsDefault.scaleY ? vsDefault.scaleY : vsCurrent.scaleY;

        ViewState vsTemp = ViewState.copy(vsDefault, ViewState.STATE_TEMP).scaleX(endScaleX).scaleY(endScaleY);
        iSource.setTag(ViewState.STATE_TEMP, vsTemp);
        animSourceViewStateTransform(iSource, vsTemp);
        animBackgroundTransform(0xFF000000);
    }


    /**
     * 处理单击的手指事件
     */
    public boolean onSingleTapConfirmed() {
        if (iSource == null) return false;
        if (frameLayout != null) {
            frameLayout.setVisibility(GONE);
        }
        ViewState vsCurrent = ViewState.write(iSource, ViewState.STATE_CURRENT);
        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
        if (vsDefault == null || (vsCurrent.scaleY <= vsDefault.scaleY && vsCurrent.scaleX <= vsDefault.scaleX)) {
            mExitScalingRef = 0;
        } else {
            mExitScalingRef = 1;
        }
        handleExitTouchResult();
        return true;
    }

    //留着
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        boolean hadTapMessage = mHandler.hasMessages(SINGLE_TAP_UP_CONFIRMED);
        if (hadTapMessage) {
            mHandler.removeMessages(SINGLE_TAP_UP_CONFIRMED);
            handleDoubleTapTouchResult();
            return true;
        } else {
            mHandler.sendEmptyMessageDelayed(SINGLE_TAP_UP_CONFIRMED, 350);
        }
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (mPictureLongPressListener != null) {
            mPictureLongPressListener.onPictureLongPress(iSource, String.valueOf(mUrlList.get(vPager.getCurrentItem())), vPager.getCurrentItem());
        }

    }


    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    /**
     * 处理响应退出图片查看
     * 控制拖动退出功能，若注释只能单点退出
     */
    private void handleExitGesture(MotionEvent e2, MotionEvent e1) {
        if (iSource == null) return;
        if (frameLayout != null) {
            frameLayout.setVisibility(GONE);
        }
        ViewState vsTouchDown = ViewState.read(iSource, ViewState.STATE_TOUCH_DOWN);
        if (vsTouchDown == null) return;

        mExitScalingRef = 1;
        final float moveY = e2.getY() - e1.getY();
        final float moveX = e2.getX() - e1.getX();
        if (moveY > 0) {
            mExitScalingRef -= moveY / getHeight();
        }
        if (mExitScalingRef < MIN_SCALE) {
            mExitScalingRef = MIN_SCALE;

        }
        iSource.setTranslationX(vsTouchDown.translationX + moveX);
        iSource.setTranslationY(vsTouchDown.translationY + moveY);
        iSource.setScaleX(vsTouchDown.scaleX * mExitScalingRef);
        iSource.setScaleY(vsTouchDown.scaleY * mExitScalingRef);
        setBackgroundColor(mColorEvaluator.evaluate(mExitScalingRef, 0x00000000, 0xFF000000));
    }


    /**
     * 处理响应单手拖拽平移
     */
    private void handleDragGesture(MotionEvent e2, MotionEvent e1) {
        if (iSource == null) return;
        final float moveY = e2.getY() - e1.getY();
        final float moveX = e2.getX() - e1.getX();

        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
        if (vsDefault == null) return;
        ViewState vsTouchDrag = ViewState.read(iSource, ViewState.STATE_DRAG);
        if (vsTouchDrag == null) return;

        float translateXValue = vsTouchDrag.translationX + moveX * 1.6f;

        String imageOrientation = (String) iSource.getTag(R.id.image_orientation);
        if ("horizontal".equals(imageOrientation)) {
            float translateXEdge = vsDefault.width * (vsTouchDrag.scaleX - 1) / 2;
            if (translateXValue > translateXEdge) {
                translateXValue = translateXEdge + (translateXValue - translateXEdge) * 0.12f;
            } else if (translateXValue < -translateXEdge) {
                translateXValue = -translateXEdge + (translateXValue - (-translateXEdge)) * 0.12f;
            }
        } else if ("vertical".equals(imageOrientation)) {
            if (vsDefault.width * vsTouchDrag.scaleX <= mWidth) {
                mTouchMode = TOUCH_MODE_SLIDE;
            } else {
                float translateXRightEdge = vsDefault.width * vsTouchDrag.scaleX / 2 - vsDefault.width / 2;
                float translateXLeftEdge = mWidth - vsDefault.width * vsTouchDrag.scaleX / 2 - vsDefault.width / 2;

                if (translateXValue > translateXRightEdge) {
                    translateXValue = translateXRightEdge + (translateXValue - translateXRightEdge) * 0.12f;
                } else if (translateXValue < translateXLeftEdge) {
                    translateXValue = translateXLeftEdge + (translateXValue - translateXLeftEdge) * 0.12f;
                }
            }
        }
        iSource.setTranslationX(translateXValue);
        iSource.setTranslationY(vsTouchDrag.translationY + moveY * 1.6f);
    }

    /**
     * 处理结束下拉退出的手指事件，进行退出图片查看或者恢复到初始状态的收尾动画<br>
     * 还需要还原背景色
     * 注销的话按返回键无法退出放大图片状态
     * 必须要用这个方法，重点！！！！！！！！！
     */
    private void handleExitTouchResult() {
        if (iSource == null) return;

        //没有退出
        if (mExitScalingRef > 0.9f) {
            ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
            if (vsDefault == null) return;
            if (frameLayout != null) {
                frameLayout.setVisibility(VISIBLE);
            }
            animSourceViewStateTransform(iSource, vsDefault);
            animBackgroundTransform(0xFF000000);
        } else {
            adapter.setDefaultDisplayConfigs(adapter.mImageSparseArray.get(vPager.getCurrentItem()),vPager.getCurrentItem(),false);
            ViewState vsOrigin = ViewState.read(iSource, ViewState.STATE_ORIGIN);
            if (vsOrigin == null) return;
            if (vsOrigin.alpha == 0)
                vsOrigin.translationX(iSource.getTranslationX()).translationY(iSource.getTranslationY());

            animSourceViewStateTransform(iSource, vsOrigin);
            animBackgroundTransform(0x00000000);

            ((FrameLayout) iSource.getParent()).getChildAt(2);
            originRef = null;
        }
    }

    /**
     * 处理结束双击的手指事件，进行图片放大到指定大小或者恢复到初始大小的收尾动画
     */
    private void handleDoubleTapTouchResult() {
        if (iSource == null) return;
        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
        if (vsDefault == null) return;
        ViewState vsCurrent = ViewState.write(iSource, ViewState.STATE_CURRENT);

        if (vsCurrent.scaleY <= vsDefault.scaleY && vsCurrent.scaleX <= vsDefault.scaleX) {
            final float expectedScale = (MAX_SCALE - vsDefault.scaleX) * 0.4f + vsDefault.scaleX;
            animSourceViewStateTransform(iSource,
                    ViewState.write(iSource, ViewState.STATE_TEMP).scaleX(expectedScale).scaleY(expectedScale));
        } else {
            animSourceViewStateTransform(iSource, vsDefault);
        }
    }


    /**
     * 处理结束拖拽模式的手指事件，进行超过边界则恢复到边界的收尾动画
     */
    private void handleDragTouchResult() {
        if (iSource == null) return;
        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
        if (vsDefault == null) return;
        ViewState vsCurrent = ViewState.write(iSource, ViewState.STATE_CURRENT);

        final float endTranslateX, endTranslateY;
        String imageOrientation = (String) iSource.getTag(R.id.image_orientation);
        if ("horizontal".equals(imageOrientation)) {
            float translateXEdge = vsDefault.width * (vsCurrent.scaleX - 1) / 2;
            if (vsCurrent.translationX > translateXEdge) endTranslateX = translateXEdge;
            else if (vsCurrent.translationX < -translateXEdge)
                endTranslateX = -translateXEdge;
            else endTranslateX = vsCurrent.translationX;

            if (vsDefault.height * vsCurrent.scaleY <= mHeight) {
                endTranslateY = vsDefault.translationY;
            } else {
                float translateYBottomEdge = vsDefault.height * vsCurrent.scaleY / 2 - vsDefault.height / 2;
                float translateYTopEdge = mHeight - vsDefault.height * vsCurrent.scaleY / 2 - vsDefault.height / 2;

                if (vsCurrent.translationY > translateYBottomEdge)
                    endTranslateY = translateYBottomEdge;
                else if (vsCurrent.translationY < translateYTopEdge)
                    endTranslateY = translateYTopEdge;
                else endTranslateY = vsCurrent.translationY;
            }
        } else if ("vertical".equals(imageOrientation)) {
            float translateYEdge = vsDefault.height * (vsCurrent.scaleY - 1) / 2;
            if (vsCurrent.translationY > translateYEdge) endTranslateY = translateYEdge;
            else if (vsCurrent.translationY < -translateYEdge)
                endTranslateY = -translateYEdge;
            else endTranslateY = vsCurrent.translationY;

            if (vsDefault.width * vsCurrent.scaleX <= mWidth) {
                endTranslateX = vsDefault.translationX;
            } else {
                float translateXRightEdge = vsDefault.width * vsCurrent.scaleX / 2 - vsDefault.width / 2;
                float translateXLeftEdge = mWidth - vsDefault.width * vsCurrent.scaleX / 2 - vsDefault.width / 2;

                if (vsCurrent.translationX > translateXRightEdge)
                    endTranslateX = translateXRightEdge;
                else if (vsCurrent.translationX < translateXLeftEdge)
                    endTranslateX = translateXLeftEdge;
                else endTranslateX = vsCurrent.translationX;
            }
        } else {
            return;
        }
        if (vsCurrent.translationX == endTranslateX && vsCurrent.translationY == endTranslateY) {
            return;// 如果没有变化跳过动画实行时间的触摸锁定
        }
        animSourceViewStateTransform(iSource,
                ViewState.write(iSource, ViewState.STATE_TEMP).translationX(endTranslateX).translationY(endTranslateY));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mPagerPositionOffsetPixels = positionOffsetPixels;
    }

    /**
     * 每当ViewPager滑动到新的一页后，此方法会被触发<br/>
     * 此刻必不可少的需要同步更新顶部索引，还原前一项后一项的状态等
     */


    @Override
    public void onPageSelected(int position) {
        vPager.setTag(position);
        iSource = adapter.mImageSparseArray.get(position);
        if (iOrigin != null) {
            iOrigin.setVisibility(View.VISIBLE);
        }
        if (position < mImageGroupList.size()) {
            iOrigin = mImageGroupList.get(position);
            if (iOrigin.getDrawable() != null) iOrigin.setVisibility(View.INVISIBLE);
        }

        //上一个图片记录，不过不清楚是什么意思
        ImageView mLast = adapter.mImageSparseArray.get(position - 1);
        if (ViewState.read(mLast, ViewState.STATE_DEFAULT) != null) {
            lastValueAnimatorBuilder = ViewState.restoreByAnim(mLast, ViewState.STATE_DEFAULT);
            lastValueAnimatorBuilder.create().start();
        }
        //下一个图片记录，不清楚含义
        ImageView mNext = adapter.mImageSparseArray.get(position + 1);
        if (ViewState.read(mNext, ViewState.STATE_DEFAULT) != null) {
            ViewState.restoreByAnim(mNext, ViewState.STATE_DEFAULT).create().start();
        }
    }


    @Override
    public void onPageScrollStateChanged(int state) {
    }

    class ImagePagerAdapter extends PagerAdapter {
        /*       private final LayoutParams lpCenter = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);*/
        public final SparseArray<ImageView> mImageSparseArray = new SparseArray<>();
        private boolean hasPlayBeginAnimation;

        @Override
        public int getCount() {
            return mImageGroupList != null ? mImageGroupList.size() : 0;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
            mImageSparseArray.remove(position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            FrameLayout itemView = new FrameLayout(container.getContext());
            container.addView(itemView);
            ImageView imageView = new ImageView(container.getContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);//注释，缩略图放大过程会变形一下
            itemView.addView(imageView);
            mImageSparseArray.put(position, imageView);
            if (setDefaultDisplayConfigs(imageView, position, hasPlayBeginAnimation)) {
                hasPlayBeginAnimation = true;
            }
            System.out.println("----------------------------------"+position);
            return itemView;
        }

        /*解决数据不刷新的问题*/
        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }


        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }


        //貌似是从缩略图开始放大的过程
        private boolean setDefaultDisplayConfigs(final ImageView imageView, final int pos, boolean hasPlayBeginAnimation) {
            boolean isFindEnterImagePicture = false;

            ViewState.write(imageView, ViewState.STATE_ORIGIN).alpha(0).scaleXBy(1.5f).scaleYBy(1.5f);
            if (pos < mImageGroupList.size()) {
                ImageView originRef = mImageGroupList.get(pos);
                if (pos == initPosition && !hasPlayBeginAnimation) {
                    isFindEnterImagePicture = true;
                    iSource = imageView;
                    iOrigin = originRef;
                }


                //注释后，放大还原动画会变形
                int[] location = new int[2];
                originRef.getLocationOnScreen(location);
                imageView.setTranslationX(location[0]);
                int locationYOfFullScreen = location[1];
                locationYOfFullScreen -= mStatusBarHeight;
                imageView.setTranslationY(locationYOfFullScreen);
                imageView.getLayoutParams().width = originRef.getWidth();
                imageView.getLayoutParams().height = originRef.getHeight();

                ViewState.write(imageView, ViewState.STATE_ORIGIN).width(originRef.getWidth()).height(originRef.getHeight());


                //下面这个方法屏蔽之后从缩略图点击放大过程，缩略图会闪一下
                Drawable bmpMirror = originRef.getDrawable();
                if (bmpMirror != null) {
                    int bmpMirrorWidth = bmpMirror.getBounds().width();
                    int bmpMirrorHeight = bmpMirror.getBounds().height();
                    ViewState vsThumb = ViewState.write(imageView, ViewState.STATE_THUMB).width(bmpMirrorWidth).height(bmpMirrorHeight)
                            .translationX((mWidth - bmpMirrorWidth) / 2).translationY((mHeight - bmpMirrorHeight) / 2);
                    imageView.setImageDrawable(bmpMirror);

                    if (isFindEnterImagePicture) {
                        animSourceViewStateTransform(imageView, vsThumb);
                    } else {
                        ViewState.restore(imageView, vsThumb.mTag);
                    }
                }
            }

            final boolean isPlayEnterAnimation = isFindEnterImagePicture;


            // loadHighDefinitionPicture，动画效果不在这里，负责将图片放大
            Glide.with(imageView.getContext()).load(new File(mUrlList.get(pos))).asBitmap().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    final int sourceDefaultWidth, sourceDefaultHeight, sourceDefaultTranslateX, sourceDefaultTranslateY;
                    int resourceImageWidth = resource.getWidth();
                    int resourceImageHeight = resource.getHeight();
                    if (resourceImageWidth * 1f / resourceImageHeight > mWidth * 1f / mHeight) {
                        sourceDefaultWidth = mWidth;
                        sourceDefaultHeight = (int) (sourceDefaultWidth * 1f / resourceImageWidth * resourceImageHeight);
                        sourceDefaultTranslateX = 0;
                        sourceDefaultTranslateY = (mHeight - sourceDefaultHeight) / 2;
                        imageView.setTag(R.id.image_orientation, "horizontal");
                    } else {
                        sourceDefaultHeight = mHeight;
                        sourceDefaultWidth = (int) (sourceDefaultHeight * 1f / resourceImageHeight * resourceImageWidth);
                        sourceDefaultTranslateY = 0;
                        sourceDefaultTranslateX = (mWidth - sourceDefaultWidth) / 2;
                        imageView.setTag(R.id.image_orientation, "vertical");
                    }
                    imageView.setImageBitmap(resource);
                    /*notifyItemChangedState(pos, false, false);*/

                    ViewState vsDefault = ViewState.write(imageView, ViewState.STATE_DEFAULT).width(sourceDefaultWidth).height(sourceDefaultHeight)
                            .translationX(sourceDefaultTranslateX).translationY(sourceDefaultTranslateY);
                    if (isPlayEnterAnimation) {
                        animSourceViewStateTransform(imageView, vsDefault);
                    } else {
                        ViewState.restore(imageView, vsDefault.mTag);
                        imageView.setAlpha(0f);
                        imageView.animate().alpha(1).start();
                    }

                }

            });
            //控制放大后的黑色的背景
            if (isPlayEnterAnimation) {
                iOrigin.setVisibility(View.INVISIBLE);
                animBackgroundTransform(0xFF000000);
            }
            return isPlayEnterAnimation;
        }
    }


    private static class GestureHandler extends Handler {
        WeakReference<ImageWatcher> mRef;

        GestureHandler(ImageWatcher ref) {
            mRef = new WeakReference<>(ref);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mRef.get() != null) {
                ImageWatcher holder = mRef.get();
                switch (msg.what) {
                    case SINGLE_TAP_UP_CONFIRMED:
                        holder.onSingleTapConfirmed();
                        break;
                    default:
                        throw new RuntimeException("Unknown message " + msg); //never
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mPagerPositionOffsetPixels == 0;
    }

    /**
     * 动画执行时加入这个监听器后会自动记录标记 {@link ImageWatcher#isInTransformAnimation} 的状态<br/>
     * isInTransformAnimation值为true的时候可以达到在动画执行时屏蔽触摸操作的目的
     */
    final AnimatorListenerAdapter mAnimTransitionStateListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationCancel(Animator animation) {
            isInTransformAnimation = false;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            isInTransformAnimation = true;
            mTouchMode = TOUCH_MODE_AUTO_FLING;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            isInTransformAnimation = false;
        }
    };

    final TypeEvaluator<Integer> mColorEvaluator = new TypeEvaluator<Integer>() {
        @Override
        public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
            int startColor = startValue;
            int endColor = endValue;

            int alpha = (int) (Color.alpha(startColor) + fraction * (Color.alpha(endColor) - Color.alpha(startColor)));
            int red = (int) (Color.red(startColor) + fraction * (Color.red(endColor) - Color.red(startColor)));
            int green = (int) (Color.green(startColor) + fraction * (Color.green(endColor) - Color.green(startColor)));
            int blue = (int) (Color.blue(startColor) + fraction * (Color.blue(endColor) - Color.blue(startColor)));
            return Color.argb(alpha, red, green, blue);
        }
    };


    public void setTranslucentStatus(int statusBarHeight) {
        mStatusBarHeight = statusBarHeight;
    }


    @Override
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        super.setBackgroundColor(color);
    }

    //不写导致放大之后图片显示在左上角，有人在外部重写他
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        MAX_TRANSLATE_X = mWidth / 2;
        MAX_TRANSLATE_Y = mHeight / 2;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animImageTransform != null) animImageTransform.cancel();
        animImageTransform = null;
        if (animBackground != null) animBackground.cancel();
        animBackground = null;
    }

    /**
     * 当界面处于图片查看状态需要在Activity中的{@link Activity#onBackPressed()}
     * 将事件传递给ImageWatcher优先处理<br/>
     * 1、当处于收尾动画执行状态时，消费返回键事件<br/>
     * 2、当图片处于放大状态时，执行图片缩放到原始大小的动画，消费返回键事件<br/>
     * 3、当图片处于原始状态时，退出图片查看，消费返回键事件<br/>
     * 4、其他情况，ImageWatcher并没有展示图片
     */
    public boolean handleBackPressed() {
        return isInTransformAnimation || (iSource != null && getVisibility() == View.VISIBLE && onSingleTapConfirmed());
    }

    /**
     * 将指定的ImageView形态(尺寸大小，缩放，旋转，平移，透明度)逐步转化到期望值
     */
    private void animSourceViewStateTransform(ImageView view, final ViewState vsResult) {
        if (view == null) return;
        if (animImageTransform != null) animImageTransform.cancel();

        animImageTransform = ViewState.restoreByAnim(view, vsResult.mTag).addListener(mAnimTransitionStateListener).create();

        if (animImageTransform != null) {
            if (vsResult.mTag == ViewState.STATE_ORIGIN) {
                animImageTransform.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // 如果是退出查看操作，动画执行完后，原始被点击的ImageView恢复可见
                        if (iOrigin != null) iOrigin.setVisibility(View.VISIBLE);

                        setVisibility(View.GONE);
                    }
                });
            }
            animImageTransform.start();
        }
    }

    /**
     * 执行ImageWatcher自身的背景色渐变至期望值[colorResult]的动画
     */
    private void animBackgroundTransform(final int colorResult) {
        if (colorResult == mBackgroundColor) return;
        if (animBackground != null) animBackground.cancel();
        final int mCurrentBackgroundColor = mBackgroundColor;
        animBackground = ValueAnimator.ofFloat(0, 1).setDuration(300);
        animBackground.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float p = (float) animation.getAnimatedValue();
                setBackgroundColor(mColorEvaluator.evaluate(p, mCurrentBackgroundColor, colorResult));
            }
        });
        animBackground.start();
    }


    public interface OnPictureLongPressListener {

        void onPictureLongPress(ImageView v, String url, int pos);
    }

    public void setOnPictureButton(RelativeLayout frameLayout) {
        this.frameLayout = frameLayout;
    }


    public void setOnPictureLongPressListener(OnPictureLongPressListener listener) {
        mPictureLongPressListener = listener;
    }

}
