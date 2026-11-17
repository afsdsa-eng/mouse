package com.afsdsa.mouse

import android.app.Activity
import android.os.Bundle
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
    private val logLines = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("mapping", MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
        }

        root.addView(TextView(this).apply {
            text = "マウスボタン変更"
            textSize = 26f
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
            text = "マウス入力テスト"
            textSize = 20f
            setPadding(0, 24, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = "マウスを接続し、このアプリの画面上で各ボタンを押してください。検出したイベントを下に表示します。"
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
                現在の制限

                このテストで確認できるのは、このアプリが前面にある間にAndroidから届いた入力だけです。別のアプリの操作を監視したり、クリックを置き換えたりする機能ではありません。

                rootなしの通常アプリでは、Android全体のマウス入力を自由に横取り・置換できる公開APIはありません。
            """.trimIndent()
            textSize = 14f
            setPadding(0, 20, 0, 0)
        })

        setContentView(root)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (isMouseEvent(event)) logMouseEvent(event, "Generic")
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (isMouseEvent(event)) logMouseEvent(event, "Touch")
        return super.dispatchTouchEvent(event)
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
        val line = "$time [$route] $action / ボタン=$pressed / buttonState=${event.buttonState} / x=${event.x.toInt()} y=${event.y.toInt()}"
        logLines.addFirst(line)
        while (logLines.size > 30) logLines.removeLast()
        runOnUiThread {
            eventLog.text = logLines.joinToString("\n")
        }
    }
}
