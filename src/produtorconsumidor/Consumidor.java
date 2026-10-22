package produtorconsumidor;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerArray;
import util.Log;

/** Consumidor: retira itens do buffer e registra o que recebeu. */
public class Consumidor extends Thread {
    private final BufferLimitado buffer;
    private final int itens;
    private final int tempoMaxConsumirMs;
    private final AtomicIntegerArray vezesConsumido; // quantas vezes cada item foi recebido

    public Consumidor(BufferLimitado buffer, int id, int itens, int tempoMaxConsumirMs,
                      AtomicIntegerArray vezesConsumido) {
        super("Consumidor-" + id);
        this.buffer = buffer;
        this.itens = itens;
        this.tempoMaxConsumirMs = tempoMaxConsumirMs;
        this.vezesConsumido = vezesConsumido;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < itens; i++) {
                int item = buffer.remover();
                if (item != BufferLimitado.VAZIO) vezesConsumido.incrementAndGet(item);
                Thread.sleep(ThreadLocalRandom.current().nextInt(tempoMaxConsumirMs + 1)); // "consome"
            }
            Log.info("terminou: consumiu " + itens + " itens");
        } catch (InterruptedException e) {
            Log.info("interrompido");
        }
    }
}
