package com.techun.demoemvttpax.utils.keyboard;


import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.techun.demoemvttpax.R;


/**
 * File description
 *
 * @author yehongbo
 * @date 2022/2/22
 */
public class KeyboardViewFactory {
    private int layoutId = R.layout.commonui_custom_keyboard_view;
    private CustomKeyboardView.OpKeyCallback opKeyCallback;

    private KeyboardViewFactory() {}

    private static class Holder {
        private static final KeyboardViewFactory INSTANCE = new KeyboardViewFactory();
    }

    public static KeyboardViewFactory getInstance() {
        return Holder.INSTANCE;
    }

    public CustomKeyboardView createKeyboardView(Context context) {
        CustomKeyboardView view = (CustomKeyboardView) LayoutInflater.from(context).inflate(layoutId, null);
        if (opKeyCallback != null) {
            view.setOpKeyCallback(opKeyCallback);
        }
        return view;
    }

    public KeyboardViewFactory setLayoutId(@LayoutRes int layoutId) {
        this.layoutId = layoutId;
        return this;
    }

    public KeyboardViewFactory setOpKeyCallback(@NonNull CustomKeyboardView.OpKeyCallback callback) {
        this.opKeyCallback = callback;
        return this;
    }
}
