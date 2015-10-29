package com.mobilebits.toggleimagebutton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.mobilebits.toggleimagebutton.utils.Gusterpolator;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


/**
 * Created by iluretar on 10/27/2015.
 */
public class ToggleImageButton extends ImageButton {


    public interface OnStateChangeListener {
        void stateChanged(View view, int state);
    }

    public static final int ANIM_DIRECTION_VERTICAL = 0;
    public static final int ANIM_DIRECTION_HORIZONTAL = 1;

    private static final int ANIM_DURATION_MS = 250;
    private static final int UNSET = -1;

    private OnStateChangeListener mOnStateChangeListener;
    private int mState = UNSET;
    private int[] mImageIds;
    private int mLevel;
    private boolean mClickEnabled = true;
    private int mParentSize;
    private int mAnimDirection;
    private Matrix mMatrix = new Matrix();
    private ValueAnimator mAnimator;

    public ToggleImageButton(Context context) {
        super(context);
        init();
    }

    public ToggleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        parseAttributes(context, attrs);
        setState(0);
    }

    public ToggleImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        parseAttributes(context, attrs);
        setState(0);
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        mOnStateChangeListener = onStateChangeListener;
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        setState(state, true);
    }

    public void setState(final int state, final boolean callListener) {
        setStateAnimatedInternal(state, callListener);
    }

    private void setStateAnimatedInternal(final int state, final boolean callListener) {
        if (mState == state || mState == UNSET) {
            setStateInternal(state, callListener);
            return;
        }

        if (mImageIds == null) {
            return;
        }

        Observable.just(combine(mState, state))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Bitmap>() {
                    @Override
                    public void call(Bitmap bitmap) {
                        if (bitmap == null) {
                            setStateInternal(state, callListener);
                        } else {
                            setImageBitmap(bitmap);

                            int offset;
                            if (mAnimDirection == ANIM_DIRECTION_VERTICAL) {
                                offset = (mParentSize + getHeight()) / 2;
                            } else if (mAnimDirection == ANIM_DIRECTION_HORIZONTAL) {
                                offset = (mParentSize + getWidth()) / 2;
                            } else {
                                return;
                            }

                            mAnimator.setFloatValues(-offset, 0.0f);
                            AnimatorSet s = new AnimatorSet();
                            s.play(mAnimator);
                            s.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    setClickEnabled(false);
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    setStateInternal(state, callListener);
                                    setClickEnabled(true);
                                }
                            });
                            s.start();
                        }
                    }
                });

    }

    public void setClickEnabled(boolean enabled) {
        mClickEnabled = enabled;
    }

    private void setStateInternal(int state, boolean callListener) {
        mState = state;
        if (mImageIds != null) {
            setImageByState(mState);
        }
        super.setImageLevel(mLevel);

        if (callListener && mOnStateChangeListener != null) {
            mOnStateChangeListener.stateChanged(ToggleImageButton.this, getState());
        }
    }

    private void nextState() {
        int state = mState + 1;
        if (state >= mImageIds.length) {
            state = 0;
        }
        setState(state);
    }

    protected void init() {
        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickEnabled) {
                    nextState();
                }
            }
        });
        setScaleType(ImageView.ScaleType.MATRIX);

        mAnimator = ValueAnimator.ofFloat(0.0f, 0.0f);
        mAnimator.setDuration(ANIM_DURATION_MS);
        mAnimator.setInterpolator(Gusterpolator.INSTANCE);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mMatrix.reset();
                if (mAnimDirection == ANIM_DIRECTION_VERTICAL) {
                    mMatrix.setTranslate(0.0f, (Float) animation.getAnimatedValue());
                } else if (mAnimDirection == ANIM_DIRECTION_HORIZONTAL) {
                    mMatrix.setTranslate((Float) animation.getAnimatedValue(), 0.0f);
                }

                setImageMatrix(mMatrix);
                invalidate();
            }
        });
    }

    private void parseAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ToggleImageButton,
                0, 0);
        int imageIds = a.getResourceId(R.styleable.ToggleImageButton_imageIds, 0);
        if (imageIds > 0) {
            overrideImageIds(imageIds);
        }
        a.recycle();
    }

    public void overrideImageIds(int resId) {
        TypedArray ids = null;
        try {
            ids = getResources().obtainTypedArray(resId);
            mImageIds = new int[ids.length()];
            for (int i = 0; i < ids.length(); i++) {
                mImageIds[i] = ids.getResourceId(i, 0);
            }
        } finally {
            if (ids != null) {
                ids.recycle();
            }
        }

        if (mState >= 0 && mState < mImageIds.length) {
            setImageByState(mState);
        }
    }

    public void setParentSize(int s) {
        mParentSize = s;
    }

    public void setAnimDirection(int d) {
        mAnimDirection = d;
    }

    @Override
    public void setImageLevel(int level) {
        super.setImageLevel(level);
        mLevel = level;
    }

    private void setImageByState(int state) {
        if (mImageIds != null) {
            setImageResource(mImageIds[state]);
        }
        super.setImageLevel(mLevel);
    }

    private Bitmap combine(int oldState, int newState) {
        // in some cases, a new set of image Ids are set via overrideImageIds()
        // and oldState overruns the array.
        // check here for that.
        if (oldState >= mImageIds.length) {
            return null;
        }

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return null;
        }

        int[] enabledState = new int[] {android.R.attr.state_enabled};

        // new state
        Drawable newDrawable = getResources().getDrawable(mImageIds[newState]).mutate();
        newDrawable.setState(enabledState);

        // old state
        Drawable oldDrawable = getResources().getDrawable(mImageIds[oldState]).mutate();
        oldDrawable.setState(enabledState);

        // combine 'em
        Bitmap bitmap = null;
        if (mAnimDirection == ANIM_DIRECTION_VERTICAL) {
            int bitmapHeight = (height*2) + ((mParentSize - height)/2);
            int oldBitmapOffset = height + ((mParentSize - height)/2);
            bitmap = Bitmap.createBitmap(width, bitmapHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            newDrawable.setBounds(0, 0, newDrawable.getIntrinsicWidth(), newDrawable.getIntrinsicHeight());
            oldDrawable.setBounds(0, oldBitmapOffset, oldDrawable.getIntrinsicWidth(), oldDrawable.getIntrinsicHeight()+oldBitmapOffset);
            newDrawable.draw(canvas);
            oldDrawable.draw(canvas);
        } else if (mAnimDirection == ANIM_DIRECTION_HORIZONTAL) {
            int bitmapWidth = (width*2) + ((mParentSize - width)/2);
            int oldBitmapOffset = width + ((mParentSize - width)/2);
            bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            newDrawable.setBounds(0, 0, newDrawable.getIntrinsicWidth(), newDrawable.getIntrinsicHeight());
            oldDrawable.setBounds(oldBitmapOffset, 0, oldDrawable.getIntrinsicWidth()+oldBitmapOffset, oldDrawable.getIntrinsicHeight());
            newDrawable.draw(canvas);
            oldDrawable.draw(canvas);
        }

        return bitmap;
    }

}
