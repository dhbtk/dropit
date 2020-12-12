package dropit.ui.main

import dropit.application.PhoneSessions
import dropit.application.dto.TokenStatus
import dropit.application.model.Phones
import dropit.application.model.authorize
import dropit.application.model.destroy
import dropit.application.settings.AppSettings
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.event.EventHandler
import dropit.infrastructure.i18n.t
import dropit.infrastructure.ui.TableResizedAdapter
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.TableEditor
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.CompletableFuture
import kotlin.math.min

class PhoneTable(
    private val bus: EventBus,
    private val display: Display,
    private val appSettings: AppSettings,
    private val phoneSessions: PhoneSessions
) {
    private lateinit var phoneLabel: Label
    private lateinit var phoneTable: Table
    private lateinit var subscription: EventHandler<Phones.PhoneChangedEvent>
    private val authorizeIcon = Image(display, javaClass.getResourceAsStream("/ui/phone/authorize.png"))
    private val deleteIcon = Image(display, javaClass.getResourceAsStream("/ui/phone/delete.png"))
    private val pairIcon = Image(display, javaClass.getResourceAsStream("/ui/phone/pair.png"))
    private val rejectIcon = Image(display, javaClass.getResourceAsStream("/ui/phone/reject.png"))

    fun init(parent: Composite) {
        phoneLabel = Label(parent, SWT.LEFT or SWT.WRAP)
            .apply {
                layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
                    .apply {
                        minimumHeight = 36
                    }
            }
        phoneLabel.pack()
        phoneTable = Table(parent, SWT.V_SCROLL)
            .apply {
                headerVisible = true
                linesVisible = true
                layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
                    .apply {
                        heightHint = 128
                    }
            }
        phoneTable.pack()
        createColumns()
        subscription = bus.subscribe(Phones.PhoneChangedEvent::class) {
            updateTable()
        }
        updateTable()
        parent.addControlListener(TableResizedAdapter(parent, phoneTable, this::resizeColumns))
    }

    fun dispose() {
        bus.unsubscribe(Phones.PhoneChangedEvent::class, subscription)
    }

    private fun updateTable() {
        display.asyncExec {
            setLabelText()
            phoneTable.removeAll()
            Phones.all().forEach { phone ->
                val item = TableItem(phoneTable, SWT.NONE)
                item.setText(0, phone.name)
                if (phone.lastConnected != null) {
                    item.setText(1, phone.lastConnected?.format(
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)))
                } else {
                    item.setText(1, "-")
                }
                if (appSettings.currentPhoneId == phone.id) {
                    item.setText(2, t("phone.status.PAIRED"))
                } else {
                    item.setText(2, t("phone.status.${phone.status}"))
                }
                if (phone.status == TokenStatus.AUTHORIZED) {
                    if (appSettings.currentPhoneId != phone.id) {
                        val pairButton = Button(phoneTable, SWT.PUSH)
                            .apply {
                                image = pairIcon
                                toolTipText = t("phoneTable.actions.pair")
                                addListener(SWT.Selection) {
                                    CompletableFuture.runAsync {
                                        phone.authorize()
                                    }
                                }
                            }
                        TableEditor(phoneTable)
                            .apply {
                                grabHorizontal = true
                                grabVertical = true
                                setEditor(pairButton, item, 3)
                            }
                    }
                    val deleteButton = Button(phoneTable, SWT.PUSH)
                        .apply {
                            image = deleteIcon
                            toolTipText = t("phoneTable.actions.delete")
                            addListener(SWT.Selection) {
                                CompletableFuture.runAsync {
                                    phone.destroy()
                                }
                            }
                        }
                    TableEditor(phoneTable)
                        .apply {
                            grabHorizontal = true
                            grabVertical = true
                            setEditor(deleteButton, item, 4)
                        }
                } else {
                    // B1 - authorize
                    // B2 - reject
                    val authorizeButton = Button(phoneTable, SWT.PUSH)
                        .apply {
                            image = authorizeIcon
                            toolTipText = t("phoneTable.actions.authorize")
                            addListener(SWT.Selection) {
                                CompletableFuture.runAsync {
                                    phone.authorize()
                                }
                            }
                        }
                    TableEditor(phoneTable)
                        .apply {
                            grabHorizontal = true
                            grabVertical = true
                            setEditor(authorizeButton, item, 3)
                        }
                    val rejectButton = Button(phoneTable, SWT.PUSH)
                        .apply {
                            image = rejectIcon
                            toolTipText = t("phoneTable.actions.reject")
                            addListener(SWT.Selection) {
                                CompletableFuture.runAsync {
                                    phone.destroy()
                                }
                            }
                        }
                    TableEditor(phoneTable)
                        .apply {
                            grabHorizontal = true
                            grabVertical = true
                            setEditor(rejectButton, item, 4)
                        }
                }
            }
        }
    }

    private fun setLabelText() {
        if (appSettings.currentPhoneId == null) {
            phoneLabel.text = t("phoneTable.phoneStatus.notPaired", appSettings.computerName)
        } else {
            val phone = Phones.current()
            val connected = phoneSessions.phoneSessions[phone?.id]?.session != null
            phoneLabel.text = t(
                "phoneTable.phoneStatus.paired",
                phone!!.name!!,
                if (connected) {
                    t("phoneTable.phoneStatus.paired.connected")
                } else {
                    t("phoneTable.phoneStatus.paired.notConnected")
                }
            )
        }
    }

    private fun createColumns() {
        arrayOf(
            "name" to SWT.LEFT, "lastConnected" to SWT.RIGHT, "status" to SWT.LEFT
        ).map { (str, align) -> t("phoneTable.columns.$str") to align }.forEach { (name, align) ->
            TableColumn(phoneTable, SWT.NONE)
                .apply {
                    text = name
                    alignment = align
                    pack()
                }
        }
        // B1
        TableColumn(phoneTable, SWT.NONE)
            .apply {
                text = " "
                alignment = SWT.CENTER
                resizable = false
                pack()
            }
        // B2
        TableColumn(phoneTable, SWT.NONE)
            .apply {
                text = " "
                alignment = SWT.CENTER
                resizable = false
                pack()
            }
    }

    private fun resizeColumns(width: Int) {
        arrayOf(3, 4).forEach {
            phoneTable.columns[it].width = 48
        }
        phoneTable.columns[1].width = min(width / 4, 112)
        phoneTable.columns[2].width = min(width / 5, 72)
        val nameWidth = width - arrayOf(1, 2, 3, 4).map { phoneTable.columns[it].width }.sum()
        phoneTable.columns[0].width = nameWidth
    }
}
