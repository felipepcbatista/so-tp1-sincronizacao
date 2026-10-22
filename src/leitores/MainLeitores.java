package leitores;

import java.util.ArrayList;
import java.util.List;
import util.Execucao;

public class MainLeitores {

    public record Resultado(boolean terminou, int leiturasFeitas, int escritasFeitas,
                            int maxLeitoresJuntos, int maxEscritoresJuntos,
                            long maiorEsperaEscritorMs, List<String> erros) {}

    public static Resultado executar(Memoria.Modo modo, int nLeitores, int nEscritores,
                                     int leituras, int escritas, int tamanho,
                                     int tempoLeituraMs, int tempoEscritaMs, int pausaMs,
                                     long timeoutMs) throws InterruptedException {
        Memoria area = new Memoria(modo, tamanho, tempoLeituraMs, tempoEscritaMs, pausaMs);

        List<Thread> todas = new ArrayList<>();
        for (int i = 1; i <= nLeitores; i++) todas.add(new Leitor(area, i, leituras));
        for (int i = 1; i <= nEscritores; i++) todas.add(new Escritor(area, i, escritas));

        todas.forEach(Thread::start);
        boolean terminou = Execucao.aguardar(todas, todas, timeoutMs, MainLeitores::descrever);
        Execucao.encerrar(todas);

        return new Resultado(terminou, area.leiturasFeitas.get(), area.escritasFeitas.get(),
                area.maxLeitoresJuntos.get(), area.maxEscritoresJuntos.get(),
                area.maiorEsperaEscritorMs.get(), List.copyOf(area.erros));
    }

    private static String descrever(Thread t) {
        if (t instanceof Leitor l) return l.estado;
        if (t instanceof Escritor e) return e.estado;
        return "";
    }

    private static Memoria.Modo lerModo(String arg) {
        return Memoria.Modo.valueOf(arg.toUpperCase().replace('-', '_'));
    }

    public static void main(String[] args) throws InterruptedException {
        Memoria.Modo modo = args.length > 0 ? lerModo(args[0]) : Memoria.Modo.CORRETO;
        int nLeitores = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int nEscritores = 2;
        int leituras = 4, escritas = 3, tamanho = 4;

        System.out.println("=== Leitores e Escritores | modo " + modo + " | " + nLeitores
                + " leitores x " + leituras + " leituras | " + nEscritores
                + " escritores x " + escritas + " escritas ===\n");

        Resultado r;
        if (modo == Memoria.Modo.PRIORIDADE_LEITOR) {
            r = executar(modo, nLeitores, nEscritores, 40, escritas, tamanho, 5, 20, 2, 8000);
        } else {
            r = executar(modo, nLeitores, nEscritores, leituras, escritas, tamanho, 40, 60, 10, 15000);
        }

        System.out.println("\n=== Resumo ===");
        System.out.printf("%-34s %s%n", "Terminou sem travar:", r.terminou() ? "SIM" : "NÃO");
        System.out.printf("%-34s %d%n", "Leituras feitas:", r.leiturasFeitas());
        System.out.printf("%-34s %d%n", "Escritas feitas:", r.escritasFeitas());
        System.out.printf("%-34s %d (leitura em paralelo é permitida)%n", "Máx. leitores juntos:", r.maxLeitoresJuntos());
        System.out.printf("%-34s %d (limite: 1)%n", "Máx. escritores juntos:", r.maxEscritoresJuntos());
        System.out.printf("%-34s %d ms%n", "Maior espera de um escritor:", r.maiorEsperaEscritorMs());
        System.out.printf("%-34s %d%n", "Violações detectadas:", r.erros().size());
        r.erros().stream().distinct().limit(10).forEach(e -> System.out.println("  - " + e));
    }
}
