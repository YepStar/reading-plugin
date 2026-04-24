package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.reader.jetbrains.model.Book;
import com.reader.jetbrains.parser.BookParser;
import com.reader.jetbrains.state.ReaderStateService;
import com.reader.jetbrains.ui.ReaderHintController;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class OpenLocalBookAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withFileFilter(file -> {
                    String name = file.getName().toLowerCase();
                    return name.endsWith(".txt") || name.endsWith(".epub");
                })
                .withTitle("Open TXT or EPUB Book");
        VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
        if (file == null) {
            return;
        }

        String regex = ReaderActionUtil.askChapterRegex(project);
        try {
            Book book = BookParser.parse(Path.of(file.getPath()), regex, ReaderActionUtil.askCharset(project));
            project.getService(ReaderStateService.class).load(book);
            Editor editor = ReaderActionUtil.editor(event, project);
            if (editor != null) {
                ReaderHintController.show(project, editor);
            }
            Messages.showInfoMessage(project, "Loaded " + book.chapters().size() + " chapters.", "Reader");
        } catch (Exception exception) {
            Messages.showErrorDialog(project, exception.getMessage(), "Failed to Open Book");
        }
    }
}
