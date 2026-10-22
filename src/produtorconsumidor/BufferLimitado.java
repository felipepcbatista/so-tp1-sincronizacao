package produtorconsumidor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import util.Log;

/**
 * Recurso compartilhado: buffer circular de tamanho fixo (Bounded Buffer).
 * Concentra o vetor, os índices e todos os semáforos do problema.
 */
public class BufferLimitado {

    /**
     * CORRETO: vazios/cheios + mutex na ordem certa.
     * SEM_MUTEX: sem exclusão mútua no vetor → condição de corrida (itens perdidos/duplicados).
     * MUTEX_ANTES: pega o mutex ANTES de esperar vaga/item → deadlock.
     */
    public enum Modo { CORRETO, SEM_MUTEX, MUTEX_ANTES }

    static final int VAZIO = -1;

    final Modo modo;
    final int capacidade;
    final int pausaMs; // dentro da região crítica: alarga a janela da condição de corrida (todos os modos)

    // ---------------- semáforos ----------------
    /** Conta as posições livres. Começa em N: o buffer começa todo vazio. */
    final Semaphore vazios;
    /** Conta as posições ocupadas. Começa em 0: não há nada para consumir. */
    final Semaphore cheios = new Semaphore(0);
    /** Exclusão mútua sobre o vetor e os índices (região crítica). */
    final Semaphore mutex = new Semaphore(1);

    // ---------------- estado compartilhado ----------------
    private final int[] dados;
    private int entrada = 0; // próxima posição em que o produtor escreve
    private int saida = 0;   // próxima posição de onde o consumidor lê

    // ---------------- instrumentação para validação ----------------
    final AtomicInteger ocupacao = new AtomicInteger();
    final AtomicInteger maxOcupacao = new AtomicInteger();
    final AtomicInteger leiturasVazias = new AtomicInteger();
    final List<String> erros = Collections.synchronizedList(new ArrayList<>());
    final Map<Thread, String> estados = new ConcurrentHashMap<>();

    public BufferLimitado(Modo modo, int capacidade, int pausaMs) {
        this.modo = modo;
        this.capacidade = capacidade;
        this.pausaMs = pausaMs;
        this.vazios = new Semaphore(capacidade);
        this.dados = new int[capacidade];
        java.util.Arrays.fill(dados, VAZIO);
    }

    /** Chamado pelo produtor. */
    void inserir(int item) throws InterruptedException {
        switch (modo) {
            case CORRETO -> {
                estado("esperando vaga (vazios)");
                vazios.acquire();            // 1º espera vaga, sem segurar nada
                estado("esperando o mutex");
                mutex.acquire();             // 2º entra na região crítica
                try {
                    escrever(item);
                } finally {
                    mutex.release();
                }
                cheios.release();            // avisa que tem item novo
            }
            case SEM_MUTEX -> {
                estado("esperando vaga (vazios)");
                vazios.acquire();
                escrever(item);              // região crítica SEM proteção
                cheios.release();
            }
            case MUTEX_ANTES -> {
                estado("esperando o mutex");
                mutex.acquire();             // pega o mutex primeiro...
                try {
                    estado("SEGURA o mutex e espera vaga (vazios)");
                    vazios.acquire();        // ...e espera vaga segurando o mutex
                    escrever(item);
                    cheios.release();
                } finally {
                    mutex.release();
                }
            }
        }
        estado("produzindo o próximo item");
    }

    /** Chamado pelo consumidor. */
    int remover() throws InterruptedException {
        int item;
        switch (modo) {
            case CORRETO -> {
                estado("esperando item (cheios)");
                cheios.acquire();            // 1º espera item, sem segurar nada
                estado("esperando o mutex");
                mutex.acquire();             // 2º entra na região crítica
                try {
                    item = ler();
                } finally {
                    mutex.release();
                }
                vazios.release();            // avisa que liberou vaga
            }
            case SEM_MUTEX -> {
                estado("esperando item (cheios)");
                cheios.acquire();
                item = ler();                // região crítica SEM proteção
                vazios.release();
            }
            default -> { // MUTEX_ANTES
                estado("esperando o mutex");
                mutex.acquire();
                try {
                    estado("SEGURA o mutex e espera item (cheios)");
                    cheios.acquire();
                    item = ler();
                    vazios.release();
                } finally {
                    mutex.release();
                }
            }
        }
        estado("consumindo o item");
        return item;
    }

    // ---------------- região crítica ----------------

    private void escrever(int item) throws InterruptedException {
        int pos = entrada;
        Thread.sleep(pausaMs);
        dados[pos] = item;
        entrada = (pos + 1) % capacidade;

        int agora = ocupacao.incrementAndGet();
        maxOcupacao.accumulateAndGet(agora, Math::max);
        if (agora > capacidade) erros.add("buffer passou da capacidade: " + agora + "/" + capacidade);
        Log.info("colocou item " + item + " na posição " + pos + "   buffer " + conteudo());
    }

    private int ler() throws InterruptedException {
        int pos = saida;
        Thread.sleep(pausaMs);
        int item = dados[pos];
        dados[pos] = VAZIO;
        saida = (pos + 1) % capacidade;

        if (ocupacao.decrementAndGet() < 0) erros.add("buffer ficou com ocupação negativa");
        if (item == VAZIO) {
            leiturasVazias.incrementAndGet();
            Log.info("!! leu a posição " + pos + " VAZIA (o item foi sobrescrito)   buffer " + conteudo());
        } else {
            Log.info("retirou item " + item + " da posição " + pos + "   buffer " + conteudo());
        }
        return item;
    }

    private String conteudo() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < capacidade; i++) {
            if (i > 0) sb.append(' ');
            sb.append(dados[i] == VAZIO ? "__" : String.format("%2d", dados[i]));
        }
        return sb.append(']').toString();
    }

    private void estado(String descricao) {
        estados.put(Thread.currentThread(), descricao);
    }
}
