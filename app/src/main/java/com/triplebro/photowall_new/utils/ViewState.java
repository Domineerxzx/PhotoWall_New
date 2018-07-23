package com.triplebro.photowall_new.utils;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.View;

import com.triplebro.photowall_new.R;


public class ViewState {
    public static final int STATE_ORIGIN = R.id.state_origin; // 缩略图初始
    public static final int STATE_THUMB = R.id.state_thumb; // 缩略图居中
    public static final int STATE_DEFAULT = R.id.state_default; // 高清图初始
    public static final int STATE_CURRENT = R.id.state_current; // 当前状态
    public static final int STATE_TEMP = R.id.state_temp; // 临时目标
    public static final int STATE_DRAG = R.id.state_touch_drag; // 高清图拖拽起点
    public static final int STATE_TOUCH_DOWN = R.id.state_touch_down; // 高清图按下起点
    public static final int STATE_TOUCH_SCALE_ROTATE = R.id.state_touch_scale_rotate; // 高清图缩放旋转起点

    public int mTag;

    public int width;
    public int height;
    public float translationX;
    public float translationY;
    public float scaleX;
    public float scaleY;
    public float rotation;
    public float alpha;//变形

    private ViewState(int tag) {
        mTag = tag;
    }

    public static ViewState write(View view, int tag) {
        if (view == null) return null;

        ViewState vs = read(view, tag);
        if (vs == null) view.setTag(tag, vs = new ViewState(tag));

        vs.width = view.getWidth();
        vs.height = view.getHeight();
        vs.translationX = view.getTranslationX();
        vs.translationY = view.getTranslationY();
        vs.scaleX = view.getScaleX();
        vs.scaleY = view.getScaleY();
        vs.rotation = view.getRotation();
        vs.alpha = view.getAlpha();
        return vs;
    }

    public static ViewState read(View view, int tag) {
        if (view == null) return null;
        return view.getTag(tag) != null ? (ViewState) view.getTag(tag) : null;
    }

    public static ViewState copy(ViewState mir, int tag) {
        ViewState vs = new ViewState(tag);
        vs.width = mir.width;
        vs.height = mir.height;
        vs.translationX = mir.translationX;
        vs.translationY = mir.translationY;
        vs.scaleX = mir.scaleX;
        vs.scaleY = mir.scaleY;
        vs.rotation = mir.rotation;
        vs.alpha = mir.alpha;
        return vs;
    }

    public static void restore(View view, int tag) {
        ViewState viewState = read(view, tag);
        if (viewState != null) {
            view.setTranslationX(viewState.translationX);
            view.setTranslationY(viewState.translationY);
            view.setScaleX(viewState.scaleX);
            view.setScaleY(viewState.scaleY);
            view.setRotation(viewState.rotation);
            view.setAlpha(viewState.alpha);
            if (view.getLayoutParams().width != viewState.width || view.getLayoutParams().height != viewState.height) {
                view.getLayoutParams().width = viewState.width;
                view.getLayoutParams().height = viewState.height;
                view.requestLayout();
            }
        }
    }

    public static ValueAnimatorBuilder restoreByAnim(final View view, int tag) {
        ValueAnimator animator = null;
        if (view != null) {
            final ViewState vsCurrent = ViewState.write(view, ViewState.STATE_CURRENT);
            if (vsCurrent.width == 0 && vsCurrent.height == 0) {
                ViewState vsOrigin = ViewState.read(view, ViewState.STATE_ORIGIN);
                if (vsOrigin != null) vsCurrent.width(vsOrigin.width).height(vsOrigin.height);
            }
            final ViewState vsResult = read(view, tag);
            if (vsResult != null) {
                animator = ValueAnimator.ofFloat(0, 1).setDuration(300);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float p = (float) animation.getAnimatedValue();
                        view.setTranslationX(vsCurrent.translationX + (vsResult.translationX - vsCurrent.translationX) * p);
                        view.setTranslationY(vsCurrent.translationY + (vsResult.translationY - vsCurrent.translationY) * p);
                        view.setScaleX(vsCurrent.scaleX + (vsResult.scaleX - vsCurrent.scaleX) * p);
                        view.setScaleY(vsCurrent.scaleY + (vsResult.scaleY - vsCurrent.scaleY) * p);
                        view.setRotation((vsCurrent.rotation + (vsResult.rotation - vsCurrent.rotation) * p) % 360);
                        view.setAlpha((vsCurrent.alpha + (vsResult.alpha - vsCurrent.alpha) * p));
                        if (vsCurrent.width != vsResult.width && vsCurrent.height != vsResult.height
                                && vsResult.width != 0 && vsResult.height != 0) {
                            view.getLayoutParams().width = (int) (vsCurrent.width + (vsResult.width - vsCurrent.width) * p);
                            view.getLayoutParams().height = (int) (vsCurrent.height + (vsResult.height - vsCurrent.height) * p);
                            view.requestLayout();
                        }
                    }
                });
            }
        }
        return new ValueAnimatorBuilder(animator);
    }

    public static class ValueAnimatorBuilder {
        ValueAnimator mAnimator;

        ValueAnimatorBuilder(ValueAnimator animator) {
            mAnimator = animator;
        }

        public ValueAnimatorBuilder addListener(Animator.AnimatorListener listener) {
            if (mAnimator != null) mAnimator.addListener(listener);
            return this;
        }

        public ValueAnimator create() {
            return mAnimator;
        }
    }

    public ViewState scaleX(float scaleX) {
        this.scaleX = scaleX;
        return this;
    }

    public ViewState scaleXBy(float value) {
        this.scaleX *= value;
        return this;
    }

    public ViewState scaleY(float scaleY) {
        this.scaleY = scaleY;
        return this;
    }

    public ViewState scaleYBy(float value) {
        this.scaleY *= value;
        return this;
    }

    public ViewState alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public ViewState width(int width) {
        this.width = width;
        return this;
    }

    public ViewState height(int height) {
        this.height = height;
        return this;
    }

    public ViewState translationX(float translationX) {
        this.translationX = translationX;
        return this;
    }

    public ViewState translationY(float translationY) {
        this.translationY = translationY;
        return this;
    }
}
