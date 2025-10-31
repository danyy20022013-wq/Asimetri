package servidormulti.manejadores;
import servidormulti.manejadores.*;
import servidormulti.ServidorMulti;
import servidormulti.UnCliente;
import servidormulti.juego.PartidaGato;
import java.io.IOException;

public class ManejadorJuego {

    public void manejarJugar(UnCliente emisor, String mensaje) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Debes iniciar sesión para jugar.");
            return;
        }
        String oponenteNombre = mensaje.substring(7).trim();
        UnCliente oponente = ServidorMulti.clientes.get(oponenteNombre);

        if (emisor.getPartidasEnJuego().containsKey(oponenteNombre)) {
            emisor.salida.writeUTF("--> Ya tienes una partida activa con " + oponenteNombre + ".");
            return;
        }
        if (oponente == null) {
            emisor.salida.writeUTF("--> El usuario '" + oponenteNombre + "' no está conectado.");
            return;
        }
        if (oponente == emisor) {
            emisor.salida.writeUTF("--> No puedes jugar contigo mismo.");
            return;
        }
        if (emisor.getUsuariosBloqueados().contains(oponenteNombre)) {
            emisor.salida.writeUTF("--> No puedes invitar a '" + oponenteNombre + "' porque lo tienes bloqueado.");
            return;
        }
        if (oponente.getUsuariosBloqueados().contains(emisor.getNombreUsuario())) {
            emisor.salida.writeUTF("--> No puedes invitar a '" + oponenteNombre + "' (te ha bloqueado).");
            return;
        }

        ServidorMulti.invitacionesPendientes.put(oponenteNombre, emisor.getNombreUsuario());
        oponente.salida.writeUTF("--> ¡" + emisor.getNombreUsuario() + " te ha invitado a jugar al Gato!");
        oponente.salida.writeUTF("--> Escribe: /aceptar " + emisor.getNombreUsuario());
        emisor.salida.writeUTF("--> Invitación enviada a " + oponenteNombre + ".");
    }

    public void manejarAceptar(UnCliente emisor, String mensaje) throws IOException {
        String invitadorNombre = mensaje.substring(9).trim();
        String invitacionReal = ServidorMulti.invitacionesPendientes.get(emisor.getNombreUsuario());

        if (invitacionReal == null || !invitacionReal.equals(invitadorNombre)) {
            emisor.salida.writeUTF("--> No tienes una invitación pendiente de " + invitadorNombre + ".");
            return;
        }

        UnCliente invitador = ServidorMulti.clientes.get(invitadorNombre);
        if (invitador == null) {
            emisor.salida.writeUTF("--> El jugador que te invitó ya no está disponible.");
            return;
        }

        ServidorMulti.invitacionesPendientes.remove(emisor.getNombreUsuario());
        PartidaGato nuevaPartida = new PartidaGato(invitador, emisor);

        invitador.agregarPartida(emisor.getNombreUsuario(), nuevaPartida);
        emisor.agregarPartida(invitadorNombre, nuevaPartida);

        nuevaPartida.iniciarPartida();
    }

    public void manejarRechazar(UnCliente emisor, String mensaje) throws IOException {
        String invitadorNombre = mensaje.substring(10).trim();
        String invitacionReal = ServidorMulti.invitacionesPendientes.get(emisor.getNombreUsuario());

        if (invitacionReal != null && invitacionReal.equals(invitadorNombre)) {
            ServidorMulti.invitacionesPendientes.remove(emisor.getNombreUsuario());
            UnCliente invitador = ServidorMulti.clientes.get(invitadorNombre);
            if (invitador != null) {
                invitador.salida.writeUTF("--> " + emisor.getNombreUsuario() + " ha rechazado tu invitación.");
            }
            emisor.salida.writeUTF("--> Has rechazado la invitación de " + invitadorNombre + ".");
        } else {
            emisor.salida.writeUTF("--> No tienes una invitación pendiente de " + invitadorNombre + ".");
        }
    }

    public void manejarMove(UnCliente emisor, String mensaje) throws IOException {
        try {
            String[] partes = mensaje.substring(6).split(" ");
            if (partes.length < 3) throw new Exception();

            String oponenteNombre = partes[0];
            int fila = Integer.parseInt(partes[1]);
            int col = Integer.parseInt(partes[2]);

            PartidaGato partida = emisor.getPartidasEnJuego().get(oponenteNombre);
            if (partida == null) {
                emisor.salida.writeUTF("--> No estás jugando contra " + oponenteNombre + ".");
                return;
            }

            partida.recibirMovimiento(emisor, fila, col);
        } catch (Exception e) {
            emisor.salida.writeUTF("--> Movimiento inválido. Formato: /move <oponente> <fila> <col>");
        }
    }

    public void manejarPartidas(UnCliente emisor) throws IOException {
        if (emisor.getPartidasEnJuego().isEmpty()) {
            emisor.salida.writeUTF("--> No estás en ninguna partida.");
        } else {
            emisor.salida.writeUTF("--- Tus Partidas Activas ---");
            for (String oponente : emisor.getPartidasEnJuego().keySet()) {
                emisor.salida.writeUTF("- Jugando contra: " + oponente);
            }
        }
    }
}