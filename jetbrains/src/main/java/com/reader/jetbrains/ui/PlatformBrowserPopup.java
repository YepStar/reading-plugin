package com.reader.jetbrains.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.reader.jetbrains.settings.ReaderSettingsService;
import com.reader.jetbrains.state.ReaderStateService;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Window;

public final class PlatformBrowserPopup {
    private PlatformBrowserPopup() {
    }

    public static void show(Project project, String url) {
        JPanel panel = new JPanel(new BorderLayout());
        ReaderSettingsService settings = ReaderSettingsService.getInstance();
        panel.setPreferredSize(settings.platformPopupSize());

        if (!JBCefApp.isSupported()) {
            panel.add(new JLabel("当前 IDE 运行时不支持内嵌浏览器。"), BorderLayout.CENTER);
            JBPopup popup = popup(panel, settings);
            showPopup(project, popup, settings.platformPopupLocation());
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
                    saveBounds(panel, settings);
                    browser.dispose();
                    return true;
                })
                .createPopup();
        showPopup(project, popup, settings.platformPopupLocation());
    }

    private static JBPopup popup(JPanel panel, ReaderSettingsService settings) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, panel)
                .setTitle("Reader Yip 网页浮窗")
                .setMovable(true)
                .setResizable(true)
                .setCancelCallback(() -> {
                    saveBounds(panel, settings);
                    return true;
                })
                .createPopup();
    }

    private static void showPopup(Project project, JBPopup popup, Point location) {
        if (location == null) {
            popup.showCenteredInCurrentWindow(project);
            return;
        }
        Component owner = owner(project);
        if (owner == null) {
            popup.showCenteredInCurrentWindow(project);
            return;
        }
        popup.showInScreenCoordinates(owner, location);
    }

    private static Component owner(Project project) {
        IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
        return frame == null ? null : frame.getComponent();
    }

    private static void saveBounds(JPanel panel, ReaderSettingsService settings) {
        Window window = SwingUtilities.getWindowAncestor(panel);
        if (window != null) {
            Dimension contentSize = panel.getSize();
            if (contentSize.width <= 0 || contentSize.height <= 0) {
                contentSize = panel.getPreferredSize();
            }
            try {
                settings.savePlatformPopupBounds(window.getLocationOnScreen(), contentSize);
            } catch (IllegalComponentStateException ignored) {
                settings.savePlatformPopupBounds(window.getLocation(), contentSize);
            }
        }
    }
}
