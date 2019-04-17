package dropit.infrastructure.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell
import org.slf4j.LoggerFactory

typealias SelectAction = () -> Unit

class MenuBuilder(val display: Display) {
    private data class MenuData(
        val title: String,
        val entries: List<Triple<String?, SelectAction?, Int?>>
    )

    private val menus = ArrayList<MenuData>()

    fun menu(title: String, vararg entries: Triple<String?, SelectAction?, Int?>): MenuBuilder {
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
            entries.forEach { (entryName, action, id) ->
                if (entryName == null && action == null) {
                    MenuItem(menu, SWT.SEPARATOR)
                } else if (entryName != null) {
                    val systemMenu = display.systemMenu?.items?.find { it.id == id }
                    if (systemMenu != null) {
                        systemMenu.addListener(SWT.Selection) {
                            LoggerFactory.getLogger(this::class.java).info("System menu clicked - ${systemMenu.id}")
                            if (action != null) {
                                action()
                            }
                        }
                    } else {
                        MenuItem(menu, SWT.PUSH)
                            .apply {
                                text = entryName
                                if (action != null) {
                                    addListener(SWT.Selection) {
                                        action()
                                    }
                                }
                            }
                    }
                }
            }
        }
        shell.menuBar = menuBar
    }
}
