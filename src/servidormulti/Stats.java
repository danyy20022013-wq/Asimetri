package servidormulti;

public class Stats {
    public final String nombre;
    public final int victorias;
    public final int empates;
    public final int derrotas;
    public final int puntos;
    public final double winrate;

    public Stats(String nombre, int victorias, int empates, int derrotas) {
        this.nombre = nombre;
        this.victorias = victorias;
        this.empates = empates;
        this.derrotas = derrotas;


        this.puntos = (victorias * 2) + empates;

        int totalPartidas = victorias + empates + derrotas;
        if (totalPartidas == 0) {
            this.winrate = 0.0;
        } else {

            this.winrate = ((double) victorias / totalPartidas) * 100.0;
        }
    }
}