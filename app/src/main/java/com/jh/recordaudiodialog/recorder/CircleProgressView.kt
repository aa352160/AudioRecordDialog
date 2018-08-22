package com.jh.recordaudiodialog.recorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.jh.recordaudiodialog.R

/**
 * Created by jh352160 on 2018/8/9.
 */
class CircleProgressView : View {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    var maxProgress = 0f
    var currentProgress = 0f
        set(value) { field = value; invalidate() }
    var strokeWidth = dip2px(context,2f)
    val rectF = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = resources.getColor(R.color.colorProgressArc)
        strokeWidth = this@CircleProgressView.strokeWidth
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val arcAngle = currentProgress / maxProgress
        canvas!!.drawArc(rectF,-90f,360f * arcAngle,false,paint)
    }

    fun dip2px(context: Context, dpValue: Float): Float {
        val scale = context.resources.displayMetrics.density
        return dpValue * scale + 0.5f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectF.set(strokeWidth/2,strokeWidth/2,w.toFloat()-strokeWidth/2,h.toFloat()-strokeWidth/2)
    }
}
