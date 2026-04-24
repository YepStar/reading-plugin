package com.reader.jetbrains.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class ReaderToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JComponent panel = createPanel(project);
        Content content = ContentFactory.getInstance().createContent(panel, "Reader", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static JComponent createPanel(Project project) {
        JBPanel<JBPanel<?>> root = new JBPanel<>(new BorderLayout());
        root.setBorder(JBUI.Borders.empty(12));

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);

        int row = 0;
        row = addSection(actions, row, "导入与阅读");
        row = addActionButton(project, actions, row, "打开 TXT / EPUB", "reader.OpenLocalBook");
        row = addActionButton(project, actions, row, "显示原生提示层", "reader.ShowNativeReader");
        row = addActionButton(project, actions, row, "隐藏原生提示层", "reader.HideNativeReader");

        row = addSection(actions, row, "章节控制");
        row = addActionButton(project, actions, row, "打开目录", "reader.OpenToc");
        row = addActionButton(project, actions, row, "下一章", "reader.NextChapter");
        row = addActionButton(project, actions, row, "自动下一章 开/关", "reader.ToggleAutoNextChapter");

        row = addSection(actions, row, "网页模式");
        row = addActionButton(project, actions, row, "平台网页登录", "reader.OpenPlatformBrowser");
        row = addActionButton(project, actions, row, "网页正文提取", "reader.OpenWebReader");

        row = addSection(actions, row, "快捷键");
        addText(actions, row++, "设置位置：Settings / Preferences > Keymap，搜索 Reader。");
        addText(actions, row++, "默认快捷键：Alt+Shift+O/R/H/T/N/A/B/W。");

        root.add(actions, BorderLayout.NORTH);
        return root;
    }

    private static int addSection(JPanel panel, int row, String text) {
        JBLabel label = new JBLabel(text);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
        GridBagConstraints constraints = constraints(row);
        constraints.insets = JBUI.insets(row == 0 ? 0 : 14, 0, 6, 0);
        panel.add(label, constraints);
        return row + 1;
    }

    private static int addActionButton(Project project, JPanel panel, int row, String text, String actionId) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> {
            AnAction action = ActionManager.getInstance().getAction(actionId);
            if (action != null) {
                ActionManager.getInstance().tryToExecute(action, null, button, "ReaderToolWindow", true);
            }
        });
        GridBagConstraints constraints = constraints(row);
        constraints.insets = JBUI.insets(0, 0, 6, 0);
        panel.add(button, constraints);
        return row + 1;
    }

    private static void addText(JPanel panel, int row, String text) {
        JBLabel label = new JBLabel("<html><body style='width: 260px;'>" + escape(text) + "</body></html>");
        GridBagConstraints constraints = constraints(row);
        constraints.insets = JBUI.insets(0, 0, 4, 0);
        panel.add(label, constraints);
    }

    private static GridBagConstraints constraints(int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        return constraints;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
