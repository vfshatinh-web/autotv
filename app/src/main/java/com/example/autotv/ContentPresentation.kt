package com.example.autotv

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.*
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class ContentPresentation(outer: Context, display: Display) : Presentation(outer, display) {
    private val prefs = outer.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private lateinit var bar: LinearLayout
    private lateinit var web: WebView
    private lateinit var tvPane: LinearLayout
    private lateinit var list: ListView
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private var channels: List<Channel> = emptyList()
    private var fullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = context
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }

        bar = LinearLayout(ctx)
        fun btn(t: String, f: () -> Unit) {
            bar.addView(Button(ctx).apply { text = t; setOnClickListener { f() } },
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        }
        btn("Web") { showWeb() }
        btn("TV") { showTv() }
        btn("◀") { if (web.canGoBack()) web.goBack() }
        btn("Home") { web.loadUrl(home()); showWeb() }
        btn("Full") { toggleFull() }
        root.addView(bar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        web = WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = WebViewClient()
            loadUrl(home())
        }
        root.addView(web, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        tvPane = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; visibility = View.GONE }
        list = ListView(ctx)
        playerView = PlayerView(ctx)
        tvPane.addView(list, LinearLayout.LayoutParams(0, MATCH_PARENT, 1f))
        tvPane.addView(playerView, LinearLayout.LayoutParams(0, MATCH_PARENT, 2f))
        root.addView(tvPane, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        setContentView(root)
    }

    private fun home() = prefs.getString("home", null)?.takeIf { it.isNotBlank() } ?: "https://www.google.com"

    fun showWeb() { web.visibility = View.VISIBLE; tvPane.visibility = View.GONE; player?.pause() }

    fun showTv() {
        web.visibility = View.GONE; tvPane.visibility = View.VISIBLE
        if (player == null) player = ExoPlayer.Builder(context).build().also { playerView.player = it }
        if (channels.isEmpty()) loadChannels() else player?.play()
    }

    private fun loadChannels() {
        val src = prefs.getString("m3u", "") ?: ""
        Thread {
            val result = try { M3uParser.load(src) } catch (e: Exception) { emptyList() }
            main.post {
                channels = result
                val names = if (result.isEmpty()) listOf("Chưa có kênh - nhập link M3U trên điện thoại")
                            else result.map { it.name }
                list.adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, names) {
                    override fun getView(pos: Int, v: View?, p: ViewGroup): View =
                        (super.getView(pos, v, p) as TextView).apply { setTextColor(Color.WHITE); textSize = 20f }
                }
                list.setOnItemClickListener { _, _, pos, _ ->
                    channels.getOrNull(pos)?.let {
                        player?.setMediaItem(MediaItem.fromUri(it.url)); player?.prepare(); player?.play()
                    }
                }
            }
        }.start()
    }

    private fun toggleFull() {
        fullscreen = !fullscreen
        val v = if (fullscreen) View.GONE else View.VISIBLE
        bar.visibility = v; list.visibility = v
    }

    /** Giả lập chạm từ màn hình xe vào view bên trong Presentation. */
    fun click(x: Float, y: Float) {
        val t = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(t, t + 60, MotionEvent.ACTION_UP, x, y, 0)
        window?.decorView?.dispatchTouchEvent(down)
        window?.decorView?.dispatchTouchEvent(up)
        down.recycle(); up.recycle()
    }

    fun scroll(dx: Float, dy: Float) {
        if (web.visibility == View.VISIBLE) web.scrollBy(dx.toInt(), dy.toInt())
        else list.smoothScrollBy(dy.toInt(), 0)
    }

    override fun onStop() {
        player?.release(); player = null
        super.onStop()
    }
}
