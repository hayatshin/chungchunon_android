package com.chugnchunon.chungchunon_android.Layout

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class CenterZoomLayoutManager(
    context: Context,
    private val mShrinkDistance: Float = 0.5f,
    val mShrinkAmount: Float = 0.12f
) : LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        scaleChildren()
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        return if (orientation == HORIZONTAL) {
            super.scrollHorizontallyBy(dx, recycler, state).also { scaleChildren() }
        } else {
            0
        }
    }

    private fun scaleChildren() {
        val midpoint = width / 2f
        val d1 = mShrinkDistance * midpoint
        for (i in 0 until childCount) {
            val child = getChildAt(i) as View
            val d = Math.min(d1, Math.abs(midpoint - (getDecoratedRight(child) + getDecoratedLeft(child)) / 2f))
            val scale = 1f - mShrinkAmount * d / d1
            child.scaleX = scale
            child.scaleY = scale
        }
    }
}