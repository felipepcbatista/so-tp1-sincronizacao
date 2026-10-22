package produtorconsumidor;

import java.util.concurrent.ThreadLocalRandom;
import util.Log;

/** Produtor: gera itens numerados e coloca no buffer. */
public class Produtor extends Thread {
    private final BufferLimitado buffer;
    private final int id;
    private final int itens;
    private final int tempoMaxProduzirMs;

    public Produtor(BufferLimitado buffer, int id, int itens, int tempoMaxProduzirMs) {
        super("Produtor-" + id);
        this.buffer = buffer;
        this.id = id;
        this.itens = itens;
        this.tempoMaxProduzirMs = tempoMaxProduzirMs;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < itens; i++) {
                Thread.sleep(ThreadLocalRandom.current().nextInt(tempoMaxProduzirMs + 1)); // "produz"
                int item = id * itens + i; // número único: dá para saber se algum item se perdeu
                buffer.inserir(item);
            }
            Log.info("terminou: produziu " + itens + " itens");
        } catch (InterruptedException e) {
            Log.info("interrompido");
        }
    }
}
