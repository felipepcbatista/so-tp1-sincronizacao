package selvagens;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Recurso compartilhado do problema: o caldeirão com N porções.
 * Contém os semáforos que sincronizam selvagens e cozinheiro.
 */
public class Caldeirao {

    private final int capacidade;
    private final boolean verbose;

    // ===== Variável compartilhada =====
    private int porcoes;

    // ===== Semáforos =====
    private final Semaphore mutex = new Semaphore(1);           // só um selvagem se serve por vez
    private final Semaphore caldeiraoVazio = new Semaphore(0);  // selvagem -> acorda o cozinheiro
    private final Semaphore caldeiraoCheio = new Semaphore(0);  // cozinheiro -> avisa que encheu

    // ===== Contadores usados somente para validação nos testes =====
    public final AtomicInteger servindoAgora = new AtomicInteger();
    public final AtomicInteger comendoAgora = new AtomicInteger();
    public final AtomicInteger maxComendoSimultaneo = new AtomicInteger();
    public final AtomicInteger violacoes = new AtomicInteger();
    public final AtomicInteger totalServido = new AtomicInteger();
    public final AtomicInteger totalEnchimentos = new AtomicInteger();

    public Caldeirao(int capacidade, boolean verbose) {
        this.capacidade = capacidade;
        this.porcoes = capacidade;      // o caldeirão começa cheio
        this.verbose = verbose;
    }

    /** Chamado pelo selvagem para se servir. */
    public void servir(String quem) throws InterruptedException {
        mutex.acquire();                                   // início da região crítica
        try {
            if (servindoAgora.incrementAndGet() != 1) violacoes.incrementAndGet();

            if (porcoes == 0) {
                if (verbose) System.out.println(quem + " encontrou o caldeirão vazio e acordou o cozinheiro");
                caldeiraoVazio.release();                  // acorda o cozinheiro
                caldeiraoCheio.acquire();                  // espera o caldeirão ser enchido
            }

            if (porcoes <= 0) violacoes.incrementAndGet(); // nunca servir de caldeirão vazio
            porcoes--;
            totalServido.incrementAndGet();
            if (verbose) System.out.println(quem + " se serviu (restam " + porcoes + ")");

            servindoAgora.decrementAndGet();
        } finally {
            mutex.release();                               // fim da região crítica
        }
    }

    /** Chamado pelo cozinheiro: dorme até ser acordado e então enche o caldeirão. */
    public void encher() throws InterruptedException {
        caldeiraoVazio.acquire();                          // dorme até um selvagem acordá-lo
        if (porcoes != 0) violacoes.incrementAndGet();     // só pode encher se estiver vazio
        porcoes = capacidade;
        totalEnchimentos.incrementAndGet();
        if (verbose) System.out.println("Cozinheiro encheu o caldeirão com " + capacidade + " porções");
        caldeiraoCheio.release();                          // avisa o selvagem que está cheio
    }

    // ===== Apoio à validação: vários selvagens podem comer ao mesmo tempo =====
    public void comecouAComer() {
        int n = comendoAgora.incrementAndGet();
        maxComendoSimultaneo.accumulateAndGet(n, Math::max);
    }

    public void terminouDeComer() {
        comendoAgora.decrementAndGet();
    }
}
