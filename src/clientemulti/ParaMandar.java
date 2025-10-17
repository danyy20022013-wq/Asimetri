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

    private void mostrarMenu() {
        System.out.println("\n--- MENÚ DE ACCIONES ---");
        System.out.println("1. Enviar mensaje a todos");
        System.out.println("2. Enviar susurro (mensaje privado)");
        System.out.println("3. Ver lista de usuarios");
        System.out.println("4. Bloquear usuario");
        System.out.println("5. Desbloquear usuario");
        System.out.println("6. Ver mi lista de bloqueados");
        System.out.println("7. Registrar una nueva cuenta");
        System.out.println("8. Iniciar sesión (Login)");
        System.out.print("Elige una opción: ");
    }

    @Override
    public void run() {
        while (true) {
            mostrarMenu();
            try {
                String opcion = teclado.readLine();
                if (opcion == null) break;

                switch (opcion.trim()) {
                    case "1": // Enviar mensaje a todos
                        System.out.print("Escribe tu mensaje público: ");
                        String mensajePublico = teclado.readLine();
                        enviarMensaje(mensajePublico);
                        break;
                    case "2": // Enviar susurro
                        System.out.print("¿A quién quieres susurrar?: ");
                        String destinatario = teclado.readLine();
                        System.out.print("Escribe tu susurro: ");
                        String mensajePrivado = teclado.readLine();
                        enviarMensaje("/w " + destinatario + " " + mensajePrivado);
                        break;
                    case "3": // Ver lista de usuarios
                        enviarMensaje("/listusers");
                        break;
                    case "4": // Bloquear usuario
                        System.out.print("¿A quién quieres bloquear?: ");
                        String usuarioABloquear = teclado.readLine();
                        enviarMensaje("/block " + usuarioABloquear);
                        break;
                    case "5": // Desbloquear usuario
                        System.out.print("¿A quién quieres desbloquear?: ");
                        String usuarioADesbloquear = teclado.readLine();
                        enviarMensaje("/unblock " + usuarioADesbloquear);
                        break;
                    case "6": // Ver lista de bloqueados
                        enviarMensaje("/blockedlist");
                        break;
                    case "7": // Registrar nueva cuenta
                        System.out.print("Elige un nombre de usuario nuevo: ");
                        String nuevoNombre = teclado.readLine();
                        System.out.print("Elige una contraseña: ");
                        String nuevaPass = teclado.readLine();
                        enviarMensaje("nombre: " + nuevoNombre + " " + nuevaPass);
                        break;
                    case "8": // Iniciar sesión
                        System.out.print("Tu nombre de usuario: ");
                        String nombreLogin = teclado.readLine();
                        System.out.print("Tu contraseña: ");
                        String passLogin = teclado.readLine();
                        enviarMensaje("/login " + nombreLogin + " " + passLogin);
                        break;
                    default:
                        System.out.println("--> Opción no válida. Por favor, elige un número del menú.");
                        break;
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