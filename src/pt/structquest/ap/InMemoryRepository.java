package pt.structquest.ap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Camada de Persistência (versão em memória).
 * Para persistir de verdade, substituir por uma implementação com base de dados
 * mantendo os mesmos métodos.
 */
public class InMemoryRepository {

    /** Progresso de um aluno numa instância da atividade. */
    public static class StudentProgress {
        public final String studentID;
        public int xpTotal = 0;
        public int missoesConcluidas = 0;
        public int tentativasTotais = 0;
        public int segfaults = 0;
        public int leaksDetetados = 0;
        public int pistasUsadas = 0;
        public String estadoAtividade = "iniciada";
        public String erroMaisFrequente = "sem dados";

        public StudentProgress(String studentID) {
            this.studentID = studentID;
        }
    }

    private final Map<String, Map<String, Object>> instances = new ConcurrentHashMap<>();
    private final Map<String, Map<String, StudentProgress>> progress = new ConcurrentHashMap<>();

    // ------------------------------------------------------------ instâncias

    public void saveInstance(String activityID, Map<String, Object> params) {
        instances.put(activityID, new LinkedHashMap<>(params));
        progress.computeIfAbsent(activityID, k -> new ConcurrentHashMap<>());
    }

    public boolean exists(String activityID) {
        return instances.containsKey(activityID);
    }

    /** Cópia da configuração, ou null se a instância não existe. */
    public Map<String, Object> loadInstance(String activityID) {
        Map<String, Object> p = instances.get(activityID);
        return p == null ? null : new LinkedHashMap<>(p);
    }

    // ---------------------------------------------------------------- alunos

    /** Regista o acesso de um aluno (cria o progresso se ainda não existir). */
    public StudentProgress recordAccess(String activityID, String studentID) {
        return progress
                .computeIfAbsent(activityID, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(studentID, StudentProgress::new);
    }

    public List<StudentProgress> listProgress(String activityID) {
        Map<String, StudentProgress> m = progress.get(activityID);
        return m == null ? new ArrayList<>() : new ArrayList<>(m.values());
    }
}
