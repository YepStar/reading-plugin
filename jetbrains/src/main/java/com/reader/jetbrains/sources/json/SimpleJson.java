package com.reader.jetbrains.sources.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimpleJson {
    private SimpleJson() {
    }

    public static Object parse(String text) {
        return new Parser(text == null ? "" : text).parse();
    }

    public static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        write(value, builder, 0);
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    public static Object path(Object root, String path) {
        if (path == null || path.isBlank() || "$".equals(path.trim())) {
            return root;
        }
        Object current = root;
        String expression = path.trim();
        if ("$[*]".equals(expression)) {
            return root;
        }
        if (expression.startsWith("$.")) {
            expression = expression.substring(2);
        }
        for (String part : expression.split("\\.")) {
            if (part.isBlank()) {
                continue;
            }
            int wildcard = part.indexOf("[*]");
            String key = wildcard < 0 ? part : part.substring(0, wildcard);
            if (current instanceof Map<?, ?> map) {
                current = map.get(key);
            } else {
                return null;
            }
            while (wildcard >= 0) {
                if (!(current instanceof List<?> list)) {
                    return null;
                }
                int nextWildcard = part.indexOf("[*]", wildcard + 3);
                if (nextWildcard >= 0) {
                    List<Object> flattened = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof List<?> nested) {
                            flattened.addAll(nested);
                        }
                    }
                    current = flattened;
                }
                wildcard = nextWildcard;
            }
        }
        return current;
    }

    public static String stringField(Object value, String field) {
        if (!(value instanceof Map<?, ?> map) || field == null || field.isBlank()) {
            return "";
        }
        Object found = map.get(field);
        return found == null ? "" : String.valueOf(found);
    }

    private static void write(Object value, StringBuilder builder, int indent) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String string) {
            builder.append('"').append(escape(string)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            builder.append("{\n");
            int index = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                indent(builder, indent + 2);
                builder.append('"').append(escape(String.valueOf(entry.getKey()))).append("\": ");
                write(entry.getValue(), builder, indent + 2);
                if (++index < map.size()) {
                    builder.append(',');
                }
                builder.append('\n');
            }
            indent(builder, indent);
            builder.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            builder.append("[\n");
            List<Object> values = new ArrayList<>();
            iterable.forEach(values::add);
            for (int i = 0; i < values.size(); i++) {
                indent(builder, indent + 2);
                write(values.get(i), builder, indent + 2);
                if (i < values.size() - 1) {
                    builder.append(',');
                }
                builder.append('\n');
            }
            indent(builder, indent);
            builder.append(']');
        } else {
            builder.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static void indent(StringBuilder builder, int indent) {
        builder.append(" ".repeat(Math.max(0, indent)));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class Parser {
        private final String text;
        private int offset;

        private Parser(String text) {
            this.text = text;
        }

        private Object parse() {
            Object value = readValue();
            skipWhitespace();
            if (offset != text.length()) {
                throw new IllegalArgumentException("JSON 末尾存在无法解析的内容");
            }
            return value;
        }

        private Object readValue() {
            skipWhitespace();
            if (offset >= text.length()) {
                throw new IllegalArgumentException("JSON 内容为空");
            }
            char ch = text.charAt(offset);
            if (ch == '{') {
                return readObject();
            }
            if (ch == '[') {
                return readArray();
            }
            if (ch == '"') {
                return readString();
            }
            if (ch == 't' && text.startsWith("true", offset)) {
                offset += 4;
                return Boolean.TRUE;
            }
            if (ch == 'f' && text.startsWith("false", offset)) {
                offset += 5;
                return Boolean.FALSE;
            }
            if (ch == 'n' && text.startsWith("null", offset)) {
                offset += 4;
                return null;
            }
            return readNumber();
        }

        private Map<String, Object> readObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                offset++;
                return map;
            }
            while (true) {
                String key = readString();
                skipWhitespace();
                expect(':');
                map.put(key, readValue());
                skipWhitespace();
                if (peek('}')) {
                    offset++;
                    return map;
                }
                expect(',');
                skipWhitespace();
            }
        }

        private List<Object> readArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                offset++;
                return list;
            }
            while (true) {
                list.add(readValue());
                skipWhitespace();
                if (peek(']')) {
                    offset++;
                    return list;
                }
                expect(',');
            }
        }

        private String readString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (offset < text.length()) {
                char ch = text.charAt(offset++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (offset >= text.length()) {
                        break;
                    }
                    char escaped = text.charAt(offset++);
                    switch (escaped) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case '/' -> builder.append('/');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(readUnicode());
                        default -> builder.append(escaped);
                    }
                } else {
                    builder.append(ch);
                }
            }
            throw new IllegalArgumentException("JSON 字符串未闭合");
        }

        private char readUnicode() {
            if (offset + 4 > text.length()) {
                throw new IllegalArgumentException("JSON unicode 转义不完整");
            }
            String value = text.substring(offset, offset + 4);
            offset += 4;
            return (char) Integer.parseInt(value, 16);
        }

        private Number readNumber() {
            int start = offset;
            while (offset < text.length() && "-+0123456789.eE".indexOf(text.charAt(offset)) >= 0) {
                offset++;
            }
            if (start == offset) {
                throw new IllegalArgumentException("JSON 值无法解析");
            }
            String number = text.substring(start, offset);
            return number.contains(".") || number.contains("e") || number.contains("E")
                    ? Double.parseDouble(number)
                    : Long.parseLong(number);
        }

        private void expect(char expected) {
            skipWhitespace();
            if (offset >= text.length() || text.charAt(offset) != expected) {
                throw new IllegalArgumentException("JSON 预期字符 " + expected);
            }
            offset++;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return offset < text.length() && text.charAt(offset) == expected;
        }

        private void skipWhitespace() {
            while (offset < text.length() && Character.isWhitespace(text.charAt(offset))) {
                offset++;
            }
        }
    }
}
