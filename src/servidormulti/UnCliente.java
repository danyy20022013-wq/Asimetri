package servidormulti;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class UnCliente implements Runnable {

    final DataOutputStream salida;
    final DataInputStream entrada;

    private String nombreUsuario;
    private final String idInvitadoOriginal;
    private boolean estaRegistrado = false;
    private int contadorMensajesInvitado = 0;

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

            salida.writeUTF("¡Bienvenido! Estás conectado como " + this.nombreUsuario + ".");
            salida.writeUTF(" Tienes 3 mensajes para enviar como invitado.");
            salida.writeUTF(" Para registrarte, usa el comando: nombre: <tu_nombre>");

            while (true) {
                String mensaje = entrada.readUTF();

                if (mensaje.startsWith("nombre: ")) {
                    if (estaRegistrado) {
                        salida.writeUTF("Ya estás identificado como " + this.nombreUsuario);
                        continue;
                    }

                    String nuevoNombre = mensaje.substring(7).trim();
                    if (ServidorMulti.clientes.containsKey(nuevoNombre)) {
                        salida.writeUTF(" Error: El nombre '" + nuevoNombre + "' ya está en uso. Elige otro.");
                    } else {
                        ServidorMulti.clientes.remove(this.idInvitadoOriginal);
                        this.nombreUsuario = nuevoNombre;
                        this.estaRegistrado = true;
                        ServidorMulti.clientes.put(this.nombreUsuario, this);

                        salida.writeUTF(" Te has identificado correctamente como: " + this.nombreUsuario);
                        System.out.println(this.idInvitadoOriginal + " se identificó como " + this.nombreUsuario);


                        for (UnCliente cliente : ServidorMulti.clientes.values()) {
                            if (cliente != this) {
                                cliente.salida.writeUTF(  this.nombreUsuario + " se ha unido al chat.");
                            }
                        }
                    }
                }
                else {
                    if (estaRegistrado) {

                        String mensajeConRemitente = this.nombreUsuario + ": " + mensaje;
                        for (UnCliente cliente : ServidorMulti.clientes.values()) {
                            if (cliente != this) {
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
                            if(contadorMensajesInvitado == 3) {
                                salida.writeUTF(" Has agotado tus mensajes de invitado. Usa nombre: <nombre> para continuar.");
                            }
                        } else {
                            salida.writeUTF(" Límite de mensajes alcanzado. Debes identificarte para chatear.");
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
                        cliente.salida.writeUTF( this.nombreUsuario + " ha abandonado el chat.");
                    }
                } catch (IOException e) {}
            }
        }
    }
}