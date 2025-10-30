package clientemulti;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ParaMandar implements Runnable {
    private final DataOutputStream salida;
    private final BufferedReader teclado;

    public ParaMandar(Socket s) throws IOException {
        this.salida = new DataOutputStream(s.getOutputStream());
        this.teclado = new BufferedReader(new InputStreamReader(System.in));
    }

    private void mostrarGuiaDeComandos() {
        System.out.println("\n--- GUÍA DE COMANDOS ---");
        System.out.println("Chat (en grupo actual): (simplemente escribe tu mensaje)");
        System.out.println("Susurro:      /w <nombre> <mensaje>");

        System.out.println("--- GRUPOS ---");
        System.out.println("Cambiar a grupo: /cambiargrupo <nombre_grupo>");
        System.out.println("Unirse a grupo:  /unirsegrupo <nombre_grupo>");
        System.out.println("Crear grupo:     /creargrupo <nombre_grupo>");
        System.out.println("Borrar grupo:    /borrargrupo <nombre_grupo>");
        System.out.println("Ver mis grupos:  /misgrupos");

        System.out.println("--- JUEGO ---");
        System.out.println("Invitar:      /jugar <oponente>");
        System.out.println("Aceptar:      /aceptar <invitador>");
        System.out.println("Rechazar:     /rechazar <invitador>");
        System.out.println("Mover:        /move <oponente> <fila> <col>");
        System.out.println("Ver partidas: /partidas");

        System.out.println("--- USUARIOS Y STATS ---");
        System.out.println("Ver conectados:   /online");
        System.out.println("Ver todos:        /listusers");
        System.out.println("Bloquear:         /block <nombre>");
        System.out.println("Desbloquear:      /unblock <nombre>");
        System.out.println("Mis Stats:        /stats");
        System.out.println("Ranking:          /ranking");

        System.out.println("--- CUENTA ---");
        System.out.println("Registrar:    nombre: <usuario> <contraseña>");
        System.out.println("Login:        /login <usuario> <contraseña>");
    }

    @Override
    public void run() {
        mostrarGuiaDeComandos();

        while (true) {
            try {
                System.out.print("\nEscribe un mensaje o comando (escribe '/menu' para ver la guía): ");
                String mensaje = teclado.readLine();
                if (mensaje == null) break;

                if (mensaje.trim().equalsIgnoreCase("/menu")) {
                    mostrarGuiaDeComandos();
                } else {
                    enviarMensaje(mensaje);
                }

            } catch (IOException e) {
                System.out.println("Error al leer del teclado o enviar mensaje. Desconectando.");
                break;
            }
        }
    }

    private void enviarMensaje(String mensaje) throws IOException {
        if (mensaje != null && !mensaje.trim().isEmpty()) {
            salida.writeUTF(mensaje);
        }
    }
}