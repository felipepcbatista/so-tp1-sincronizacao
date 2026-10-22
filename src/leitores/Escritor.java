package leitores;

import util.Log;

public class Escritor extends Thread {
    private final Memoria area;
    private final int id;
    private final int escritas;
    volatile String estado = "criado";

    public Escritor(Memoria area, int id, int escritas) {
        super("Escritor-" + id);
        this.area = area;
        this.id = id;
        this.escritas = escritas;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            for (int k = 1; k <= escritas; k++) {
                estado = "esperando para escrever";
                area.entrarEscrita();

                estado = "escrevendo";
                int valor = id * 100 + k;
                area.escrever(valor);
                Log.info("escreveu " + valor + " nas " + area.tamanho() + " posições");

                area.sairEscrita();
                estado = "fora da área";
            }
            estado = "terminou";
        } catch (InterruptedException e) {
            estado = "interrompido";
        }
    }
}
