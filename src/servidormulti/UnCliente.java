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

    // Método para manejar la lógica de post-autenticación
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

                // --- CAMBIO: Comando de registro ahora necesita contraseña ---
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
                        DataBaseManager.registrarUsuario(nuevoNombre, password);
                        ServidorMulti.usuariosRegistrados.add(nuevoNombre);
                        salida.writeUTF("--> ¡Registro exitoso! Ahora inicia sesión con tu nueva cuenta.");
                    }
                }
                // --- ¡NUEVO! Comando para iniciar sesión ---
                else if (mensaje.startsWith("/login ")) {
                    if (estaRegistrado) { continue; }
                    String[] partes = mensaje.substring(7).trim().split(" ", 2);
                    if (partes.length < 2) {
                        salida.writeUTF("--> Formato incorrecto. Se necesita usuario y contraseña.");
                        continue;
                    }
                    String nombreLogin = partes[0];
                    String passwordLogin = partes[1];

                    // Evitar que alguien inicie sesión en una cuenta ya conectada
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
                // (El resto de los comandos como /block, /w, etc. no cambian)
                // ...
            }
        } catch (IOException ex) {
            // (Lógica de desconexión sin cambios)
        }
    }
}