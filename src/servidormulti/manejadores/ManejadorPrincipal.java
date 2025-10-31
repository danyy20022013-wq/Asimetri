package servidormulti.manejadores;

import servidormulti.UnCliente;
import java.io.IOException;

public class ManejadorPrincipal {

    private final ManejadorAutenticacion authHandler;
    private final ManejadorGrupos grupoHandler;
    private final ManejadorJuego juegoHandler;
    private final ManejadorSocial socialHandler;
    private final ManejadorInfo infoHandler;

    public ManejadorPrincipal() {
        this.authHandler = new ManejadorAutenticacion();
        this.grupoHandler = new ManejadorGrupos();
        this.juegoHandler = new ManejadorJuego();
        this.socialHandler = new ManejadorSocial();
        this.infoHandler = new ManejadorInfo();
    }

    public void procesarComando(UnCliente emisor, String mensaje) throws IOException {

        if (mensaje.startsWith("nombre: ")) {
            authHandler.manejarRegistro(emisor, mensaje);
        } else if (mensaje.startsWith("/login ")) {
            authHandler.manejarLogin(emisor, mensaje);
        } else if (mensaje.startsWith("/jugar ")) {
            juegoHandler.manejarJugar(emisor, mensaje);
        } else if (mensaje.startsWith("/aceptar ")) {
            juegoHandler.manejarAceptar(emisor, mensaje);
        } else if (mensaje.startsWith("/rechazar ")) {
            juegoHandler.manejarRechazar(emisor, mensaje);
        } else if (mensaje.startsWith("/move ")) {
            juegoHandler.manejarMove(emisor, mensaje);
        } else if (mensaje.equals("/partidas")) {
            juegoHandler.manejarPartidas(emisor);
        } else if (mensaje.startsWith("/creargrupo ")) {
            grupoHandler.manejarCrearGrupo(emisor, mensaje);
        } else if (mensaje.startsWith("/borrargrupo ")) {
            grupoHandler.manejarBorrarGrupo(emisor, mensaje);
        } else if (mensaje.startsWith("/unirsegrupo ")) {
            grupoHandler.manejarUnirseGrupo(emisor, mensaje);
        } else if (mensaje.startsWith("/cambiargrupo ")) {
            grupoHandler.manejarCambiarGrupo(emisor, mensaje);
        } else if (mensaje.equals("/misgrupos")) {
            grupoHandler.manejarMisGrupos(emisor);
        } else if (mensaje.equals("/listusers")) {
            infoHandler.manejarListUsers(emisor);
        } else if (mensaje.equals("/online")) {
            infoHandler.manejarOnline(emisor);
        } else if (mensaje.startsWith("/w ")) {
            socialHandler.manejarSusurro(emisor, mensaje);
        } else if (mensaje.startsWith("/block ")) {
            socialHandler.manejarBlock(emisor, mensaje);
        } else if (mensaje.startsWith("/unblock ")) {
            socialHandler.manejarUnblock(emisor, mensaje);
        } else if (mensaje.equals("/blockedlist")) {
            socialHandler.manejarBlockedList(emisor);
        } else if (mensaje.equals("/ranking")) {
            infoHandler.manejarRanking(emisor);
        } else if (mensaje.equals("/stats")) {
            infoHandler.manejarStats(emisor);
        } else {
            grupoHandler.manejarChatDeGrupo(emisor, mensaje);
        }
    }
}