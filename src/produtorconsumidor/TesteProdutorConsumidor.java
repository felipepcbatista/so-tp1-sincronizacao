package produtorconsumidor;

import util.Log;

/**
 * Validação automática do Produtor e Consumidor.
 * Uso: java -cp out produtorconsumidor.TesteProdutorConsumidor
 */
public class TesteProdutorConsumidor {
    private static int passou = 0, falhou = 0;

    private static void verificar(boolean condicao, String descricao) {
        if (condicao) passou++;
        else falhou++;
        System.out.println((condicao ? "  [OK]    " : "  [FALHA] ") + descricao);
    }

    private static String resumo(MainProdutorConsumidor.Resultado r) {
        return String.format("%d de %d consumidos 1 vez, perdidos %d, duplicados %d, leituras vazias %d, ocupação máx. %d/%d",
                r.consumidosUmaVez(), r.produzidos(), r.perdidos(), r.duplicados(), r.leiturasVazias(),
                r.maxOcupacao(), r.capacidade());
    }

    public static void main(String[] args) throws InterruptedException {
        Log.ativo = false;
        BufferLimitado.Modo correto = BufferLimitado.Modo.CORRETO;

        int execucoes = 20;
        System.out.println("Teste 1 - CORRETO sob alta disputa: " + execucoes + " execuções x 3 produtores x 3 consumidores x 300 itens, buffer de 5");
        for (int e = 1; e <= execucoes; e++) {
            MainProdutorConsumidor.Resultado r = MainProdutorConsumidor.executar(correto, 3, 3, 300, 5, 0, 0, 0, 20_000);
            verificar(r.terminou() && r.consistente() && r.consumidosUmaVez() == r.produzidos() && r.maxOcupacao() <= r.capacidade(),
                    String.format("execução %2d: %s", e, resumo(r)));
        }

        System.out.println("\nTeste 2 - CORRETO com buffer de 1 posição (caso extremo)");
        MainProdutorConsumidor.Resultado umaPosicao = MainProdutorConsumidor.executar(correto, 3, 3, 200, 1, 0, 0, 0, 20_000);
        verificar(umaPosicao.terminou() && umaPosicao.consistente() && umaPosicao.maxOcupacao() == 1, resumo(umaPosicao));

        System.out.println("\nTeste 3 - CORRETO com a pausa na região crítica que provoca a corrida no SEM_MUTEX");
        MainProdutorConsumidor.Resultado comPausa = MainProdutorConsumidor.executar(correto, 3, 3, 30, 5, 0, 0, 1, 20_000);
        verificar(comPausa.terminou() && comPausa.consistente(), resumo(comPausa));

        System.out.println("\nTeste 4 - SEM_MUTEX (controle negativo): deve perder ou duplicar itens");
        MainProdutorConsumidor.Resultado semMutex = MainProdutorConsumidor.executar(BufferLimitado.Modo.SEM_MUTEX, 3, 3, 30, 5, 0, 0, 1, 20_000);
        verificar(semMutex.terminou() && !semMutex.consistente(), "condição de corrida detectada: " + resumo(semMutex));

        System.out.println("\nTeste 5 - MUTEX_ANTES (controle negativo): deve travar");
        MainProdutorConsumidor.Resultado mutexAntes = MainProdutorConsumidor.executar(BufferLimitado.Modo.MUTEX_ANTES, 2, 2, 50, 3, 0, 5, 0, 3_000);
        verificar(!mutexAntes.terminou(), "deadlock detectado depois de " + (mutexAntes.consumidosUmaVez()) + " itens consumidos");

        System.out.printf("%nResultado: %d OK, %d falhas%n", passou, falhou);
        System.exit(falhou == 0 ? 0 : 1);
    }
}
