package com.github.nadiatarashkevich.mvcexplandcollapse.settings

import com.github.nadiatarashkevich.mvcexplandcollapse.MyBundle
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class MvcSettingsConfigurable : Configurable {
    private var mySettingsComponent: JPanel? = null
    private val myFolderFields = Array(5) { JBTextField() }
    private val myShortcutLabels = Array(5) { JBLabel() }

    override fun getDisplayName(): String = MyBundle.message("mvc.settings.title")

    override fun createComponent(): JComponent {
        val descriptionArea = JBTextArea(MyBundle.message("mvc.settings.description"))
        descriptionArea.isEditable = false
        descriptionArea.isFocusable = false
        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        descriptionArea.background = UIUtil.getPanelBackground()
        descriptionArea.font = UIUtil.getLabelFont()
        descriptionArea.margin = JBUI.insetsBottom(10)

        val formBuilder = FormBuilder.createFormBuilder()
            .addComponent(descriptionArea)
            .addVerticalGap(10)

        for (i in 0 until 5) {
            val panel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
            myFolderFields[i].columns = 20
            panel.add(myFolderFields[i])
            panel.add(myShortcutLabels[i])

            formBuilder.addLabeledComponent("Folder name ${i + 1}:", panel)
        }

        val configureKeymapButton = JButton(MyBundle.message("mvc.settings.configure_keymap"))
        configureKeymapButton.addActionListener {
            val project = null // Or try to find a project context if possible, but null is often accepted
            com.intellij.ide.actions.ShowSettingsUtilImpl.showSettingsDialog(project, "Keymap", "expand/collapse")
        }
        formBuilder.addComponent(configureKeymapButton)

        mySettingsComponent = formBuilder.panel
        return mySettingsComponent!!
    }

    override fun isModified(): Boolean {
        val settings = MvcSettingsState.instance.state
        val currentNames = myFolderFields.map { it.text.trim() }.filter { it.isNotEmpty() }
        return currentNames != settings.folderNames
    }

    override fun apply() {
        val settings = MvcSettingsState.instance.state
        settings.folderNames = myFolderFields.map { it.text.trim() }.filter { it.isNotEmpty() }
        updateShortcuts()
    }

    override fun reset() {
        val settings = MvcSettingsState.instance.state
        for (i in 0 until 5) {
            myFolderFields[i].text = settings.folderNames.getOrNull(i) ?: ""
        }
        updateShortcuts()
    }

    private fun updateShortcuts() {
        val actionManager = ActionManager.getInstance()
        val settings = MvcSettingsState.instance.state
        for (i in 0 until 5) {
            val folderName = settings.folderNames.getOrNull(i)
            if (folderName != null) {
                val actionId = "MvcExpand_${i + 1}"
                val action = actionManager.getAction(actionId)
                val shortcutText = action?.shortcutSet?.shortcuts?.firstOrNull()?.let {
                    KeymapUtil.getShortcutText(it)
                } ?: MyBundle.message("mvc.settings.no_shortcut")
                myShortcutLabels[i].text = " ($shortcutText)"
            } else {
                myShortcutLabels[i].text = ""
            }
        }
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
