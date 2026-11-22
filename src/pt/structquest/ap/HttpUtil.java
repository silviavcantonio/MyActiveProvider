package pt.structquest.ap;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Funções auxiliares para ler pedidos e enviar respostas. */
public final class HttpUtil {

    private HttpUtil() {}

    public static final String JSON = "application/json; charset=utf-8";
    public static final String HTML = "text/html; charset=utf-8";
    public static final String TEXT = "text/plain; charset=utf-8";

    /** Parâmetros da query string (?a=1&b=2). */
    public static Map<String, String> query(HttpExchange ex) {
        return parseForm(ex.getRequestURI().getRawQuery());
    }

    /** Decodifica "a=1&b=2" (query string ou application/x-www-form-urlencoded). */
    public static Map<String, String> parseForm(String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8),
                  URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }

    public static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static String contentType(HttpExchange ex) {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        return ct == null ? "" : ct.toLowerCase();
    }

    public static void send(HttpExchange ex, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        send(ex, status, JSON, Json.write(body));
    }

    public static void sendError(HttpExchange ex, int status, String message) throws IOException {
        sendJson(ex, status, Map.of("error", message));
    }

    /** Escapa texto para inserir em HTML. */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** URL base do pedido, p. ex. http://localhost:8080 (ou BASE_URL, se definida). */
    public static String baseUrl(HttpExchange ex) {
        String env = System.getenv("BASE_URL");
        if (env != null && !env.isBlank()) return env.replaceAll("/+$", "");
        String host = ex.getRequestHeaders().getFirst("Host");
        if (host == null) host = "localhost";
        return "http://" + host;
    }
}
