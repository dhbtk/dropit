package dropit.ui.main

import dropit.application.PhoneSessionService
import dropit.application.dto.TokenStatus
import dropit.application.settings.AppSettings
import dropit.domain.service.PhoneService
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.event.EventHandler
import dropit.infrastructure.i18n.t
import dropit.infrastructure.ui.TableResizedAdapter
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.TableEditor
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Table
import org.eclipse.swt.widgets.TableColumn
import org.eclipse.swt.widgets.TableItem
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.CompletableFuture
import kotlin.math.min

class PhoneTable(
    private val window: Shell,
    private val bus: EventBus,
    private val phoneService: PhoneService,
    private val display: Display,
    private val appSettings: AppSettings,
    private val phoneSessionService: PhoneSessionService
) {
    lateinit var phoneLabel: Label
    lateinit var phoneTable: Table
    lateinit var subscription: EventHandler<PhoneService.PhoneChangedEvent>
    val authorizeIcon = Image(display, javaClass.getResourceAsStream("/ui/phone/authorize.png"))
    val deleteIcon = Image(display, javaClass.getResourceAsStream("/ui/phone/delete.png"))
    val pairIcon = Image(display, javaClass.getResourceAsStream("/ui/phone/pair.png"))
    val rejectIcon = Image(display, javaClass.getResourceAsStream("/ui/phone/reject.png"))

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
        subscription = bus.subscribe(PhoneService.PhoneChangedEvent::class) {
            updateTable()
        }
        updateTable()
        parent.addControlListener(TableResizedAdapter(parent, phoneTable, this::resizeColumns))
    }

    fun dispose() {
        bus.unsubscribe(PhoneService.PhoneChangedEvent::class, subscription)
    }

    private fun updateTable() {
        display.asyncExec {
            setLabelText()
            phoneTable.removeAll()
            phoneService.listPhones(false).forEach { phone ->
                val item = TableItem(phoneTable, SWT.NONE)
                item.setText(0, phone.name)
                if (phone.lastConnected != null) {
                    item.setText(1, phone.lastConnected?.format(
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)))
                } else {
                    item.setText(1, "-")
                }
                if (appSettings.settings.currentPhoneId == phone.id) {
                    item.setText(2, t("phone.status.PAIRED"))
                } else {
                    item.setText(2, t("phone.status.${phone.status}"))
                }
                if (phone.status == TokenStatus.AUTHORIZED) {
                    if (appSettings.settings.currentPhoneId != phone.id) {
                        val pairButton = Button(phoneTable, SWT.PUSH)
                            .apply {
                                image = pairIcon
                                toolTipText = t("phoneTable.actions.pair")
                                addListener(SWT.Selection) {
                                    CompletableFuture.runAsync {
                                        phoneService.authorizePhone(phone.id!!)
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
                                    phoneService.deletePhone(phone.id!!)
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
                                    phoneService.authorizePhone(phone.id!!)
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
                                    phoneService.deletePhone(phone.id!!)
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
        if (appSettings.settings.currentPhoneId == null) {
            phoneLabel.text = t("phoneTable.phoneStatus.notPaired", appSettings.settings.computerName)
        } else {
            val phone = phoneService.listPhones(false).find { it.id == appSettings.settings.currentPhoneId }
            val connected = phoneSessionService.phoneSessions[phone?.id]?.session != null
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
