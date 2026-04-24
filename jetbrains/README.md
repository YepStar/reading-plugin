# Reader for JetBrains

JetBrains IDE reading plugin inspired by W-Reader, with a smaller core model and a native editor hint reading surface.

## Implemented in this scaffold

- Open local `.txt` and `.epub` files.
- Split chapters with a configurable regular expression.
- Jump through a table of contents.
- Show/hide a JetBrains-native editor hint reader.
- Move to the next chapter manually or on an automatic timer.
- Open login-capable platform pages in an embedded JCEF browser.
- Open a third-party novel web page and extract likely article text into the reader.

## Main Actions

- `Reader | Open Local Book`
- `Reader | Show Native Reader`
- `Reader | Hide Native Reader`
- `Reader | Open Table of Contents`
- `Reader | Next Chapter`
- `Reader | Toggle Auto Next Chapter`
- `Reader | Open Platform Browser`
- `Reader | Open Web Reader`

Default shortcuts are declared in `src/main/resources/META-INF/plugin.xml`.

## Build

```bash
./gradlew buildPlugin
```

This project uses the IntelliJ Platform Gradle Plugin and downloads the target IDE during the first build.
