package dropit.infrastructure.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.events.ControlEvent
import org.eclipse.swt.events.ControlListener
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Table

class TableResizedAdapter(
    val parent: Composite, val table: Table, val callback: (width: Int) -> Unit
) : ControlListener {
    override fun controlMoved(e: ControlEvent?) {
    }

    override fun controlResized(e: ControlEvent) {
        val area = parent.clientArea
        val size = table.computeSize(SWT.DEFAULT, SWT.DEFAULT)
        val vBar = table.verticalBar
        var width = area.width - table.computeTrim(0, 0, 0, 0).width
        if (size.y > area.height + table.headerHeight) {
            // Subtract the scrollbar width from the total column width
            // if a vertical scrollbar will be required
            val vBarSize = vBar.size
            width -= vBarSize.x
        }
        val oldSize = table.size
        if (oldSize.x > area.width) {
            // table is getting smaller so make the columns
            // smaller first and then resize the table to
            // match the client area width
            callback(width)
            table.setSize(area.width, area.height)
        } else {
            // table is getting bigger so make the table
            // bigger first and then make the columns wider
            // to match the client area width
            table.setSize(area.width, area.height)
            callback(width)
        }
    }
}