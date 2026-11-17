package com.afsdsa.mouse

import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val actions = listOf(
        "変更なし", "左クリック", "右クリック", "中央クリック",
        "戻る", "進む", "コピー", "貼り付け"
    )
    private val buttons = listOf("左ボタン", "右ボタン", "中央ボタン", "サイドボタン1", "サイドボタン2")
    private val keys = listOf("left", "right", "middle", "back", "forward")
    private val spinners = mutableListOf<Spinner>()
    private lateinit var eventLog: TextView
    private lateinit var rightClickTestArea: TextView
    private val logLines = ArrayDeque<String>()
    private var backButtonWasDown = false
    private var previousButtonState = 0
    private lateinit var shizukuStatus: TextView
    private val shizukuPermissionCode = 1001
    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == shizukuPermissionCode) {
                updateShizukuStatus()
                Toast.makeText(
                    this,
                    if (grantResult == PackageManager.PERMISSION_GRANTED) "Shizuku権限を許可しました"
                    else "Shizuku権限が許可されませんでした",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("mapping", MODE_PRIVATE)
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
        }

        root.addView(TextView(this).apply {
            text = "マウスボタン変更"
            textSize = 26f
        })
        root.addView(TextView(this).apply {
            text = "Shizuku接続"
            textSize = 19f
            setPadding(0, 18, 0, 4)
        })
        shizukuStatus = TextView(this).apply {
            textSize = 14f
            setPadding(0, 4, 0, 8)
        }
        root.addView(shizukuStatus)
        root.addView(Button(this).apply {
            text = "Shizuku権限を確認・許可"
            setOnClickListener { requestShizukuPermission() }
        })
        root.addView(TextView(this).apply {
            text = "Shizukuが未起動の場合は、Shizukuアプリでワイヤレスデバッグから起動してください。"
            textSize = 13f
            setPadding(0, 4, 0, 12)
        })

        root.addView(TextView(this).apply {
            text = "ボタン割り当て"
            textSize = 18f
            setPadding(0, 24, 0, 8)
        })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, actions).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        buttons.forEachIndexed { index, label ->
            root.addView(TextView(this).apply {
                text = label
                textSize = 16f
                setPadding(0, 12, 0, 4)
            })
            val spinner = Spinner(this).apply {
                this.adapter = adapter
                setSelection(prefs.getInt(keys[index], 0).coerceIn(actions.indices))
            }
            spinners.add(spinner)
            root.addView(spinner)
        }

        root.addView(Button(this).apply {
            text = "設定を保存"
            setOnClickListener {
                prefs.edit().apply {
                    spinners.forEachIndexed { index, spinner ->
                        putInt(keys[index], spinner.selectedItemPosition)
                    }
                }.apply()
                Toast.makeText(this@MainActivity, "設定を保存しました", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(TextView(this).apply {
            text = "右ボタン変換テスト"
            textSize = 20f
            setPadding(0, 24, 0, 8)
        })
        rightClickTestArea = TextView(this).apply {
            text = "ここにマウスポインターを置いて右ボタンを押してください。\nbuttonStateに「戻る」が含まれる入力を右クリックに変換します。"
            textSize = 15f
            setPadding(18, 28, 18, 28)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            isClickable = true
            setOnContextClickListener {
                text = "右クリックを検出しました！\n「戻る」ボタン入力を、このアプリ内で右クリックとして扱いました。"
                Toast.makeText(this@MainActivity, "右クリックに変換しました（このアプリ内のみ）", Toast.LENGTH_SHORT).show()
                true
            }
        }
        root.addView(rightClickTestArea, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 130
        ))

        root.addView(TextView(this).apply {
            text = "マウス入力ログ"
            textSize = 20f
            setPadding(0, 24, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = "マウスを接続してボタンを押すと、検出したイベントを下に表示します。"
            textSize = 14f
        })
        eventLog = TextView(this).apply {
            text = "まだ入力は検出されていません。"
            textSize = 13f
            setTextIsSelectable(true)
            setPadding(12, 12, 12, 12)
        }
        root.addView(ScrollView(this).apply {
            addView(eventLog)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 220
            ).apply { topMargin = 8 }
        })

        root.addView(TextView(this).apply {
            text = """
                重要な制限

                保存した割り当ては、テスト領域にポインターを置いた状態での動作確認に使います。Android全体や他のアプリの入力を変更するものではありません。

                注意: この端末では物理右ボタンとサイドボタン2が同じBUTTON_BACKとして届くため、Android側で区別できません。現在の検出方式では両方に同じ「右ボタン」設定が適用されます。

                このマウスでは、右ボタンがACTION_BUTTON_PRESSではなく、buttonState=8を含むホバー移動として通知されることを確認しました。そのためbuttonStateの変化を監視して変換します。
            """.trimIndent()
            textSize = 14f
            setPadding(0, 20, 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(root) })
        updateShizukuStatus()
    }

    override fun onResume() {
        super.onResume()
        updateShizukuStatus()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        super.onDestroy()
    }

    private fun requestShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizukuが起動していません。Shizukuアプリで起動してください。", Toast.LENGTH_LONG).show()
            updateShizukuStatus()
            return
        }
        if (Shizuku.isPreV11()) {
            Toast.makeText(this, "Shizuku v11以降が必要です。", Toast.LENGTH_LONG).show()
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Shizuku権限は許可済みです。", Toast.LENGTH_SHORT).show()
        } else {
            Shizuku.requestPermission(shizukuPermissionCode)
        }
        updateShizukuStatus()
    }

    private fun updateShizukuStatus() {
        if (!::shizukuStatus.isInitialized) return
        val running = Shizuku.pingBinder()
        if (!running) {
            shizukuStatus.text = "状態: 未起動"
            return
        }
        val granted = !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        val uid = runCatching { Shizuku.getUid() }.getOrDefault(-1)
        val mode = when (uid) {
            0 -> "root"
            2000 -> "ADB / shell"
            else -> "UID $uid"
        }
        shizukuStatus.text = "状態: 起動中 / 権限: ${if (granted) "許可済み" else "未許可"} / 実行モード: $mode"
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!isMouseEvent(event)) return super.dispatchGenericMotionEvent(event)

        logMouseEvent(event, "Generic")
        val inTestArea = isInsideRightClickTestArea(event)
        val current = event.buttonState
        val changed = current xor previousButtonState
        previousButtonState = current

        if (inTestArea && changed != 0) {
            val prefs = getSharedPreferences("mapping", MODE_PRIVATE)
            val mappings = listOf(
                MotionEvent.BUTTON_PRIMARY to "left",
                MotionEvent.BUTTON_SECONDARY to "right",
                MotionEvent.BUTTON_TERTIARY to "middle",
                MotionEvent.BUTTON_FORWARD to "back",
                MotionEvent.BUTTON_BACK to "right"
            )
            var handled = false
            mappings.forEach { (mask, key) ->
                val pressedNow = current and mask != 0
                val wasPressed = (current xor changed) and mask != 0
                if (changed and mask != 0 && pressedNow && !wasPressed) {
                    val actionIndex = prefs.getInt(key, 0).coerceIn(actions.indices)
                    val selectedAction = actions[actionIndex]
                    applyMappedAction(selectedAction)
                    addLog("割り当て実行: $key → $selectedAction")
                    handled = selectedAction != "変更なし" || handled
                }
            }
            if (handled) return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun applyMappedAction(action: String) {
        when (action) {
            "左クリック", "中央クリック" -> rightClickTestArea.performClick()
            "右クリック" -> rightClickTestArea.performContextClick()
            "戻る" -> Toast.makeText(this, "戻る操作（テスト領域内）", Toast.LENGTH_SHORT).show()
            "進む" -> Toast.makeText(this, "進む操作（テスト領域内）", Toast.LENGTH_SHORT).show()
            "コピー" -> Toast.makeText(this, "コピー操作（テスト領域内）", Toast.LENGTH_SHORT).show()
            "貼り付け" -> Toast.makeText(this, "貼り付け操作（テスト領域内）", Toast.LENGTH_SHORT).show()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (isMouseEvent(event)) logMouseEvent(event, "Touch")
        return super.dispatchTouchEvent(event)
    }

    private fun isInsideRightClickTestArea(event: MotionEvent): Boolean {
        val location = IntArray(2)
        rightClickTestArea.getLocationOnScreen(location)
        val x = event.rawX
        val y = event.rawY
        return x >= location[0] && x <= location[0] + rightClickTestArea.width &&
            y >= location[1] && y <= location[1] + rightClickTestArea.height
    }

    private fun isMouseEvent(event: MotionEvent): Boolean {
        return event.isFromSource(InputDevice.SOURCE_MOUSE) ||
            event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)
    }

    private fun logMouseEvent(event: MotionEvent, route: String) {
        val action = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> "押下"
            MotionEvent.ACTION_UP -> "解放"
            MotionEvent.ACTION_MOVE -> "移動"
            MotionEvent.ACTION_HOVER_MOVE -> "ホバー移動"
            MotionEvent.ACTION_HOVER_ENTER -> "ホバー開始"
            MotionEvent.ACTION_HOVER_EXIT -> "ホバー終了"
            MotionEvent.ACTION_BUTTON_PRESS -> "ボタン押下"
            MotionEvent.ACTION_BUTTON_RELEASE -> "ボタン解放"
            MotionEvent.ACTION_SCROLL -> "スクロール"
            else -> "action=${event.actionMasked}"
        }
        val pressed = buildList {
            if (event.buttonState and MotionEvent.BUTTON_PRIMARY != 0) add("左")
            if (event.buttonState and MotionEvent.BUTTON_SECONDARY != 0) add("右")
            if (event.buttonState and MotionEvent.BUTTON_TERTIARY != 0) add("中央")
            if (event.buttonState and MotionEvent.BUTTON_BACK != 0) add("戻る")
            if (event.buttonState and MotionEvent.BUTTON_FORWARD != 0) add("進む")
        }.ifEmpty { listOf("なし") }.joinToString("+")

        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.JAPAN).format(Date())
        addLog("$time [$route] $action / ボタン=$pressed / buttonState=${event.buttonState} / x=${event.x.toInt()} y=${event.y.toInt()}")
    }

    private fun addLog(line: String) {
        logLines.addFirst(line)
        while (logLines.size > 30) logLines.removeLast()
        runOnUiThread {
            if (::eventLog.isInitialized) eventLog.text = logLines.joinToString("\n")
        }
    }
}
