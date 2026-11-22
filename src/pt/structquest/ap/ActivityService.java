package pt.structquest.ap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gestor de Atividades: esquema de parâmetros, deploy e configuração guardada. */
public class ActivityService {

    private final InMemoryRepository repo;

    public ActivityService(InMemoryRepository repo) {
        this.repo = repo;
    }

    private static Map<String, Object> param(String name, String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("type", type);
        return m;
    }

    /** Resposta de json_params_url: lista de parâmetros configuráveis pelo docente. */
    public List<Map<String, Object>> getParamSchema() {
        List<Map<String, Object>> l = new ArrayList<>();
        l.add(param("mundos_ativos", "text/plain"));
        l.add(param("ids_missoes", "text/plain"));
        l.add(param("modo", "text/plain"));
        l.add(param("max_tentativas", "integer"));
        l.add(param("tempo_limite_minutos", "integer"));
        l.add(param("vidas_por_missao", "integer"));
        l.add(param("custo_pista_xp", "integer"));
        l.add(param("penalizacao_leak_xp", "integer"));
        l.add(param("exigir_memoria_limpa", "boolean"));
        l.add(param("desbloqueio_sequencial", "boolean"));
        l.add(param("leaderboard_ativo", "boolean"));
        l.add(param("leaderboard_anonimo", "boolean"));
        return l;
    }

    /** Valores por omissão, usados quando o docente não preenche um parâmetro. */
    public Map<String, Object> defaults() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("mundos_ativos", "listas,pilhas_filas");
        d.put("ids_missoes", "");
        d.put("modo", "treino");
        d.put("max_tentativas", 5L);
        d.put("tempo_limite_minutos", 30L);
        d.put("vidas_por_missao", 3L);
        d.put("custo_pista_xp", 10L);
        d.put("penalizacao_leak_xp", 5L);
        d.put("exigir_memoria_limpa", false);
        d.put("desbloqueio_sequencial", true);
        d.put("leaderboard_ativo", false);
        d.put("leaderboard_anonimo", true);
        return d;
    }

    /** Cria (ou atualiza) a instância com os parâmetros recebidos e devolve a configuração final. */
    public Map<String, Object> deploy(String activityID, Map<String, Object> params) {
        Map<String, Object> cfg = defaults();
        if (params != null) {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                if (cfg.containsKey(e.getKey())) cfg.put(e.getKey(), e.getValue());
            }
        }
        repo.saveInstance(activityID, cfg);
        return cfg;
    }

    /** Atualiza a configuração de uma instância existente; null se não existe. */
    public Map<String, Object> saveConfig(String activityID, Map<String, Object> params) {
        if (!repo.exists(activityID)) return null;
        Map<String, Object> cfg = repo.loadInstance(activityID);
        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (cfg.containsKey(e.getKey())) cfg.put(e.getKey(), e.getValue());
        }
        repo.saveInstance(activityID, cfg);
        return cfg;
    }

    public Map<String, Object> getInstance(String activityID) {
        return repo.loadInstance(activityID);
    }

    public boolean exists(String activityID) {
        return repo.exists(activityID);
    }
}
