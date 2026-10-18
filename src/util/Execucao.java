package util;

import java.util.List;
import java.util.function.Function;

/** Espera as threads terminarem e detecta deadlock por tempo limite. */
public final class Execucao {
    private Execucao() {}

    /**
     * Espera as threads de 'esperar' até o tempo limite.
     * Se alguma continuar viva, considera deadlock e imprime o estado de cada thread de 'descrever'.
     */
    public static boolean aguardar(List<? extends Thread> esperar, List<? extends Thread> descrever,
                                   long timeoutMs, Function<Thread, String> estado) throws InterruptedException {
        long limite = System.currentTimeMillis() + timeoutMs;
        for (Thread t : esperar) {
            long resta = limite - System.currentTimeMillis();
            if (resta > 0) t.join(resta);
        }
        boolean terminou = esperar.stream().noneMatch(Thread::isAlive);
        if (!terminou && Log.ativo) {
            System.out.println();
            System.out.println("!!! DEADLOCK: a execução não terminou em " + timeoutMs + " ms. Estado de cada thread:");
            for (Thread t : descrever) {
                System.out.printf("    %-16s [%-7s] %s%n", t.getName(), t.getState(), estado.apply(t));
            }
        }
        return terminou;
    }

    /** Interrompe as threads (acquire/sleep lançam InterruptedException) e espera elas saírem. */
    public static void encerrar(List<? extends Thread> threads) throws InterruptedException {
        threads.forEach(Thread::interrupt);
        for (Thread t : threads) t.join(1000);
    }
}
