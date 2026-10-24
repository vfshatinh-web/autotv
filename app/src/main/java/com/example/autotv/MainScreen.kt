package com.example.autotv

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.graphics.Rect
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Dùng bề mặt (Surface) của template điều hướng để vẽ nội dung tuỳ ý:
 * VirtualDisplay -> Presentation chứa WebView / ExoPlayer.
 */
class MainScreen(ctx: CarContext) : Screen(ctx), SurfaceCallback {
    private var vd: VirtualDisplay? = null
    private var pres: ContentPresentation? = null

    init {
        ctx.getCarService(AppManager::class.java).setSurfaceCallback(this)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = release()
        })
    }

    override fun onGetTemplate(): Template =
        NavigationTemplate.Builder()
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(Action.Builder().setTitle("Web").setOnClickListener { pres?.showWeb() }.build())
                    .addAction(Action.Builder().setTitle("TV").setOnClickListener { pres?.showTv() }.build())
                    .build()
            ).build()

    override fun onSurfaceAvailable(c: SurfaceContainer) {
        release()
        val surface = c.surface ?: return
        val dm = carContext.getSystemService(DisplayManager::class.java)
        vd = dm.createVirtualDisplay(
            "AutoTV", c.width, c.height, c.dpi, surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
        )
        pres = ContentPresentation(carContext.applicationContext, vd!!.display).also { it.show() }
    }

    override fun onSurfaceDestroyed(c: SurfaceContainer) = release()
    override fun onVisibleAreaChanged(r: Rect) {}
    override fun onStableAreaChanged(r: Rect) {}

    override fun onClick(x: Float, y: Float) { pres?.click(x, y) }
    override fun onScroll(dx: Float, dy: Float) { pres?.scroll(dx, dy) }

    private fun release() {
        pres?.dismiss(); pres = null
        vd?.release(); vd = null
    }
}
