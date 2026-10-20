package selvagens;

/** Testes automáticos de validação da sincronização. */
public class TesteSelvagens {

    private static final long TIMEOUT_MS = 30000;

    public static void main(String[] args) throws InterruptedException {
        boolean t1 = executar("Teste 1: N=4, 5 selvagens, 6 refeições cada", 4, 5, 6, true);
        boolean t2 = executar("Teste 2: N=1 (caldeirão esvazia a cada porção), 3 selvagens, 4 refeições", 1, 3, 4, false);
        boolean t3 = executar("Teste 3: N=100 (cozinheiro nunca deve ser acordado), 3 selvagens, 5 refeições", 100, 3, 5, false);

        System.out.println("\n===== RESUMO =====");
        System.out.println("Teste 1: " + (t1 ? "APROVADO" : "REPROVADO"));
        System.out.println("Teste 2: " + (t2 ? "APROVADO" : "REPROVADO"));
        System.out.println("Teste 3: " + (t3 ? "APROVADO" : "REPROVADO"));
        if (!(t1 && t2 && t3)) System.exit(1);
    }

    private static boolean executar(String titulo, int n, int nSelvagens, int refeicoes,
                                    boolean exigeComerEmParalelo) throws InterruptedException {
        System.out.println("--- " + titulo + " ---");
        Caldeirao c = new Caldeirao(n, false);
        Cozinheiro cozinheiro = new Cozinheiro(c);
        Selvagem[] ss = new Selvagem[nSelvagens];

        cozinheiro.start();
        for (int i = 0; i < nSelvagens; i++) {
            ss[i] = new Selvagem(c, i + 1, refeicoes, 30, false);
            ss[i].start();
        }

        boolean travou = false;
        for (Selvagem s : ss) {
            s.join(TIMEOUT_MS);
            if (s.isAlive()) travou = true;
        }
        cozinheiro.interrupt();
        cozinheiro.join(2000);

        int total = nSelvagens * refeicoes;
        int enchimentosEsperados = (total + n - 1) / n - 1;   // teto(total / N) - 1

        System.out.println("Threads terminaram (sem deadlock)  : " + (!travou ? "SIM" : "NAO"));
        System.out.println("Violações de sincronização         : " + c.violacoes.get());
        System.out.println("Porções servidas / esperadas       : " + c.totalServido.get() + " / " + total);
        System.out.println("Enchimentos / esperados            : " + c.totalEnchimentos.get() + " / " + enchimentosEsperados);
        System.out.println("Máx. selvagens comendo juntos      : " + c.maxComendoSimultaneo.get());

        return !travou
                && c.violacoes.get() == 0
                && c.totalServido.get() == total
                && c.totalEnchimentos.get() == enchimentosEsperados
                && (!exigeComerEmParalelo || c.maxComendoSimultaneo.get() > 1);
    }
}
