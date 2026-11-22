package pt.structquest.ap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Interface Web (docente e aluno).
 *
 *   GET   /config?activityID=...                       config_url        (E1)
 *   POST  /config?activityID=...  (formulário)         grava configuração (C06)
 *   GET   /activity?activityID=...&studentID=...       URL da atividade  (E4)
 *
 * Nesta versão básica as páginas são simples; o editor de código, o mapa de
 * mundos e o envio de missões ficam para as iterações seguintes.
 */
public class WebInterface {

    private final ActivityService activities;
    private final InMemoryRepository repo;

    public WebInterface(ActivityService activities, InMemoryRepository repo) {
        this.activities = activities;
        this.repo = repo;
    }

    public void register(HttpServer server) {
        server.createContext("/config", this::handleConfig);
        server.createContext("/activity", this::handleActivity);
    }

    // ------------------------------------------------------------------ E1

    private void handleConfig(HttpExchange ex) throws IOException {
        String activityID = HttpUtil.query(ex).get("activityID");
        if (activityID == null || activityID.isBlank()) {
            HttpUtil.send(ex, 400, HttpUtil.HTML, page("Configuração", "<p>Falta o parâmetro <code>activityID</code>.</p>"));
            return;
        }
        String method = ex.getRequestMethod();
        if ("GET".equals(method)) {
            HttpUtil.send(ex, 200, HttpUtil.HTML, configPage(activityID, null));
        } else if ("POST".equals(method)) {
            Map<String, String> form = HttpUtil.parseForm(HttpUtil.readBody(ex));
            Map<String, Object> typed = convert(form);
            Map<String, Object> saved = activities.saveConfig(activityID, typed);   // C06
            if (saved == null) {
                HttpUtil.send(ex, 404, HttpUtil.HTML, configPage(activityID,
                        "A instância ainda não existe (falta o deploy). Nada foi guardado."));
            } else {
                HttpUtil.send(ex, 200, HttpUtil.HTML, configPage(activityID, "Configuração guardada."));
            }
        } else {
            HttpUtil.sendError(ex, 405, "Método não permitido");
        }
    }

    /** Converte o formulário para os tipos do esquema (checkbox ausente = false). */
    private Map<String, Object> convert(Map<String, String> form) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map<String, Object> p : activities.getParamSchema()) {
            String name = (String) p.get("name");
            String type = (String) p.get("type");
            switch (type) {
                case "boolean" -> out.put(name, form.containsKey(name));
                case "integer" -> {
                    String v = form.get(name);
                    if (v != null && !v.isBlank()) {
                        try { out.put(name, Long.parseLong(v.trim())); } catch (NumberFormatException ignored) { }
                    }
                }
                default -> { if (form.containsKey(name)) out.put(name, form.get(name)); }
            }
        }
        return out;
    }

    private String configPage(String activityID, String message) {
        Map<String, Object> current = activities.getInstance(activityID);
        boolean deployed = current != null;
        Map<String, Object> values = deployed ? current : activities.defaults();

        StringBuilder sb = new StringBuilder();
        sb.append("<h1>StructQuest – configuração</h1>");
        sb.append("<p>Atividade: <code>").append(HttpUtil.esc(activityID)).append("</code></p>");
        if (message != null) sb.append("<p class=\"msg\">").append(HttpUtil.esc(message)).append("</p>");
        if (!deployed) {
            sb.append("<p class=\"msg\">Instância ainda não criada: estes são os valores por omissão.</p>");
        }
        sb.append("<form method=\"post\" action=\"/config?activityID=")
          .append(HttpUtil.esc(java.net.URLEncoder.encode(activityID, java.nio.charset.StandardCharsets.UTF_8)))
          .append("\">");
        for (Map<String, Object> p : activities.getParamSchema()) {
            String name = (String) p.get("name");
            String type = (String) p.get("type");
            Object v = values.get(name);
            sb.append("<label>").append(HttpUtil.esc(name)).append("</label>");
            switch (type) {
                case "boolean" -> sb.append("<input type=\"checkbox\" name=\"").append(HttpUtil.esc(name)).append("\"")
                        .append(Boolean.TRUE.equals(v) ? " checked" : "").append(">");
                case "integer" -> sb.append("<input type=\"number\" name=\"").append(HttpUtil.esc(name))
                        .append("\" value=\"").append(HttpUtil.esc(String.valueOf(v))).append("\">");
                default -> sb.append("<input type=\"text\" name=\"").append(HttpUtil.esc(name))
                        .append("\" value=\"").append(HttpUtil.esc(v == null ? "" : String.valueOf(v))).append("\">");
            }
        }
        sb.append("<button type=\"submit\">Guardar</button></form>");
        return page("Configuração", sb.toString());
    }

    // ------------------------------------------------------------------ E4

    private void handleActivity(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            HttpUtil.sendError(ex, 405, "Método não permitido");
            return;
        }
        Map<String, String> q = HttpUtil.query(ex);
        String activityID = q.get("activityID");
        String studentID = q.get("studentID");
        if (activityID == null || activityID.isBlank()) {
            HttpUtil.send(ex, 400, HttpUtil.HTML, page("Atividade", "<p>Falta o parâmetro <code>activityID</code>.</p>"));
            return;
        }
        Map<String, Object> cfg = activities.getInstance(activityID);          // C09
        if (cfg == null) {
            HttpUtil.send(ex, 404, HttpUtil.HTML, page("Atividade", "<p>Atividade desconhecida.</p>"));
            return;
        }
        InMemoryRepository.StudentProgress progress = null;
        if (studentID != null && !studentID.isBlank()) {
            progress = repo.recordAccess(activityID, studentID);               // C10 / C19
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<h1>StructQuest</h1>");
        sb.append("<p>Atividade <code>").append(HttpUtil.esc(activityID)).append("</code>");
        if (progress != null) sb.append(" · aluno <code>").append(HttpUtil.esc(studentID)).append("</code>");
        sb.append("</p>");
        if (progress != null) {
            sb.append("<p>XP: <b>").append(progress.xpTotal).append("</b> · missões concluídas: <b>")
              .append(progress.missoesConcluidas).append("</b></p>");
        } else {
            sb.append("<p class=\"msg\">Sem <code>studentID</code> no pedido: o progresso não é registado.</p>");
        }
        sb.append("<h2>Configuração da atividade</h2><ul>");
        for (Map.Entry<String, Object> e : cfg.entrySet()) {
            sb.append("<li><code>").append(HttpUtil.esc(e.getKey())).append("</code>: ")
              .append(HttpUtil.esc(String.valueOf(e.getValue()))).append("</li>");
        }
        sb.append("</ul><p><i>Mapa de mundos, editor de C e submissão de missões: por implementar.</i></p>");
        HttpUtil.send(ex, 200, HttpUtil.HTML, page("StructQuest", sb.toString()));
    }

    // ------------------------------------------------------------ auxiliares

    private static String page(String title, String body) {
        return "<!DOCTYPE html><html lang=\"pt\"><head><meta charset=\"utf-8\">"
             + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
             + "<title>" + HttpUtil.esc(title) + "</title>"
             + "<style>body{font-family:system-ui,sans-serif;max-width:640px;margin:2rem auto;padding:0 1rem}"
             + "label{display:block;margin-top:.8rem;font-weight:600}"
             + "input[type=text],input[type=number]{width:100%;padding:.4rem;box-sizing:border-box}"
             + "button{margin-top:1rem;padding:.5rem 1rem}.msg{background:#eef;padding:.5rem;border-radius:4px}"
             + "code{background:#f2f2f2;padding:0 .2rem}</style></head><body>"
             + body + "</body></html>";
    }
}
