package com.chugnchunon.chungchunon_android.Layout

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build.VERSION_CODES
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.chugnchunon.chungchunon_android.R


class SquareRelativeLayout : RelativeLayout {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Set a square layout.
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var newWidthSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        super.onMeasure(newWidthSpec, heightMeasureSpec)
    }

}