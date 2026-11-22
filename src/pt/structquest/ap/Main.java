package pt.structquest.ap;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * StructQuest – Activity Provider para a Inven!RA (versão básica).
 *
 * Variáveis de ambiente opcionais:
 *   PORT      porta do servidor (por omissão 8080)
 *   BASE_URL  URL público do serviço, usado no activityURL devolvido no deploy
 *             (por omissão, deduzido do cabeçalho Host do pedido)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8081;
        String env = System.getenv("PORT");
        if (env != null && !env.isBlank()) port = Integer.parseInt(env.trim());

        // Montagem dos componentes (injeção manual)
        InMemoryRepository repo = new InMemoryRepository();
        ActivityService activities = new ActivityService(repo);
        AnalyticsService analytics = new AnalyticsService(repo);
        IntegrationController integration = new IntegrationController(activities, analytics);
        WebInterface web = new WebInterface(activities, repo);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        integration.register(server);
        web.register(server);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("StructQuest Activity Provider a escutar na porta " + port);
        System.out.println("  GET       /config?activityID=...            (config_url)");
        System.out.println("  GET       /params                           (json_params_url)");
        System.out.println("  GET|POST  /deploy                           (deploy_url)");
        System.out.println("  GET       /activity?activityID=&studentID=  (URL da atividade)");
        System.out.println("  GET       /analytics-list                   (analytics_list_url)");
        System.out.println("  GET|POST  /analytics                        (analytics_url)");
    }
}
