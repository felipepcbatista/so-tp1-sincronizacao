package fumantes;

import util.Log;

/**
 * Validação automática do Problema dos Fumantes.
 * Uso: java -cp out fumantes.TesteFumantes
 */
public class TesteFumantes {
    private static int passou = 0, falhou = 0;

    private static void verificar(boolean condicao, String descricao) {
        if (condicao) passou++;
        else falhou++;
        System.out.println((condicao ? "  [OK]    " : "  [FALHA] ") + descricao);
    }

    public static void main(String[] args) throws InterruptedException {
        Log.ativo = false; // sem log linha a linha: são milhares de rodadas

        int execucoes = 20, rodadas = 300;
        System.out.println("Teste 1 - versão CORRETA: " + execucoes + " execuções x " + rodadas + " rodadas");
        for (int e = 1; e <= execucoes; e++) {
            MainFumantes.Resultado r = MainFumantes.executar(Mesa.Modo.CORRETO, rodadas, 1, 0, 15_000);
            boolean ok = r.terminou()
                    && r.erros().isEmpty()
                    && r.maxFumandoJuntos() == 1
                    && r.totalCigarros() == rodadas
                    && java.util.Arrays.equals(r.cigarros(), r.faltantes());
            if (!ok) {
                System.out.println("  execução " + e + " falhou: " + r);
            }
            verificar(ok, String.format("execução %2d: terminou, %d cigarros, cigarros por fumante %s = rodadas por faltante %s, máx. fumando junto = %d",
                    e, r.totalCigarros(), java.util.Arrays.toString(r.cigarros()),
                    java.util.Arrays.toString(r.faltantes()), r.maxFumandoJuntos()));
        }

        System.out.println("\nTeste 2 - versão INGÊNUA (controle negativo): deve travar");
        MainFumantes.Resultado ingenuo = MainFumantes.executar(Mesa.Modo.INGENUO, 50, 5, 20, 3_000);
        verificar(!ingenuo.terminou(), "versão ingênua entrou em deadlock depois de "
                + ingenuo.totalCigarros() + " cigarros (esperado: travar antes de 50)");

        System.out.printf("%nResultado: %d OK, %d falhas%n", passou, falhou);
        System.exit(falhou == 0 ? 0 : 1);
    }
}
