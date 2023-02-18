package com.chugnchunon.chungchunon_android.Layout

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler


class BarChartCustomRenderer(
    chart: BarDataProvider?,
    animator: ChartAnimator?,
    viewPortHandler: ViewPortHandler?,
    myColors: ArrayList<Int>
) :
    BarChartRenderer(chart, animator, viewPortHandler) {
    private val myPaint: Paint
    private val myColors: ArrayList<Int>
    override fun drawValues(c: Canvas) {
        super.drawValues(c)
        // you can modify the original method
        // so that everything is drawn on the canvas inside a single loop
        // also you can add logic here to meet your requirements
        var colorIndex = 0
        for (i in 0 until mChart.barData.dataSetCount) {
            val buffer = mBarBuffers[i]
            var left: Float
            var right: Float
            var top: Float
            var bottom: Float
            var j = 0
            while (j < buffer.buffer.size * mAnimator.phaseX) {
                myPaint.setColor(myColors[colorIndex++])
                left = buffer.buffer[j]
                right = buffer.buffer[j + 2]
                top = buffer.buffer[j + 1]
                bottom = buffer.buffer[j + 3]
                //                myPaint.setShader(new LinearGradient(left,top,right,bottom, Color.CYAN, myColors.get(colorIndex++), Shader.TileMode.MIRROR ));
                c.drawRect(left, top, right, top + 5f, myPaint)
                j += 4
            }
        }
    }

    fun drawValue(
        c: Canvas,
        formatter: IValueFormatter,
        value: Float,
        entry: Entry,
        dataSetIndex: Int,
        x: Float,
        y: Float,
        color: Int
    ) {
        val text = formatter.getFormattedValue(value, entry, dataSetIndex, mViewPortHandler)
        val splitText: Array<String>
        if (text.contains(",")) {
            splitText = text.split(",").toTypedArray()
            val paintStyleOne = Paint(mValuePaint)
            val paintStyleTwo = Paint(mValuePaint)
            paintStyleOne.setColor(Color.BLACK)
            paintStyleTwo.setColor(Color.BLUE)
            c.drawText(splitText[0], x, y - 20f, paintStyleOne)
            c.drawText(splitText[1], x, y, paintStyleTwo)
        }
        //else{
//            super.drawValue(c, formatter, value, entry, dataSetIndex, x, y, color);
        //}
    }

    init {
        myPaint = Paint()
        this.myColors = myColors
    }
}