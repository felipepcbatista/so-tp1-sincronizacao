package barbearia;

import util.Log;

public class Cliente extends Thread {
    private final Barbearia loja;
    private final int id;
    volatile String estado = "a caminho da barbearia";

    public Cliente(Barbearia loja, int id) {
        super("Cliente-" + id);
        this.loja = loja;
        this.id = id;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            estado = "entrando, conferindo as cadeiras";
            if (!loja.entrarNaSala()) {
                estado = "foi embora, sala cheia";
                Log.info("sala cheia, fui embora sem cortar o cabelo");
                return;
            }

            estado = "esperando na sala";
            Log.info("sentou na sala de espera");

            if (loja.modo == Barbearia.Modo.MUTEX_PRESO) {
                estado = "SEGURA o mutex e espera o barbeiro";
                loja.barbeiros.acquire();
                loja.mutex.release();
            } else {
                loja.barbeiros.acquire();
            }

            estado = "na cadeira do barbeiro";
            loja.ocuparCadeira(id);
            Log.info("sentou na cadeira do barbeiro");

            loja.corteFeito.acquire();
            loja.liberarCadeira();
            loja.clienteSaiu.release();

            estado = "atendido, saiu da barbearia";
            Log.info("cabelo cortado, saiu da barbearia");
        } catch (InterruptedException e) {
            Log.info("desistiu de esperar");
        }
    }
}
