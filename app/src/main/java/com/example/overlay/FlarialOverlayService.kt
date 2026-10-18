package com.example.overlay

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

object FlarialOverlayStateBridge {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _enabledModules = MutableStateFlow(
        setOf(
            "fps_ping",
            "keystrokes",
            "fullbright",
            "zoom_fov",
            "coordinates",
            "crosshair_mod",
            "entity_esp",
            "time_changer",
            "game_speed_fov"
        )
    )
    val enabledModules: StateFlow<Set<String>> = _enabledModules.asStateFlow()

    var onModuleToggleFromOverlay: ((String) -> Unit)? = null

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun updateEnabledModules(ids: Set<String>) {
        _enabledModules.value = ids
    }
}

class FlarialOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var rootContainer: LinearLayout? = null
    private var expandedPanel: LinearLayout? = null
    private var isMenuExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        FlarialOverlayStateBridge.setServiceRunning(true)
        createFloatingOverlayWindow()
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun createFloatingOverlayWindow() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16)
            y = dpToPx(96)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. Draggable Floating Pill Header ("FLARIAL v1.26+ • 120 FPS")
        val pillView = TextView(this).apply {
            text = "⚡ FLARIAL v1.26+  •  120 FPS"
            setTextColor(Color.parseColor("#00F0FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
            setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(Color.parseColor("#E60B0F1D"))
                setStroke(dpToPx(1), Color.parseColor("#00F0FF"))
            }
        }

        // 2. Expandable ClickGUI Panel
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14).toFloat()
                setColor(Color.parseColor("#F20E1322"))
                setStroke(dpToPx(1), Color.parseColor("#8B5CF6"))
            }
            val lp = LinearLayout.LayoutParams(dpToPx(236), LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dpToPx(6)
            layoutParams = lp
        }

        val headerTitle = TextView(this).apply {
            text = "FLARIAL CLICKGUI (v1.19–v1.26+)"
            setTextColor(Color.parseColor("#F8FAFC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(0, 0, 0, dpToPx(6))
        }
        panel.addView(headerTitle)

        val moduleDefinitions = listOf(
            "fps_ping" to "FPS & Ping Counter",
            "keystrokes" to "Keystrokes (WASD)",
            "fullbright" to "Fullbright 1000%",
            "zoom_fov" to "Optic Zoom & FOV",
            "coordinates" to "XYZ Coordinates",
            "crosshair_mod" to "Custom Crosshair",
            "entity_esp" to "Player ESP & Scale",
            "time_changer" to "Time & Sky Changer",
            "game_speed_fov" to "Game Speed & FOV"
        )

        val moduleRows = mutableMapOf<String, TextView>()

        fun refreshModuleRows() {
            val activeSet = FlarialOverlayStateBridge.enabledModules.value
            moduleDefinitions.forEach { (id, label) ->
                val row = moduleRows[id] ?: return@forEach
                val isOn = id in activeSet
                row.text = if (isOn) "●  $label  [ON]" else "○  $label  [OFF]"
                row.setTextColor(
                    if (isOn) Color.parseColor("#00F0FF") else Color.parseColor("#94A3B8")
                )
                row.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(8).toFloat()
                    setColor(
                        if (isOn) Color.parseColor("#2900F0FF") else Color.parseColor("#1A1E293B")
                    )
                    setStroke(
                        dpToPx(1),
                        if (isOn) Color.parseColor("#6600F0FF") else Color.parseColor("#33283556")
                    )
                }
            }
        }

        val scrollView = ScrollView(this)
        val listLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        moduleDefinitions.forEach { (id, label) ->
            val btn = TextView(this).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.MONOSPACE
                setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = dpToPx(5)
                layoutParams = lp
                setOnClickListener {
                    FlarialOverlayStateBridge.onModuleToggleFromOverlay?.invoke(id)
                    refreshModuleRows()
                }
            }
            moduleRows[id] = btn
            listLayout.addView(btn)
        }
        scrollView.addView(listLayout)
        panel.addView(scrollView)

        val closeBtn = TextView(this).apply {
            text = "✕ Hide System Overlay"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FF2E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(4))
            setOnClickListener {
                stopSelf()
            }
        }
        panel.addView(closeBtn)

        refreshModuleRows()

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        pillView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    runCatching { wm.updateViewLayout(container, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = abs(event.rawX - initialTouchX)
                    val dy = abs(event.rawY - initialTouchY)
                    if (dx < dpToPx(8) && dy < dpToPx(8)) {
                        isMenuExpanded = !isMenuExpanded
                        refreshModuleRows()
                        panel.visibility = if (isMenuExpanded) View.VISIBLE else View.GONE
                    }
                    true
                }
                else -> false
            }
        }

        container.addView(pillView)
        container.addView(panel)

        rootContainer = container
        expandedPanel = panel

        runCatching {
            wm.addView(container, params)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        FlarialOverlayStateBridge.setServiceRunning(false)
        rootContainer?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        rootContainer = null
    }
}
