package fumantes;

import java.util.concurrent.ThreadLocalRandom;
import util.Log;

/** Representante de cigarros: estoque infinito, serve dois ingredientes por rodada. */
public class Agente extends Thread {
    private final Mesa mesa;
    private final int rodadas;
    volatile String estado = "iniciando";

    public Agente(Mesa mesa, int rodadas) {
        super("Agente");
        this.mesa = mesa;
        this.rodadas = rodadas;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            for (int r = 1; r <= rodadas; r++) {
                Ingrediente falta = Ingrediente.values()[ThreadLocalRandom.current().nextInt(3)];
                Ingrediente[] par = falta.complemento();

                mesa.colocar(par[0], par[1]);
                mesa.rodadasPorFaltante.incrementAndGet(falta.ordinal());
                Log.info("rodada " + r + ": colocou " + par[0] + " + " + par[1] + " na mesa");

                if (mesa.modo == Mesa.Modo.CORRETO) {
                    // acorda SÓ o fumante que tem o ingrediente que falta
                    mesa.fumante[falta.ordinal()].release();
                } else {
                    // versão ingênua: libera os ingredientes e deixa os fumantes disputarem
                    mesa.ingrediente[par[0].ordinal()].release();
                    mesa.ingrediente[par[1].ordinal()].release();
                }

                estado = "esperando o fumante terminar o cigarro (rodada " + r + ")";
                mesa.agente.acquire(); // bloqueia até o fumante avisar que terminou
            }
            estado = "terminou";
            Log.info("fim do expediente: " + rodadas + " rodadas servidas");
        } catch (InterruptedException e) {
            Log.info("interrompido");
        }
    }
}
