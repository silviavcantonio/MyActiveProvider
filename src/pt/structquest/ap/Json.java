package pt.structquest.ap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor e escritor de JSON mínimo, sem dependências externas.
 * Tipos: Map (objeto), List (array), String, Number, Boolean e null.
 */
public final class Json {

    private Json() {}

    // ---------------------------------------------------------------- escrita

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    private static void write(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            writeString(s, sb);
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> it) {
            sb.append('[');
            boolean first = true;
            for (Object o : it) {
                if (!first) sb.append(',');
                first = false;
                write(o, sb);
            }
            sb.append(']');
        } else {
            writeString(v.toString(), sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ---------------------------------------------------------------- leitura

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWs();
        Object v = p.value();
        p.skipWs();
        if (p.pos != text.length()) throw p.error("conteúdo inesperado após o valor");
        return v;
    }

    /** Devolve o objeto raiz como Map, ou lança IllegalArgumentException. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (v instanceof Map<?, ?>) return (Map<String, Object>) v;
        throw new IllegalArgumentException("Era esperado um objeto JSON");
    }

    private static final class Parser {
        final String s;
        int pos = 0;

        Parser(String s) { this.s = s; }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("JSON inválido na posição " + pos + ": " + msg);
        }

        void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        Object value() {
            if (pos >= s.length()) throw error("fim inesperado");
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't', 'f' -> bool();
                case 'n' -> nul();
                default -> number();
            };
        }

        Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            pos++; // {
            skipWs();
            if (peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                if (peek() != '"') throw error("era esperada uma chave");
                String key = string();
                skipWs();
                expect(':');
                skipWs();
                m.put(key, value());
                skipWs();
                if (peek() == ',') { pos++; continue; }
                expect('}');
                return m;
            }
        }

        List<Object> array() {
            List<Object> l = new ArrayList<>();
            pos++; // [
            skipWs();
            if (peek() == ']') { pos++; return l; }
            while (true) {
                skipWs();
                l.add(value());
                skipWs();
                if (peek() == ',') { pos++; continue; }
                expect(']');
                return l;
            }
        }

        String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= s.length()) break;
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"', '\\', '/' -> sb.append(e);
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            if (pos + 4 > s.length()) throw error("escape \\u incompleto");
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> throw error("escape inválido");
                    }
                } else {
                    sb.append(c);
                }
            }
            throw error("string sem fecho");
        }

        Boolean bool() {
            if (s.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            if (s.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            throw error("valor inválido");
        }

        Object nul() {
            if (s.startsWith("null", pos)) { pos += 4; return null; }
            throw error("valor inválido");
        }

        Number number() {
            int start = pos;
            while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) pos++;
            if (start == pos) throw error("valor inválido");
            String t = s.substring(start, pos);
            try {
                if (t.contains(".") || t.contains("e") || t.contains("E")) return Double.valueOf(t);
                return Long.valueOf(t);
            } catch (NumberFormatException ex) {
                throw error("número inválido");
            }
        }

        char peek() {
            if (pos >= s.length()) throw error("fim inesperado");
            return s.charAt(pos);
        }

        void expect(char c) {
            if (peek() != c) throw error("era esperado '" + c + "'");
            pos++;
        }
    }
}
