package dropit.ui.settings

import arrow.core.Either
import arrow.core.flatMap
import dropit.APP_NAME
import dropit.application.model.ShowFileAction
import dropit.application.settings.AppSettings
import dropit.infrastructure.i18n.t
import dropit.infrastructure.ui.GuiIntegrations
import dropit.ui.ShellContainer
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusListener
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import java.io.File
import java.text.MessageFormat
import java.util.*
import javax.inject.Inject

typealias SaveCallback = () -> Either<String, Unit>

class SettingsWindow @Inject constructor(
    private val appSettings: AppSettings,
    private val guiIntegrations: GuiIntegrations,
    private val display: Display
) : ShellContainer() {
    override val window = Shell(display, SWT.CLOSE or SWT.TITLE or SWT.MAX or SWT.RESIZE)
    private val saveCallbacks: ArrayList<SaveCallback> = ArrayList()
    private val descriptionFont = display.systemFont.fontData.map {
        it.apply { height *= 0.85f }
    }.let { Font(display, it.toTypedArray()) }

    init {
        window.text = t("settingsWindow.title")
        window.image = Image(display, javaClass.getResourceAsStream("/ui/icon.png"))
        window.size = Point(600, 400)
        window.layout = GridLayout(1, false)
            .apply {
                this.marginHeight = 8
                this.marginWidth = 8
            }

        computerNameSetting()
        transferFolderSettings()
        privacySettings()

        window.addListener(SWT.Close) {
            val invalidField = saveCallbacks.map { it() }.find { it.isLeft() }
            invalidField?.mapLeft(this::openModal)

            it.doit = invalidField == null
            if (it.doit) {
                display.asyncExec { guiIntegrations.afterWindowClose() }
            }
        }
        window.addDisposeListener {
            descriptionFont.dispose()
        }
        window.pack()
        window.minimumSize = window.size
        window.open()
    }

    private fun computerNameSetting() {
        val composite = createComposite()

        settingLabel(composite, t("settings.computerName.label"))

        val textField = Text(composite, SWT.SINGLE or SWT.BORDER)
        textField.apply {
            text = appSettings.computerName
            layoutData = GridData(GridData.FILL_HORIZONTAL)

            val saveCallback: SaveCallback = {
                Either.right(textField.text).flatMap { text ->
                    if (text == null || text.trim().isBlank()) {
                        Either.left(t("settings.computerName.required"))
                    } else {
                        Either.right(text)
                    }
                }.map { path ->
                    this.text = path
                    appSettings.computerName = path
                    Unit
                }.mapLeft { message ->
                    this.text = appSettings.computerName
                    this.setFocus()
                    message
                }
            }
            addFocusListener(focusListener(saveCallback))
            saveCallbacks += saveCallback
            pack()
        }

        spacer(composite)
        description(composite, t("settings.computerName.description"))

        spacer(composite)
        Button(composite, SWT.CHECK).apply {
            selection = appSettings.keepWindowOnTop
            text = t("settings.keepWindowOnTop.label")
            addListener(SWT.Selection) {
                appSettings.keepWindowOnTop = this.selection
            }
        }

        separator(composite)
        composite.pack()
    }

    private fun transferFolderSettings() {
        val composite = createComposite()
        settingLabel(composite, t("settings.rootTransferFolder.label"))

        val rootTransferFolderField = Text(composite, SWT.SINGLE or SWT.BORDER).apply {
            text = appSettings.rootTransferFolder
            layoutData = GridData(GridData.FILL_HORIZONTAL)

            val saveCallback: SaveCallback = {
                Either.right(this.text).flatMap { text ->
                    if (text == null || text.trim().isBlank()) {
                        Either.left(t("settings.rootTransferFolder.required"))
                    } else {
                        Either.right(text)
                    }
                }.flatMap { path ->
                    try {
                        Either.right(File(path).canonicalPath.toString())
                    } catch (e: Exception) {
                        Either.left(t("settings.rootTransferFolder.invalid"))
                    }
                }.flatMap { path ->
                    val file = File(path)
                    if ((file.exists() && file.isDirectory) || file.mkdirs()) {
                        Either.right(path)
                    } else {
                        Either.left(t("settings.rootTransferFolder.notCreated"))
                    }
                }.map { path ->
                    this.text = path
                    appSettings.rootTransferFolder = path
                }.mapLeft { message ->
                    this.text = appSettings.rootTransferFolder
                    this.setFocus()
                    message
                }
            }
            addFocusListener(focusListener(saveCallback))
            saveCallbacks += saveCallback
            pack()
        }

        spacer(composite)
        Button(composite, SWT.PUSH).apply {
            text = t("settings.rootTransferFolder.selectFolder")

            addListener(SWT.Selection) {
                val directory = DirectoryDialog(window).open()
                if (directory != null) {
                    rootTransferFolderField.text = directory
                }
            }
        }

        settingLabel(composite, t("settings.transferFolderName.label"))
        Text(composite, SWT.SINGLE or SWT.BORDER).apply {
            text = appSettings.transferFolderName
            layoutData = GridData(GridData.FILL_HORIZONTAL)

            val saveCallback: SaveCallback = {
                Either.right(this.text).flatMap { text ->
                    if (text == null || text.trim().isBlank()) {
                        Either.left(t("settings.transferFolderName.required"))
                    } else {
                        Either.right(text)
                    }
                }.flatMap { path ->
                    try {
                        MessageFormat(path).format(arrayOf(Date(), "Transfer"))
                        Either.right(path)
                    } catch (e: IllegalArgumentException) {
                        Either.left(t("settings.transferFolderName.invalid"))
                    }
                }.map { path ->
                    this.text = path
                    appSettings.transferFolderName = path
                }.mapLeft { message ->
                    this.text = appSettings.transferFolderName
                    this.setFocus()
                    message
                }
            }
            addFocusListener(focusListener(saveCallback))
            saveCallbacks += saveCallback
            pack()
        }
        spacer(composite)
        description(composite, t("settings.transferFolderName.description"))

        spacer(composite)
        Button(composite, SWT.CHECK).apply {
            selection = appSettings.separateTransferFolders
            text = t("settings.separateTransferFolders.label")
            addListener(SWT.Selection) {
                appSettings.separateTransferFolders = this.selection
            }
        }

        settingLabel(composite, t("settings.afterReceivingFiles.label"))
        Combo(composite, SWT.DROP_DOWN or SWT.READ_ONLY).apply {
            add(t("settings.afterReceivingFiles.doNothing"))
            add(t("settings.afterReceivingFiles.openFolder"))
            add(t("settings.afterReceivingFiles.openFile"))

            when {
                !appSettings.openTransferOnCompletion -> select(0)
                appSettings.showTransferAction == ShowFileAction.OPEN_FOLDER -> select(1)
                else -> select(2)
            }

            addListener(SWT.Selection) {
                when (selectionIndex) {
                    1 -> {
                        appSettings.showTransferAction = ShowFileAction.OPEN_FOLDER
                        appSettings.openTransferOnCompletion = true
                    }
                    2 -> {
                        appSettings.showTransferAction = ShowFileAction.OPEN_FILE
                        appSettings.openTransferOnCompletion = true
                    }
                    else -> {
                        appSettings.openTransferOnCompletion = false
                    }
                }
            }
        }

        spacer(composite)
        Button(composite, SWT.PUSH).apply {
            text = t("settings.configureByFileType.label")
        }

        separator(composite)
    }

    private fun privacySettings() {
        val composite = createComposite()

        settingLabel(composite, " ")
        Button(composite, SWT.CHECK).apply {
            selection = appSettings.logClipboardTransfers
            text = t("settings.logClipboardTransfers.label")
            addListener(SWT.Selection) {
                appSettings.logClipboardTransfers = this.selection
            }
            pack()
        }
        composite.pack()
    }

    private fun focusListener(callback: SaveCallback) = FocusListener.focusLostAdapter {
        callback().mapLeft(this::openModal)
    }

    private fun openModal(text: String) {
        MessageBox(window).apply {
            this.text = APP_NAME
            message = text
            open()
        }
    }

    private fun createComposite() = Composite(window, 0).apply {
        layoutData = GridData(GridData.FILL_HORIZONTAL)
        layoutFor(this)
    }

    private fun layoutFor(composite: Composite) = GridLayout(2, true).apply {
        marginTop = 0
        marginBottom = 4
        composite.layout = this
    }

    private fun settingLabel(composite: Composite, labelText: String) = Label(composite, SWT.RIGHT or SWT.WRAP).apply {
        text = labelText
        layoutData = GridData(GridData.FILL_HORIZONTAL)
        pack()
    }

    private fun spacer(composite: Composite) = Label(composite, SWT.NONE)

    private fun description(composite: Composite, labelText: String) = Label(composite, SWT.LEFT or SWT.WRAP).apply {
        text = labelText
        layoutData = GridData(GridData.FILL_HORIZONTAL)
        font = descriptionFont
        pack()
    }

    private fun separator(composite: Composite) = Label(composite, SWT.HORIZONTAL or SWT.SEPARATOR).apply {
        layoutData = GridData(GridData.FILL_HORIZONTAL).apply {
            horizontalSpan = 2
            verticalIndent = 4
        }
    }
}
