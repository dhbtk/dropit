package dropit.infrastructure.ui

interface GuiIntegrations {
    fun onGuiInit(isFirstStart: Boolean)

    fun beforeMainWindowOpen()

    fun afterMainWindowClose()

    class Default : GuiIntegrations {
        override fun onGuiInit(isFirstStart: Boolean) {

        }

        override fun beforeMainWindowOpen() {

        }

        override fun afterMainWindowClose() {

        }

    }
}
