package util;

/** Log com o tempo desde o início da execução e o nome da thread que imprimiu. */
public final class Log {
    private static final long INICIO = System.nanoTime();
    public static volatile boolean ativo = true;

    private Log() {}

    public static void info(String msg) {
        if (!ativo) return;
        long ms = (System.nanoTime() - INICIO) / 1_000_000;
        // printf é sincronizado internamente: as linhas de threads diferentes não se misturam
        System.out.printf("[%6d ms] %-16s %s%n", ms, Thread.currentThread().getName(), msg);
    }
}
