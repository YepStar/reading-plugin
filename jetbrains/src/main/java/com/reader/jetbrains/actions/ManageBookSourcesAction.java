package com.reader.jetbrains.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.reader.jetbrains.sources.BookSource;
import com.reader.jetbrains.sources.BookSourceService;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextArea;
import java.awt.Dimension;
import java.util.List;

public final class ManageBookSourcesAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        BookSourceService service = project.getService(BookSourceService.class);
        while (true) {
            String[] operations = {"新增书源 JSON", "编辑当前书源 JSON", "启用/停用书源", "设为默认书源", "删除书源", "重置内置书源", "关闭"};
            int operationIndex = ReaderDialogs.chooseIndex(
                    project,
                    "书源管理",
                    "选择书源管理操作：",
                    operations,
                    "编辑当前书源 JSON"
            );
            String operation = operationIndex < 0 ? null : operations[operationIndex];
            if (operation == null || "关闭".equals(operation)) {
                return;
            }
            try {
                if ("新增书源 JSON".equals(operation)) {
                    addSource(project, service);
                } else if ("编辑当前书源 JSON".equals(operation)) {
                    editSource(project, service);
                } else if ("启用/停用书源".equals(operation)) {
                    toggleSource(project, service);
                } else if ("设为默认书源".equals(operation)) {
                    selectSource(project, service);
                } else if ("删除书源".equals(operation)) {
                    deleteSource(project, service);
                } else if ("重置内置书源".equals(operation)) {
                    if (Messages.showYesNoDialog(project, "确定重置为内置书源？自定义书源会被清空。", "书源管理", Messages.getWarningIcon()) == Messages.YES) {
                        service.resetDefaults();
                    }
                }
            } catch (Exception exception) {
                Messages.showErrorDialog(project, exception.getMessage(), "书源管理");
            }
        }
    }

    private static void addSource(Project project, BookSourceService service) {
        String json = askJson(project, "新增书源 JSON", service.toJson(new BookSource()));
        if (json == null) {
            return;
        }
        BookSource source = service.fromJson(json);
        service.save(source);
        service.select(source.id);
    }

    private static void editSource(Project project, BookSourceService service) {
        BookSource source = chooseSource(project, service.sources(), "选择要编辑的书源");
        if (source == null) {
            return;
        }
        String json = askJson(project, "编辑书源 JSON", service.toJson(source));
        if (json == null) {
            return;
        }
        service.save(service.fromJson(json));
    }

    private static void toggleSource(Project project, BookSourceService service) {
        BookSource source = chooseSource(project, service.sources(), "选择要启用/停用的书源");
        if (source == null) {
            return;
        }
        source.enabled = !source.enabled;
        service.save(source);
    }

    private static void selectSource(Project project, BookSourceService service) {
        BookSource source = chooseSource(project, service.sources(), "选择默认书源");
        if (source != null) {
            service.select(source.id);
        }
    }

    private static void deleteSource(Project project, BookSourceService service) {
        BookSource source = chooseSource(project, service.sources(), "选择要删除的书源");
        if (source != null && Messages.showYesNoDialog(project, "删除书源：" + source.name + "？", "书源管理", Messages.getWarningIcon()) == Messages.YES) {
            service.remove(source.id);
        }
    }

    private static BookSource chooseSource(Project project, List<BookSource> sources, String title) {
        String[] labels = sources.stream().map(BookSource::toString).toArray(String[]::new);
        int choice = ReaderDialogs.chooseIndex(project, title, title + "：", labels, labels.length == 0 ? "" : labels[0]);
        if (choice < 0) {
            return null;
        }
        return sources.get(choice);
    }

    private static String askJson(Project project, String title, String initialText) {
        JTextArea area = new JTextArea(initialText, 24, 72);
        area.setLineWrap(false);
        JBScrollPane scrollPane = new JBScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(720, 520));
        DialogBuilder builder = new DialogBuilder(project);
        builder.setTitle(title);
        builder.setCenterPanel(scrollPane);
        return builder.showAndGet() ? area.getText() : null;
    }
}
