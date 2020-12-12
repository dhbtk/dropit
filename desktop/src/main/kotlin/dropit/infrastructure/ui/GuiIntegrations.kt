package dropit.infrastructure.ui

interface GuiIntegrations {
    var quitCallback: () -> Unit
    var aboutCallback: () -> Unit
    var preferencesCallback: () -> Unit

    fun onGuiInit(isFirstStart: Boolean)

    fun beforeWindowOpen()

    fun afterWindowClose()

    class Default : GuiIntegrations {
        override var quitCallback = {}
        override var aboutCallback = {}
        override var preferencesCallback = {}

        override fun onGuiInit(isFirstStart: Boolean) {}

        override fun beforeWindowOpen() {}

        override fun afterWindowClose() {}
    }
}
