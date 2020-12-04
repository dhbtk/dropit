package dropit.ui.main

import dropit.ui.service.ClipboardService
import org.eclipse.swt.dnd.DND
import org.eclipse.swt.dnd.DropTargetEvent
import org.eclipse.swt.dnd.DropTargetListener
import org.eclipse.swt.dnd.FileTransfer
import org.eclipse.swt.widgets.Shell

class FileDropListener(
    private val transferType: FileTransfer,
    private val window: Shell,
    private val clipboardService: ClipboardService
) : DropTargetListener {
    override fun dragEnter(event: DropTargetEvent) {
        if (event.detail == DND.DROP_DEFAULT) {
            if (event.operations and DND.DROP_COPY != 0) {
                event.detail = DND.DROP_COPY
            } else {
                event.detail = DND.DROP_NONE
            }
        }
        // will accept text but prefer to have files dropped
        for (i in event.dataTypes.indices) {
            if (transferType.isSupportedType(event.dataTypes[i])) {
                event.currentDataType = event.dataTypes[i]
                // files should only be copied
                if (event.detail != DND.DROP_COPY) {
                    event.detail = DND.DROP_NONE
                }
                break
            }
        }
    }

    override fun dragOver(event: DropTargetEvent) {
        event.feedback = DND.FEEDBACK_INSERT_AFTER
    }

    override fun dragOperationChanged(event: DropTargetEvent) {
        if (event.detail == DND.DROP_DEFAULT) {
            if (event.operations and DND.DROP_COPY != 0) {
                event.detail = DND.DROP_COPY
            } else {
                event.detail = DND.DROP_NONE
            }
        }
        // allow text to be moved but files should only be copied
        if (transferType.isSupportedType(event.currentDataType)) {
            if (event.detail != DND.DROP_COPY) {
                event.detail = DND.DROP_NONE
            }
        }
    }

    override fun dragLeave(event: DropTargetEvent) {}

    override fun dropAccept(event: DropTargetEvent) {}

    override fun drop(event: DropTargetEvent) {
        if (transferType.isSupportedType(event.currentDataType)) {
            val files = transferType.nativeToJava(event.currentDataType)
            if (files != null && files is Array<*>) {
                clipboardService.sendFilesToPhone(window, files.filterIsInstance<String>().toTypedArray())
            }
        }
    }
}
