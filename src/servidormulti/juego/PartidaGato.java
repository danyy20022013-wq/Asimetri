package servidormulti.juego;

import servidormulti.ServidorMulti;
import servidormulti.UnCliente;

import java.io.IOException;

public class PartidaGato {
    private final UnCliente jugadorX;
    private final UnCliente jugadorO;
    private final JuegoGato juego;
    private UnCliente turnoActual;

    public PartidaGato(UnCliente j1, UnCliente j2) {
        this.jugadorX = j1;
        this.jugadorO = j2;
        this.juego = new JuegoGato();
    }

    private void enviarMensajeAmbos(String msg) {
        try {
            jugadorX.salida.writeUTF(msg);
            jugadorO.salida.writeUTF(msg);
        } catch (IOException e) {
            System.out.println("Error al enviar mensaje de juego: " + e.getMessage());
        }
    }

    public void iniciarPartida() {
        enviarMensajeAmbos("--> ¡Partida iniciada! " + jugadorX.getNombreUsuario() + " (X) vs " + jugadorO.getNombreUsuario() + " (O)");
        this.turnoActual = jugadorX;
        enviarMensajeAmbos(juego.imprimirTablero());
        try {
            jugadorX.salida.writeUTF("--> Es tu turno (X).");
            jugadorO.salida.writeUTF("--> Es el turno de " + jugadorX.getNombreUsuario() + ".");
        } catch (IOException e) {}
    }

    public synchronized void recibirMovimiento(UnCliente remitente, int fila, int col) {
        if (remitente != turnoActual) {
            try {
                remitente.salida.writeUTF("--> No es tu turno.");
            } catch (IOException e) {}
            return;
        }

        char simbolo = (remitente == jugadorX) ? 'X' : 'O';
        if (!juego.hacerMovimiento(fila, col, simbolo)) {
            try {
                remitente.salida.writeUTF("--> Movimiento inválido. Intenta de nuevo.");
            } catch (IOException e) {}
            return;
        }

        enviarMensajeAmbos(juego.imprimirTablero());

        if (juego.getGanador() != ' ') {
            enviarMensajeAmbos("--> ¡Juego terminado! El ganador es " + remitente.getNombreUsuario() + " (" + simbolo + ")");
            terminarPartida();
        } else if (juego.estaLleno()) {
            enviarMensajeAmbos("--> ¡Juego terminado! Es un empate (gato).");
            terminarPartida();
        } else {

            turnoActual = (turnoActual == jugadorX) ? jugadorO : jugadorX;
            try {
                turnoActual.salida.writeUTF("--> Es tu turno ("+ ((turnoActual == jugadorX) ? 'X' : 'O') +").");
            } catch (IOException e) {}
        }
    }

    private void terminarPartida() {
        jugadorX.setEstaEnJuego(false);
        jugadorO.setEstaEnJuego(false);
        ServidorMulti.partidasActivas.remove(jugadorX.getNombreUsuario());
        ServidorMulti.partidasActivas.remove(jugadorO.getNombreUsuario());
    }

    public void abandonarPartida(UnCliente jugadorQueAbandona) {
        UnCliente otroJugador = (jugadorQueAbandona == jugadorX) ? jugadorO : jugadorX;
        try {
            otroJugador.salida.writeUTF("--> " + jugadorQueAbandona.getNombreUsuario() + " se ha desconectado. ¡Tú ganas!");
        } catch (IOException e) {}
        terminarPartida();
    }
}
