package com.afsdsa.mouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import rikka.shizuku.Shizuku

class MouseAccessibilityService : AccessibilityService() {
    private var injector: IInputInjector? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            injector = IInputInjector.Stub.asInterface(service)
            bound = true
            enableMouseCapture()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            injector = null
            bound = false
            disableMouseCapture()
        }
    }

    private val args by lazy {
        Shizuku.UserServiceArgs(ComponentName(this, InputInjectorUserService::class.java))
            .daemon(false)
            .tag("mouse-input-injector")
            .version(1)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (Build.VERSION.SDK_INT < 34) return
        if (!Shizuku.pingBinder() || Shizuku.isPreV11() ||
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Shizukuを起動し、このアプリに権限を許可してください", Toast.LENGTH_LONG).show()
            return
        }
        runCatching {
            Shizuku.bindUserService(args, connection)
        }.onFailure {
            Toast.makeText(this, "Shizuku UserService起動失敗: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun enableMouseCapture() {
        if (Build.VERSION.SDK_INT < 34 || injector == null) return
        val info = serviceInfo
        info.setMotionEventSources(InputDevice.SOURCE_MOUSE or InputDevice.SOURCE_MOUSE_RELATIVE)
        serviceInfo = info
    }

    private fun disableMouseCapture() {
        if (Build.VERSION.SDK_INT >= 34) {
            runCatching {
                val info = serviceInfo
                info.setMotionEventSources(0)
                serviceInfo = info
            }
        }
    }

    override fun onMotionEvent(event: MotionEvent) {
        if (Build.VERSION.SDK_INT < 34) return
        val bridge = injector ?: return
        val prefs = getSharedPreferences("mapping", MODE_PRIVATE)
        var mappedState = event.buttonState
        var mappedActionButton = event.actionButton
        val mappings = listOf(
            MotionEvent.BUTTON_PRIMARY to "left",
            MotionEvent.BUTTON_SECONDARY to "right",
            MotionEvent.BUTTON_TERTIARY to "middle",
            MotionEvent.BUTTON_BACK to "back",
            MotionEvent.BUTTON_FORWARD to "forward"
        )
        mappings.forEach { (mask, key) ->
            if (event.buttonState and mask != 0 || event.actionButton == mask) {
                val choice = prefs.getInt(key, 0)
                val outputMask = when (choice) {
                    1 -> MotionEvent.BUTTON_PRIMARY
                    2 -> MotionEvent.BUTTON_SECONDARY
                    3 -> MotionEvent.BUTTON_TERTIARY
                    4 -> MotionEvent.BUTTON_BACK
                    5 -> MotionEvent.BUTTON_FORWARD
                    6 -> 0
                    7 -> 0
                    else -> mask
                }
                mappedState = mappedState and mask.inv() or outputMask
                if (event.actionButton == mask) mappedActionButton = outputMask
                if (choice == 6 || choice == 7) {
                    val wasDown = event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS ||
                        event.actionMasked == MotionEvent.ACTION_DOWN
                    val wasUp = event.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE ||
                        event.actionMasked == MotionEvent.ACTION_UP
                    if (wasDown || wasUp) {
                        val keyCode = if (choice == 6) KeyEvent.KEYCODE_C else KeyEvent.KEYCODE_V
                        val meta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
                        if (wasDown) bridge.injectKey(KeyEvent.ACTION_DOWN, keyCode, meta, event.eventTime)
                        if (wasUp) bridge.injectKey(KeyEvent.ACTION_UP, keyCode, meta, event.eventTime)
                    }
                }
            }
        }
        // Every captured event must be forwarded: Android does not deliver captured sources
        // to other apps. The injected copy carries the remapped button state.
        bridge.injectMotion(event, mappedState, mappedActionButton)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        disableMouseCapture()
        if (bound) runCatching { Shizuku.unbindUserService(args, connection, true) }
        bound = false
        injector = null
        super.onDestroy()
    }
}
