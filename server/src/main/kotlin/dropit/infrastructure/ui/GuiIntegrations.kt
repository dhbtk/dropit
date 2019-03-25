package dropit.infrastructure.ui

val nullCallback = {}

interface GuiIntegrations {
    var aboutCallback: () -> Unit
    var preferencesCallback: () -> Unit

    fun afterDisplayInit()

    fun onGuiInit(isFirstStart: Boolean)

    fun beforeMainWindowOpen()

    fun afterMainWindowClose()

    class Default : GuiIntegrations {
        override var aboutCallback = nullCallback
        override var preferencesCallback = nullCallback

        override fun afterDisplayInit() {

        }

        override fun onGuiInit(isFirstStart: Boolean) {

        }

        override fun beforeMainWindowOpen() {

        }

        override fun afterMainWindowClose() {

        }


    }
}
