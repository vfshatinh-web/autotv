package com.example.autotv

import android.app.Activity
import android.os.Bundle
import android.widget.*

/** Màn hình trên điện thoại: nhập link M3U và trang web mặc định. */
class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = getSharedPreferences("cfg", MODE_PRIVATE)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 40) }
        val m3u = EditText(this).apply { hint = "Link danh sách IPTV (.m3u)"; setText(p.getString("m3u", "")) }
        val home = EditText(this).apply { hint = "Trang web mặc định"; setText(p.getString("home", "https://www.google.com")) }
        val save = Button(this).apply {
            text = "Lưu"
            setOnClickListener {
                p.edit().putString("m3u", m3u.text.toString().trim())
                    .putString("home", home.text.toString().trim()).apply()
                Toast.makeText(this@MainActivity, "Đã lưu", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(TextView(this).apply { text = "Cấu hình AutoTV (áp dụng khi mở lại trên Android Auto)" })
        root.addView(m3u); root.addView(home); root.addView(save)
        setContentView(root)
    }
}
