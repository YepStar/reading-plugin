package com.reader.jetbrains.ui;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.reader.jetbrains.model.Book;
import com.reader.jetbrains.parser.BookParser;
import com.reader.jetbrains.parser.WebTextExtractor;
import com.reader.jetbrains.state.ReaderStateService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class BrowserDialog extends DialogWrapper {
    private final Project project;
    private final String initialUrl;
    private final List<JBCefJSQuery> pendingQueries = new ArrayList<>();
    private JBCefBrowser browser;

    public BrowserDialog(Project project, String url) {
        super(project, false);
        this.project = project;
        this.initialUrl = url;
        setTitle("Reader-plugin-yip 平台网页");
        setOKButtonText("关闭");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(1100, 760));

        JButton importButton = new JButton("将当前页面导入原生提示层");
        importButton.addActionListener(event -> importCurrentPage());
        panel.add(importButton, BorderLayout.NORTH);

        if (!JBCefApp.isSupported()) {
            panel.add(new JLabel("当前 IDE 运行时不支持内嵌浏览器。"), BorderLayout.CENTER);
            return panel;
        }

        browser = new JBCefBrowser(initialUrl);
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
        for (JBCefJSQuery query : pendingQueries) {
            query.dispose();
        }
        pendingQueries.clear();
        super.dispose();
    }

    private void importCurrentPage() {
        if (browser == null) {
            Messages.showWarningDialog(project, "当前没有可导入的网页。", "Reader-plugin-yip");
            return;
        }

        JBCefJSQuery query = JBCefJSQuery.create(browser);
        pendingQueries.add(query);
        query.addHandler(payload -> {
            SwingUtilities.invokeLater(() -> {
                pendingQueries.remove(query);
                query.dispose();
                importPayload(payload);
            });
            return new JBCefJSQuery.Response(null);
        });

        String expression = """
                encodeURIComponent(location.href) + '\\n' +
                encodeURIComponent(document.title || '') + '\\n' +
                encodeURIComponent(document.documentElement.outerHTML || '')
                """;
        browser.getCefBrowser().executeJavaScript(
                query.inject(expression),
                browser.getCefBrowser().getURL(),
                0
        );
    }

    private void importPayload(String payload) {
        String[] parts = payload == null ? new String[0] : payload.split("\\n", 3);
        if (parts.length < 3) {
            Messages.showWarningDialog(project, "未能读取当前网页内容。", "Reader-plugin-yip");
            return;
        }

        String url = decode(parts[0]);
        String title = decode(parts[1]);
        String html = decode(parts[2]);
        project.getService(ReaderStateService.class).setLastPlatformUrl(url);

        try {
            Book book = WebTextExtractor.extractFromHtml(url, title, html, BookParser.DEFAULT_CHAPTER_REGEX);
            project.getService(ReaderStateService.class).load(book);
            var editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor != null) {
                ReaderHintController.show(project, editor);
            }
            Messages.showInfoMessage(project, "已将当前网页导入原生提示层。", "Reader-plugin-yip");
        } catch (Exception exception) {
            Messages.showErrorDialog(project, exception.getMessage(), "导入网页失败");
        }
    }

    private void rememberCurrentUrl() {
        if (browser != null && browser.getCefBrowser() != null) {
            String url = browser.getCefBrowser().getURL();
            project.getService(ReaderStateService.class).setLastPlatformUrl(url);
        }
    }

    private static String decode(String text) {
        return URLDecoder.decode(text == null ? "" : text, StandardCharsets.UTF_8);
    }
}
