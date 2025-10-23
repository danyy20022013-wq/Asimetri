package servidormulti.juego;

public class JuegoGato {
    private final char[][] tablero;
    private char ganador;

    public JuegoGato() {
        tablero = new char[3][3];
        ganador = ' ';
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tablero[i][j] = '-';
            }
        }
    }


    public String imprimirTablero() {
        StringBuilder sb = new StringBuilder("\n  0 1 2\n");
        for (int i = 0; i < 3; i++) {
            sb.append(i).append(" ");
            for (int j = 0; j < 3; j++) {
                sb.append(tablero[i][j]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }


    public boolean hacerMovimiento(int fila, int col, char jugador) {
        if (fila < 0 || fila > 2 || col < 0 || col > 2 || tablero[fila][col] != '-') {
            return false;
        }
        tablero[fila][col] = jugador;
        if (verificarGanador(jugador)) {
            ganador = jugador;
        }
        return true;
    }

    public boolean estaLleno() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tablero[i][j] == '-') {
                    return false;
                }
            }
        }
        return true;
    }

    public char getGanador() {
        return ganador;
    }

    private boolean verificarGanador(char jugador) {

        for (int i = 0; i < 3; i++) {
            if (tablero[i][0] == jugador && tablero[i][1] == jugador && tablero[i][2] == jugador) return true;
        }

        for (int j = 0; j < 3; j++) {
            if (tablero[0][j] == jugador && tablero[1][j] == jugador && tablero[2][j] == jugador) return true;
        }

        if (tablero[0][0] == jugador && tablero[1][1] == jugador && tablero[2][2] == jugador) return true;
        if (tablero[0][2] == jugador && tablero[1][1] == jugador && tablero[2][0] == jugador) return true;
        return false;
    }
}