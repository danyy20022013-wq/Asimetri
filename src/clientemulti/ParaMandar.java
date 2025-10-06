package clientemulti;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ParaMandar implements Runnable {
    final BufferedReader teclado;
    final DataOutputStream salida;


    public ParaMandar(Socket s, BufferedReader teclado) throws IOException {
        this.salida = new DataOutputStream(s.getOutputStream());
        this.teclado = teclado;
    }

    @Override
    public void run() {
        while (true) {
            String mensaje;
            try {
                mensaje = teclado.readLine();
                salida.writeUTF(mensaje);
            } catch (IOException ex) {
                System.out.println("Error al enviar mensaje.");
                break;
            }
        }
    }
}
