package dropit.infrastructure.ui

import dropit.APP_NAME
import org.eclipse.swt.widgets.Display
import java.lang.reflect.Method

const val ACTIVATION_POLICY_REGULAR = 0L
const val ACTIVATION_POLICY_PROHIBITED = 2L

class CocoaIntegration : GuiIntegrations {
    override var aboutCallback = nullCallback
    override var preferencesCallback = nullCallback
    private val cocoaUIEnhancer = CocoaUIEnhancer(APP_NAME)
    private val nsApplication: Any
    private val setActivationPolicy: Method

    init {
        // we don't care if any of these fail because that means we have serious classpath issues
        val nsAppClass = Class.forName("org.eclipse.swt.internal.cocoa.NSApplication")
        val sharedAppMethod = nsAppClass.getMethod("sharedApplication")
        nsApplication = sharedAppMethod.invoke(null)
        setActivationPolicy = nsAppClass.getMethod("setActivationPolicy", Long::class.javaPrimitiveType)
    }

    override fun afterDisplayInit() {
        cocoaUIEnhancer.hookApplicationMenu(
            Display.getDefault(),
            {
                // todo
            },
            {
                aboutCallback()
            },
            {
                preferencesCallback()
            }
        )
    }

    override fun onGuiInit(isFirstStart: Boolean) {
        if (!isFirstStart) {
            setActivationPolicy.invoke(nsApplication, ACTIVATION_POLICY_PROHIBITED)
        }
    }

    override fun beforeMainWindowOpen() {
        setActivationPolicy.invoke(nsApplication, ACTIVATION_POLICY_REGULAR)
    }

    override fun afterMainWindowClose() {
        setActivationPolicy.invoke(nsApplication, ACTIVATION_POLICY_PROHIBITED)
    }
}
