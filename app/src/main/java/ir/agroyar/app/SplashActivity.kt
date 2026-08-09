package ir.agroyar.app

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.statusBarColor = Color.rgb(244, 243, 210)
            window.navigationBarColor = Color.rgb(42, 115, 55)
            buildSplash()
        } catch (_: Throwable) {
            launchMain()
        }
    }

    private fun buildSplash() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.rgb(244, 243, 210),
                    Color.rgb(231, 237, 183),
                    Color.rgb(141, 187, 88),
                    Color.rgb(42, 115, 55)
                )
            )
            alpha = 0f
            scaleX = 0.92f
            scaleY = 0.92f
        }
        val logo = SplashLogoView(this)
        root.addView(logo, LinearLayout.LayoutParams(dp(210), dp(210)))

        root.addView(TextView(this).apply {
            text = "AgroYar"
            textSize = 42f
            setTextColor(Color.rgb(11, 87, 47))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })

        root.addView(TextView(this).apply {
            text = "اگرویار"
            textSize = 30f
            setTextColor(Color.rgb(11, 87, 47))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(TextView(this).apply {
            text = "همراه هوشمند کشاورزی"
            textSize = 15f
            setTextColor(Color.rgb(56, 109, 53))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })

        setContentView(root)
        root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(650L).start()
        handler.postDelayed({
            if (!isFinishing) {
                root.animate().alpha(0f).setDuration(250L).withEndAction { launchMain() }.start()
            }
        }, 1500L)
        handler.postDelayed({ launchMain() }, 2500L)
    }

    private fun launchMain() {
        if (launched || isFinishing) return
        launched = true
        try {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        } catch (_: Throwable) {
            try {
                val fallback = TextView(this).apply {
                    text = "AgroYar\nخطا در اجرای صفحه اصلی"
                    textSize = 20f
                    setTextColor(Color.rgb(25, 35, 25))
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.rgb(248, 250, 243))
                }
                setContentView(fallback)
                launched = false
                return
            } catch (_: Throwable) {
                return
            }
        }
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}

private class SplashLogoView(activity: Activity) : View(activity) {
    private val green = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(46, 125, 50)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(11, 87, 47)
        style = Paint.Style.FILL
    }
    private val lime = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(155, 203, 36)
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cx = w / 2f
        val cy = h / 2f

        green.strokeWidth = w * 0.035f
        canvas.drawArc(RectF(w * 0.12f, h * 0.12f, w * 0.88f, h * 0.88f), 145f, 250f, false, green)

        green.strokeWidth = w * 0.026f
        canvas.drawLine(cx, h * 0.66f, cx, h * 0.27f, green)

        canvas.drawOval(RectF(w * 0.41f, h * 0.12f, w * 0.56f, h * 0.31f), lime)
        canvas.save()
        canvas.rotate(-32f, w * 0.37f, h * 0.40f)
        canvas.drawOval(RectF(w * 0.24f, h * 0.33f, w * 0.48f, h * 0.47f), dark)
        canvas.restore()
        canvas.save()
        canvas.rotate(32f, w * 0.63f, h * 0.42f)
        canvas.drawOval(RectF(w * 0.52f, h * 0.35f, w * 0.76f, h * 0.49f), lime)
        canvas.restore()

        green.strokeWidth = w * 0.05f
        canvas.drawArc(RectF(w * 0.18f, h * 0.54f, w * 0.82f, h * 0.75f), 5f, 165f, false, green)
        green.strokeWidth = w * 0.042f
        canvas.drawArc(RectF(w * 0.22f, h * 0.62f, w * 0.78f, h * 0.82f), 4f, 165f, false, green)
        green.strokeWidth = w * 0.035f
        canvas.drawArc(RectF(w * 0.29f, h * 0.69f, w * 0.71f, h * 0.87f), 2f, 166f, false, green)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = w * 0.008f
        }
        canvas.drawCircle(cx, cy * 0.92f, w * 0.42f, glow)
    }
}
