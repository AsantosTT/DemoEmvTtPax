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
 * 20210516 	        xieYb                  Create
 * ===========================================================================================
 *
 */
package com.techun.demoemvttpax.utils.keyboard.text;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.tecnologiatransaccional.ttpaxsdk.App;

/**
 * The type Editor action listener.
 */
public abstract class EditorActionListener implements TextView.OnEditorActionListener {


    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                App.mBaseApplication.runOnUiThread(this::onKeyOk);
                return true;
            }
        } else if (actionId == EditorInfo.IME_ACTION_DONE) {
            App.mBaseApplication.runOnUiThread(this::onKeyOk);
            return true;
        } else if (actionId == EditorInfo.IME_ACTION_NONE) {
            App.mBaseApplication.runOnUiThread(this::onKeyCancel);
            return true;
        }
        return false;
    }

    /**
     * On key ok.
     */
    protected abstract void onKeyOk();

    /**
     * On key cancel.
     */
    protected abstract void onKeyCancel();
}
