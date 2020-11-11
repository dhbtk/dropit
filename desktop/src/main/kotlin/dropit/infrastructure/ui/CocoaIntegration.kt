package dropit.infrastructure.ui

import java.lang.reflect.Method

const val ACTIVATION_POLICY_REGULAR = 0L
const val ACTIVATION_POLICY_PROHIBITED = 2L

class CocoaIntegration : GuiIntegrations {
    override var quitCallback = nullCallback
    override var aboutCallback = nullCallback
    override var preferencesCallback = nullCallback
    private val nsApplication: Any
    private val setActivationPolicy: Method
    var openWindows = 0

    init {
        // we don't care if any of these fail because that means we have serious classpath issues
        val nsAppClass = Class.forName("org.eclipse.swt.internal.cocoa.NSApplication")
        val sharedAppMethod = nsAppClass.getMethod("sharedApplication")
        nsApplication = sharedAppMethod.invoke(null)
        setActivationPolicy = nsAppClass.getMethod("setActivationPolicy", Long::class.javaPrimitiveType)
    }

    override fun afterDisplayInit() {
    }

    override fun onGuiInit(isFirstStart: Boolean) {
        if (!isFirstStart) {
            setActivationPolicy.invoke(nsApplication, ACTIVATION_POLICY_PROHIBITED)
        }
    }

    override fun beforeWindowOpen() {
        openWindows++
        setActivationPolicy.invoke(nsApplication, ACTIVATION_POLICY_REGULAR)
    }

    override fun afterWindowClose() {
        openWindows--
        if(openWindows == 0) {
            setActivationPolicy.invoke(nsApplication, ACTIVATION_POLICY_PROHIBITED)
        }
    }
}
