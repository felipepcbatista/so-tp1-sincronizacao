package fumantes;

import java.util.ArrayList;
import java.util.List;
import util.Execucao;

/**
 * Problema dos Fumantes.
 * Uso: java -cp out fumantes.MainFumantes [correto|ingenuo] [rodadas]
 */
public class MainFumantes {

    public record Resultado(boolean terminou, int rodadas, int[] cigarros, int[] faltantes,
                            int maxFumandoJuntos, List<String> erros) {
        public int totalCigarros() {
            int soma = 0;
            for (int c : cigarros) soma += c;
            return soma;
        }
    }

    public static Resultado executar(Mesa.Modo modo, int rodadas, int tempoMaxFumarMs,
                                     int pausaMs, long timeoutMs) throws InterruptedException {
        Mesa mesa = new Mesa(modo, tempoMaxFumarMs, pausaMs);

        Agente agente = new Agente(mesa, rodadas);
        List<Fumante> fumantes = new ArrayList<>();
        for (Ingrediente i : Ingrediente.values()) fumantes.add(new Fumante(mesa, i));

        fumantes.forEach(Thread::start); // começam bloqueados
        agente.start();

        List<Thread> todas = new ArrayList<>(fumantes);
        todas.add(0, agente);
        // o agente só termina se todas as rodadas fecharem; se ele travar, é deadlock
        boolean terminou = Execucao.aguardar(List.of(agente), todas, timeoutMs, MainFumantes::descrever);
        // fumantes ficam em laço infinito: com o expediente encerrado, são dispensados
        Execucao.encerrar(todas);

        int[] cigarros = new int[3];
        int[] faltantes = new int[3];
        for (int k = 0; k < 3; k++) {
            cigarros[k] = mesa.cigarrosPorFumante.get(k);
            faltantes[k] = mesa.rodadasPorFaltante.get(k);
        }
        return new Resultado(terminou, rodadas, cigarros, faltantes, mesa.maxFumandoJuntos.get(), List.copyOf(mesa.erros));
    }

    private static String descrever(Thread t) {
        if (t instanceof Agente a) return a.estado;
        if (t instanceof Fumante f) return f.estado;
        return "";
    }

    public static void main(String[] args) throws InterruptedException {
        Mesa.Modo modo = args.length > 0 ? Mesa.Modo.valueOf(args[0].toUpperCase()) : Mesa.Modo.CORRETO;
        int rodadas = args.length > 1 ? Integer.parseInt(args[1]) : 10;

        System.out.println("=== Problema dos Fumantes | modo " + modo + " | " + rodadas + " rodadas ===\n");
        Resultado r = executar(modo, rodadas, 300, 100, rodadas * 400L + 3000);

        System.out.println("\n=== Resumo ===");
        System.out.println("Terminou sem deadlock:   " + (r.terminou() ? "SIM" : "NÃO"));
        for (Ingrediente i : Ingrediente.values()) {
            System.out.printf("Fumante de %-8s fumou %2d cigarros (agente deixou faltar %s em %2d rodadas)%n",
                    i, r.cigarros()[i.ordinal()], i, r.faltantes()[i.ordinal()]);
        }
        System.out.println("Total de cigarros:       " + r.totalCigarros() + " de " + rodadas);
        System.out.println("Máx. fumando ao mesmo tempo: " + r.maxFumandoJuntos());
        System.out.println("Violações detectadas:    " + r.erros().size());
        r.erros().forEach(e -> System.out.println("  - " + e));
    }
}
