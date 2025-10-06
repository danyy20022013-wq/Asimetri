package servidormulti;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class UnCliente implements Runnable {

    final DataOutputStream salida;
    final DataInputStream entrada;
    String nombreUsuario;

    UnCliente(Socket s) throws IOException {
        this.salida = new DataOutputStream(s.getOutputStream());
        this.entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        try {

            this.nombreUsuario = entrada.readUTF();


            ServidorMulti.clientes.put(this.nombreUsuario, this);


            System.out.println("Se conectó: " + this.nombreUsuario);
            for (UnCliente cliente : ServidorMulti.clientes.values()) {
                if (cliente != this) { // No enviar el mensaje a sí mismo
                    cliente.salida.writeUTF("--> " + this.nombreUsuario + " se ha unido al chat.");
                }
            }


            while (true) {
                String mensaje = entrada.readUTF();

                if (mensaje.startsWith("@")) {
                    String[] partes = mensaje.split(" ", 2);
                    if (partes.length < 2) continue;

                    String destinatarioNombre = partes[0].substring(1);
                    UnCliente destinatario = ServidorMulti.clientes.get(destinatarioNombre);

                    if (destinatario != null) {
                        String mensajePrivado = this.nombreUsuario + " (privado): " + partes[1];
                        destinatario.salida.writeUTF(mensajePrivado);
                    } else {
                        this.salida.writeUTF("--> Usuario '" + destinatarioNombre + "' no encontrado.");
                    }
                } else {
                    String mensajeConRemitente = this.nombreUsuario + ": " + mensaje;
                    for (UnCliente cliente : ServidorMulti.clientes.values()) {
                        cliente.salida.writeUTF(mensajeConRemitente);
                    }
                }
            }
        } catch (IOException ex) {

            if (this.nombreUsuario != null) {
                System.out.println(this.nombreUsuario + " se ha desconectado.");
                ServidorMulti.clientes.remove(this.nombreUsuario);

                try {
                    for (UnCliente cliente : ServidorMulti.clientes.values()) {
                        cliente.salida.writeUTF("--> " + this.nombreUsuario + " ha abandonado el chat.");
                    }
                } catch (IOException e) {

                }
            }
        }
    }
}