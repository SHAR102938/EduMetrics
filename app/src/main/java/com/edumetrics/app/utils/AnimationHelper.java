package com.edumetrics.app.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;

public class AnimationHelper {

    /**
     * Fade in view with slide up effect.
     */
    public static void slideUpFadeIn(View view, int delay) {
        view.setAlpha(0f);
        view.setTranslationY(60f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Scale up animation (for success icons, etc.)
     */
    public static void scaleUp(View view) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setAlpha(0f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    /**
     * Animate progress bar fill.
     */
    public static void animateProgressBar(ProgressBar progressBar, int targetProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress);
        animator.setDuration(1000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    /**
     * Pulse animation for attention.
     */
    public static void pulse(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(300);
        set.start();
    }

    /**
     * Stagger animation for RecyclerView items.
     */
    public static void staggeredSlideUp(View view, int position) {
        slideUpFadeIn(view, position * 80);
    }

    /**
     * Simple fade in.
     */
    public static void fadeIn(View view, int duration) {
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .start();
    }

    /**
     * Button press feedback.
     */
    public static void buttonPress(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start())
                .start();
    }
}
