package com.reader.jetbrains.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Objects;

final class ReaderDialogs {
    private ReaderDialogs() {
    }

    static int chooseIndex(Project project, String title, String message, String[] values, String initialValue) {
        if (values.length == 0) {
            return -1;
        }
        JBList<String> list = new JBList<>(values);
        int selectedIndex = indexOf(values, initialValue);
        list.setSelectedIndex(selectedIndex);
        list.ensureIndexIsVisible(selectedIndex);
        list.setBorder(JBUI.Borders.empty());

        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(420, Math.min(360, Math.max(120, values.length * 30))));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JBLabel(message), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        DialogBuilder builder = new DialogBuilder(project);
        builder.setTitle(title);
        builder.setCenterPanel(panel);
        if (!builder.showAndGet()) {
            return -1;
        }
        return list.getSelectedIndex();
    }

    static String editableChoice(Project project, String title, String message, String[] values, String initialValue) {
        JComboBox<String> comboBox = new JComboBox<>(values);
        comboBox.setEditable(true);
        comboBox.setSelectedItem(initialValue);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(520, 80));
        panel.add(new JBLabel(message), BorderLayout.NORTH);
        panel.add(comboBox, BorderLayout.CENTER);

        DialogBuilder builder = new DialogBuilder(project);
        builder.setTitle(title);
        builder.setCenterPanel(panel);
        if (!builder.showAndGet()) {
            return null;
        }
        Object selected = comboBox.getSelectedItem();
        return selected == null ? null : selected.toString();
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (Objects.equals(values[i], value)) {
                return i;
            }
        }
        return 0;
    }
}
