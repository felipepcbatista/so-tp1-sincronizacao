package fumantes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Recurso compartilhado: a mesa onde o agente coloca dois ingredientes.
 * Concentra o estado compartilhado e todos os semáforos do problema.
 */
public class Mesa {

    /** CORRETO: o agente acorda o fumante certo. INGENUO: cada ingrediente é um semáforo e os fumantes disputam. */
    public enum Modo { CORRETO, INGENUO }

    final Modo modo;
    final int tempoMaxFumarMs;
    final int pausaEntreIngredientesMs; // só no INGENUO: alarga a janela em que o deadlock acontece

    // ---------------- semáforos ----------------
    /** Exclusão mútua sobre o conteúdo da mesa (região crítica). */
    final Semaphore mutex = new Semaphore(1);
    /** O agente dorme aqui depois de servir; o fumante dá release quando termina de fumar. */
    final Semaphore agente = new Semaphore(0);
    /** CORRETO: um semáforo por fumante, todos começam em 0 (fumantes bloqueados). */
    final Semaphore[] fumante = { new Semaphore(0), new Semaphore(0), new Semaphore(0) };
    /** INGENUO: um semáforo por ingrediente na mesa. */
    final Semaphore[] ingrediente = { new Semaphore(0), new Semaphore(0), new Semaphore(0) };

    // ---------------- estado compartilhado ----------------
    private final EnumSet<Ingrediente> naMesa = EnumSet.noneOf(Ingrediente.class);

    // ---------------- instrumentação para validação ----------------
    final AtomicInteger fumandoAgora = new AtomicInteger();
    final AtomicInteger maxFumandoJuntos = new AtomicInteger();
    final AtomicIntegerArray cigarrosPorFumante = new AtomicIntegerArray(3);
    final AtomicIntegerArray rodadasPorFaltante = new AtomicIntegerArray(3);
    final List<String> erros = Collections.synchronizedList(new ArrayList<>());

    public Mesa(Modo modo, int tempoMaxFumarMs, int pausaEntreIngredientesMs) {
        this.modo = modo;
        this.tempoMaxFumarMs = tempoMaxFumarMs;
        this.pausaEntreIngredientesMs = pausaEntreIngredientesMs;
    }

    /** Agente coloca dois ingredientes. Região crítica protegida por mutex. */
    void colocar(Ingrediente a, Ingrediente b) throws InterruptedException {
        mutex.acquire();
        try {
            if (!naMesa.isEmpty()) erros.add("agente serviu com a mesa ainda ocupada: " + naMesa);
            naMesa.add(a);
            naMesa.add(b);
        } finally {
            mutex.release();
        }
    }

    /** Fumante retira da mesa os dois ingredientes que lhe faltam. Região crítica protegida por mutex. */
    void retirar(Ingrediente fumanteTem) throws InterruptedException {
        mutex.acquire();
        try {
            EnumSet<Ingrediente> esperado = EnumSet.complementOf(EnumSet.of(fumanteTem));
            if (!naMesa.equals(esperado)) {
                erros.add("fumante de " + fumanteTem + " acordou mas a mesa tinha " + naMesa);
            }
            naMesa.clear();
        } finally {
            mutex.release();
        }
    }
}
