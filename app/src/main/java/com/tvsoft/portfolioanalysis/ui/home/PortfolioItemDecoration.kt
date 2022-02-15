package com.tvsoft.portfolioanalysis.ui.home

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView

class PortfolioItemDecoration: RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
return
        if(view.id % 2 == 0)
            outRect.right = 50
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        return
        val childCount = parent.childCount

        val layoutManager = parent.layoutManager;
        for (i in 0..childCount) {
/*
            if (i % 2 == 0)
                continue
*/
            val viewGroup: ViewGroup = (parent.getChildAt(i) ?: continue) as ViewGroup
            val headerView = viewGroup.getChildAt(0) ?: continue
            headerView.left = 0
            val mPaint = Paint()
            mPaint.color = Color.BLUE
            c.drawRect(0f, headerView.top.toFloat(), 100f, headerView.bottom.toFloat(), mPaint)
            headerView.draw(c)

//             динамически получить ширину каждого элемента
/*            val left = layoutManager?.getLeftDecorationWidth(child)
            val cx = 0f//(left?.div(2))?.toFloat() ?: 0.0f
            val cy = (child.top + child.height / 2).toFloat()
            val mPaint = Paint()
            mPaint.color = Color.BLUE
            c.drawRect(child.left.toFloat(), child.top.toFloat(), child.right.toFloat(), child.bottom.toFloat(), mPaint)*/
        }
    }
}