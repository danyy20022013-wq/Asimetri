package clientemulti;

import java.io.IOException;
import java.net.Socket;

public class ClienteMulti {

    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 8080);

        System.out.println("Conectado al servidor de chat. ¡Bienvenido!");
        System.out.println("El servidor te enviará instrucciones iniciales.");


        ParaRecibir paraRecibir = new ParaRecibir(s);
        Thread hiloParaRecibir = new Thread(paraRecibir);
        hiloParaRecibir.start();


        ParaMandar paraMandar = new ParaMandar(s);
        Thread hiloParaMandar = new Thread(paraMandar);
        hiloParaMandar.start();
    }
}