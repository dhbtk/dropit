package dropit.ui

import dagger.Module
import dagger.Provides
import dropit.infrastructure.ui.GuiIntegrations
import org.eclipse.swt.widgets.Display
import javax.inject.Singleton

@Module
class UIModule {
    @Provides
    @Singleton
    fun display() = Display.getDefault()

    @Provides
    @Singleton
    fun guiIntegrations(desktopIntegrations: DesktopIntegrations): GuiIntegrations {
        return desktopIntegrations.buildGuiIntegrations()
    }
}
