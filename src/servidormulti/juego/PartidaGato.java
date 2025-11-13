package servidormulti.juego;
import servidormulti.database.*;
import servidormulti.UnCliente;
import servidormulti.database.EstadisticasDB;

import java.io.IOException;

public class PartidaGato {
    private final UnCliente jugadorX;
    private final UnCliente jugadorO;
    private final JuegoGato juego;
    private UnCliente turnoActual;

    public PartidaGato(UnCliente j1, UnCliente j2) {
        this.juego = new JuegoGato();

        if (Math.random() > 0.5) {
            this.jugadorX = j1;
            this.jugadorO = j2;
        } else {
            this.jugadorX = j2;
            this.jugadorO = j1;
        }
    }

    private void enviarMensajeAmbos(String msg) {
        try {
            jugadorX.salida.writeUTF(msg);
            jugadorO.salida.writeUTF(msg);
        } catch (IOException e) {
            System.err.println("Error enviando datos de la partida: " + e.getMessage());
        }
    }

    public void iniciarPartida() {
        enviarMensajeAmbos("--> ¡Partida iniciada! " + jugadorX.getNombreUsuario() + " (X) vs " + jugadorO.getNombreUsuario() + " (O)");
        this.turnoActual = jugadorX;
        enviarMensajeAmbos(juego.imprimirTablero());
        try {
            jugadorX.salida.writeUTF("--> Es tu turno (X) contra " + jugadorO.getNombreUsuario());
            jugadorO.salida.writeUTF("--> Es el turno de " + jugadorX.getNombreUsuario() + " (X)");
        } catch (IOException e) { System.err.println("Error al iniciar  la partida: " + e.getMessage()); }
    }

    public synchronized void recibirMovimiento(UnCliente remitente, int fila, int col) {
        if (remitente != turnoActual) {
            try {
                remitente.salida.writeUTF("--> No es tu turno en esta partida.");
            } catch (IOException e) { System.err.println("Error enviando aviso de turno incorrecto: " + e.getMessage()); }
            return;
        }

        char simbolo = (remitente == jugadorX) ? 'X' : 'O';
        if (!juego.hacerMovimiento(fila, col, simbolo)) {
            try {
                remitente.salida.writeUTF("--> Movimiento inválido. Intenta de nuevo.");
            } catch (IOException e) { System.err.println("Error enviando aviso de movimiento inválido: " + e.getMessage()); }
            return;
        }

        enviarMensajeAmbos(juego.imprimirTablero());

        if (juego.getGanador() != ' ') {
            enviarMensajeAmbos("--> ¡Juego terminado! El ganador es " + remitente.getNombreUsuario() + " (" + simbolo + ")");

            UnCliente perdedor = (remitente == jugadorX) ? jugadorO : jugadorX;
            EstadisticasDB.registrarResultado(remitente.getNombreUsuario(), perdedor.getNombreUsuario());

            terminarPartida();

        } else if (juego.estaLleno()) {
            enviarMensajeAmbos("--> ¡Juego terminado! Es un empate (gato).");

            EstadisticasDB.registrarEmpate(jugadorX.getNombreUsuario(), jugadorO.getNombreUsuario());

            terminarPartida();

        } else {
            turnoActual = (turnoActual == jugadorX) ? jugadorO : jugadorX;
            try {
                String oponente = (turnoActual == jugadorX) ? jugadorO.getNombreUsuario() : jugadorX.getNombreUsuario();
                turnoActual.salida.writeUTF("--> Es tu turno ("+ ((turnoActual == jugadorX) ? 'X' : 'O') +") contra " + oponente);
            } catch (IOException e) { System.err.println("Error al notificar el siguiente turno: " + e.getMessage());; }
        }
    }

    private void terminarPartida() {
        jugadorX.removerPartida(jugadorO.getNombreUsuario());
        jugadorO.removerPartida(jugadorX.getNombreUsuario());
    }

    public void abandonarPartida(UnCliente jugadorQueAbandona) {
        UnCliente otroJugador = (jugadorQueAbandona == jugadorX) ? jugadorO : jugadorX;
        try {
            otroJugador.salida.writeUTF("--> " + jugadorQueAbandona.getNombreUsuario() + " se ha desconectado de su partida. ¡Tú ganas!");
        } catch (IOException e) { System.err.println("Error al notificar abandono de partida: " + e.getMessage()); }

            EstadisticasDB.registrarResultado(otroJugador.getNombreUsuario(), jugadorQueAbandona.getNombreUsuario());

        terminarPartida();
    }
}