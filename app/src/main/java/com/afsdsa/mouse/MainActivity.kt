package com.afsdsa.mouse

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val actions = listOf(
        "変更なし", "左クリック", "右クリック", "中央クリック",
        "戻る", "進む", "コピー", "貼り付け"
    )
    private val buttons = listOf("左ボタン", "右ボタン", "中央ボタン", "サイドボタン1", "サイドボタン2")
    private val keys = listOf("left", "right", "middle", "back", "forward")
    private val spinners = mutableListOf<Spinner>()

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
        }.also { it.setPadding(0, 18, 0, 18) })

        root.addView(TextView(this).apply {
            text = """
                Android全体への適用について

                通常権限のアプリは、他のアプリに届くマウスクリックを横取りして別のクリックへ置き換えることができません。
                このAPKは割り当て設定を保存する初期版です。設定を保存しても、Android全体のマウス操作はまだ変更されません。

                システム全体で実際に変更するには、端末メーカーの機能またはOS側の対応が必要です。rootなしで可能な範囲を今後検証します。
            """.trimIndent()
            textSize = 14f
            setPadding(0, 24, 0, 0)
        })

        setContentView(root)
    }
}
