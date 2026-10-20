package selvagens;

import java.util.Random;

/** Thread do selvagem: se serve do caldeirão e depois come por um tempo aleatório. */
public class Selvagem extends Thread {

    private final Caldeirao caldeirao;
    private final int id;
    private final int refeicoes;
    private final int tempoMaxComendo;
    private final boolean verbose;
    private final Random random = new Random();

    public Selvagem(Caldeirao caldeirao, int id, int refeicoes, int tempoMaxComendo, boolean verbose) {
        super("Selvagem-" + id);
        this.caldeirao = caldeirao;
        this.id = id;
        this.refeicoes = refeicoes;
        this.tempoMaxComendo = tempoMaxComendo;
        this.verbose = verbose;
    }

    @Override
    public void run() {
        String nome = "Selvagem " + id;
        try {
            for (int i = 0; i < refeicoes; i++) {
                caldeirao.servir(nome);

                caldeirao.comecouAComer();                       // comer NÃO é região crítica
                if (verbose) System.out.println(nome + " está comendo");
                Thread.sleep(random.nextInt(tempoMaxComendo));
                caldeirao.terminouDeComer();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
