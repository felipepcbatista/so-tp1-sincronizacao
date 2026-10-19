package barbearia;

import util.Log;

public class TesteBarbearia {
    private static int passou = 0, falhou = 0;

    private static void verificar(boolean condicao, String descricao) {
        if (condicao) passou++;
        else falhou++;
        System.out.println((condicao ? "  [OK]    " : "  [FALHA] ") + descricao);
    }

    public static void main(String[] args) throws InterruptedException {
        Log.ativo = false;

        int execucoes = 20, clientes = 200, cadeiras = 3;
        System.out.println("Teste 1 - CORRETO sob alta disputa: " + execucoes + " execuções x "
                + clientes + " clientes x " + cadeiras + " cadeiras");
        for (int e = 1; e <= execucoes; e++) {
            MainBarbearia.Resultado r = MainBarbearia.executar(
                    Barbearia.Modo.CORRETO, cadeiras, clientes, 0, 1, 0, 20_000);
            boolean ok = r.terminou()
                    && r.erros().isEmpty()
                    && r.contabilizados() == clientes
                    && r.cortesFeitos() == r.atendidos()
                    && r.maxEsperando() <= cadeiras
                    && r.maxNaCadeira() == 1;
            if (!ok) System.out.println("  execução " + e + " falhou: " + r);
            verificar(ok, String.format(
                    "execução %2d: %d atendidos + %d foram embora = %d, cortes = %d, máx. na sala = %d, máx. na cadeira = %d",
                    e, r.atendidos(), r.desistiram(), r.contabilizados(), r.cortesFeitos(),
                    r.maxEsperando(), r.maxNaCadeira()));
        }

        System.out.println("\nTeste 2 - CORRETO com barbeiro ocioso: o barbeiro precisa dormir");
        MainBarbearia.Resultado ocioso = MainBarbearia.executar(
                Barbearia.Modo.CORRETO, cadeiras, 8, 60, 5, 0, 10_000);
        verificar(ocioso.terminou() && ocioso.vezesQueDormiu() > 0 && ocioso.desistiram() == 0
                        && ocioso.atendidos() == 8 && ocioso.erros().isEmpty(),
                "chegadas espaçadas: barbeiro dormiu " + ocioso.vezesQueDormiu() + "x e atendeu "
                        + ocioso.atendidos() + "/8 sem ninguém ir embora");

        System.out.println("\nTeste 3 - CORRETO com sala lotada: clientes precisam ir embora");
        MainBarbearia.Resultado lotada = MainBarbearia.executar(
                Barbearia.Modo.CORRETO, 2, 60, 0, 20, 0, 20_000);
        verificar(lotada.terminou() && lotada.desistiram() > 0 && lotada.maxEsperando() <= 2
                        && lotada.contabilizados() == 60 && lotada.erros().isEmpty(),
                "60 clientes de uma vez para 2 cadeiras: " + lotada.desistiram()
                        + " foram embora, máx. na sala = " + lotada.maxEsperando() + " (limite 2)");

        System.out.println("\nTeste 4 - SEM_MUTEX (controle negativo): race condition na sala de espera");
        MainBarbearia.Resultado semMutex = MainBarbearia.executar(
                Barbearia.Modo.SEM_MUTEX, cadeiras, 60, 0, 1, 15, 20_000);
        boolean violou = semMutex.maxEsperando() > cadeiras || !semMutex.erros().isEmpty();
        verificar(violou, "sem mutex a sala estourou: máx. na sala = " + semMutex.maxEsperando()
                + " para " + cadeiras + " cadeiras, " + semMutex.erros().size() + " violações registradas");

        System.out.println("\nTeste 5 - MUTEX_PRESO (controle negativo): deve travar");
        MainBarbearia.Resultado preso = MainBarbearia.executar(
                Barbearia.Modo.MUTEX_PRESO, cadeiras, 12, 0, 5, 0, 3_000);
        verificar(!preso.terminou(), "cliente esperando o barbeiro com o mutex na mão entrou em deadlock depois de "
                + preso.atendidos() + " atendimentos (esperado: travar antes de 12)");

        System.out.printf("%nResultado: %d OK, %d falhas%n", passou, falhou);
        System.exit(falhou == 0 ? 0 : 1);
    }
}
