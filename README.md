# Reader Yip

[中文使用说明](README_zh.md)

This repository is organized for two editor plugin targets:

- `jetbrains/`: JetBrains IDE plugin, implemented first.
- `vscode/`: VS Code plugin placeholder for a later implementation.

The JetBrains version keeps the W-Reader-style native editor hint surface and adds a cleaner core around local TXT/EPUB parsing, chapter regex splitting, table-of-contents jumps, shortcut-driven display controls, automatic chapter switching, compact platform browser popups, web-reader modes, and manageable online book sources loaded from `book-source-config/`. Online source books persist their catalog, reading position, and loaded chapter text in project state.

## Quick Start

After installing the JetBrains plugin and restarting the IDE:

1. Open `View > Tool Windows > Reader Yip`.
2. Use `Open TXT / EPUB` to load a local book, `Web Text Extractor` to load a novel page, or `Online Source Search` to search built-in W-Reader-style sources.
3. Use `Toggle Native Reader` / `显示/隐藏原生提示层` to show or hide the editor hint reader.
4. Manage online sources with `书源管理`.
5. Adjust reader behavior in `Settings / Preferences > Tools > Reader Yip`.

Book source JSON examples and field documentation live in [`book-source-config/README_zh.md`](book-source-config/README_zh.md).
