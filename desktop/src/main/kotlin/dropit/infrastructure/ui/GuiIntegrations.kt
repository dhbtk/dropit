package dropit.infrastructure.ui

val nullCallback = {}

interface GuiIntegrations {
    var quitCallback: () -> Unit
    var aboutCallback: () -> Unit
    var preferencesCallback: () -> Unit

    fun afterDisplayInit()

    fun onGuiInit(isFirstStart: Boolean)

    fun beforeWindowOpen()

    fun afterWindowClose()

    class Default : GuiIntegrations {
        override var quitCallback = nullCallback
        override var aboutCallback = nullCallback
        override var preferencesCallback = nullCallback

        override fun afterDisplayInit() {

        }

        override fun onGuiInit(isFirstStart: Boolean) {

        }

        override fun beforeWindowOpen() {

        }

        override fun afterWindowClose() {

        }


    }
}
