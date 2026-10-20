package selvagens;

/** Demonstração: 5 selvagens, caldeirão com 4 porções, 3 refeições por selvagem. */
public class MainSelvagens {

    public static void main(String[] args) throws InterruptedException {
        final int N = 4, SELVAGENS = 5, REFEICOES = 3;

        Caldeirao caldeirao = new Caldeirao(N, true);
        Cozinheiro cozinheiro = new Cozinheiro(caldeirao);
        Selvagem[] selvagens = new Selvagem[SELVAGENS];

        cozinheiro.start();
        for (int i = 0; i < SELVAGENS; i++) {
            selvagens[i] = new Selvagem(caldeirao, i + 1, REFEICOES, 30, true);
            selvagens[i].start();
        }

        for (Selvagem s : selvagens) s.join();
        cozinheiro.interrupt();     // fim do jantar: o cozinheiro pode parar
        cozinheiro.join();

        System.out.println("\nFim do jantar. Porções servidas: " + caldeirao.totalServido.get()
                + " | enchimentos: " + caldeirao.totalEnchimentos.get());
    }
}
