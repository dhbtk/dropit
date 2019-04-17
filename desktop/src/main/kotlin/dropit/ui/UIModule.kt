package dropit.ui

import dagger.Module
import dagger.Provides
import dropit.APP_NAME
import dropit.infrastructure.ui.GuiIntegrations
import org.eclipse.swt.widgets.Display
import javax.inject.Singleton

@Module
class UIModule {
    @Provides
    @Singleton
    fun display(guiIntegrations: GuiIntegrations): Display {
        Display.setAppName(APP_NAME)
        return Display.getDefault().apply {
            guiIntegrations.afterDisplayInit()
        }
    }

    @Provides
    @Singleton
    fun guiIntegrations(desktopIntegrations: DesktopIntegrations): GuiIntegrations {
        return desktopIntegrations.buildGuiIntegrations()
    }
}
