package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.reader.jetbrains.model.Book;
import com.reader.jetbrains.parser.WebTextExtractor;
import com.reader.jetbrains.state.ReaderStateService;
import com.reader.jetbrains.ui.ReaderHintController;
import org.jetbrains.annotations.NotNull;

public final class OpenWebReaderAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        String url = Messages.showInputDialog(project, "Novel page URL", "Web Reader", Messages.getQuestionIcon(), "https://", null);
        if (url == null || url.isBlank()) {
            return;
        }
        String regex = ReaderActionUtil.askChapterRegex(project);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading web reader page", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    Book book = WebTextExtractor.extract(normalizeUrl(url), regex);
                    project.getService(ReaderStateService.class).load(book);
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        Editor editor = ReaderActionUtil.editor(event, project);
                        if (editor != null) {
                            ReaderHintController.show(project, editor);
                        }
                        Messages.showInfoMessage(project, "Loaded " + book.chapters().size() + " readable sections.", "Reader");
                    });
                } catch (Exception exception) {
                    javax.swing.SwingUtilities.invokeLater(() ->
                            Messages.showErrorDialog(project, exception.getMessage(), "Failed to Load Web Page")
                    );
                }
            }
        });
    }

    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") ? trimmed : "https://" + trimmed;
    }
}
