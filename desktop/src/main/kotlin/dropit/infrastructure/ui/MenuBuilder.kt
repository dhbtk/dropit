package dropit.infrastructure.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell

typealias SelectAction = () -> Unit

class MenuBuilder {
    private data class MenuData(
        val title: String,
        val entries: List<Pair<String?, SelectAction?>>
    )

    private val menus = ArrayList<MenuData>()

    fun menu(title: String, vararg entries: Pair<String?, SelectAction?>): MenuBuilder {
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
            entries.forEach { (entryName, action) ->
                if (entryName == null && action == null) {
                    MenuItem(menu, SWT.SEPARATOR)
                } else if (entryName != null) {
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
        shell.menuBar = menuBar
    }
}