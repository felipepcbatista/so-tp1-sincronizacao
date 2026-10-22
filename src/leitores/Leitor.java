package leitores;

import java.util.Arrays;
import util.Log;

public class Leitor extends Thread {
    private final Memoria area;
    private final int id;
    private final int leituras;
    volatile String estado = "criado";

    public Leitor(Memoria area, int id, int leituras) {
        super("Leitor-" + id);
        this.area = area;
        this.id = id;
        this.leituras = leituras;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            for (int k = 1; k <= leituras; k++) {
                estado = "esperando para ler";
                area.entrarLeitura();

                estado = "lendo";
                int[] visto = area.ler();
                Log.info("leu " + Arrays.toString(visto));

                area.sairLeitura();
                estado = "fora da área";
            }
            estado = "terminou";
        } catch (InterruptedException e) {
            estado = "interrompido";
        }
    }
}
