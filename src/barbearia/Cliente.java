package barbearia;

import util.Log;

/**
 * Cliente: chega, senta se houver cadeira livre e vai embora se a sala estiver cheia.
 * Não há laço — cada cliente corta o cabelo uma única vez, como diz o enunciado.
 */
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
                // Versão quebrada: espera o barbeiro SEM ter liberado o mutex.
                // O barbeiro acorda, tenta pegar o mutex para chamar o próximo e trava:
                // cliente espera o barbeiro, barbeiro espera o mutex. Posse e espera -> deadlock.
                estado = "SEGURA o mutex e espera o barbeiro";
                loja.barbeiros.acquire();
                loja.mutex.release(); // nunca chega aqui
            } else {
                loja.barbeiros.acquire(); // espera o barbeiro ficar livre
            }

            estado = "na cadeira do barbeiro";
            loja.ocuparCadeira(id);
            Log.info("sentou na cadeira do barbeiro");

            loja.corteFeito.acquire(); // só levanta quando o corte termina
            loja.liberarCadeira();
            loja.clienteSaiu.release(); // libera a cadeira para o próximo

            estado = "atendido, saiu da barbearia";
            Log.info("cabelo cortado, saiu da barbearia");
        } catch (InterruptedException e) {
            Log.info("desistiu de esperar");
        }
    }
}
