package barbearia;

import java.util.ArrayList;
import java.util.List;
import util.Execucao;

public class MainBarbearia {

    public record Resultado(boolean terminou, int totalClientes, int atendidos, int desistiram,
                            int cortesFeitos, int maxEsperando, int maxNaCadeira, int vezesQueDormiu,
                            List<String> erros) {
        public int contabilizados() {
            return atendidos + desistiram;
        }
    }

    public static Resultado executar(Barbearia.Modo modo, int cadeiras, int totalClientes,
                                     int intervaloChegadaMs, int tempoCorteMs, int pausaMs,
                                     long timeoutMs) throws InterruptedException {
        Barbearia loja = new Barbearia(modo, cadeiras, tempoCorteMs, pausaMs);

        Barbeiro barbeiro = new Barbeiro(loja);
        barbeiro.start();

        List<Cliente> clientes = new ArrayList<>();
        for (int i = 1; i <= totalClientes; i++) {
            Cliente c = new Cliente(loja, i);
            clientes.add(c);
            c.start();
            if (intervaloChegadaMs > 0) Thread.sleep(intervaloChegadaMs);
        }

        List<Thread> todas = new ArrayList<>(clientes);
        todas.add(0, barbeiro);
        boolean terminou = Execucao.aguardar(clientes, todas, timeoutMs, MainBarbearia::descrever);
        Execucao.encerrar(todas);

        return new Resultado(terminou, totalClientes, loja.atendidos.get(), loja.desistiram.get(),
                loja.cortesFeitos.get(), loja.maxEsperando.get(), loja.maxNaCadeira.get(),
                loja.vezesQueDormiu.get(), List.copyOf(loja.erros));
    }

    private static String descrever(Thread t) {
        if (t instanceof Barbeiro b) return b.estado;
        if (t instanceof Cliente c) return c.estado;
        return "";
    }

    private static Barbearia.Modo lerModo(String arg) {
        return Barbearia.Modo.valueOf(arg.toUpperCase().replace('-', '_'));
    }

    public static void main(String[] args) throws InterruptedException {
        Barbearia.Modo modo = args.length > 0 ? lerModo(args[0]) : Barbearia.Modo.CORRETO;
        int totalClientes = args.length > 1 ? Integer.parseInt(args[1]) : 12;
        int cadeiras = args.length > 2 ? Integer.parseInt(args[2]) : 3;

        System.out.println("=== Barbeiro Dorminhoco | modo " + modo + " | " + totalClientes
                + " clientes | " + cadeiras + " cadeiras na sala ===\n");

        Resultado r;
        if (modo == Barbearia.Modo.CORRETO) {
            r = executar(modo, cadeiras, totalClientes, 60, 250, 0, totalClientes * 400L + 3000);
        } else {
            r = executar(modo, cadeiras, totalClientes, 0, 100, 30, 5000);
        }

        System.out.println("\n=== Resumo ===");
        System.out.printf("%-32s %s%n", "Terminou sem deadlock:", r.terminou() ? "SIM" : "NÃO");
        System.out.printf("%-32s %d%n", "Clientes atendidos:", r.atendidos());
        System.out.printf("%-32s %d (sala cheia)%n", "Clientes que foram embora:", r.desistiram());
        System.out.printf("%-32s %d (deve ser %d)%n", "Soma:", r.contabilizados(), r.totalClientes());
        System.out.printf("%-32s %d%n", "Cortes feitos pelo barbeiro:", r.cortesFeitos());
        System.out.printf("%-32s %d (limite: %d cadeiras)%n", "Máx. na sala de espera:", r.maxEsperando(), cadeiras);
        System.out.printf("%-32s %d (limite: 1)%n", "Máx. na cadeira do barbeiro:", r.maxNaCadeira());
        System.out.printf("%-32s %d%n", "Vezes que o barbeiro dormiu:", r.vezesQueDormiu());
        System.out.printf("%-32s %d%n", "Violações detectadas:", r.erros().size());
        r.erros().stream().distinct().limit(10).forEach(e -> System.out.println("  - " + e));
    }
}
