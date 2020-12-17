package dropit.ui.main

import dropit.application.PhoneSessions
import dropit.application.model.Phones
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import dropit.jooq.tables.records.PhoneRecord
import dropit.ui.pairing.PairingDialog
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.ImageData
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import javax.inject.Inject

class PairingStatusWidget @Inject constructor(
    private val display: Display,
    private val eventBus: EventBus,
    private val pairingDialog: PairingDialog,
    private val phoneSessions: PhoneSessions
) {
    private val scaledImageData =
        ImageData(javaClass.getResourceAsStream("/ui/smartphone.png")).scaledTo(64, 64)
    private val connectedImage = Image(display, scaledImageData)
    private val notConnectedImage = Image(display, connectedImage, SWT.IMAGE_GRAY)
    private lateinit var group: Composite
    private lateinit var subgroup: Composite
    private lateinit var icon: Label
    private lateinit var statusLabel: Label
    private lateinit var infoLabel: Label
    private lateinit var triggerPairingButton: Button
    private var pairedPhone: PhoneRecord? = null
    private var paired = false
    private var connected = false

    fun init(window: Shell) {
        initWidgets(window)
        initEvents()
        updateState()
        updateWidgets()
    }

    private fun initWidgets(window: Shell) {
        group = Composite(window, 0).apply {
            layout = GridLayout(2, false)
            layoutData = GridData(GridData.FILL_HORIZONTAL)
        }
        icon = Label(group, SWT.CENTER).apply {
            layoutData = GridData(SWT.CENTER, SWT.CENTER, false, false).apply {
                minimumHeight = 64
                minimumWidth = 64
                widthHint = 64
                heightHint = 64
            }
            image = Image(
                display,
                ImageData(javaClass.getResourceAsStream("/ui/smartphone.png")).scaledTo(64, 64)
            )
            pack()
        }
        subgroup = Composite(group, 0).apply {
            layout = GridLayout(1, false)
            layoutData = GridData(GridData.FILL_BOTH)
        }
        statusLabel = Label(subgroup, SWT.LEFT or SWT.WRAP)
        infoLabel = Label(subgroup, SWT.LEFT or SWT.WRAP).apply {
            layoutData = GridData(SWT.BEGINNING, SWT.END, true, true)
        }
        triggerPairingButton = Button(subgroup, SWT.PUSH).apply {
            text = t("pairingStatusWidget.triggerPairingButton")
            addListener(SWT.Selection) { pairingDialog.open() }
            layoutData = GridData(SWT.BEGINNING, SWT.END, true, true)
        }
    }

    private fun initEvents() {
        eventBus.subscribe(Phones.PhoneChangedEvent::class) { updateState() }.also { sub ->
            group.addDisposeListener { eventBus.unsubscribe(sub) }
        }
    }

    private fun updateState() {
        pairedPhone = Phones.current()
        paired = pairedPhone != null
        connected = phoneSessions.defaultPhoneConnected()
        display.asyncExec(::updateWidgets)
    }

    private fun updateWidgets() {
        statusLabel.text = statusLabelText()

        infoLabel.text = infoLabelText()
        infoLabel.visible = paired
        (infoLabel.layoutData as GridData).exclude = !paired

        triggerPairingButton.visible = !paired
        (triggerPairingButton.layoutData as GridData).exclude = paired

        icon.image = if (connected) connectedImage else notConnectedImage

        subgroup.pack(true)
    }

    private fun statusLabelText(): String {
        return when {
            connected -> t("pairingStatusWidget.statusLabel.connected", pairedPhone?.name)
            paired -> t("pairingStatusWidget.statusLabel.paired", pairedPhone?.name)
            else -> t("pairingStatusWidget.statusLabel.notPaired")
        }
    }

    private fun infoLabelText(): String {
        return when {
            connected -> t("pairingStatusWidget.infoLabel.connected")
            else -> t("pairingStatusWidget.infoLabel.paired")
        }
    }
}