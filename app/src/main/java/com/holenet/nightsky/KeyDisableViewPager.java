package com.holenet.nightsky;

import android.content.Context;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class KeyDisableViewPager extends ViewPager {
    public KeyDisableViewPager(Context context) {
        super(context);
    }

    public KeyDisableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean executeKeyEvent(KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    handled = true;//arrowScroll(FOCUS_LEFT);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    handled = true;//arrowScroll(FOCUS_RIGHT);
                    break;
                case KeyEvent.KEYCODE_TAB:
                    if (KeyEventCompat.hasNoModifiers(event)) {
                        handled = true;//arrowScroll(FOCUS_FORWARD);
                    } else if (KeyEventCompat.hasModifiers(event, KeyEvent.META_SHIFT_ON)) {
                        handled = true;//arrowScroll(FOCUS_BACKWARD);
                    }
                    break;
            }
        }
        return handled;
    }
}
