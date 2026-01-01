package com.example.mobileproject

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SeatSpacingDecoration(
    private val spanCount: Int = 4,
    private val spacingPx: Int,
    private val aislePx: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val col = position % spanCount  // 0,1,2,3

        // Genel küçük boşluk
        outRect.top = spacingPx
        outRect.bottom = spacingPx

        // Sol-sağ küçük spacing
        outRect.left = spacingPx
        outRect.right = spacingPx

        // ✅ Koridor: 2. ve 3. kolon arasına ekstra boşluk
        // col=1 (2. koltuk) -> sağa aisle
        // col=2 (3. koltuk) -> sola aisle
        if (col == 1) outRect.right += aislePx
        if (col == 2) outRect.left += aislePx
    }
}
