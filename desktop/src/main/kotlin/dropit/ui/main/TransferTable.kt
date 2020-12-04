package dropit.ui.main

import dropit.domain.entity.TransferSource
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.event.EventHandler
import dropit.infrastructure.i18n.t
import dropit.infrastructure.ui.TableResizedAdapter
import dropit.ui.service.TransferStatusService
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Table
import org.eclipse.swt.widgets.TableColumn
import org.eclipse.swt.widgets.TableItem
import java.util.UUID
import kotlin.math.min


class TransferTable(
    private val eventBus: EventBus,
    private val transferStatusService: TransferStatusService,
    private val display: Display
) {
    lateinit var transferTable: Table
    lateinit var subscription: EventHandler<TransferStatusService.TransferUpdatedEvent>
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
        subscription = eventBus.subscribe(TransferStatusService.TransferUpdatedEvent::class) {
            updateTable()
        }
        updateTable()
        parent.addControlListener(TableResizedAdapter(parent, transferTable, this::resizeColumns))
    }

    fun dispose() {
        eventBus.unsubscribe(TransferStatusService.TransferUpdatedEvent::class, subscription)
    }

    private fun updateTable() {
        display.asyncExec {
            transferStatusService.currentTransfers.forEach { transfer ->
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
            val ids = transferStatusService.currentTransfers.map { it.id }
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
