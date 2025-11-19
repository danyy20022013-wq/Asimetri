package servidormulti.manejadores;

import servidormulti.ServidorMulti;
import servidormulti.UnCliente;
import servidormulti.database.GruposDB;
import servidormulti.grupos.Grupo;
import java.io.IOException;
import java.util.List;

public class ManejadorGrupos {

    public void manejarCrearGrupo(UnCliente emisor, String mensaje) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Solo usuarios registrados pueden crear grupos.");
            return;
        }
        String nombreGrupo = mensaje.substring(12).trim();
        if (GruposDB.crearGrupo(nombreGrupo)) {
            emisor.salida.writeUTF("--> Grupo '" + nombreGrupo + "' creado.");
            Grupo nuevoGrupo = GruposDB.getGrupoPorNombre(nombreGrupo);
            GruposDB.unirUsuarioAGrupo(emisor.getNombreUsuario(), nuevoGrupo.id);
            emisor.salida.writeUTF("--> Te has unido al grupo '" + nombreGrupo + "'.");
        } else {
            emisor.salida.writeUTF("--> Error: El grupo '" + nombreGrupo + "' ya existe o es inválido.");
        }
    }

    public void manejarBorrarGrupo(UnCliente emisor, String mensaje) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Solo usuarios registrados pueden borrar grupos.");
            return;
        }
        String nombreGrupo = mensaje.substring(13).trim();
        if (nombreGrupo.equalsIgnoreCase("Todos")) {
            emisor.salida.writeUTF("--> No se puede borrar el grupo 'Todos'.");
            return;
        }
        if (GruposDB.borrarGrupo(nombreGrupo)) {
            emisor.salida.writeUTF("--> Grupo '" + nombreGrupo + "' borrado.");
        } else {
            emisor.salida.writeUTF("--> Error: El grupo '" + nombreGrupo + "' no existe.");
        }
    }

    public void manejarUnirseGrupo(UnCliente emisor, String mensaje) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Solo usuarios registrados pueden unirse a grupos.");
            return;
        }
        String nombreGrupo = mensaje.substring(13).trim();
        Grupo grupo = GruposDB.getGrupoPorNombre(nombreGrupo);
        if (grupo != null) {
            GruposDB.unirUsuarioAGrupo(emisor.getNombreUsuario(), grupo.id);
            emisor.salida.writeUTF("--> Te has unido al grupo '" + nombreGrupo + "'.");
        } else {
            emisor.salida.writeUTF("--> El grupo '" + nombreGrupo + "' no existe.");
        }
    }

    public void manejarCambiarGrupo(UnCliente emisor, String mensaje) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Solo usuarios registrados pueden cambiar de grupo.");
            return;
        }
        String nombreGrupo = mensaje.substring(14).trim();
        Grupo nuevoGrupo = GruposDB.getGrupoPorNombre(nombreGrupo);
        if (nuevoGrupo != null) {
            List<String> miembros = GruposDB.getNombresMiembrosDeGrupo(nuevoGrupo.id);
            if (miembros.contains(emisor.getNombreUsuario())) {
                emisor.setGrupoActual(nuevoGrupo);
                emisor.salida.writeUTF("--> Has cambiado al grupo '" + emisor.getGrupoActual().nombre + "'.");
                ServidorMulti.grupoManager.enviarMensajesNoVistos(emisor, emisor.getGrupoActual());
            } else {
                emisor.salida.writeUTF("--> No eres miembro del grupo '" + nombreGrupo + "'. Usa /unirsegrupo primero.");
            }
        } else {
            emisor.salida.writeUTF("--> El grupo '" + nombreGrupo + "' no existe.");
        }
    }

    public void manejarMisGrupos(UnCliente emisor) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Solo usuarios registrados tienen grupos.");
            return;
        }
        List<Grupo> grupos = GruposDB.getGruposDeUsuario(emisor.getNombreUsuario());
        emisor.salida.writeUTF("--- Tus Grupos ---");
        for (Grupo g : grupos) {
            emisor.salida.writeUTF("- " + g.nombre);
        }
    }

    public void manejarChatDeGrupo(UnCliente emisor, String mensaje) throws IOException {
        if (!emisor.estaRegistrado() && emisor.getContadorMensajesInvitado() >= ServidorMulti.LIMITE_MENSAJES) {
            emisor.salida.writeUTF("--> Límite de mensajes de invitado alcanzado. Debes iniciar sesión.");
            return;
        }
        if (!emisor.estaRegistrado()) {
            emisor.incrementarMensajesInvitado();
        }

        ServidorMulti.grupoManager.procesarMensaje(emisor, emisor.getGrupoActual(), mensaje);
    }
}