package com.reader.jetbrains.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.reflect.Method;

public final class BrowserDialog extends DialogWrapper {
    private final String url;
    private JComponent browserComponent;

    public BrowserDialog(Project project, String url) {
        super(project);
        this.url = url;
        setTitle("Reader Browser");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(1100, 760));
        browserComponent = createBrowserComponent(url);
        panel.add(browserComponent, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createBrowserComponent(String url) {
        try {
            Class<?> appClass = Class.forName("com.intellij.ui.jcef.JBCefApp");
            Method isSupported = appClass.getMethod("isSupported");
            if (!(Boolean) isSupported.invoke(null)) {
                return new JLabel("JCEF is not supported in this IDE runtime.");
            }

            Class<?> browserClass = Class.forName("com.intellij.ui.jcef.JBCefBrowser");
            Object browser = browserClass.getConstructor(String.class).newInstance(url);
            Method getComponent = browserClass.getMethod("getComponent");
            return (JComponent) getComponent.invoke(browser);
        } catch (ReflectiveOperationException exception) {
            Messages.showWarningDialog("Embedded browser is unavailable in this IDE runtime.", "Reader Browser");
            return new JLabel(url);
        }
    }
}
