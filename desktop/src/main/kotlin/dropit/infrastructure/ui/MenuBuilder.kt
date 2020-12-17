package dropit.infrastructure.ui

import dropit.ui.DesktopIntegrations
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell

typealias SelectAction = () -> Unit

class MenuBuilder(val display: Display, private val menus: MutableList<MenuData> = ArrayList()) {
    data class MenuData(
        val title: String,
        val entries: List<MenuEntry>
    )

    fun menu(title: String, vararg entries: MenuEntry): MenuBuilder {
        menus.add(MenuData(title, entries.asList()))
        return this
    }

    fun build(shell: Shell) {
        val menuBar = Menu(shell, SWT.BAR)
        menus.forEach { (title, entries) ->
            val menu = Menu(shell, SWT.DROP_DOWN)
            MenuItem(menuBar, SWT.CASCADE)
                .apply {
                    text = title
                    this.menu = menu
                }
            entries.forEach { it.create(menu) }
        }
        shell.menuBar = menuBar
    }

    interface MenuEntry {
        fun create(menu: Menu)
    }

    class Item(
        private val title: String,
        val action: SelectAction = {},
        private val systemMenuId: Int? = null,
        private val accelerator: Int? = null
    ) : MenuEntry {
        val correctedTitle
            get() = if (DesktopIntegrations().currentOS == DesktopIntegrations.OperatingSystem.MACOSX) {
                title.replace("Ctrl+", "⌘+")
            } else {
                title
            }

        override fun create(menu: Menu) {
            val sysMenuItem = menu.display.systemMenu?.items?.find { it.id == systemMenuId }
            if (sysMenuItem != null) {
                sysMenuItem.addListener(SWT.Selection) { action() }
            } else {
                MenuItem(menu, SWT.PUSH).apply {
                    text = correctedTitle
                    accelerator = this@Item.accelerator ?: 0
                    addListener(SWT.Selection) { action() }
                }
            }
        }
    }

    class Separator : MenuEntry {
        override fun create(menu: Menu) {
            MenuItem(menu, SWT.SEPARATOR)
        }
    }
}

class MenuBarBuilderDsl {
    val menus = ArrayList<MenuBuilder.MenuData>()

    fun menu(title: String, action: MenuBuilderDsl.() -> Unit) {
        val menuBuilder = MenuBuilderDsl()
        action(menuBuilder)
        menus.add(MenuBuilder.MenuData(title, menuBuilder.items))
    }
}

class MenuBuilderDsl {
    val items = ArrayList<MenuBuilder.MenuEntry>()

    fun item(
        title: String,
        action: SelectAction = {},
        accelerator: Int? = null,
        systemMenuId: Int? = null
    ) {
        items.add(MenuBuilder.Item(title, action, systemMenuId, accelerator))
    }

    fun separator() {
        items.add(MenuBuilder.Separator())
    }
}

fun windowMenu(window: Shell, action: MenuBarBuilderDsl.() -> Unit) {
    val dsl = MenuBarBuilderDsl().also { action(it) }
    MenuBuilder(window.display, dsl.menus).build(window)
}
