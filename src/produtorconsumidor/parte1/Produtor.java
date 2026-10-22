package produtorconsumidor.parte1;

// Parte I: código do enunciado, sem alterações de lógica (só o nome do pacote).
public class Produtor extends Thread {
    private Buffer buf;
    private int numero;

    public Produtor(Buffer buf, int numero) {
        this.buf = buf;
        this.numero = numero;
    }

    public void run() {
        for (int i = 0; i < 5; i++) { // armazena 10 valores no buffer
            buf.put(i);
            System.out.println("Produtor #" + numero + " put: " + i);
            try {
                sleep((int) (Math.random() * 100));
            } catch (InterruptedException e) {
            }
        }
    }
}
