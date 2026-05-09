package com.reader.jetbrains.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import com.reader.jetbrains.settings.ReaderSettingsService;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;

public final class BrowserDialog extends DialogWrapper {
    private final Project project;
    private final String initialUrl;
    private JBCefBrowser browser;
    private JBCefClient client;

    public BrowserDialog(Project project, String url) {
        super(project, false);
        this.project = project;
        this.initialUrl = url;
        setTitle("Reader Yip 平台网页");
        setOKButtonText("关闭");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(1100, 760));

        if (!JBCefApp.isSupported()) {
            panel.add(new JLabel("当前 IDE 运行时不支持内嵌浏览器。"), BorderLayout.CENTER);
            return panel;
        }

        client = JBCefApp.getInstance().createClient();
        browser = JBCefBrowser.createBuilder()
                .setClient(client)
                .setUrl(initialUrl)
                .setCreateImmediately(false)
                .build();
        browser.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser cefBrowser, CefFrame frame, String targetUrl, String targetFrameName) {
                if (targetUrl != null && !targetUrl.isBlank()) {
                    SwingUtilities.invokeLater(() -> {
                        browser.loadURL(targetUrl);
                        ReaderSettingsService.getInstance().setLastPlatformUrl(targetUrl);
                    });
                }
                return true;
            }
        }, browser.getCefBrowser());
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                rememberUrl(cefBrowser);
            }
        }, browser.getCefBrowser());
        panel.add(browser.getComponent(), BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    protected void doOKAction() {
        rememberCurrentUrl();
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        rememberCurrentUrl();
        super.doCancelAction();
    }

    @Override
    public void dispose() {
        rememberCurrentUrl();
        if (client != null) {
            client.dispose();
            client = null;
        }
        super.dispose();
    }

    private void rememberCurrentUrl() {
        if (browser != null && browser.getCefBrowser() != null) {
            rememberUrl(browser.getCefBrowser());
        }
    }

    private void rememberUrl(CefBrowser cefBrowser) {
        if (cefBrowser == null) {
            return;
        }
        String url = cefBrowser.getURL();
        ReaderSettingsService.getInstance().setLastPlatformUrl(url);
    }

}
