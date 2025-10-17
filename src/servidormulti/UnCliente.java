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

        // Anunciamos la conexión a los demás usuarios
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente != this) {
                cliente.salida.writeUTF("--> " + this.nombreUsuario + " se ha unido al chat.");
            }
        }
    }

    @Override
    public void run() {
        try {
            // Se conecta inicialmente como invitado
            ServidorMulti.clientes.put(this.nombreUsuario, this);
            System.out.println("Se conectó un nuevo cliente: " + this.nombreUsuario);
            salida.writeUTF("--> ¡Bienvenido! Usa el menú del cliente para registrarte o iniciar sesión.");

            while (true) {
                String mensaje = entrada.readUTF();

                // Comando para registrar
                if (mensaje.startsWith("nombre: ")) {
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
                        // Guarda el nuevo usuario en la base de datos
                        DataBaseManager.registrarUsuario(nuevoNombre, password);
                        ServidorMulti.usuariosRegistrados.add(nuevoNombre);
                        salida.writeUTF("--> ¡Registro exitoso! Iniciando sesión automáticamente...");

                        // Inicia sesión automáticamente después de registrarse.
                        finalizarAutenticacion(nuevoNombre);
                    }
                }
                // Comando para iniciar sesión
                else if (mensaje.startsWith("/login ")) {
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

                else if (mensaje.equals("/listusers")) {
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
                else if (mensaje.startsWith("/w ")) {
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