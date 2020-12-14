package dropit.ui.pairing

import dropit.application.model.Phones
import dropit.application.settings.AppSettings
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import dropit.jooq.tables.records.PhoneRecord
import dropit.ui.main.PairingQrCode
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PairingDialog @Inject constructor(
    private val eventBus: EventBus,
    private val display: Display,
    private val appSettings: AppSettings
) {
    private var phone: PhoneRecord? = null
    private lateinit var window: Shell
    private lateinit var updateSubscription: EventBus.Subscription
    private lateinit var acceptSubscription: EventBus.Subscription
    private lateinit var rejectSubscription: EventBus.Subscription
    private var pairingQrCode: PairingQrCode? = null
    private var pairingForm: PairingForm? = null

    fun open(phoneRecord: PhoneRecord? = null) {
        phone = phoneRecord

        if (this::window.isInitialized && !this.window.isDisposed) {
            update()
            return
        }

        openWindow()
    }

    private fun openWindow() {
        window = Shell(display, SWT.TITLE or SWT.CLOSE or SWT.MIN).apply {
            text = t("pairingDialog.title")
            image = Image(display, javaClass.getResourceAsStream("/ui/icon.png"))
            size = Point(512, 512)
            minimumSize = Point(512, 512)
            layout = GridLayout(1, false).apply {
                marginHeight = 8
                marginWidth = 8
            }
            addListener(SWT.Close) {
                eventBus.unsubscribe(updateSubscription)
                eventBus.unsubscribe(acceptSubscription)
                eventBus.unsubscribe(rejectSubscription)
            }
        }
        updateSubscription = eventBus.subscribe(Phones.NewPhoneRequestEvent::class) { phone ->
            display.asyncExec {
                if (phone.id != this.phone?.id) {
                    this.phone = phone
                    update()
                }
            }
        }
        acceptSubscription = eventBus.subscribe(Phones.PhonePairedEvent::class) { phone ->
            display.asyncExec {
                if (phone.id == this.phone?.id) {
                    window.close()
                }
            }
        }
        rejectSubscription = eventBus.subscribe(Phones.PhoneRejectedEvent::class) { phone ->
            display.asyncExec {
                if (phone.id == this.phone?.id) {
                    window.close()
                }
            }
        }
        update()

        window.open()
    }

    private fun update() {
        val phoneRecord = phone
        if (phoneRecord != null) {
            pairingQrCode?.group?.dispose()
            pairingQrCode = null
            pairingForm = PairingForm(phoneRecord, window)
        } else {
            pairingForm?.group?.dispose()
            pairingQrCode = PairingQrCode(appSettings, window)
        }

        window.pack()
    }
}
