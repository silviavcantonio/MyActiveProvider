package pt.structquest.ap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Serviço de Analytics: declara os analytics e calcula os valores por aluno. */
public class AnalyticsService {

    private final InMemoryRepository repo;

    public AnalyticsService(InMemoryRepository repo) {
        this.repo = repo;
    }

    private static Map<String, Object> decl(String name, String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("type", type);
        return m;
    }

    private static Map<String, Object> val(String name, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }

    /** Resposta de analytics_list_url. */
    public Map<String, Object> getAnalyticsList() {
        List<Map<String, Object>> quant = new ArrayList<>();
        quant.add(decl("xp_total", "integer"));
        quant.add(decl("missoes_concluidas", "integer"));
        quant.add(decl("tentativas_totais", "integer"));
        quant.add(decl("segfaults", "integer"));
        quant.add(decl("leaks_detetados", "integer"));
        quant.add(decl("pistas_usadas", "integer"));

        List<Map<String, Object>> qual = new ArrayList<>();
        qual.add(decl("estado_atividade", "text/plain"));
        qual.add(decl("erro_mais_frequente", "text/plain"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("quantAnalytics", quant);
        out.put("qualAnalytics", qual);
        return out;
    }

    /** Resposta de analytics_url: valores por aluno da instância. */
    public List<Map<String, Object>> getAnalytics(String activityID) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (InMemoryRepository.StudentProgress p : repo.listProgress(activityID)) {
            List<Map<String, Object>> quant = new ArrayList<>();
            quant.add(val("xp_total", p.xpTotal));
            quant.add(val("missoes_concluidas", p.missoesConcluidas));
            quant.add(val("tentativas_totais", p.tentativasTotais));
            quant.add(val("segfaults", p.segfaults));
            quant.add(val("leaks_detetados", p.leaksDetetados));
            quant.add(val("pistas_usadas", p.pistasUsadas));

            List<Map<String, Object>> qual = new ArrayList<>();
            qual.add(val("estado_atividade", p.estadoAtividade));
            qual.add(val("erro_mais_frequente", p.erroMaisFrequente));

            Map<String, Object> student = new LinkedHashMap<>();
            student.put("inveniraStdID", p.studentID);
            student.put("quantAnalytics", quant);
            student.put("qualAnalytics", qual);
            result.add(student);
        }
        return result;
    }
}
