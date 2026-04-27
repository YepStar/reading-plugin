package com.reader.jetbrains.ui;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.reader.jetbrains.model.Book;
import com.reader.jetbrains.model.Chapter;
import com.reader.jetbrains.settings.ReaderSettingsService;
import com.reader.jetbrains.state.ReaderStateService;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.lang.reflect.Method;

public final class ReaderHintController {
    private ReaderHintController() {
    }

    public static void show(Project project, Editor editor) {
        ReaderStateService state = project.getService(ReaderStateService.class);
        Chapter chapter = state.currentChapter();
        if (chapter == null) {
            return;
        }

        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setText(toHtml(state.book(), chapter));
        pane.setCaretPosition(0);

        Color background = JBUI.CurrentTheme.Editor.Tooltip.BACKGROUND;
        pane.setBackground(background);

        JBScrollPane scrollPane = new JBScrollPane(pane);
        ReaderSettingsService settings = ReaderSettingsService.getInstance();
        scrollPane.setPreferredSize(new Dimension(settings.hintWidth(), settings.hintHeight()));
        scrollPane.setBorder(JBUI.Borders.empty());

        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(state.hintScrollValue()));
        HintManager.getInstance().showInformationHint(editor, scrollPane, () -> {
            state.setHintScrollValue(scrollPane.getVerticalScrollBar().getValue());
            state.setHintVisible(false);
        });
        state.setHintVisible(true);
    }

    public static void hide(Project project) {
        ReaderStateService state = project.getService(ReaderStateService.class);
        try {
            HintManager manager = HintManager.getInstance();
            Method hideAllHints = manager.getClass().getMethod("hideAllHints");
            hideAllHints.invoke(manager);
        } catch (ReflectiveOperationException ignored) {
            try {
                HintManager manager = HintManager.getInstance();
                Method hideHints = manager.getClass().getMethod("hideHints", int.class, boolean.class, boolean.class);
                hideHints.invoke(manager, HintManager.HIDE_BY_ESCAPE, false, false);
            } catch (ReflectiveOperationException ignoredAgain) {
                // Older IDEs differ slightly here. Escape still closes editor hints.
            }
        }
        state.setHintVisible(false);
    }

    private static String toHtml(Book book, Chapter chapter) {
        ReaderSettingsService settings = ReaderSettingsService.getInstance();
        String maxWidth = settings.maxLineChars() <= 0 ? "" : "max-width: " + settings.maxLineChars() + "em;";
        return """
                <html>
                  <body style="font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif; font-size: %dpx; margin: %dpx %dpx; %s">
                    <div style="font-size: 12px; color: #8a8f98; margin-bottom: 8px;">%s</div>
                    <h3 style="margin: 0 0 8px 0;">%s</h3>
                    <pre style="white-space: pre-wrap; word-wrap: break-word; font-family: inherit; margin: 0;">%s</pre>
                  </body>
                </html>
                """.formatted(
                settings.fontSize(),
                settings.verticalPadding(),
                settings.horizontalPadding(),
                maxWidth,
                escape(book == null ? "Reader" : book.title()),
                escape(chapter.title()),
                escape(chapter.text())
        );
    }

    private static String escape(String text) {
        return text == null ? "" : text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
