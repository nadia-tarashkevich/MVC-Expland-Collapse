package com.github.nadiatarashkevich.mvcexplandcollapse.actions

import com.github.nadiatarashkevich.mvcexplandcollapse.settings.MvcSettingsConfigurable
import com.github.nadiatarashkevich.mvcexplandcollapse.settings.MvcSettingsState
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.TreePath

class MvcExpandCollapseActionGroup : ActionGroup(), com.intellij.openapi.project.DumbAware {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val folderNames = MvcSettingsState.instance.state.folderNames
        val actionManager = ActionManager.getInstance()

        return folderNames.indices.mapNotNull { i ->
            val actionId = "MvcExpand_${i + 1}"
            actionManager.getAction(actionId)
        }.toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

abstract class MvcExpandActionBase(private val index: Int) : AnAction() {
    private var consecutiveMisses = 0

    override fun update(e: AnActionEvent) {
        val project = e.project
        val folderNames = MvcSettingsState.instance.state.folderNames
        val folderName = folderNames.getOrNull(index)

        if (folderName == null || project == null) {
            e.presentation.isVisible = false
            return
        }

        e.presentation.isVisible = true
        e.presentation.text = getShortName(folderName)

        val projectView = ProjectView.getInstance(project)
        val currentPane = projectView.currentProjectViewPane
        val tree = currentPane?.tree

        if (tree != null) {
            val found = findFolders(tree, folderName)
            val isAllExpanded = found.isNotEmpty() && found.all { tree.isExpanded(it) }
            val shortName = getShortName(folderName)
            e.presentation.icon = MvcStateIcon(shortName, isAllExpanded)
        } else {
            val shortName = getShortName(folderName)
            e.presentation.icon = MvcStateIcon(shortName, true)
        }
    }

    private fun getShortName(folderName: String): String {
        return when (folderName.lowercase()) {
            "app" -> "App"
            "controllers" -> "Ctr"
            "views" -> "View"
            else -> folderName
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val folderNames = MvcSettingsState.instance.state.folderNames
        val targetFolderName = folderNames.getOrNull(index) ?: return

        val settings = MvcSettingsState.instance.state
        if (settings.firstLaunch) {
            settings.firstLaunch = false
            ShowSettingsUtil.getInstance().showSettingsDialog(project, MvcSettingsConfigurable::class.java)
            return
        }

        val projectView = ProjectView.getInstance(project)
        val currentPane = projectView.currentProjectViewPane ?: return
        val tree = currentPane.tree ?: return

        val found = findFolders(tree, targetFolderName)
        if (found.isNotEmpty()) {
            consecutiveMisses = 0
            val isAllExpanded = found.all { tree.isExpanded(it) }

            if (isAllExpanded) {
                // Already expanded, so "to fold" (collapse) - Requirement: collapse a folder, leave the rest as is
                found.forEach { tree.collapsePath(it) }
            } else {
                // Not all expanded, so "to unfold" (expand) - Requirement: expand a folder and all of its subfolders. Collapse the rest.
                // 1. Collapse all first to "collapse the rest"
                TreeUtil.collapseAll(tree, 1)

                // 2. Expand target and all subfolders
                found.forEach { path ->
                    // Use TreeUtil to expand all children of this path
                    expandRecursively(tree, path)
                }
            }
        } else {
            consecutiveMisses++
            if (consecutiveMisses >= 3) {
                consecutiveMisses = 0
                ShowSettingsUtil.getInstance().showSettingsDialog(project, MvcSettingsConfigurable::class.java)
            }
        }
    }

    private fun expandRecursively(tree: JTree, path: TreePath) {
        tree.expandPath(path)
        val model = tree.model
        val node = path.lastPathComponent
        val childCount = model.getChildCount(node)
        for (i in 0 until childCount) {
            val child = model.getChild(node, i)
            val childPath = path.pathByAddingChild(child)
            expandRecursively(tree, childPath)
        }
    }

    private fun findFolders(tree: JTree, folderName: String): List<TreePath> {
        val model = tree.model
        val root = model.root ?: return emptyList()
        val result = mutableListOf<TreePath>()
        searchRecursively(model, TreePath(root), folderName, result)
        return result
    }

    private fun searchRecursively(model: javax.swing.tree.TreeModel, parentPath: TreePath, targetName: String, result: MutableList<TreePath>): Boolean {
        val parent = parentPath.lastPathComponent
        val childCount = model.getChildCount(parent)
        var foundInThisBranch = false

        for (i in 0 until childCount) {
            val child = model.getChild(parent, i)
            val childPath = parentPath.pathByAddingChild(child)
            val nodeName = child.toString()

            if (nodeName.equals(targetName, ignoreCase = true)) {
                result.add(childPath)
                foundInThisBranch = true
            } else {
                if (searchRecursively(model, childPath, targetName, result)) {
                    foundInThisBranch = true
                }
            }
        }
        return foundInThisBranch
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
class MvcExpandAction1 : MvcExpandActionBase(0)
class MvcExpandAction2 : MvcExpandActionBase(1)
class MvcExpandAction3 : MvcExpandActionBase(2)
class MvcExpandAction4 : MvcExpandActionBase(3)
class MvcExpandAction5 : MvcExpandActionBase(4)

class MvcStateIcon(private val text: String, private val isExpanded: Boolean) : Icon {
    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as java.awt.Graphics2D
        try {
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = UIUtil.getLabelForeground()
            val font = JBUI.Fonts.miniFont()
            g2.font = font
            val fm = g2.fontMetrics
            val textWidth = fm.stringWidth(text)
            val textHeight = fm.ascent

            val iconWidth = iconWidth
            val iconHeight = iconHeight

            // Draw text centered horizontally, shifted left to make room for symbols
            val symbolGap = 4
            val symbolWidth = 6
            val totalWidth = textWidth + symbolGap + symbolWidth

            val startX = x + (iconWidth - totalWidth) / 2
            val drawY = y + (iconHeight + textHeight) / 2 - 2

            g2.drawString(text, startX, drawY)

            // Draw symbols (chevrons)
            val symbolX = startX + textWidth + symbolGap
            val centerY = y + iconHeight / 2

            g2.stroke = java.awt.BasicStroke(1.2f)
            if (isExpanded) {
                // Collapse sign (chevrons pointing in)
                // Upper chevron pointing down
                g2.drawLine(symbolX, centerY - 4, symbolX + 3, centerY - 1)
                g2.drawLine(symbolX + 3, centerY - 1, symbolX + 6, centerY - 4)
                // Lower chevron pointing up
                g2.drawLine(symbolX, centerY + 4, symbolX + 3, centerY + 1)
                g2.drawLine(symbolX + 3, centerY + 1, symbolX + 6, centerY + 4)
            } else {
                // Expand sign (chevrons pointing out)
                // Upper chevron pointing up
                g2.drawLine(symbolX, centerY - 1, symbolX + 3, centerY - 4)
                g2.drawLine(symbolX + 3, centerY - 4, symbolX + 6, centerY - 1)
                // Lower chevron pointing down
                g2.drawLine(symbolX, centerY + 1, symbolX + 3, centerY + 4)
                g2.drawLine(symbolX + 3, centerY + 4, symbolX + 6, centerY + 1)
            }
        } finally {
            g2.dispose()
        }
    }

    override fun getIconWidth(): Int = 40
    override fun getIconHeight(): Int = 16
}
