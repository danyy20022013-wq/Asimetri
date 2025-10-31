package servidormulti.manejadores;
import servidormulti.manejadores.*;
import servidormulti.EstadisticasManager;
import servidormulti.ServidorMulti;
import servidormulti.UnCliente;
import java.io.IOException;

public class ManejadorInfo {

    public void manejarListUsers(UnCliente emisor) throws IOException {
        if (ServidorMulti.usuariosRegistrados.isEmpty()) {
            emisor.salida.writeUTF("--> Aún no hay usuarios registrados en el servidor.");
        } else {
            StringBuilder listaUsuarios = new StringBuilder("--- Usuarios Registrados (Todos) ---\n");
            for (String usuario : ServidorMulti.usuariosRegistrados) {
                if (ServidorMulti.clientes.containsKey(usuario)) {
                    listaUsuarios.append("- ").append(usuario).append(" (Online)\n");
                } else {
                    listaUsuarios.append("- ").append(usuario).append(" (Offline)\n");
                }
            }
            emisor.salida.writeUTF(listaUsuarios.toString());
        }
    }

    public void manejarOnline(UnCliente emisor) throws IOException {
        if (!emisor.estaRegistrado() && emisor.getContadorMensajesInvitado() >= 3) {
            emisor.salida.writeUTF("--> Debes iniciar sesión para ver la lista.");
            return;
        }
        StringBuilder onlineUsuarios = new StringBuilder("--- Usuarios Conectados ---\n");
        int count = 0;
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente.estaRegistrado() && cliente != emisor) {
                onlineUsuarios.append("- ").append(cliente.getNombreUsuario()).append("\n");
                count++;
            }
        }
        if (count == 0) {
            emisor.salida.writeUTF("--> No hay otros usuarios registrados conectados.");
        } else {
            emisor.salida.writeUTF(onlineUsuarios.toString());
        }
    }

    public void manejarRanking(UnCliente emisor) throws IOException {
        String ranking = EstadisticasManager.getRankingGeneralFormateado();
        emisor.salida.writeUTF(ranking);
    }

    public void manejarStats(UnCliente emisor) throws IOException {
        if (!emisor.estaRegistrado()) {
            emisor.salida.writeUTF("--> Debes iniciar sesión para ver tus estadísticas.");
            return;
        }
        String stats = EstadisticasManager.getStatsPersonalesFormateado(emisor.getNombreUsuario());
        emisor.salida.writeUTF(stats);
    }
}