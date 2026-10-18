package fumantes;

public enum Ingrediente {
    TABACO, PAPEL, FOSFORO;

    /** Os dois ingredientes que NÃO são este, em ordem fixa. */
    public Ingrediente[] complemento() {
        Ingrediente[] outros = new Ingrediente[2];
        int k = 0;
        for (Ingrediente i : values()) {
            if (i != this) outros[k++] = i;
        }
        return outros;
    }

    @Override
    public String toString() {
        return switch (this) {
            case TABACO -> "tabaco";
            case PAPEL -> "papel";
            case FOSFORO -> "fósforo";
        };
    }
}
