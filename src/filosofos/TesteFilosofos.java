package filosofos;

import java.util.Arrays;
import util.Log;

/**
 * Validação automática do Jantar dos Filósofos.
 * Uso: java -cp out filosofos.TesteFilosofos
 */
public class TesteFilosofos {
    private static int passou = 0, falhou = 0;

    private static void verificar(boolean condicao, String descricao) {
        if (condicao) passou++;
        else falhou++;
        System.out.println((condicao ? "  [OK]    " : "  [FALHA] ") + descricao);
    }

    public static void main(String[] args) throws InterruptedException {
        Log.ativo = false;
        int n = 5;

        int execucoes = 20, refeicoes = 200;
        System.out.println("Teste 1 - GARÇOM sob alta disputa: " + execucoes + " execuções x " + n + " filósofos x " + refeicoes + " refeições");
        for (int e = 1; e <= execucoes; e++) {
            MainFilosofos.Resultado r = MainFilosofos.executar(Jantar.Modo.GARCOM, n, refeicoes, 0, 1, 0, 20_000);
            boolean todosComeram = Arrays.stream(r.refeicoes()).allMatch(x -> x == refeicoes);
            verificar(r.terminou() && r.erros().isEmpty() && todosComeram && r.maxComendoJuntos() <= n / 2,
                    String.format("execução %2d: terminou, refeições %s, máx. comendo junto = %d, violações = %d",
                            e, Arrays.toString(r.refeicoes()), r.maxComendoJuntos(), r.erros().size()));
        }

        System.out.println("\nTeste 2 - GARÇOM com a pausa que provoca deadlock na versão ingênua");
        MainFilosofos.Resultado comPausa = MainFilosofos.executar(Jantar.Modo.GARCOM, n, 10, 0, 2, 20, 20_000);
        verificar(comPausa.terminou() && comPausa.erros().isEmpty(),
                "terminou com pausa de 20 ms entre os hashis, refeições " + Arrays.toString(comPausa.refeicoes()));

        System.out.println("\nTeste 3 - INGÊNUO com a mesma pausa (controle negativo): deve travar");
        MainFilosofos.Resultado ingenuo = MainFilosofos.executar(Jantar.Modo.INGENUO, n, 10, 0, 2, 20, 3_000);
        verificar(!ingenuo.terminou(), "versão ingênua entrou em deadlock, refeições até travar " + Arrays.toString(ingenuo.refeicoes()));

        System.out.printf("%nResultado: %d OK, %d falhas%n", passou, falhou);
        System.exit(falhou == 0 ? 0 : 1);
    }
}
