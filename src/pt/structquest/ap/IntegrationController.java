package pt.structquest.ap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador de Integração: único ponto de entrada das chamadas da Inven!RA.
 *
 *   GET        /               mensagem de atividade ativa (health check)
 *   GET        /params          json_params_url        (E2)
 *   GET|POST   /deploy          deploy_url             (E3)
 *   GET        /analytics-list  analytics_list_url     (E5)
 *   GET|POST   /analytics       analytics_url          (E6)
 *
 * NOTA: caminhos, métodos HTTP e nomes dos campos são os assumidos nas propostas
 * anteriores. Confirmar no documento "Activity Providers na Inven!RA".
 */
public class IntegrationController {

    private final ActivityService activities;
    private final AnalyticsService analytics;

    public IntegrationController(ActivityService activities, AnalyticsService analytics) {
        this.activities = activities;
        this.analytics = analytics;
    }

    public void register(HttpServer server) {
        server.createContext("/", this::handleRoot);
        server.createContext("/params", this::handleParams);
        server.createContext("/deploy", this::handleDeploy);
        server.createContext("/analytics-list", this::handleAnalyticsList);
        server.createContext("/analytics", this::handleAnalytics);
    }

    // ------------------------------------------------------------------ E2

    private void handleParams(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            HttpUtil.sendError(ex, 405, "Método não permitido");
            return;
        }
        HttpUtil.sendJson(ex, 200, activities.getParamSchema());          // C01
    }

    // ------------------------------------------------------------------ E3

    private void handleDeploy(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (!"GET".equals(method) && !"POST".equals(method)) {
            HttpUtil.sendError(ex, 405, "Método não permitido");
            return;
        }
        Map<String, Object> req;
        try {
            req = readRequest(ex);
        } catch (IllegalArgumentException e) {
            HttpUtil.sendError(ex, 400, e.getMessage());
            return;
        }
        String activityID = asString(req.get("activityID"));
        if (activityID == null || activityID.isBlank()) {
            HttpUtil.sendError(ex, 400, "Falta o campo activityID");
            return;
        }

        // Parâmetros: ou vêm em "params", ou são os restantes campos do pedido.
        Map<String, Object> params = new LinkedHashMap<>();
        Object nested = req.get("params");
        if (nested instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) params.put(String.valueOf(e.getKey()), e.getValue());
        } else {
            params.putAll(req);
            params.remove("activityID");
        }

        activities.deploy(activityID, params);                            // C02

        String url = HttpUtil.baseUrl(ex) + "/activity?activityID="
                + URLEncoder.encode(activityID, StandardCharsets.UTF_8);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("activityURL", url);
        HttpUtil.sendJson(ex, 200, out);
    }

    // ------------------------------------------------------------------ E5

    private void handleAnalyticsList(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            HttpUtil.sendError(ex, 405, "Método não permitido");
            return;
        }
        HttpUtil.sendJson(ex, 200, analytics.getAnalyticsList());         // C03
    }

    // ------------------------------------------------------------------ E6

    private void handleAnalytics(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (!"GET".equals(method) && !"POST".equals(method)) {
            HttpUtil.sendError(ex, 405, "Método não permitido");
            return;
        }
        Map<String, Object> req;
        try {
            req = readRequest(ex);
        } catch (IllegalArgumentException e) {
            HttpUtil.sendError(ex, 400, e.getMessage());
            return;
        }
        String activityID = asString(req.get("activityID"));
        if (activityID == null || activityID.isBlank()) {
            HttpUtil.sendError(ex, 400, "Falta o campo activityID");
            return;
        }
        if (!activities.exists(activityID)) {
            HttpUtil.sendError(ex, 404, "Atividade desconhecida: " + activityID);
            return;
        }
        HttpUtil.sendJson(ex, 200, analytics.getAnalytics(activityID));   // C04
    }

    // ------------------------------------------------------------ auxiliares

    /** Junta a query string com o corpo (JSON ou formulário). O corpo prevalece. */
    private static Map<String, Object> readRequest(HttpExchange ex) throws IOException {
        Map<String, Object> req = new LinkedHashMap<>(HttpUtil.query(ex));
        String body = HttpUtil.readBody(ex).trim();
        if (!body.isEmpty()) {
            if (HttpUtil.contentType(ex).contains("application/x-www-form-urlencoded")) {
                req.putAll(HttpUtil.parseForm(body));
            } else {
                req.putAll(Json.parseObject(body));
            }
        }
        return req;
    }

    private static String asString(Object o) {
        if (o == null) return null;
        if (o instanceof Double d && d == Math.rint(d)) return String.valueOf(d.longValue());
        return String.valueOf(o);
    }
}
