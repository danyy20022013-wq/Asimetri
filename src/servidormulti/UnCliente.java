package servidormulti;

import servidormulti.database.BloqueosDB;
import servidormulti.database.GruposDB;
import servidormulti.grupos.Grupo;
import servidormulti.juego.PartidaGato;
import servidormulti.manejadores.ManejadorPrincipal;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Set;

public class UnCliente implements Runnable {

    public final DataOutputStream salida;
    final DataInputStream entrada;

    private final ManejadorPrincipal manejadorPrincipal;

    private String nombreUsuario;
    private final String idInvitadoOriginal;
    private boolean estaRegistrado = false;
    private int contadorMensajesInvitado = 0;
    private Set<String> usuariosBloqueados;
    private final HashMap<String, PartidaGato> partidasEnJuego = new HashMap<>();
    private Grupo grupoActual;

    UnCliente(Socket s, String idInvitado) throws IOException {
        this.salida = new DataOutputStream(s.getOutputStream());
        this.entrada = new DataInputStream(s.getInputStream());
        this.idInvitadoOriginal = idInvitado;
        this.nombreUsuario = idInvitado;
        this.grupoActual = GruposDB.getGrupoPorNombre("Todos");

        this.manejadorPrincipal = new ManejadorPrincipal();
    }


    public String getNombreUsuario() { return nombreUsuario; }
    public Grupo getGrupoActual() { return grupoActual; }
    public Set<String> getUsuariosBloqueados() { return usuariosBloqueados; }
    public HashMap<String, PartidaGato> getPartidasEnJuego() { return partidasEnJuego; }
    public boolean estaRegistrado() { return estaRegistrado; }
    public int getContadorMensajesInvitado() { return contadorMensajesInvitado; }

    public void setGrupoActual(Grupo grupo) { this.grupoActual = grupo; }
    public void agregarPartida(String oponente, PartidaGato partida) { this.partidasEnJuego.put(oponente, partida); }
    public void removerPartida(String oponente) { this.partidasEnJuego.remove(oponente); }
    public void incrementarMensajesInvitado() { this.contadorMensajesInvitado++; }

    public void finalizarAutenticacion(String nombreExitoso) throws IOException {
        ServidorMulti.clientes.remove(this.idInvitadoOriginal);
        this.nombreUsuario = nombreExitoso;
        this.estaRegistrado = true;
        this.usuariosBloqueados = BloqueosDB.cargarListaDeBloqueados(this.nombreUsuario);
        ServidorMulti.clientes.put(this.nombreUsuario, this);

        salida.writeUTF("--> ¡Autenticación exitosa! Bienvenido, " + this.nombreUsuario);
        System.out.println(this.idInvitadoOriginal + " se ha identificado como " + this.nombreUsuario);

        ServidorMulti.grupoManager.procesarMensaje(this, this.grupoActual, "(Se ha conectado)");
        ServidorMulti.grupoManager.enviarMensajesNoVistos(this, this.grupoActual);
    }

    private void manejarDesconexion() {
        System.out.println(this.nombreUsuario + " se ha desconectado.");
        ServidorMulti.clientes.remove(this.nombreUsuario);

        if (!this.partidasEnJuego.isEmpty()) {
            Set<String> oponentes = Set.copyOf(this.partidasEnJuego.keySet());
            for (String oponente : oponentes) {
                PartidaGato partida = this.partidasEnJuego.get(oponente);
                if (partida != null) {
                    partida.abandonarPartida(this);
                }
            }
        }

        if (this.estaRegistrado) {
            try {
                String msgDesconexion = "(Se ha desconectado)";
                ServidorMulti.grupoManager.procesarMensaje(this, this.grupoActual, msgDesconexion);
            } catch (Exception e) {
                System.err.println("Error al notificar desconexión de " + this.nombreUsuario + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        try {
            ServidorMulti.clientes.put(this.nombreUsuario, this);
            System.out.println("Se conectó un nuevo cliente: " + this.nombreUsuario);
            salida.writeUTF("--> ¡Bienvenido! Estás en el grupo 'Todos'.");
            salida.writeUTF("--> Usa /menu para ver la guía de comandos.");

            if (estaRegistrado) {
                ServidorMulti.grupoManager.enviarMensajesNoVistos(this, this.grupoActual);
            }

            while (true) {
                String mensaje = entrada.readUTF();
                manejadorPrincipal.procesarComando(this, mensaje);
            }
        } catch (IOException ex) {
            this.manejarDesconexion();
        }
    }
}