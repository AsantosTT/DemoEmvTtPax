/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2019-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date	                Author	               Action
 * 20210514 	        xieYb                  Create
 * ===========================================================================================
 *
 */
package com.techun.demoemvttpax.utils.keyboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;


import com.techun.demoemvttpax.R;

import java.util.List;

/**
 * The type Custom keyboard view.
 */
public class CustomKeyboardView extends KeyboardView {
    private OpKeyCallback opKeyCallback = codes -> codes < 0;

    private Drawable mKeyBgDrawable;
    private Drawable mOpKeyBgDrawable;

    private int mKeyTextColor = Color.BLACK;
    private int mOpKeyTextColor = Color.WHITE;

    private final Paint paint = new Paint();

    private final Context mContext;
    private Rect rect;

    /**
     * Instantiates a new Custom keyboard view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public CustomKeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Instantiates a new Custom keyboard view.
     *
     * @param context      the context
     * @param attrs        the attrs
     * @param defStyleAttr the def style attr
     */
    public CustomKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initResources();

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomKeyboardView);
        if (array.hasValue(R.styleable.CustomKeyboardView_keyTextColor)) {
            mKeyTextColor = array.getColor(R.styleable.CustomKeyboardView_keyTextColor,
                    Color.BLACK);
        }
        if (array.hasValue(R.styleable.CustomKeyboardView_opKeyTextColor)) {
            mOpKeyTextColor = array.getColor(R.styleable.CustomKeyboardView_opKeyTextColor,
                    Color.WHITE);
        }
        array.recycle();
    }

    private void initResources() {
        mKeyBgDrawable = ContextCompat.getDrawable(mContext, R.drawable.commonui_btn_bg_dark);
        mOpKeyBgDrawable = ContextCompat.getDrawable(mContext, R.drawable.commonui_btn_bg_light);
        rect = new Rect();
    }

    @Override
    public void onDraw(Canvas canvas) {
        List<Keyboard.Key> keys = getKeyboard().getKeys();
        for (Keyboard.Key key : keys) {
            canvas.save();

            int offsetY = 0;
            if (key.y == 0) {
                offsetY = 1;
            }
            int initDrawY = key.y + offsetY;
            rect.left = key.x;
            rect.top = initDrawY;
            rect.right = key.x + key.width;
            rect.bottom = key.y + key.height;
            canvas.clipRect(rect);
            drawIcon(canvas, key, rect);
            drawText(canvas, key, initDrawY);
            canvas.restore();
        }
    }

    private void drawIcon(Canvas canvas, Keyboard.Key key, Rect rect) {
        Drawable drawable = null;
        if (key.codes != null && key.codes.length != 0) {
            drawable = opKeyCallback.isOpKey(key.codes[0]) ? mOpKeyBgDrawable : mKeyBgDrawable;
        }

        if (drawable != null && null == key.icon) {
            int[] state = key.getCurrentDrawableState();
            drawable.setState(state);
            drawable.setBounds(rect);
            drawable.draw(canvas);
        }

        if (key.icon != null) {
            int[] state = key.getCurrentDrawableState();
            key.icon.setState(state);
            key.icon.setBounds(rect);
            key.icon.draw(canvas);
        }
    }

    private void drawText(Canvas canvas, Keyboard.Key key, int initDrawY) {
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(mContext.getResources().getDimension(R.dimen.commonui_font_size_key));
        paint.setColor(opKeyCallback.isOpKey(key.codes[0]) ? mOpKeyTextColor : mKeyTextColor);

        if (key.label != null) {
            canvas.drawText(
                    key.label.toString(),
                    key.x + (key.width / 2),
                    initDrawY + (key.height + paint.getTextSize() - paint.descent()) / 2,
                    paint
            );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (me.getPointerCount() > 1) {
            // Ignore multi-touch
            return true;
        } else {
            if (me.getAction() == MotionEvent.ACTION_UP) {
                performClick();
            }
            return super.onTouchEvent(me);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /**
     * to resolve the problem that width use match_parent doesn't match_parent in Aries8
     * @param widthMeasureSpec CustomKeyboardView's width measureSpec which determined by it's parent and self LayoutParams
     * @param heightMeasureSpec CustomKeyboardView's height measureSpec which determined by it's parent and self LayoutParams
     */
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec,getMeasuredHeight());
    }

    public void setOpKeyCallback(@NonNull OpKeyCallback opKeyCallback) {
        this.opKeyCallback = opKeyCallback;
    }

    public interface OpKeyCallback {
        boolean isOpKey(int codes);
    }
}
