package filosofos;

import java.util.concurrent.ThreadLocalRandom;
import util.Log;

/** Filósofo: alterna entre meditar e comer. Para comer precisa dos dois hashis vizinhos. */
public class Filosofo extends Thread {
    private final Jantar mesa;
    private final int id;
    volatile String estado = "meditando";

    public Filosofo(Jantar mesa, int id) {
        super("Filosofo-" + id);
        this.mesa = mesa;
        this.id = id;
        setDaemon(true);
    }

    @Override
    public void run() {
        int dir = mesa.direita(id), esq = mesa.esquerda(id);
        try {
            for (int k = 1; k <= mesa.refeicoesPorFilosofo; k++) {
                meditar();

                if (mesa.modo == Jantar.Modo.GARCOM) {
                    estado = "esperando o garçom liberar lugar";
                    if (!mesa.garcom.tryAcquire()) {
                        Log.info("garçom barrou: já há " + (mesa.n - 1) + " disputando hashis");
                        mesa.garcom.acquire(); // no máximo N-1 entram na disputa
                    }
                }

                estado = "esperando hashi " + dir;
                pegar(dir, "direita");
                Thread.sleep(mesa.pausaEntreHashisMs);
                estado = "SEGURA hashi " + dir + " e espera hashi " + esq;
                pegar(esq, "esquerda");

                comer(k);

                soltar(esq);
                soltar(dir);
                Log.info("soltou hashis " + dir + " e " + esq + ", volta a meditar");
                if (mesa.modo == Jantar.Modo.GARCOM) mesa.garcom.release();
            }
            estado = "satisfeito";
            Log.info("satisfeito, saiu da mesa");
        } catch (InterruptedException e) {
            Log.info("interrompido");
        }
    }

    private void pegar(int h, String lado) throws InterruptedException {
        mesa.hashi[h].acquire();
        // invariante: o semáforo garantiu exclusão, então o hashi tem que estar livre
        if (!mesa.donoDoHashi.compareAndSet(h, Jantar.LIVRE, id)) {
            mesa.erros.add("filósofo " + id + " pegou hashi " + h + " que estava com " + mesa.donoDoHashi.get(h));
        }
        Log.info("pegou hashi " + h + " (" + lado + ")");
    }

    private void soltar(int h) {
        if (!mesa.donoDoHashi.compareAndSet(h, id, Jantar.LIVRE)) {
            mesa.erros.add("filósofo " + id + " soltou hashi " + h + " que não era dele");
        }
        mesa.hashi[h].release();
    }

    private void comer(int k) throws InterruptedException {
        estado = "comendo";
        mesa.comendo.set(id, 1);
        int agora = mesa.comendoAgora.incrementAndGet();
        mesa.maxComendoJuntos.accumulateAndGet(agora, Math::max);

        int vizDir = (id + mesa.n - 1) % mesa.n, vizEsq = (id + 1) % mesa.n;
        if (mesa.comendo.get(vizDir) == 1 || mesa.comendo.get(vizEsq) == 1) {
            mesa.erros.add("filósofo " + id + " comeu junto com um vizinho");
        }

        Log.info("COMENDO (refeição " + k + "/" + mesa.refeicoesPorFilosofo + ")");
        Thread.sleep(ThreadLocalRandom.current().nextInt(mesa.tempoMaxComerMs + 1));

        mesa.comendoAgora.decrementAndGet();
        mesa.comendo.set(id, 0);
        mesa.refeicoes.incrementAndGet(id);
    }

    private void meditar() throws InterruptedException {
        estado = "meditando";
        Thread.sleep(ThreadLocalRandom.current().nextInt(mesa.tempoMaxPensarMs + 1));
    }
}
