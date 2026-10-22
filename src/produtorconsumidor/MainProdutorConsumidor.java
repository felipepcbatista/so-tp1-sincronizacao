package produtorconsumidor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import util.Execucao;
import util.Log;

/**
 * Produtor e Consumidor (Bounded Buffer) com semáforos.
 * Uso: java -cp out produtorconsumidor.MainProdutorConsumidor [correto|sem-mutex|mutex-antes] [itens por produtor]
 */
public class MainProdutorConsumidor {

    public record Resultado(boolean terminou, int produzidos, int consumidosUmaVez, int perdidos,
                            int duplicados, int leiturasVazias, int maxOcupacao, int capacidade,
                            List<String> erros) {
        public boolean consistente() {
            return perdidos == 0 && duplicados == 0 && leiturasVazias == 0 && erros.isEmpty();
        }
    }

    public static Resultado executar(BufferLimitado.Modo modo, int produtores, int consumidores,
                                     int itensPorProdutor, int capacidade, int produzirMs, int consumirMs,
                                     int pausaMs, long timeoutMs) throws InterruptedException {
        int total = produtores * itensPorProdutor;
        if (total % consumidores != 0) throw new IllegalArgumentException("total de itens deve dividir entre os consumidores");

        BufferLimitado buffer = new BufferLimitado(modo, capacidade, pausaMs);
        AtomicIntegerArray vezesConsumido = new AtomicIntegerArray(total);

        List<Thread> threads = new ArrayList<>();
        for (int c = 0; c < consumidores; c++) {
            threads.add(new Consumidor(buffer, c, total / consumidores, consumirMs, vezesConsumido));
        }
        for (int p = 0; p < produtores; p++) {
            threads.add(new Produtor(buffer, p, itensPorProdutor, produzirMs));
        }
        Log.info("iniciando " + produtores + " produtores e " + consumidores + " consumidores");
        threads.forEach(Thread::start);

        boolean terminou = Execucao.aguardar(threads, threads, timeoutMs,
                t -> buffer.estados.getOrDefault(t, "iniciando"));
        Execucao.encerrar(threads);

        int umaVez = 0, perdidos = 0, duplicados = 0;
        for (int i = 0; i < total; i++) {
            int v = vezesConsumido.get(i);
            if (v == 1) umaVez++;
            else if (v == 0) perdidos++;
            else duplicados++;
        }
        return new Resultado(terminou, total, umaVez, perdidos, duplicados, buffer.leiturasVazias.get(),
                buffer.maxOcupacao.get(), capacidade, List.copyOf(buffer.erros));
    }

    private static BufferLimitado.Modo lerModo(String arg) {
        return BufferLimitado.Modo.valueOf(arg.toUpperCase().replace('-', '_'));
    }

    public static void main(String[] args) throws InterruptedException {
        BufferLimitado.Modo modo = args.length > 0 ? lerModo(args[0]) : BufferLimitado.Modo.CORRETO;
        int itens = args.length > 1 ? Integer.parseInt(args[1]) : 6;
        int produtores = 2, consumidores = 2, capacidade = 3;

        System.out.println("=== Produtor e Consumidor | modo " + modo + " | " + produtores + " produtores, "
                + consumidores + " consumidores, " + itens + " itens cada, buffer de " + capacidade + " posições ===\n");
        Resultado r = executar(modo, produtores, consumidores, itens, capacidade, 100, 150, 10,
                produtores * itens * 200L + 3000);

        System.out.println("\n=== Resumo ===");
        System.out.println("Terminou sem deadlock:         " + (r.terminou() ? "SIM" : "NÃO"));
        System.out.println("Itens previstos:               " + r.produzidos());
        System.out.println("Consumidos exatamente 1 vez:   " + r.consumidosUmaVez());
        System.out.println("Itens que nunca chegaram:      " + r.perdidos());
        System.out.println("Itens lidos mais de 1 vez:     " + r.duplicados());
        System.out.println("Leituras de posição vazia:     " + r.leiturasVazias());
        System.out.println("Ocupação máxima do buffer:     " + r.maxOcupacao() + " de " + r.capacidade());
        r.erros().forEach(e -> System.out.println("  - " + e));
    }
}
