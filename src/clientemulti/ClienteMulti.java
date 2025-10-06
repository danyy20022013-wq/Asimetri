package clientemulti;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClienteMulti {

    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 8080);


        System.out.print("Introduce tu nombre de usuario: ");
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String nombreUsuario = teclado.readLine();


        DataOutputStream salida = new DataOutputStream(s.getOutputStream());
        salida.writeUTF(nombreUsuario);

        System.out.println("¡Conectado! Ahora puedes empezar a chatear.");

        ParaMandar paraMandar = new ParaMandar(s, teclado);
        Thread hiloParaMandar = new Thread(paraMandar);
        hiloParaMandar.start();

        ParaRecibir paraRecibir = new ParaRecibir(s);
        Thread hiloParaRecibir = new Thread(paraRecibir);
        hiloParaRecibir.start();
    }
}
