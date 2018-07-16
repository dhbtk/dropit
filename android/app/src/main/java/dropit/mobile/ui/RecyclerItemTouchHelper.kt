package dropit.mobile.ui

import android.graphics.Canvas
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import dropit.mobile.ui.adapter.ListFileAdapter

class RecyclerItemTouchHelper(dragDirs: Int, swipeDirs: Int, val listener: SwipeListener) : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {
    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?) = true

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (viewHolder != null) {
            getDefaultUIUtil().onSelected((viewHolder as ListFileAdapter.ListViewHolder).foreground)
        }
    }

    override fun onChildDrawOver(c: Canvas?, recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        getDefaultUIUtil().onDrawOver(c, recyclerView, (viewHolder as ListFileAdapter.ListViewHolder).foreground, dX, dY, actionState, isCurrentlyActive)
    }

    override fun clearView(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?) {
        getDefaultUIUtil().clearView((viewHolder as ListFileAdapter.ListViewHolder).foreground)
    }

    override fun onChildDraw(c: Canvas?, recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        getDefaultUIUtil().onDraw(c, recyclerView, (viewHolder as ListFileAdapter.ListViewHolder).foreground, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
        listener.onSwiped(viewHolder as ListFileAdapter.ListViewHolder, direction)
    }

    interface SwipeListener {
        fun onSwiped(viewHolder: ListFileAdapter.ListViewHolder, direction: Int)
    }
}