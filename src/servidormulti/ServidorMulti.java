package servidormulti;
import servidormulti.database.*;
import servidormulti.grupos.GrupoManager;
import servidormulti.juego.PartidaGato;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Set;

public class ServidorMulti {

    public static HashMap<String, UnCliente> clientes = new HashMap<>();
    public static Set<String> usuariosRegistrados;
   public  static final HashMap<String, String> invitacionesPendientes = new HashMap<>();

    public static final GrupoManager grupoManager = new GrupoManager();

    private static int contadorInvitados = 1;

    public static void main(String[] args) {
        ConexionDB.inicializar();
        usuariosRegistrados = UsuariosDB.cargarUsuariosRegistrados();
        System.out.println("Cargados " + usuariosRegistrados.size() + " usuarios registrados desde la base de datos.");

        int puerto = 8080;
        try (ServerSocket servidorSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en el puerto " + puerto);

            while (true) {
                Socket socket = servidorSocket.accept();
                String idInvitado = "chango-" + contadorInvitados++;
                UnCliente uncliente = new UnCliente(socket, idInvitado);
                Thread hilo = new Thread(uncliente);
                hilo.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}