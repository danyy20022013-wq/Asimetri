package servidormulti.grupos;

import servidormulti.ServidorMulti;
import servidormulti.UnCliente;
import servidormulti.database.GruposDB;
import servidormulti.database.MensajesDB;

import java.io.IOException;
import java.util.List;

public class GrupoManager {

    public void procesarMensaje(UnCliente emisor, Grupo grupo, String contenido) {
        String nombreEmisor = emisor.getNombreUsuario();

        long nuevoMensajeId = MensajesDB.almacenarMensaje(grupo.id, nombreEmisor, contenido);
        if (nuevoMensajeId == -1) {
            try {
                emisor.salida.writeUTF("--> Error al enviar el mensaje.");
            } catch (IOException e) { e.printStackTrace(); }
            return;
        }

        String mensajeFormateado = String.format("[%s] %s: %s", grupo.nombre, nombreEmisor, contenido);

        List<String> miembros = GruposDB.getNombresMiembrosDeGrupo(grupo.id);

        for (String nombreMiembro : miembros) {
            UnCliente miembroConectado = ServidorMulti.clientes.get(nombreMiembro);

            if (miembroConectado != null && miembroConectado.getGrupoActual().id == grupo.id) {

                if (!miembroConectado.getUsuariosBloqueados().contains(nombreEmisor)) {
                    try {
                        miembroConectado.salida.writeUTF(mensajeFormateado);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        MensajesDB.actualizarUltimoLeido(nombreEmisor, grupo.id, nuevoMensajeId);
    }

    public void enviarMensajesNoVistos(UnCliente usuario, Grupo grupo) {
        List<Mensaje> mensajes = MensajesDB.getMensajesNoVistos(usuario.getNombreUsuario(), grupo.id);

        if (mensajes.isEmpty()) {
            try {
                usuario.salida.writeUTF("--> No hay mensajes nuevos en el grupo '" + grupo.nombre + "'.");
            } catch (IOException e) { e.printStackTrace(); }
            return;
        }

        try {
            usuario.salida.writeUTF("--- Mostrando mensajes no leídos de '" + grupo.nombre + "' ---");
            long ultimoMensajeId = -1;

            for (Mensaje msg : mensajes) {
                if (!usuario.getUsuariosBloqueados().contains(msg.emisor)) {
                    String msgFormateado = String.format("[%s] (%s) %s: %s",
                            grupo.nombre,
                            msg.timestamp.toString().substring(11, 16), // Muestra solo la hora HH:MM
                            msg.emisor,
                            msg.contenido);
                    usuario.salida.writeUTF(msgFormateado);
                }
                ultimoMensajeId = msg.id;
            }

            if (ultimoMensajeId != -1) {
                MensajesDB.actualizarUltimoLeido(usuario.getNombreUsuario(), grupo.id, ultimoMensajeId);
            }
            usuario.salida.writeUTF("--- Fin de los mensajes no leídos ---");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}