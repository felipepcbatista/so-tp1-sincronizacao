package selvagens;

/** Thread do cozinheiro: dorme e só acorda quando um selvagem encontra o caldeirão vazio. */
public class Cozinheiro extends Thread {

    private final Caldeirao caldeirao;

    public Cozinheiro(Caldeirao caldeirao) {
        super("Cozinheiro");
        this.caldeirao = caldeirao;
    }

    @Override
    public void run() {
        try {
            while (true) {
                caldeirao.encher();
            }
        } catch (InterruptedException e) {
            // encerrado ao final do jantar (interrompido pela Main ou pelo teste)
        }
    }
}
