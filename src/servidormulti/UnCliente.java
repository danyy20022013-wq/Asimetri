package servidormulti;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;

public class UnCliente implements Runnable {

    final DataOutputStream salida;
    final DataInputStream entrada;

    private String nombreUsuario;
    private final String idInvitadoOriginal;
    private boolean estaRegistrado = false;
    private int contadorMensajesInvitado = 0;
    private Set<String> usuariosBloqueados;

    UnCliente(Socket s, String idInvitado) throws IOException {
        this.salida = new DataOutputStream(s.getOutputStream());
        this.entrada = new DataInputStream(s.getInputStream());
        this.idInvitadoOriginal = idInvitado;
        this.nombreUsuario = idInvitado;
    }

    private void finalizarAutenticacion(String nombreExitoso) throws IOException {
        ServidorMulti.clientes.remove(this.idInvitadoOriginal);
        this.nombreUsuario = nombreExitoso;
        this.estaRegistrado = true;
        this.usuariosBloqueados = DataBaseManager.cargarListaDeBloqueados(this.nombreUsuario);

        ServidorMulti.clientes.put(this.nombreUsuario, this);
        salida.writeUTF("--> ¡Autenticación exitosa! Bienvenido, " + this.nombreUsuario);
        System.out.println(this.idInvitadoOriginal + " se ha identificado como " + this.nombreUsuario);

        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente != this) {
                cliente.salida.writeUTF("--> " + this.nombreUsuario + " se ha unido al chat.");
            }
        }
    }

    @Override
    public void run() {
        try {
            ServidorMulti.clientes.put(this.nombreUsuario, this);
            System.out.println("Se conectó un nuevo cliente: " + this.nombreUsuario);
            salida.writeUTF("--> ¡Bienvenido! Usa el menú del cliente para registrarte o iniciar sesión.");

            while (true) {
                String mensaje = entrada.readUTF();

                if (mensaje.startsWith("nombre: ")) { // Registrar
                    if (estaRegistrado) { continue; }
                    String[] partes = mensaje.substring(7).trim().split(" ", 2);
                    if (partes.length < 2) {
                        salida.writeUTF("--> Formato incorrecto. Se necesita usuario y contraseña.");
                        continue;
                    }
                    String nuevoNombre = partes[0];
                    String password = partes[1];

                    if (DataBaseManager.usuarioExiste(nuevoNombre)) {
                        salida.writeUTF("--> Error: El nombre '" + nuevoNombre + "' ya está registrado.");
                    } else {
                        DataBaseManager.registrarUsuario(nuevoNombre, password);
                        ServidorMulti.usuariosRegistrados.add(nuevoNombre);
                        salida.writeUTF("--> ¡Registro exitoso! Ahora inicia sesión con tu nueva cuenta.");
                    }
                }
                else if (mensaje.startsWith("/login ")) { // Iniciar sesión
                    if (estaRegistrado) { continue; }
                    String[] partes = mensaje.substring(7).trim().split(" ", 2);
                    if (partes.length < 2) {
                        salida.writeUTF("--> Formato incorrecto. Se necesita usuario y contraseña.");
                        continue;
                    }
                    String nombreLogin = partes[0];
                    String passwordLogin = partes[1];

                    if (ServidorMulti.clientes.containsKey(nombreLogin)) {
                        salida.writeUTF("--> Error: El usuario '" + nombreLogin + "' ya está conectado.");
                        continue;
                    }

                    if (DataBaseManager.validarLogin(nombreLogin, passwordLogin)) {
                        finalizarAutenticacion(nombreLogin);
                    } else {
                        salida.writeUTF("--> Error: Nombre de usuario o contraseña incorrectos.");
                    }
                }
                // --- ¡LÓGICA CORREGIDA Y RESTAURADA! ---
                else if (mensaje.equals("/listusers")) { // Ver usuarios
                    if (ServidorMulti.usuariosRegistrados.isEmpty()) {
                        salida.writeUTF("--> Aún no hay usuarios registrados en el servidor.");
                    } else {
                        StringBuilder listaUsuarios = new StringBuilder("--- Usuarios Registrados ---\n");
                        for (String usuario : ServidorMulti.usuariosRegistrados) {
                            if (ServidorMulti.clientes.containsKey(usuario)) {
                                listaUsuarios.append("- ").append(usuario).append(" (Online)\n");
                            } else {
                                listaUsuarios.append("- ").append(usuario).append(" (Offline)\n");
                            }
                        }
                        salida.writeUTF(listaUsuarios.toString());
                    }
                }
                else if (mensaje.startsWith("/w ")) { // Susurrar
                    if (!estaRegistrado) {
                        salida.writeUTF("--> Debes iniciar sesión para enviar susurros."); continue;
                    }
                    String[] partes = mensaje.split(" ", 3);
                    if (partes.length < 3) {
                        salida.writeUTF("--> Uso incorrecto. Formato: /w <nombre> <mensaje>"); continue;
                    }
                    String destinatarioNombre = partes[1];
                    String mensajeSusurro = partes[2];
                    UnCliente destinatario = ServidorMulti.clientes.get(destinatarioNombre);
                    if (destinatario != null) {
                        if (destinatario.usuariosBloqueados.contains(this.nombreUsuario)) {
                            salida.writeUTF("--> No puedes susurrar a " + destinatarioNombre + " (te ha bloqueado)."); continue;
                        }
                        String msgParaDest = this.nombreUsuario + " (te susurra): " + mensajeSusurro;
                        destinatario.salida.writeUTF(msgParaDest);
                        String confirmacion = "(Le susurras a " + destinatarioNombre + "): " + mensajeSusurro;
                        this.salida.writeUTF(confirmacion);
                    } else {
                        salida.writeUTF("--> Usuario '" + destinatarioNombre + "' no está conectado.");
                    }
                }
                else if (mensaje.startsWith("/block ")) { // Bloquear
                    if (!estaRegistrado) { salida.writeUTF("--> Debes iniciar sesión para bloquear."); continue; }
                    String usuarioABloquear = mensaje.substring(7).trim();
                    if (!DataBaseManager.usuarioExiste(usuarioABloquear)) {
                        salida.writeUTF("--> El usuario '" + usuarioABloquear + "' no está registrado.");
                    } else if (this.usuariosBloqueados.contains(usuarioABloquear)) {
                        salida.writeUTF("--> Ya tienes a '" + usuarioABloquear + "' bloqueado.");
                    } else {
                        DataBaseManager.bloquearUsuario(this.nombreUsuario, usuarioABloquear);
                        this.usuariosBloqueados.add(usuarioABloquear);
                        salida.writeUTF("--> Has bloqueado a '" + usuarioABloquear + "'.");
                    }
                }
                else if (mensaje.startsWith("/unblock ")) { // Desbloquear
                    if (!estaRegistrado) { salida.writeUTF("--> Debes iniciar sesión para desbloquear."); continue; }
                    String usuarioADesbloquear = mensaje.substring(9).trim();
                    if (!this.usuariosBloqueados.contains(usuarioADesbloquear)) {
                        salida.writeUTF("--> No tienes a '" + usuarioADesbloquear + "' en tu lista de bloqueados.");
                    } else {
                        DataBaseManager.desbloquearUsuario(this.nombreUsuario, usuarioADesbloquear);
                        this.usuariosBloqueados.remove(usuarioADesbloquear);
                        salida.writeUTF("--> Has desbloqueado a '" + usuarioADesbloquear + "'.");
                    }
                }
                else if (mensaje.equals("/blockedlist")) { // Ver lista de bloqueados
                    if (!estaRegistrado) { salida.writeUTF("--> Debes iniciar sesión para ver tu lista."); continue; }
                    if (this.usuariosBloqueados.isEmpty()) {
                        salida.writeUTF("--> Tu lista de bloqueados está vacía.");
                    } else {
                        salida.writeUTF("--> Usuarios bloqueados: " + String.join(", ", this.usuariosBloqueados));
                    }
                }
                else { // Mensaje público
                    if (estaRegistrado) {
                        String mensajeConRemitente = this.nombreUsuario + ": " + mensaje;
                        for (UnCliente cliente : ServidorMulti.clientes.values()) {
                            if (cliente != this && !cliente.usuariosBloqueados.contains(this.nombreUsuario)) {
                                cliente.salida.writeUTF(mensajeConRemitente);
                            }
                        }
                    } else { // Mensaje de invitado
                        if (contadorMensajesInvitado < 3) {
                            contadorMensajesInvitado++;
                            String mensajeInvitado = this.nombreUsuario + " (invitado): " + mensaje;
                            for (UnCliente cliente : ServidorMulti.clientes.values()) {
                                if (cliente != this) {
                                    cliente.salida.writeUTF(mensajeInvitado);
                                }
                            }
                        } else {
                            salida.writeUTF("--> Límite de mensajes de invitado alcanzado. Debes iniciar sesión.");
                        }
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println(this.nombreUsuario + " se ha desconectado.");
            ServidorMulti.clientes.remove(this.nombreUsuario);
            if (this.estaRegistrado) {
                try {
                    for (UnCliente cliente : ServidorMulti.clientes.values()) {
                        cliente.salida.writeUTF("--> " + this.nombreUsuario + " ha abandonado el chat.");
                    }
                } catch (IOException e) {}
            }
        }
    }
}