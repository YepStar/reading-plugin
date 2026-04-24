# Reading Plugin

[中文使用说明](README_zh.md)

This repository is organized for two editor plugin targets:

- `jetbrains/`: JetBrains IDE plugin, implemented first.
- `vscode/`: VS Code plugin placeholder for a later implementation.

The JetBrains version keeps the W-Reader-style native editor hint surface and adds a cleaner core around local TXT/EPUB parsing, chapter regex splitting, table-of-contents jumps, shortcut-driven display controls, automatic chapter switching, and browser/web-reader modes.

## Quick Start

After installing the JetBrains plugin and restarting the IDE:

1. Open `View > Tool Windows > Reader`.
2. Use `Open TXT / EPUB` to load a local book, or `Web Text Extractor` to load a novel page.
3. Use `Show Native Reader` to display the editor hint reader.
4. Customize shortcuts in `Settings / Preferences > Keymap`, then search `Reader`.
