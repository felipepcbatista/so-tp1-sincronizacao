package barbearia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Barbearia {

    public enum Modo { CORRETO, SEM_MUTEX, MUTEX_PRESO }

    final Modo modo;
    final int cadeiras;
    final int tempoCorteMs;
    final int pausaNaDecisaoMs;

    final Semaphore mutex = new Semaphore(1);
    final Semaphore clientes = new Semaphore(0);
    final Semaphore barbeiros = new Semaphore(0);
    final Semaphore corteFeito = new Semaphore(0);
    final Semaphore clienteSaiu = new Semaphore(0);

    private int esperando = 0;

    final AtomicInteger maxEsperando = new AtomicInteger();
    final AtomicInteger naCadeiraAgora = new AtomicInteger();
    final AtomicInteger maxNaCadeira = new AtomicInteger();
    final AtomicInteger atendidos = new AtomicInteger();
    final AtomicInteger desistiram = new AtomicInteger();
    final AtomicInteger cortesFeitos = new AtomicInteger();
    final AtomicInteger vezesQueDormiu = new AtomicInteger();
    final List<String> erros = Collections.synchronizedList(new ArrayList<>());

    public Barbearia(Modo modo, int cadeiras, int tempoCorteMs, int pausaNaDecisaoMs) {
        this.modo = modo;
        this.cadeiras = cadeiras;
        this.tempoCorteMs = tempoCorteMs;
        this.pausaNaDecisaoMs = pausaNaDecisaoMs;
    }

    boolean entrarNaSala() throws InterruptedException {
        if (modo != Modo.SEM_MUTEX) mutex.acquire();
        boolean ficou = false;
        try {
            if (esperando >= cadeiras) {
                desistiram.incrementAndGet();
                return false;
            }
            // se quebrar esse mutex eu n sei oq faço desculpa professor :(
            if (modo == Modo.SEM_MUTEX) Thread.sleep(pausaNaDecisaoMs);

            esperando++;
            int agora = esperando;
            maxEsperando.accumulateAndGet(agora, Math::max);
            if (agora > cadeiras) {
                erros.add("sala de espera estourou: " + agora + " clientes para " + cadeiras + " cadeiras");
            }
            clientes.release();
            ficou = true;
            return true;
        } finally {
            if (modo == Modo.CORRETO || (modo == Modo.MUTEX_PRESO && !ficou)) mutex.release();
        }
    }

    void chamarProximo() throws InterruptedException {
        mutex.acquire();
        try {
            esperando--;
            if (esperando < 0) erros.add("barbeiro chamou cliente com a sala vazia (esperando = " + esperando + ")");
            barbeiros.release();
        } finally {
            mutex.release();
        }
    }

    void ocuparCadeira(int idCliente) {
        int agora = naCadeiraAgora.incrementAndGet();
        maxNaCadeira.accumulateAndGet(agora, Math::max);
        if (agora > 1) erros.add("cliente " + idCliente + " sentou na cadeira com " + (agora - 1) + " já lá");
    }

    void liberarCadeira() {
        naCadeiraAgora.decrementAndGet();
        atendidos.incrementAndGet();
    }
}
