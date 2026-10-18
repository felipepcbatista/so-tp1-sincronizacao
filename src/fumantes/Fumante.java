package fumantes;

import java.util.concurrent.ThreadLocalRandom;
import util.Log;

/** Fumante que tem estoque infinito de UM ingrediente e precisa dos outros dois. */
public class Fumante extends Thread {
    private final Mesa mesa;
    private final Ingrediente tem;
    volatile String estado = "bloqueado, esperando o agente";

    public Fumante(Mesa mesa, Ingrediente tem) {
        super("Fumante-" + tem);
        this.mesa = mesa;
        this.tem = tem;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (mesa.modo == Mesa.Modo.CORRETO) {
                    estado = "bloqueado, esperando o agente";
                    mesa.fumante[tem.ordinal()].acquire(); // dorme até o agente chamar ESTE fumante
                } else {
                    pegarIngredientesIngenuo();
                }

                mesa.retirar(tem);
                Log.info("pegou os ingredientes e enrolou o cigarro");
                fumar();
                Log.info("terminou de fumar, desbloqueia o agente");
                mesa.agente.release();
            }
        } catch (InterruptedException e) {
            Log.info("foi embora");
        }
    }

    /** Versão ingênua: pega um ingrediente de cada vez. Pode ficar segurando um e esperando o outro para sempre. */
    private void pegarIngredientesIngenuo() throws InterruptedException {
        Ingrediente[] precisa = tem.complemento();
        estado = "esperando " + precisa[0];
        mesa.ingrediente[precisa[0].ordinal()].acquire();
        Log.info("pegou " + precisa[0] + ", agora precisa de " + precisa[1]);
        Thread.sleep(mesa.pausaEntreIngredientesMs);
        estado = "SEGURA " + precisa[0] + " e espera " + precisa[1];
        mesa.ingrediente[precisa[1].ordinal()].acquire();
    }

    private void fumar() throws InterruptedException {
        estado = "fumando";
        int agora = mesa.fumandoAgora.incrementAndGet();
        mesa.maxFumandoJuntos.accumulateAndGet(agora, Math::max);
        Log.info("fumando no fumódromo...");
        Thread.sleep(ThreadLocalRandom.current().nextInt(mesa.tempoMaxFumarMs + 1));
        mesa.fumandoAgora.decrementAndGet();
        mesa.cigarrosPorFumante.incrementAndGet(tem.ordinal());
    }
}
