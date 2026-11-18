package com.afsdsa.mouse;

import android.view.MotionEvent;

interface IInputInjector {
    boolean injectMotion(in MotionEvent event, int buttonState, int actionButton);
    boolean injectKey(int action, int keyCode, int metaState, long eventTime);
}
