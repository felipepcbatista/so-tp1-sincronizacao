package leitores;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Memoria {

    public enum Modo { CORRETO, PRIORIDADE_LEITOR, SEM_SINCRONIA }

    final Modo modo;
    final int tempoLeituraMs;
    final int tempoEscritaMs;
    final int pausaEntrePosicoesMs;

    final Semaphore fila = new Semaphore(1, true);
    final Semaphore mutex = new Semaphore(1);
    final Semaphore recurso = new Semaphore(1, true);

    private int leitores = 0;
    private final int[] m;

    final AtomicInteger leitoresAgora = new AtomicInteger();
    final AtomicInteger maxLeitoresJuntos = new AtomicInteger();
    final AtomicInteger escritoresAgora = new AtomicInteger();
    final AtomicInteger maxEscritoresJuntos = new AtomicInteger();
    final AtomicInteger leiturasFeitas = new AtomicInteger();
    final AtomicInteger escritasFeitas = new AtomicInteger();
    final AtomicLong maiorEsperaEscritorMs = new AtomicLong();
    final List<String> erros = Collections.synchronizedList(new ArrayList<>());

    public Memoria(Modo modo, int tamanho, int tempoLeituraMs, int tempoEscritaMs, int pausaEntrePosicoesMs) {
        this.modo = modo;
        this.tempoLeituraMs = tempoLeituraMs;
        this.tempoEscritaMs = tempoEscritaMs;
        this.pausaEntrePosicoesMs = pausaEntrePosicoesMs;
        this.m = new int[tamanho];
    }

    void entrarLeitura() throws InterruptedException {
        if (modo == Modo.SEM_SINCRONIA) return;

        if (modo == Modo.CORRETO) {
            fila.acquire();
        }
        mutex.acquire();
        leitores++;
        if (leitores == 1) recurso.acquire();
        mutex.release();
        if (modo == Modo.CORRETO) {
            fila.release();
        }
    }

    void sairLeitura() throws InterruptedException {
        if (modo == Modo.SEM_SINCRONIA) return;

        mutex.acquire();
        leitores--;
        if (leitores == 0) recurso.release();
        mutex.release();
    }

    void entrarEscrita() throws InterruptedException {
        if (modo == Modo.SEM_SINCRONIA) return;

        long inicio = System.currentTimeMillis();
        if (modo == Modo.CORRETO) {
            fila.acquire();
            recurso.acquire();
            fila.release();
        } else {
            recurso.acquire();
        }
        long esperou = System.currentTimeMillis() - inicio;
        maiorEsperaEscritorMs.accumulateAndGet(esperou, Math::max);
    }

    void sairEscrita() {
        if (modo == Modo.SEM_SINCRONIA) return;
        recurso.release();
    }

    int[] ler() throws InterruptedException {
        int agora = leitoresAgora.incrementAndGet();
        maxLeitoresJuntos.accumulateAndGet(agora, Math::max);
        if (escritoresAgora.get() > 0) {
            erros.add("leitor entrou na área com um escritor dentro");
        }

        int[] copia = new int[m.length];
        for (int i = 0; i < m.length; i++) {
            copia[i] = m[i];
            if (pausaEntrePosicoesMs > 0) Thread.sleep(pausaEntrePosicoesMs);
        }
        if (Arrays.stream(copia).distinct().count() > 1) {
            erros.add("leitura inconsistente: " + Arrays.toString(copia));
        }

        Thread.sleep(tempoLeituraMs);
        leitoresAgora.decrementAndGet();
        leiturasFeitas.incrementAndGet();
        return copia;
    }

    void escrever(int valor) throws InterruptedException {
        int agora = escritoresAgora.incrementAndGet();
        maxEscritoresJuntos.accumulateAndGet(agora, Math::max);
        if (agora > 1) erros.add("dois escritores na área ao mesmo tempo");
        if (leitoresAgora.get() > 0) {
            erros.add("escritor entrou na área com " + leitoresAgora.get() + " leitor(es) dentro");
        }

        for (int i = 0; i < m.length; i++) {
            m[i] = valor;
            if (pausaEntrePosicoesMs > 0) Thread.sleep(pausaEntrePosicoesMs);
        }

        Thread.sleep(tempoEscritaMs);
        escritoresAgora.decrementAndGet();
        escritasFeitas.incrementAndGet();
    }

    int tamanho() {
        return m.length;
    }
}
