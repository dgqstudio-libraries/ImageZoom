package com.dgqstudio.imagezoom

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.values

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr),
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    View.OnTouchListener {

    private var mMatrix: Matrix = Matrix()

    private lateinit var mScaleDetector: ScaleGestureDetector
    private lateinit var mGestureDetector: GestureDetector
    var zoomMode: ZoomMode = ZoomMode.DEFAULT

    var originalWidth = 0f
    var originalHeight = 0f
    var mViewWidth = 0
    var mViewHeight = 0

    var currentScale = 1f
    var minimumScale = 1f
    var maximumScale = 8f

    private var touchPoint = PointF()

    companion object {
        const val ZoomableImageViewTAG = "ZoomableImageView"
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ZoomableImageView,
            0,
            0
        ).apply {
            try {
                init()
            } finally {
                recycle()
            }
        }
    }

    private fun init() {
        setImageViewAttrs()

        mScaleDetector = ScaleGestureDetector(context, ScalingListener())
        mGestureDetector = GestureDetector(context, this)
        setOnTouchListener(this)
    }

    private fun setImageViewAttrs() {
        isClickable = true
        scaleType = ScaleType.MATRIX

        mMatrix.reset()
        imageMatrix = mMatrix
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        adjustImageScale()
    }

    private fun adjustImageScale() {
        val drawable = drawable ?: return
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        val viewWidth = width
        val viewHeight = height

        val scaleX = viewWidth.toFloat() / drawableWidth
        val scaleY = viewHeight.toFloat() / drawableHeight
        val scale = scaleX.coerceAtMost(scaleY)

        originalWidth = scaleX
        originalHeight = scaleY

        mMatrix.reset()
        mMatrix.postScale(scale, scale)

        imageMatrix = mMatrix
    }

    override fun onTouch(mView: View?, mMouseEvent: MotionEvent?): Boolean {
        if (mMouseEvent != null) {
            mScaleDetector.onTouchEvent(mMouseEvent)
            mGestureDetector.onTouchEvent(mMouseEvent)

            val currentPoint = PointF(mMouseEvent.x, mMouseEvent.y)

            when (mMouseEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    zoomMode = ZoomMode.DRAGGING

                    touchPoint.set(currentPoint)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (zoomMode == ZoomMode.DRAGGING) {
                        val changeInX = currentPoint.x - touchPoint.x
                        val changeInY = currentPoint.y - touchPoint.y

                        val xTranslate = getXTranslate(changeInX)
                        val yTranslate = getYTranslate(changeInY)

                        mMatrix.postTranslate(xTranslate, yTranslate)

                        touchPoint.set(currentPoint)
                    }
                }

                MotionEvent.ACTION_UP -> zoomMode = ZoomMode.DEFAULT
            }

            imageMatrix = mMatrix
            Log.d(ZoomableImageViewTAG, zoomMode.toString())
        }

        return false
    }

    private fun getXTranslate(changeInX: Float): Float {
        val mMatrixValues = mMatrix.values()
        val maxTransX = (mViewWidth * currentScale) - mViewWidth

        val leftLimit = mMatrixValues[Matrix.MTRANS_X] + changeInX > 0f
        val rightLimit = maxTransX + (mMatrixValues[Matrix.MTRANS_X] + changeInX) <= 0f

        return if (!leftLimit && !rightLimit) changeInX else 0f
    }

    private fun getYTranslate(changeInY: Float): Float {
        val mMatrixValues = mMatrix.values()
        val maxTransY = (mViewHeight * currentScale) - mViewHeight

        val topLimit = mMatrixValues[Matrix.MTRANS_Y] + changeInY > 0f
        val bottomLimit = maxTransY + (mMatrixValues[Matrix.MTRANS_Y] + changeInY) <= 0f

        return if (!topLimit && !bottomLimit) changeInY else 0f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        mViewWidth = MeasureSpec.getSize(widthMeasureSpec)
        mViewHeight = MeasureSpec.getSize(heightMeasureSpec)
    }

    private inner class ScalingListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            zoomMode = ZoomMode.SCALING
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val mScaleFactor = detector.scaleFactor

            currentScale *= mScaleFactor

            if (currentScale > maximumScale) {
                currentScale = maximumScale

                return true
            } else if (currentScale < minimumScale) {
                currentScale = minimumScale

                return true
            }

            if (
                originalWidth * currentScale <= mViewWidth ||
                originalHeight * currentScale <= mViewHeight
            ) {
                mMatrix.postScale(
                    mScaleFactor,
                    mScaleFactor,
                    mViewWidth / 2.toFloat(),
                    mViewHeight / 2.toFloat()
                )
            } else {
                mMatrix.postScale(
                    mScaleFactor,
                    mScaleFactor,
                    detector.focusX,
                    detector.focusY
                )
            }

            fixVerticalTranslateIfNecessary()
            fixHorizontalTranslateIfNecessary()

            return true
        }
    }

    private fun fixVerticalTranslateIfNecessary() {
        val mMatrixValues = mMatrix.values()
        val maxTransY = (mViewHeight * currentScale) - mViewHeight

        val topLimit = mMatrixValues[Matrix.MTRANS_Y] > 0f
        val bottomLimit = maxTransY + mMatrixValues[Matrix.MTRANS_Y] <= 0f

        val value: Float
        if (topLimit) {
            value = mMatrixValues[Matrix.MTRANS_Y]
            mMatrix.postTranslate(0f, -value)
        } else if (bottomLimit) {
            value = maxTransY + mMatrixValues[Matrix.MTRANS_Y]
            mMatrix.postTranslate(0f, -value)
        }
    }

    private fun fixHorizontalTranslateIfNecessary() {
        val mMatrixValues = mMatrix.values()
        val maxTransX = (mViewWidth * currentScale) - mViewWidth

        val leftLimit = mMatrixValues[Matrix.MTRANS_X] > 0f
        val rightLimit = maxTransX + mMatrixValues[Matrix.MTRANS_X] <= 0f

        val value: Float
        if (leftLimit) {
            value = mMatrixValues[Matrix.MTRANS_X]
            mMatrix.postTranslate(-value, 0f)
        } else if (rightLimit) {
            value = maxTransX + mMatrixValues[Matrix.MTRANS_X]
            mMatrix.postTranslate(-value, 0f)
        }
    }

    override fun onDown(p0: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(p0: MotionEvent) {

    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    override fun onLongPress(p0: MotionEvent) {

    }

    override fun onFling(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    override fun onSingleTapConfirmed(p0: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTap(p0: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTapEvent(p0: MotionEvent): Boolean {
        return false
    }
}