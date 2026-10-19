package barbearia;

import java.util.concurrent.ThreadLocalRandom;
import util.Log;

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
                if (loja.clientes.availablePermits() == 0) {
                    loja.vezesQueDormiu.incrementAndGet();
                    estado = "dormindo, barbearia vazia";
                    Log.info("ninguém na loja, vou dormir na cadeira");
                }
                loja.clientes.acquire();

                estado = "acordou, chamando o próximo da sala";
                loja.chamarProximo();

                cortar();
                loja.corteFeito.release();
                loja.clienteSaiu.acquire();
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
