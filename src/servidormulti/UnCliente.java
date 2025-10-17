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

    @Override
    public void run() {
        try {
            ServidorMulti.clientes.put(this.nombreUsuario, this);
            System.out.println("Se conectó un nuevo cliente: " + this.nombreUsuario);
            salida.writeUTF("--> ¡Bienvenido! Para registrarte, usa: nombre: <tu_nombre>");

            while (true) {
                String mensaje = entrada.readUTF();

                if (mensaje.startsWith("nombre: ")) {
                    if (estaRegistrado) {
                        salida.writeUTF("--> Ya estás identificado como " + this.nombreUsuario);
                        continue;
                    }
                    String nuevoNombre = mensaje.substring(7).trim();

                    if (DataBaseManager.usuarioExiste(nuevoNombre)) {
                        salida.writeUTF("--> Error: El nombre '" + nuevoNombre + "' ya está registrado. Elige otro.");
                    } else {
                        DataBaseManager.registrarUsuario(nuevoNombre);
                        ServidorMulti.usuariosRegistrados.add(nuevoNombre);

                        ServidorMulti.clientes.remove(this.idInvitadoOriginal);
                        this.nombreUsuario = nuevoNombre;
                        this.estaRegistrado = true;

                        this.usuariosBloqueados = DataBaseManager.cargarListaDeBloqueados(this.nombreUsuario);

                        ServidorMulti.clientes.put(this.nombreUsuario, this);
                        salida.writeUTF("--> Te has identificado correctamente como: " + this.nombreUsuario);
                        System.out.println(this.idInvitadoOriginal + " se identificó como " + this.nombreUsuario);
                        for (UnCliente cliente : ServidorMulti.clientes.values()) {
                            if (cliente != this) {
                                cliente.salida.writeUTF("--> " + this.nombreUsuario + " se ha unido al chat.");
                            }
                        }
                    }
                }
                else if (mensaje.startsWith("/block ")) {
                    if (!estaRegistrado) {
                        salida.writeUTF("--> Debes estar registrado para bloquear usuarios.");
                        continue;
                    }
                    String usuarioABloquear = mensaje.substring(7).trim();
                    if (!DataBaseManager.usuarioExiste(usuarioABloquear)) {
                        salida.writeUTF("--> El usuario '" + usuarioABloquear + "' no está registrado.");
                    } else if (this.usuariosBloqueados.contains(usuarioABloquear)) {
                        salida.writeUTF("--> Ya tienes a '" + usuarioABloquear + "' en tu lista de bloqueados.");
                    } else {
                        DataBaseManager.bloquearUsuario(this.nombreUsuario, usuarioABloquear);
                        this.usuariosBloqueados.add(usuarioABloquear);
                        salida.writeUTF("--> Has bloqueado a '" + usuarioABloquear + "'.");
                    }
                }
                else if (mensaje.startsWith("/unblock ")) {
                    if (!estaRegistrado) {
                        salida.writeUTF("--> Debes estar registrado para desbloquear usuarios.");
                        continue;
                    }
                    String usuarioADesbloquear = mensaje.substring(9).trim();
                    if (!this.usuariosBloqueados.contains(usuarioADesbloquear)) {
                        salida.writeUTF("--> No tienes a '" + usuarioADesbloquear + "' en tu lista de bloqueados.");
                    } else {
                        DataBaseManager.desbloquearUsuario(this.nombreUsuario, usuarioADesbloquear);
                        this.usuariosBloqueados.remove(usuarioADesbloquear);
                        salida.writeUTF("--> Has desbloqueado a '" + usuarioADesbloquear + "'.");
                    }
                }
                else {
                    if (estaRegistrado) {
                        String mensajeConRemitente = this.nombreUsuario + ": " + mensaje;
                        for (UnCliente cliente : ServidorMulti.clientes.values()) {
                            if (cliente != this && !cliente.usuariosBloqueados.contains(this.nombreUsuario)) {
                                cliente.salida.writeUTF(mensajeConRemitente);
                            }
                        }
                    } else {
                        if (contadorMensajesInvitado < 3) {
                            contadorMensajesInvitado++;
                            String mensajeInvitado = this.nombreUsuario + " (invitado): " + mensaje;
                            for (UnCliente cliente : ServidorMulti.clientes.values()) {
                                if (cliente != this) {
                                    cliente.salida.writeUTF(mensajeInvitado);
                                }
                            }
                        } else {
                            salida.writeUTF("--> Límite de mensajes alcanzado. Debes identificarte para chatear.");
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