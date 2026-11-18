package com.afsdsa.mouse

import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import java.lang.reflect.Method

/**
 * Runs inside Shizuku's shell/root UserService process.
 * Hidden InputManager APIs are intentionally accessed only in this privileged process.
 */
class InputInjectorUserService : IInputInjector.Stub() {
    private val injectMethod: Method by lazy {
        val cls = Class.forName("android.hardware.input.InputManagerGlobal")
        val instance = cls.getDeclaredMethod("getInstance").invoke(null)
        cls.getDeclaredMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
            .also { it.isAccessible = true }
    }

    private val inputManagerGlobal: Any by lazy {
        val cls = Class.forName("android.hardware.input.InputManagerGlobal")
        cls.getDeclaredMethod("getInstance").invoke(null)
    }

    override fun injectMotion(event: MotionEvent, buttonState: Int, actionButton: Int): Boolean {
        return runCatching {
            val copy = MotionEvent.obtain(event)
            try {
                val setButtonState = MotionEvent::class.java.getDeclaredMethod("setButtonState", Int::class.javaPrimitiveType)
                setButtonState.isAccessible = true
                setButtonState.invoke(copy, buttonState)
                val setActionButton = MotionEvent::class.java.getDeclaredMethod("setActionButton", Int::class.javaPrimitiveType)
                setActionButton.isAccessible = true
                setActionButton.invoke(copy, actionButton)
                injectMethod.invoke(inputManagerGlobal, copy, 0) as Boolean
            } finally {
                copy.recycle()
            }
        }.getOrDefault(false)
    }

    override fun injectKey(action: Int, keyCode: Int, metaState: Int, eventTime: Long): Boolean {
        return runCatching {
            val event = KeyEvent(eventTime, eventTime, action, keyCode, 0, metaState)
            try {
                injectMethod.invoke(inputManagerGlobal, event, 0) as Boolean
            } finally {
                event.recycle()
            }
        }.getOrDefault(false)
    }
}
