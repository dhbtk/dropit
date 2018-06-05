package dropit.ui.view.phone

import dropit.application.dto.TokenStatus
import dropit.domain.entity.Phone
import dropit.domain.service.PhoneService
import dropit.infrastructure.i18n.t
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import javafx.util.Callback
import tornadofx.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class PhoneListView : View() {
    val phones = ArrayList<Phone>().observable()
    val phoneService: PhoneService by di()

    var showDeniedPhones = false

    override val root = vbox {
        hbox {
            alignment = Pos.BASELINE_RIGHT
            paddingAll = 5
            checkbox("Show denied phones") {
                isSelected = showDeniedPhones
                action {
                    showDeniedPhones = isSelected
                    listAllPhones()
                }
            }
        }
        tableview(phones) {
            isEditable = false
            val table = this
            readonlyColumn("Name", Phone::name) {
                prefWidthProperty().bind(table.widthProperty().subtract(477))
            }
            readonlyColumn("Status", Phone::status) {
                prefWidth = 150.0
            }.cellFormat { status ->
                text = t("phone.status.$status")
                alignment = Pos.CENTER
                style {
                    fontWeight = FontWeight.BOLD
                    textFill = when (status) {
                        TokenStatus.DENIED -> Color.RED
                        TokenStatus.AUTHORIZED -> Color.GREEN
                        else -> Color.BLACK
                    }
                }
            }
            readonlyColumn("Creation date", Phone::createdAt) {
                prefWidth = 175.0
            }.cellFormat {
                text = it?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                alignment = Pos.CENTER
            }
            val actionColumn = TableColumn<Phone, Phone>("Actions")
            actionColumn.prefWidth = 150.0
            actionColumn.setCellValueFactory { ReadOnlyObjectWrapper<Phone>(it.value) }
            actionColumn.cellFactory = Callback<TableColumn<Phone, Phone>, TableCell<Phone, Phone>> {
                object : TableCell<Phone, Phone>() {
                    val approveButton = Button("Approve")
                    val denyButton = Button("Deny")
                    val deleteButton = Button("Delete")
                    val box = HBox()

                    init {
                        box.alignment = Pos.CENTER
                    }

                    override fun updateItem(item: Phone?, empty: Boolean) {
                        super.updateItem(item, empty)
                        if (item == null) {
                            graphic = null
                        } else {
                            graphic = box
                            box.clear()
                            if (item.status == TokenStatus.PENDING || item.status == TokenStatus.DENIED) {
                                box.add(approveButton)
                                approveButton.setOnAction { approvePhone(item) }
                                if (item.status == TokenStatus.DENIED) {
                                    box.add(deleteButton)
                                    deleteButton.setOnAction { deletePhone(item) }
                                }
                            } else {
                                box.add(denyButton)
                                denyButton.setOnAction { denyPhone(item) }
                            }
                        }
                    }
                }
            }
            columns.add(actionColumn)
        }
    }

    private fun listAllPhones() {
        runAsync {
            phoneService.listPhones(showDeniedPhones)
        } ui {
            phones.clear()
            phones.addAll(it)
        }
    }

    private fun approvePhone(phone: Phone) {
        runAsync {
            phoneService.authorizePhone(phone.id!!)
        } success {
            listAllPhones()
        }
    }

    private fun denyPhone(phone: Phone) {
        runAsync {
            phoneService.denyPhone(phone.id!!)
        } success {
            listAllPhones()
        }
    }

    private fun deletePhone(phone: Phone) {
        runAsync {
            phoneService.deletePhone(phone.id!!)
        } success {
            listAllPhones()
        }
    }

    init {
        phoneService.phoneChangeListener = {
            listAllPhones()
        }

        listAllPhones()

        Thread {
            Thread.sleep(2000)
        }.start()
    }
}