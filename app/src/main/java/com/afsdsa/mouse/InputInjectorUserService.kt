package com.afsdsa.mouse

import android.hardware.input.InputManager
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

    override fun injectMotion(
        action: Int, x: Float, y: Float, metaState: Int, buttonState: Int,
        source: Int, downTime: Long, eventTime: Long
    ): Boolean {
        return runCatching {
            val event = MotionEvent.obtain(downTime, eventTime, action, x, y, metaState)
            event.source = source
            event.buttonState = buttonState
            try {
                injectMethod.invoke(inputManagerGlobal, event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC) as Boolean
            } finally {
                event.recycle()
            }
        }.getOrDefault(false)
    }

    override fun injectKey(action: Int, keyCode: Int, metaState: Int, eventTime: Long): Boolean {
        return runCatching {
            val event = KeyEvent(eventTime, eventTime, action, keyCode, 0, metaState)
            try {
                injectMethod.invoke(inputManagerGlobal, event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC) as Boolean
            } finally {
                event.recycle()
            }
        }.getOrDefault(false)
    }
}
