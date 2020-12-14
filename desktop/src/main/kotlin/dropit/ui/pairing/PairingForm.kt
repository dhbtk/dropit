package dropit.ui.pairing

import dropit.application.model.authorize
import dropit.application.model.destroy
import dropit.jooq.tables.records.PhoneRecord
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*

class PairingForm(private val phoneRecord: PhoneRecord, private val window: Shell) {
    val group = Group(window, SWT.SHADOW_ETCHED_OUT).also { group ->
        group.text = "Pairing"
        group.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
            .apply {
                minimumHeight = 128
            }
        group.layout = GridLayout(1, false).apply {
            marginHeight = 32
            marginWidth = 64
        }
        group.size = window.size
    }
    private val label = Label(group, SWT.CENTER or SWT.WRAP).apply {
        text = "Pair with this phone?"
        layoutData = GridData(GridData.FILL_HORIZONTAL).apply {
            minimumHeight = 64
        }
        pack()
    }
    private val canvas = Label(group, SWT.CENTER).apply {
        layoutData = GridData(SWT.CENTER, SWT.CENTER, true, true).apply {
            widthHint = 384
            heightHint = 384
        }
        image = Image(display, javaClass.getResourceAsStream("/ui/smartphone.png"))
        pack()
    }
    private val phoneNameLabel = Label(group, SWT.CENTER).apply {
        text = phoneRecord.name
        layoutData = GridData(GridData.FILL_HORIZONTAL)
        pack()
    }
    private val buttonGroup = Composite(group, 0).apply {
        layoutData = GridData(SWT.CENTER, SWT.FILL, true, true)
        layout = GridLayout(2, true).apply {
            marginWidth = 16
            marginHeight = 32
        }
    }
    private val noButton = Button(buttonGroup, SWT.PUSH).apply {
        text = "No"
        layoutData = GridData(SWT.FILL, SWT.CENTER, false, false)
        pack()
        addListener(SWT.Selection) { phoneRecord.destroy() }
    }
    private val yesButton = Button(buttonGroup, SWT.PUSH).apply {
        text = "Yes"
        layoutData = GridData(SWT.FILL, SWT.CENTER, false, false)
        pack()
        addListener(SWT.Selection) { phoneRecord.authorize() }
    }

    init {
        buttonGroup.pack()
    }
}