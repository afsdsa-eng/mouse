package com.afsdsa.mouse;

interface IInputInjector {
    boolean injectMotion(int action, float x, float y, int metaState, int buttonState, int source, long downTime, long eventTime);
    boolean injectKey(int action, int keyCode, int metaState, long eventTime);
}
