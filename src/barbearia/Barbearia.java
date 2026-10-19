package barbearia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Recurso compartilhado: a barbearia.
 * Concentra o estado compartilhado (sala de espera e cadeira do barbeiro) e todos os semáforos.
 */
public class Barbearia {

    /**
     * CORRETO: solução do Tanenbaum, com a sala de espera protegida por mutex.
     * SEM_MUTEX: o cliente decide se fica sem exclusão mútua (race condition: a sala estoura).
     * MUTEX_PRESO: o cliente espera o barbeiro ainda segurando o mutex (deadlock).
     */
    public enum Modo { CORRETO, SEM_MUTEX, MUTEX_PRESO }

    final Modo modo;
    final int cadeiras;          // cadeiras da sala de espera (n do enunciado)
    final int tempoCorteMs;
    final int pausaNaDecisaoMs;  // só no SEM_MUTEX: alarga a janela em que a race condition acontece

    // ---------------- semáforos ----------------
    /** Exclusão mútua sobre a sala de espera (região crítica em volta de 'esperando'). */
    final Semaphore mutex = new Semaphore(1);
    /** Conta os clientes à espera. O barbeiro dorme aqui quando a barbearia está vazia. */
    final Semaphore clientes = new Semaphore(0);
    /** 0 ou 1: barbeiro ocioso pronto para cortar. O cliente espera aqui a sua vez. */
    final Semaphore barbeiros = new Semaphore(0);
    /** Fim do corte: o barbeiro libera, o cliente espera para então levantar da cadeira. */
    final Semaphore corteFeito = new Semaphore(0);
    /** Cadeira desocupada: o cliente libera ao levantar, o barbeiro espera antes de chamar o próximo. */
    final Semaphore clienteSaiu = new Semaphore(0);

    // ---------------- estado compartilhado ----------------
    /**
     * Clientes à espera de atendimento (o 'waiting' do enunciado). É uma cópia de 'clientes',
     * porque não há como ler o valor atual de um semáforo. Só pode ser tocado dentro do mutex.
     */
    private int esperando = 0;

    // ---------------- instrumentação para validação ----------------
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

    /**
     * Cliente entra na sala de espera, se houver cadeira livre.
     * Região crítica: lê 'esperando', decide, e incrementa.
     *
     * @return true se sentou e vai esperar; false se a sala estava cheia e ele foi embora.
     */
    boolean entrarNaSala() throws InterruptedException {
        if (modo != Modo.SEM_MUTEX) mutex.acquire();
        boolean ficou = false;
        try {
            if (esperando >= cadeiras) {
                desistiram.incrementAndGet();
                return false;
            }
            // Versão quebrada: a decisão já foi tomada, mas o incremento só acontece depois da pausa.
            // Sem mutex, outros clientes leem o mesmo 'esperando' antigo e sentam na mesma cadeira.
            if (modo == Modo.SEM_MUTEX) Thread.sleep(pausaNaDecisaoMs);

            esperando++;
            int agora = esperando;
            maxEsperando.accumulateAndGet(agora, Math::max);
            if (agora > cadeiras) {
                erros.add("sala de espera estourou: " + agora + " clientes para " + cadeiras + " cadeiras");
            }
            clientes.release(); // acorda o barbeiro
            ficou = true;
            return true;
        } finally {
            // No MUTEX_PRESO quem ficou sai daqui AINDA segurando o mutex: é esse o defeito.
            // Quem foi embora libera normalmente, nos três modos que usam mutex.
            if (modo == Modo.CORRETO || (modo == Modo.MUTEX_PRESO && !ficou)) mutex.release();
        }
    }

    /** Barbeiro tira um cliente da sala de espera. Região crítica. */
    void chamarProximo() throws InterruptedException {
        mutex.acquire();
        try {
            esperando--;
            if (esperando < 0) erros.add("barbeiro chamou cliente com a sala vazia (esperando = " + esperando + ")");
            barbeiros.release(); // avisa o cliente que a cadeira do barbeiro está livre
        } finally {
            mutex.release();
        }
    }

    /** Cliente senta na cadeira do barbeiro. Só pode haver um por vez. */
    void ocuparCadeira(int idCliente) {
        int agora = naCadeiraAgora.incrementAndGet();
        maxNaCadeira.accumulateAndGet(agora, Math::max);
        if (agora > 1) erros.add("cliente " + idCliente + " sentou na cadeira com " + (agora - 1) + " já lá");
    }

    /** Cliente sai da cadeira do barbeiro, já cortado. */
    void liberarCadeira() {
        naCadeiraAgora.decrementAndGet();
        atendidos.incrementAndGet();
    }
}
