package com.example.kotlin_customer_nom_movie_ticket.helper  // ← dùng đúng package của bạn

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.google.android.material.progressindicator.LinearProgressIndicator

class ProgressTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearProgressIndicator(context, attrs) {

    private var text: String = ""
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 36f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    fun setProgressWithText(progress: Int, max: Int) {
        this.progress = progress
        this.max = max
        this.text = "$progress / $max"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText(
            text,
            width / 2f,
            height / 2f - (textPaint.descent() + textPaint.ascent()) / 2,
            textPaint
        )
    }
}
