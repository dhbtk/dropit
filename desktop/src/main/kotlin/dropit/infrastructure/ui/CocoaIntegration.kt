package dropit.infrastructure.ui

import java.lang.reflect.Method

class CocoaIntegration : GuiIntegrations {
    override var quitCallback = {}
    override var aboutCallback = {}
    override var preferencesCallback = {}
    private val nsApplication: Any
    private val setActivationPolicy: Method
    private var openWindows = 0

    init {
        val nsAppClass = Class.forName("org.eclipse.swt.internal.cocoa.NSApplication")
        val sharedAppMethod = nsAppClass.getMethod("sharedApplication")
        nsApplication = sharedAppMethod.invoke(null)
        setActivationPolicy = nsAppClass.getMethod("setActivationPolicy", Long::class.javaPrimitiveType)
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

    companion object {
        private const val ACTIVATION_POLICY_REGULAR = 0L
        private const val ACTIVATION_POLICY_PROHIBITED = 2L
    }
}
