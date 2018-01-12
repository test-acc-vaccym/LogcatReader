package com.dp.logcatapp.services

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.view.GravityCompat
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import com.dp.logcatapp.R

class RecordOverlayButton : BaseService() {
    companion object {
        val TAG = RecordOverlayButton::class.qualifiedName
        val KEY_POS_X = TAG + "_pos_x"
        val KEY_POS_Y = TAG + "_pos_y"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rootView: View
    private var rootViewLayoutParams: WindowManager.LayoutParams = createRootViewLayoutParams()

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initView()
        return START_STICKY
    }

    fun initView() {
        rootView = LayoutInflater.from(this)
                .inflate(R.layout.record_overlay_button, null)

        val gestureDetector = GestureDetector(this, MyGestureListener())

        rootViewLayoutParams.gravity = Gravity.TOP or GravityCompat.START
        rootViewLayoutParams.x = getInitRootViewX()
        rootViewLayoutParams.y = getInitRootViewY()

        val recordButton = rootView.findViewById<ImageView>(R.id.imgBtnStartRecording)
        recordButton.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        rootView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                rootView.viewTreeObserver.removeOnPreDrawListener(this)

                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                val displaymetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displaymetrics)

                val width = rootView.measuredWidth
                val height = rootView.measuredHeight

                if (rootViewLayoutParams.x + width > displaymetrics.widthPixels) {
                    rootViewLayoutParams.x = displaymetrics.widthPixels - width
                }
                if (rootViewLayoutParams.y + height > displaymetrics.heightPixels) {
                    rootViewLayoutParams.y = displaymetrics.heightPixels - height
                }
                if (rootViewLayoutParams.x < 0) {
                    rootViewLayoutParams.x = 0
                }
                if (rootViewLayoutParams.y < 0) {
                    rootViewLayoutParams.y = 0
                }

                windowManager.updateViewLayout(rootView, rootViewLayoutParams)

                rootView.alpha = 0.0f
                rootView.scaleX = 0.75f
                rootView.scaleY = 0.75f
                rootView.animate()
                        .alpha(0.75f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime)
                                .toLong())
                        .setInterpolator(DecelerateInterpolator())
                        .setListener(null)
                return true
            }
        })

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(rootView, rootViewLayoutParams)
    }

    private fun getInitRootViewX() = sharedPreferences.getInt(KEY_POS_X, 0)

    private fun getInitRootViewY() = sharedPreferences.getInt(KEY_POS_Y, 0)

    private fun createRootViewLayoutParams(): WindowManager.LayoutParams {
        return if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)
        } else {
            WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)
        }
    }

    fun saveRootViewPosition(x: Int, y: Int) {
        sharedPreferences.edit()
                .putInt(KEY_POS_X, x)
                .putInt(KEY_POS_Y, y)
                .apply()
    }

    private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {

        private var mInitialX: Int = 0
        private var mInitialY: Int = 0
        private var mInitialTouchX: Int = 0
        private var mInitialTouchY: Int = 0

        override fun onDown(e: MotionEvent): Boolean {
            mInitialX = rootViewLayoutParams.x
            mInitialY = rootViewLayoutParams.y

            mInitialTouchX = e.rawX.toInt()
            mInitialTouchY = e.rawY.toInt()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            rootViewLayoutParams.x = mInitialX + (e2.rawX - mInitialTouchX).toInt()
            rootViewLayoutParams.y = mInitialY + (e2.rawY - mInitialTouchY).toInt()
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.updateViewLayout(rootView, rootViewLayoutParams)
            saveRootViewPosition(rootViewLayoutParams.x, rootViewLayoutParams.y)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            return false
        }
    }
}