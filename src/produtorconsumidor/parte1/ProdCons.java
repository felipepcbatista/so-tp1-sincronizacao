package produtorconsumidor.parte1;

// Parte I: código do enunciado, sem alterações de lógica (só o nome do pacote).
public class ProdCons {

    public static void main(String[] args) {
        Buffer buf = new Buffer();
        int i;
        for (i = 0; i < 1; i++) {
            new Consumidor(buf, i).start();
            new Produtor(buf, i).start();
        }
    }
}
