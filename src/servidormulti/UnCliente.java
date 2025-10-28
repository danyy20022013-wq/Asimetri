package servidormulti;

import servidormulti.juego.PartidaGato;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Set;

public class UnCliente implements Runnable {

    public final DataOutputStream salida;
    final DataInputStream entrada;

    private String nombreUsuario;
    private final String idInvitadoOriginal;
    private boolean estaRegistrado = false;
    private int contadorMensajesInvitado = 0;
    private Set<String> usuariosBloqueados;

    private final HashMap<String, PartidaGato> partidasEnJuego = new HashMap<>();


    UnCliente(Socket s, String idInvitado) throws IOException {
        this.salida = new DataOutputStream(s.getOutputStream());
        this.entrada = new DataInputStream(s.getInputStream());
        this.idInvitadoOriginal = idInvitado;
        this.nombreUsuario = idInvitado;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void agregarPartida(String oponente, PartidaGato partida) {
        this.partidasEnJuego.put(oponente, partida);
    }

    public void removerPartida(String oponente) {
        this.partidasEnJuego.remove(oponente);
    }

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
            salida.writeUTF("--> ¡Bienvenido! El menú de comandos se mostrará en tu cliente.");

            while (true) {
                String mensaje = entrada.readUTF();

                // Lógica de registro
                if (mensaje.startsWith("nombre: ")) {
                    if (estaRegistrado) { continue; }

                    String[] partes = mensaje.substring(8).trim().split(" ", 2);

                    if (partes.length < 2) {
                        salida.writeUTF("--> Formato incorrecto. Se necesita: nombre: <usuario> <contraseña>"); continue;
                    }
                    String nuevoNombre = partes[0];
                    String password = partes[1];

                    if (DataBaseManager.usuarioExiste(nuevoNombre)) {
                        salida.writeUTF("--> Error: El nombre '" + nuevoNombre + "' ya está registrado.");
                    } else {
                        DataBaseManager.registrarUsuario(nuevoNombre, password);
                        ServidorMulti.usuariosRegistrados.add(nuevoNombre);
                        salida.writeUTF("--> ¡Registro exitoso! Iniciando sesión automáticamente...");
                        finalizarAutenticacion(nuevoNombre);
                    }
                }
                // Lógica de inicio de sesión
                else if (mensaje.startsWith("/login ")) {
                    if (estaRegistrado) { continue; }

                    String[] partes = mensaje.substring(7).trim().split(" ", 2);

                    if (partes.length < 2) {
                        salida.writeUTF("--> Formato incorrecto. Se necesita: /login <usuario> <contraseña>"); continue;
                    }
                    String nombreLogin = partes[0];
                    String passwordLogin = partes[1];

                    if (ServidorMulti.clientes.containsKey(nombreLogin)) {
                        salida.writeUTF("--> Error: El usuario '" + nombreLogin + "' ya está conectado."); continue;
                    }

                    if (DataBaseManager.validarLogin(nombreLogin, passwordLogin)) {
                        finalizarAutenticacion(nombreLogin);
                    } else {
                        salida.writeUTF("--> Error: Nombre de usuario o contraseña incorrectos.");
                    }
                }
                // Lógica de juego
                else if (mensaje.startsWith("/jugar ")) {
                    if (!estaRegistrado) {
                        salida.writeUTF("--> Debes iniciar sesión para jugar."); continue;
                    }
                    String oponenteNombre = mensaje.substring(7).trim();
                    UnCliente oponente = ServidorMulti.clientes.get(oponenteNombre);

                    if (this.partidasEnJuego.containsKey(oponenteNombre)) {
                        salida.writeUTF("--> Ya tienes una partida activa con " + oponenteNombre + "."); continue;
                    }
                    if (oponente == null) {
                        salida.writeUTF("--> El usuario '" + oponenteNombre + "' no está conectado."); continue;
                    }
                    if (oponente == this) {
                        salida.writeUTF("--> No puedes jugar contigo mismo."); continue;
                    }
                    if (this.usuariosBloqueados.contains(oponenteNombre)) {
                        salida.writeUTF("--> No puedes invitar a '" + oponenteNombre + "' porque lo tienes bloqueado."); continue;
                    }
                    if (oponente.usuariosBloqueados.contains(this.nombreUsuario)) {
                        salida.writeUTF("--> No puedes invitar a '" + oponenteNombre + "' (te ha bloqueado)."); continue;
                    }

                    ServidorMulti.invitacionesPendientes.put(oponenteNombre, this.nombreUsuario);
                    oponente.salida.writeUTF("--> ¡" + this.nombreUsuario + " te ha invitado a jugar al Gato!");
                    oponente.salida.writeUTF("--> Escribe: /aceptar " + this.nombreUsuario);
                    salida.writeUTF("--> Invitación enviada a " + oponenteNombre + ".");
                }
                else if (mensaje.startsWith("/aceptar ")) {
                    String invitadorNombre = mensaje.substring(9).trim();
                    String invitacionReal = ServidorMulti.invitacionesPendientes.get(this.nombreUsuario);

                    if (invitacionReal == null || !invitacionReal.equals(invitadorNombre)) {
                        salida.writeUTF("--> No tienes una invitación pendiente de " + invitadorNombre + "."); continue;
                    }

                    UnCliente invitador = ServidorMulti.clientes.get(invitadorNombre);
                    if (invitador == null) {
                        salida.writeUTF("--> El jugador que te invitó ya no está disponible."); continue;
                    }

                    ServidorMulti.invitacionesPendientes.remove(this.nombreUsuario);
                    PartidaGato nuevaPartida = new PartidaGato(invitador, this);

                    invitador.agregarPartida(this.nombreUsuario, nuevaPartida);
                    this.agregarPartida(invitadorNombre, nuevaPartida);

                    nuevaPartida.iniciarPartida();
                }
                else if (mensaje.startsWith("/rechazar ")) {
                    String invitadorNombre = mensaje.substring(10).trim();
                    String invitacionReal = ServidorMulti.invitacionesPendientes.get(this.nombreUsuario);

                    if (invitacionReal != null && invitacionReal.equals(invitadorNombre)) {
                        ServidorMulti.invitacionesPendientes.remove(this.nombreUsuario);
                        UnCliente invitador = ServidorMulti.clientes.get(invitadorNombre);
                        if(invitador != null) {
                            invitador.salida.writeUTF("--> " + this.nombreUsuario + " ha rechazado tu invitación.");
                        }
                        salida.writeUTF("--> Has rechazado la invitación de " + invitadorNombre + ".");
                    } else {
                        salida.writeUTF("--> No tienes una invitación pendiente de " + invitadorNombre + ".");
                    }
                }
                else if (mensaje.startsWith("/move ")) {
                    try {
                        String[] partes = mensaje.substring(6).split(" ");
                        if (partes.length < 3) throw new Exception();

                        String oponenteNombre = partes[0];
                        int fila = Integer.parseInt(partes[1]);
                        int col = Integer.parseInt(partes[2]);

                        PartidaGato partida = this.partidasEnJuego.get(oponenteNombre);
                        if (partida == null) {
                            salida.writeUTF("--> No estás jugando contra " + oponenteNombre + ".");
                            continue;
                        }

                        partida.recibirMovimiento(this, fila, col);
                    } catch (Exception e) {
                        salida.writeUTF("--> Movimiento inválido. Formato: /move <oponente> <fila> <col>");
                    }
                }
                else if (mensaje.equals("/partidas")) {
                    if (this.partidasEnJuego.isEmpty()) {
                        salida.writeUTF("--> No estás en ninguna partida.");
                    } else {
                        salida.writeUTF("--- Tus Partidas Activas ---");
                        for (String oponente : this.partidasEnJuego.keySet()) {
                            salida.writeUTF("- Jugando contra: " + oponente);
                        }
                    }
                }

                // Lógica de chat
                else if (mensaje.equals("/listusers")) {
                    if (ServidorMulti.usuariosRegistrados.isEmpty()) {
                        salida.writeUTF("--> Aún no hay usuarios registrados en el servidor.");
                    } else {
                        StringBuilder listaUsuarios = new StringBuilder("--- Usuarios Registrados (Todos) ---\n");
                        for (String usuario : ServidorMulti.usuariosRegistrados) {
                            if (ServidorMulti.clientes.containsKey(usuario)) {
                                listaUsuarios.append("- ").append(usuario).append(" (Online)\n");
                            } else {
                                listaUsuarios.append("- ").append(usuario).append(" (Offline)\n");
                            }
                        }
                        salida.writeUTF(listaUsuarios.toString());
                    }
                }
                else if (mensaje.equals("/online")) {
                    if (!estaRegistrado && contadorMensajesInvitado >= 3) {
                        salida.writeUTF("--> Debes iniciar sesión para ver la lista."); continue;
                    }
                    StringBuilder onlineUsuarios = new StringBuilder("--- Usuarios Conectados ---\n");
                    int count = 0;
                    for (UnCliente cliente : ServidorMulti.clientes.values()) {
                        if (cliente.estaRegistrado && cliente != this) {
                            onlineUsuarios.append("- ").append(cliente.getNombreUsuario()).append("\n");
                            count++;
                        }
                    }
                    if (count == 0) {
                        salida.writeUTF("--> No hay otros usuarios registrados conectados.");
                    } else {
                        salida.writeUTF(onlineUsuarios.toString());
                    }
                }
                else if (mensaje.startsWith("/w ")) {
                    if (!estaRegistrado) {
                        salida.writeUTF("--> Debes iniciar sesión para enviar susurros."); continue;
                    }
                    String[] partes = mensaje.split(" ", 3);
                    if (partes.length < 3) {
                        salida.writeUTF("--> Uso incorrecto. Formato: /w <nombre> <mensaje>"); continue;
                    }
                    String destinatarioNombre = partes[1];
                    String mensajeSusurro = partes[2];
                    UnCliente destinatario = ServidorMulti.clientes.get(destinatarioNombre);

                    if (destinatario != null) {
                        if (destinatario.usuariosBloqueados.contains(this.nombreUsuario)) {
                            salida.writeUTF("--> No puedes susurrar a " + destinatarioNombre + " (te ha bloqueado)."); continue;
                        }
                        String msgParaDest = this.nombreUsuario + " (te susurra): " + mensajeSusurro;
                        destinatario.salida.writeUTF(msgParaDest);
                        String confirmacion = "(Le susurras a " + destinatarioNombre + "): " + mensajeSusurro;
                        this.salida.writeUTF(confirmacion);
                    } else {
                        salida.writeUTF("--> Usuario '" + destinatarioNombre + "' no está conectado.");
                    }
                }
                else if (mensaje.startsWith("/block ")) {
                    if (!estaRegistrado) { salida.writeUTF("--> Debes iniciar sesión para bloquear."); continue; }
                    String usuarioABloquear = mensaje.substring(7).trim();
                    if (!DataBaseManager.usuarioExiste(usuarioABloquear)) {
                        salida.writeUTF("--> El usuario '" + usuarioABloquear + "' no está registrado.");
                    } else if (this.usuariosBloqueados.contains(usuarioABloquear)) {
                        salida.writeUTF("--> Ya tienes a '" + usuarioABloquear + "' bloqueado.");
                    } else {
                        DataBaseManager.bloquearUsuario(this.nombreUsuario, usuarioABloquear);
                        this.usuariosBloqueados.add(usuarioABloquear);
                        salida.writeUTF("--> Has bloqueado a '" + usuarioABloquear + "'.");
                    }
                }
                else if (mensaje.startsWith("/unblock ")) {
                    if (!estaRegistrado) { salida.writeUTF("--> Debes iniciar sesión para desbloquear."); continue; }
                    String usuarioADesbloquear = mensaje.substring(9).trim();
                    if (!this.usuariosBloqueados.contains(usuarioADesbloquear)) {
                        salida.writeUTF("--> No tienes a '" + usuarioADesbloquear + "' en tu lista de bloqueados.");
                    } else {
                        DataBaseManager.desbloquearUsuario(this.nombreUsuario, usuarioADesbloquear);
                        this.usuariosBloqueados.remove(usuarioADesbloquear);
                        salida.writeUTF("--> Has desbloqueado a '" + usuarioADesbloquear + "'.");
                    }
                }
                else if (mensaje.equals("/blockedlist")) {
                    if (!estaRegistrado) { salida.writeUTF("--> Debes iniciar sesión para ver tu lista."); continue; }
                    if (this.usuariosBloqueados.isEmpty()) {
                        salida.writeUTF("--> Tu lista de bloqueados está vacía.");
                    } else {
                        salida.writeUTF("--> Usuarios bloqueados: " + String.join(", ", this.usuariosBloqueados));
                    }
                }

                // Comandos de estadísticas
                else if (mensaje.equals("/ranking")) {
                    String ranking = EstadisticasManager.getRankingGeneralFormateado();
                    salida.writeUTF(ranking);
                }
                else if (mensaje.equals("/stats")) {
                    if (!estaRegistrado) {
                        salida.writeUTF("--> Debes iniciar sesión para ver tus estadísticas.");
                        continue;
                    }
                    String stats = EstadisticasManager.getStatsPersonalesFormateado(this.nombreUsuario);
                    salida.writeUTF(stats);
                }

                // Mensaje público
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
                            salida.writeUTF("--> Límite de mensajes de invitado alcanzado. Debes iniciar sesión.");
                        }
                    }
                }
            }
        } catch (IOException ex) {

            if (!this.partidasEnJuego.isEmpty()) {
                Set<String> oponentes = Set.copyOf(this.partidasEnJuego.keySet());
                for (String oponente : oponentes) {
                    PartidaGato partida = this.partidasEnJuego.get(oponente);
                    if (partida != null) {
                        partida.abandonarPartida(this);
                    }
                }
            }

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