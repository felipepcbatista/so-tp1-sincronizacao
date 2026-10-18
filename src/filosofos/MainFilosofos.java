package filosofos;

import java.util.ArrayList;
import java.util.List;
import util.Execucao;

/**
 * Jantar dos Filósofos.
 * Uso: java -cp out filosofos.MainFilosofos [garcom|ingenuo] [refeicoes]
 */
public class MainFilosofos {

    public record Resultado(boolean terminou, int[] refeicoes, int maxComendoJuntos, List<String> erros) {}

    public static Resultado executar(Jantar.Modo modo, int n, int refeicoes, int pensarMs, int comerMs,
                                     int pausaMs, long timeoutMs) throws InterruptedException {
        Jantar mesa = new Jantar(n, modo, refeicoes, pensarMs, comerMs, pausaMs);
        List<Filosofo> filosofos = new ArrayList<>();
        for (int i = 0; i < n; i++) filosofos.add(new Filosofo(mesa, i));

        filosofos.forEach(Thread::start);
        boolean terminou = Execucao.aguardar(filosofos, filosofos, timeoutMs, t -> ((Filosofo) t).estado);
        Execucao.encerrar(filosofos);

        int[] feitas = new int[n];
        for (int i = 0; i < n; i++) feitas[i] = mesa.refeicoes.get(i);
        return new Resultado(terminou, feitas, mesa.maxComendoJuntos.get(), List.copyOf(mesa.erros));
    }

    public static void main(String[] args) throws InterruptedException {
        Jantar.Modo modo = args.length > 0 ? Jantar.Modo.valueOf(args[0].toUpperCase()) : Jantar.Modo.GARCOM;
        int refeicoes = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        int n = 5;

        System.out.println("=== Jantar dos Filósofos | modo " + modo + " | " + n + " filósofos x " + refeicoes + " refeições ===\n");
        Resultado r = executar(modo, n, refeicoes, 100, 100, 100, refeicoes * 1500L + 3000);

        System.out.println("\n=== Resumo ===");
        System.out.println("Terminou sem deadlock:        " + (r.terminou() ? "SIM" : "NÃO"));
        for (int i = 0; i < n; i++) {
            System.out.printf("Filósofo %d comeu %d de %d vezes%n", i, r.refeicoes()[i], refeicoes);
        }
        System.out.println("Máx. comendo ao mesmo tempo:  " + r.maxComendoJuntos() + " (limite físico com 5 hashis: 2)");
        System.out.println("Violações detectadas:         " + r.erros().size());
        r.erros().forEach(e -> System.out.println("  - " + e));
    }
}
