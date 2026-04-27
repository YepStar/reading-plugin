package com.reader.jetbrains.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import com.reader.jetbrains.state.ReaderStateService;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.concurrent.atomic.AtomicReference;

public final class PlatformBrowserPopup {
    private PlatformBrowserPopup() {
    }

    public static void show(Project project, String url) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(520, 360));
        AtomicReference<JBPopup> popupRef = new AtomicReference<>();

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        toolbar.setBorder(JBUI.Borders.empty(2, 4));
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(event -> {
            JBPopup popup = popupRef.get();
            if (popup != null) {
                popup.cancel();
            }
        });
        toolbar.add(closeButton);
        panel.add(toolbar, BorderLayout.NORTH);

        if (!JBCefApp.isSupported()) {
            panel.add(new JLabel("当前 IDE 运行时不支持内嵌浏览器。"), BorderLayout.CENTER);
            JBPopup popup = popup(panel);
            popupRef.set(popup);
            popup.showCenteredInCurrentWindow(project);
            return;
        }

        JBCefBrowser browser = JBCefBrowser.createBuilder()
                .setUrl(url)
                .setCreateImmediately(false)
                .build();
        browser.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser cefBrowser, CefFrame frame, String targetUrl, String targetFrameName) {
                if (targetUrl != null && !targetUrl.isBlank()) {
                    SwingUtilities.invokeLater(() -> {
                        browser.loadURL(targetUrl);
                        project.getService(ReaderStateService.class).setLastPlatformUrl(targetUrl);
                    });
                }
                return true;
            }
        }, browser.getCefBrowser());
        panel.add(browser.getComponent(), BorderLayout.CENTER);

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, browser.getComponent())
                .setTitle("Reader Yip 网页浮窗")
                .setMovable(true)
                .setResizable(true)
                .setCancelOnClickOutside(false)
                .setRequestFocus(true)
                .setCancelCallback(() -> {
                    String currentUrl = browser.getCefBrowser().getURL();
                    project.getService(ReaderStateService.class).setLastPlatformUrl(currentUrl);
                    browser.dispose();
                    return true;
                })
                .createPopup();
        popupRef.set(popup);
        popup.showCenteredInCurrentWindow(project);
    }

    private static JBPopup popup(JPanel panel) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, panel)
                .setTitle("Reader Yip 网页浮窗")
                .setMovable(true)
                .setResizable(true)
                .createPopup();
    }
}
