package barbearia;

import java.util.concurrent.ThreadLocalRandom;
import util.Log;

/** Barbeiro: dorme na cadeira enquanto não há clientes e corta o cabelo de um cliente por vez. */
public class Barbeiro extends Thread {
    private final Barbearia loja;
    volatile String estado = "chegando para trabalhar";

    public Barbeiro(Barbearia loja) {
        super("Barbeiro");
        this.loja = loja;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Bloqueia no semáforo 'clientes', que começa em 0: é aqui que o barbeiro dorme.
                if (loja.clientes.availablePermits() == 0) {
                    loja.vezesQueDormiu.incrementAndGet();
                    estado = "dormindo, barbearia vazia";
                    Log.info("ninguém na loja, vou dormir na cadeira");
                }
                loja.clientes.acquire();

                estado = "acordou, chamando o próximo da sala";
                loja.chamarProximo(); // região crítica: tira um da sala e avisa que a cadeira está livre

                cortar();
                loja.corteFeito.release(); // avisa o cliente que o corte acabou
                loja.clienteSaiu.acquire(); // só chama o próximo depois que a cadeira esvazia
            }
        } catch (InterruptedException e) {
            estado = "fechou a barbearia";
            Log.info("fim do expediente, fechou a barbearia");
        }
    }

    private void cortar() throws InterruptedException {
        estado = "cortando cabelo";
        Log.info("cortando cabelo...");
        Thread.sleep(ThreadLocalRandom.current().nextInt(loja.tempoCorteMs + 1));
        loja.cortesFeitos.incrementAndGet();
        Log.info("terminou o corte");
    }
}
