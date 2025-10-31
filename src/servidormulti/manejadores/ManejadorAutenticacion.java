package servidormulti.manejadores;
import servidormulti.manejadores.*;
import servidormulti.database.*;
import servidormulti.ServidorMulti;
import servidormulti.UnCliente;
import servidormulti.database.UsuariosDB;
import java.io.IOException;

public class ManejadorAutenticacion {

    public void manejarRegistro(UnCliente emisor, String mensaje) throws IOException {
        if (emisor.estaRegistrado()) { return; }

        String[] partes = mensaje.substring(8).trim().split(" ", 2);
        if (partes.length < 2) {
            emisor.salida.writeUTF("--> Formato incorrecto. Se necesita: nombre: <usuario> <contraseña>");
            return;
        }
        String nuevoNombre = partes[0];
        String password = partes[1];

        if (UsuariosDB.usuarioExiste(nuevoNombre)) {
            emisor.salida.writeUTF("--> Error: El nombre '" + nuevoNombre + "' ya está registrado.");
        } else {
            UsuariosDB.registrarUsuario(nuevoNombre, password);
            ServidorMulti.usuariosRegistrados.add(nuevoNombre);
            emisor.salida.writeUTF("--> ¡Registro exitoso! Iniciando sesión automáticamente...");
            emisor.finalizarAutenticacion(nuevoNombre);
        }
    }

    public void manejarLogin(UnCliente emisor, String mensaje) throws IOException {
        if (emisor.estaRegistrado()) { return; }

        String[] partes = mensaje.substring(7).trim().split(" ", 2);
        if (partes.length < 2) {
            emisor.salida.writeUTF("--> Formato incorrecto. Se necesita: /login <usuario> <contraseña>");
            return;
        }
        String nombreLogin = partes[0];
        String passwordLogin = partes[1];

        if (ServidorMulti.clientes.containsKey(nombreLogin)) {
            emisor.salida.writeUTF("--> Error: El usuario '" + nombreLogin + "' ya está conectado.");
            return;
        }

        if (UsuariosDB.validarLogin(nombreLogin, passwordLogin)) {
            emisor.finalizarAutenticacion(nombreLogin);
        } else {
            emisor.salida.writeUTF("--> Error: Nombre de usuario o contraseña incorrectos.");
        }
    }
}