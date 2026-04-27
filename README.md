# Reader-plugin-yip

[中文使用说明](README_zh.md)

This repository is organized for two editor plugin targets:

- `jetbrains/`: JetBrains IDE plugin, implemented first.
- `vscode/`: VS Code plugin placeholder for a later implementation.

The JetBrains version keeps the W-Reader-style native editor hint surface and adds a cleaner core around local TXT/EPUB parsing, chapter regex splitting, table-of-contents jumps, shortcut-driven display controls, automatic chapter switching, browser/web-reader modes, and manageable online book sources loaded from `book-source-config/`.

## Quick Start

After installing the JetBrains plugin and restarting the IDE:

1. Open `View > Tool Windows > Reader-plugin-yip`.
2. Use `Open TXT / EPUB` to load a local book, `Web Text Extractor` to load a novel page, or `Online Source Search` to search built-in W-Reader-style sources.
3. Use `Toggle Native Reader` / `显示/隐藏原生提示层` to show or hide the editor hint reader.
4. Manage online sources with `书源管理`.
5. Customize shortcuts in `Settings / Preferences > Keymap`, then search `Reader-plugin-yip`.

Book source JSON examples and field documentation live in [`book-source-config/README_zh.md`](book-source-config/README_zh.md).
