package filosofos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Recurso compartilhado: a mesa redonda com N hashis.
 * Filósofo f(i) usa o hashi p(i) à direita e p(i+1) à esquerda.
 */
public class Jantar {

    /** GARCOM: no máximo N-1 filósofos disputam hashis ao mesmo tempo. INGENUO: todos pegam direita e depois esquerda. */
    public enum Modo { GARCOM, INGENUO }

    static final int LIVRE = -1;

    final int n;
    final Modo modo;
    final int refeicoesPorFilosofo;
    final int tempoMaxPensarMs, tempoMaxComerMs;
    final int pausaEntreHashisMs; // alarga a janela do deadlock (aplicada nos DOIS modos)

    // ---------------- semáforos ----------------
    /** Um semáforo binário por hashi: só um filósofo segura cada hashi. */
    final Semaphore[] hashi;
    /** Garçom: contador iniciado em N-1. Quebra a espera circular. fair=true evita inanição. */
    final Semaphore garcom;

    // ---------------- instrumentação para validação ----------------
    final AtomicIntegerArray donoDoHashi;   // quem segura cada hashi (LIVRE se ninguém)
    final AtomicIntegerArray comendo;       // 1 se o filósofo i está comendo
    final AtomicIntegerArray refeicoes;     // refeições feitas por filósofo
    final AtomicInteger comendoAgora = new AtomicInteger();
    final AtomicInteger maxComendoJuntos = new AtomicInteger();
    final List<String> erros = Collections.synchronizedList(new ArrayList<>());

    public Jantar(int n, Modo modo, int refeicoesPorFilosofo, int tempoMaxPensarMs,
                  int tempoMaxComerMs, int pausaEntreHashisMs) {
        this.n = n;
        this.modo = modo;
        this.refeicoesPorFilosofo = refeicoesPorFilosofo;
        this.tempoMaxPensarMs = tempoMaxPensarMs;
        this.tempoMaxComerMs = tempoMaxComerMs;
        this.pausaEntreHashisMs = pausaEntreHashisMs;

        hashi = new Semaphore[n];
        for (int i = 0; i < n; i++) hashi[i] = new Semaphore(1, true);
        garcom = new Semaphore(n - 1, true);

        donoDoHashi = new AtomicIntegerArray(n);
        for (int i = 0; i < n; i++) donoDoHashi.set(i, LIVRE);
        comendo = new AtomicIntegerArray(n);
        refeicoes = new AtomicIntegerArray(n);
    }

    int direita(int filosofo) { return filosofo; }

    int esquerda(int filosofo) { return (filosofo + 1) % n; }
}
