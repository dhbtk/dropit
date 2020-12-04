package dropit.ui

import dropit.infrastructure.ui.GuiIntegrations
import javax.inject.Provider
import kotlin.reflect.KProperty

class WindowDelegate<T : ShellContainer>(
    private val guiIntegrations: GuiIntegrations,
    private val provider: Provider<T>
) {
    private lateinit var window: T
    operator fun getValue(thisRef: GraphicalInterface, property: KProperty<*>): T {
        if (!this::window.isInitialized) return createWindow()

        return if (window.window.shell != null && !window.window.shell.isDisposed) {
            window
        } else {
            createWindow()
        }
    }

    private fun createWindow(): T {
        guiIntegrations.beforeWindowOpen()
        window = provider.get()

        return window
    }
}
