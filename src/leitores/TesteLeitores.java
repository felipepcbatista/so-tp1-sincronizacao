package leitores;

import util.Log;

public class TesteLeitores {
    private static int passou = 0, falhou = 0;

    private static void verificar(boolean condicao, String descricao) {
        if (condicao) passou++;
        else falhou++;
        System.out.println((condicao ? "  [OK]    " : "  [FALHA] ") + descricao);
    }

    public static void main(String[] args) throws InterruptedException {
        Log.ativo = false;

        int execucoes = 20, nLeitores = 6, nEscritores = 3, leituras = 20, escritas = 10;
        System.out.println("Teste 1 - CORRETO sob alta disputa: " + execucoes + " execuções x "
                + nLeitores + " leitores x " + nEscritores + " escritores");
        for (int e = 1; e <= execucoes; e++) {
            MainLeitores.Resultado r = MainLeitores.executar(
                    Memoria.Modo.CORRETO, nLeitores, nEscritores, leituras, escritas, 4, 0, 0, 0, 20_000);
            boolean ok = r.terminou()
                    && r.erros().isEmpty()
                    && r.leiturasFeitas() == nLeitores * leituras
                    && r.escritasFeitas() == nEscritores * escritas
                    && r.maxEscritoresJuntos() == 1;
            if (!ok) System.out.println("  execução " + e + " falhou: " + r);
            verificar(ok, String.format(
                    "execução %2d: %d leituras, %d escritas, máx. leitores juntos = %d, máx. escritores juntos = %d, violações = %d",
                    e, r.leiturasFeitas(), r.escritasFeitas(), r.maxLeitoresJuntos(),
                    r.maxEscritoresJuntos(), r.erros().size()));
        }

        System.out.println("\nTeste 2 - CORRETO permite leitura em paralelo");
        MainLeitores.Resultado paralelo = MainLeitores.executar(
                Memoria.Modo.CORRETO, 6, 1, 10, 3, 4, 30, 10, 0, 20_000);
        verificar(paralelo.terminou() && paralelo.maxLeitoresJuntos() > 1 && paralelo.erros().isEmpty(),
                "até " + paralelo.maxLeitoresJuntos() + " leitores leram ao mesmo tempo (o ponto do problema)");

        System.out.println("\nTeste 3 - CORRETO não deixa o escritor passar fome");
        MainLeitores.Resultado justo = MainLeitores.executar(
                Memoria.Modo.CORRETO, 8, 1, 40, 5, 4, 5, 5, 0, 20_000);
        verificar(justo.terminou() && justo.escritasFeitas() == 5 && justo.erros().isEmpty(),
                "com 8 leitores insistentes o escritor fez as 5 escritas, esperando no máximo "
                        + justo.maiorEsperaEscritorMs() + " ms");

        System.out.println("\nTeste 4 - PRIORIDADE_LEITOR (controle negativo): escritor passa fome");
        MainLeitores.Resultado fome = MainLeitores.executar(
                Memoria.Modo.PRIORIDADE_LEITOR, 8, 1, 40, 5, 4, 5, 5, 0, 20_000);
        verificar(fome.maiorEsperaEscritorMs() > justo.maiorEsperaEscritorMs() * 3,
                "escritor esperou " + fome.maiorEsperaEscritorMs() + " ms contra "
                        + justo.maiorEsperaEscritorMs() + " ms da versão justa");

        System.out.println("\nTeste 5 - SEM_SINCRONIA (controle negativo): leitura inconsistente");
        MainLeitores.Resultado solta = MainLeitores.executar(
                Memoria.Modo.SEM_SINCRONIA, 6, 3, 30, 20, 6, 0, 0, 1, 20_000);
        verificar(!solta.erros().isEmpty(),
                "sem semáforo apareceram " + solta.erros().size()
                        + " violações (leitor pegou o vetor no meio de uma escrita)");

        System.out.printf("%nResultado: %d OK, %d falhas%n", passou, falhou);
        System.exit(falhou == 0 ? 0 : 1);
    }
}
