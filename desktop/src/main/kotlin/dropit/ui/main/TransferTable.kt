package dropit.ui.main

import dropit.application.model.TransferSource
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.event.EventHandler
import dropit.infrastructure.i18n.t
import dropit.infrastructure.ui.TableResizedAdapter
import dropit.ui.service.TransferStatusMonitor
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.indexOf
import kotlin.collections.map
import kotlin.collections.sum
import kotlin.math.min


class TransferTable(
    private val eventBus: EventBus,
    private val transferStatusMonitor: TransferStatusMonitor,
    private val display: Display
) {
    lateinit var transferTable: Table
    lateinit var subscription: EventHandler<TransferStatusMonitor.TransferUpdatedEvent>
    private val rowMap = HashMap<UUID, TableItem>()
    private val downloadImage = Image(display, javaClass.getResourceAsStream("/ui/transfer/download.png"))
    private val uploadImage = Image(display, javaClass.getResourceAsStream("/ui/transfer/upload.png"))

    fun init(parent: Composite) {
        transferTable = Table(parent, SWT.V_SCROLL)
            .apply {
                headerVisible = true
                linesVisible = true
                layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
                    .apply {
                        heightHint = 128
                    }
            }
        transferTable.pack()
        createColumns()
        subscription = eventBus.subscribe(TransferStatusMonitor.TransferUpdatedEvent::class) {
            updateTable()
        }
        updateTable()
        parent.addControlListener(TableResizedAdapter(parent, transferTable, this::resizeColumns))
    }

    fun dispose() {
        eventBus.unsubscribe(TransferStatusMonitor.TransferUpdatedEvent::class, subscription)
    }

    private fun updateTable() {
        display.asyncExec {
            transferStatusMonitor.currentTransfers.forEach { transfer ->
                val tableItem = rowMap.computeIfAbsent(transfer.id) { TableItem(transferTable, SWT.NONE) }
                tableItem.setImage(0, if (transfer.source == TransferSource.PHONE) {
                    downloadImage
                } else {
                    uploadImage
                })
                tableItem.setText(1, transfer.name)
                tableItem.setText(2, transfer.humanSize())
                tableItem.setText(3, "${transfer.progress}%")
                tableItem.setText(4, transfer.humanSpeed())
                tableItem.setText(5, transfer.humanEta())
            }
            val ids = transferStatusMonitor.currentTransfers.map { it.id }
            rowMap.filter { (id, _) -> id !in ids }.forEach { (id, item) ->
                val index = transferTable.items.indexOf(item)
                transferTable.remove(index)
                item.dispose()
                rowMap.remove(id)
            }
        }
    }

    private fun createColumns() {
        TableColumn(transferTable, SWT.NONE)
            .apply {
                text = " "
                alignment = SWT.CENTER
                pack()
            }

        arrayOf(
            "name" to SWT.LEFT, "size" to SWT.RIGHT, "progress" to SWT.RIGHT, "speed" to SWT.RIGHT, "eta" to SWT.RIGHT
        ).map { (str, align) -> t("transferTable.columns.$str") to align }.forEach { (name, align) ->
            TableColumn(transferTable, SWT.NONE)
                .apply {
                    text = name
                    alignment = align
                    pack()
                }
        }
    }

    private fun resizeColumns(width: Int) {
        transferTable.columns[0].width = min(26, width)
        val smallWidth = min(width / 6, 65)
        for (i in 2..5) {
            transferTable.columns[i].width = smallWidth
        }
        val nameWidth = width - arrayOf(0, 2, 3, 4, 5).map { transferTable.columns[it].width }.sum()
        transferTable.columns[1].width = nameWidth
    }
}
