package dropit.ui

import dagger.Module
import dagger.Provides
import org.eclipse.swt.widgets.Display
import javax.inject.Singleton

@Module
class UIModule {
    @Provides
    @Singleton
    fun display() = Display.getDefault()
}