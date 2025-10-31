package servidormulti.manejadores;

import servidormulti.ServidorMulti;
import servidormulti.UnCliente;
import servidormulti.database.BloqueosDB;
import servidormulti.database.UsuariosDB;
import java.io.IOException;

public class ManejadorSocial {

    public void manejarSusurro(UnCliente emisor, String mensaje) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Debes iniciar sesión para enviar susurros.");
            return;
        }
        String[] partes = mensaje.split(" ", 3);
        if (partes.length < 3) {
            emisor.salida.writeUTF("--> Uso incorrecto. Formato: /w <nombre> <mensaje>");
            return;
        }
        String destinatarioNombre = partes[1];
        String mensajeSusurro = partes[2];
        UnCliente destinatario = ServidorMulti.clientes.get(destinatarioNombre);

        if (destinatario != null) {
            if (destinatario.getUsuariosBloqueados().contains(emisor.getNombreUsuario())) {
                emisor.salida.writeUTF("--> No puedes susurrar a " + destinatarioNombre + " (te ha bloqueado).");
                return;
            }
            String msgParaDest = emisor.getNombreUsuario() + " (te susurra): " + mensajeSusurro;
            destinatario.salida.writeUTF(msgParaDest);
            String confirmacion = "(Le susurras a " + destinatarioNombre + "): " + mensajeSusurro;
            emisor.salida.writeUTF(confirmacion);
        } else {
            emisor.salida.writeUTF("--> Usuario '" + destinatarioNombre + "' no está conectado.");
        }
    }

    public void manejarBlock(UnCliente emisor, String mensaje) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Debes iniciar sesión para bloquear.");
            return;
        }
        String usuarioABloquear = mensaje.substring(7).trim();
        if (!UsuariosDB.usuarioExiste(usuarioABloquear)) {
            emisor.salida.writeUTF("--> El usuario '" + usuarioABloquear + "' no está registrado.");
        } else if (emisor.getUsuariosBloqueados().contains(usuarioABloquear)) {
            emisor.salida.writeUTF("--> Ya tienes a '" + usuarioABloquear + "' bloqueado.");
        } else {
            BloqueosDB.bloquearUsuario(emisor.getNombreUsuario(), usuarioABloquear);
            emisor.getUsuariosBloqueados().add(usuarioABloquear);
            emisor.salida.writeUTF("--> Has bloqueado a '" + usuarioABloquear + "'.");
        }
    }

    public void manejarUnblock(UnCliente emisor, String mensaje) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Debes iniciar sesión para desbloquear.");
            return;
        }
        String usuarioADesbloquear = mensaje.substring(9).trim();
        if (!emisor.getUsuariosBloqueados().contains(usuarioADesbloquear)) {
            emisor.salida.writeUTF("--> No tienes a '" + usuarioADesbloquear + "' en tu lista de bloqueados.");
        } else {
            BloqueosDB.desbloquearUsuario(emisor.getNombreUsuario(), usuarioADesbloquear);
            emisor.getUsuariosBloqueados().remove(usuarioADesbloquear);
            emisor.salida.writeUTF("--> Has desbloqueado a '" + usuarioADesbloquear + "'.");
        }
    }

    public void manejarBlockedList(UnCliente emisor) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Debes iniciar sesión para ver tu lista.");
            return;
        }
        if (emisor.getUsuariosBloqueados().isEmpty()) {
            emisor.salida.writeUTF("--> Tu lista de bloqueados está vacía.");
        } else {
            emisor.salida.writeUTF("--> Usuarios bloqueados: " + String.join(", ", emisor.getUsuariosBloqueados()));
        }
    }
}