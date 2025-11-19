package servidormulti.database;

import servidormulti.grupos.Mensaje;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MensajesDB {

    public static long almacenarMensaje(int grupoId, String emisor, String contenido) {
        String sql = "INSERT INTO mensajes (grupo_id, emisor_nombre, contenido) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, grupoId);
            pstmt.setString(2, emisor);
            pstmt.setString(3, contenido);

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
        return -1;
    }

    public static List<Mensaje> getMensajesNoVistos(String nombreUsuario, int grupoId) {
        List<Mensaje> mensajes = new ArrayList<>();
        String sql = "SELECT m.mensaje_id, m.emisor_nombre, m.contenido, m.timestamp "
                + "FROM mensajes m "
                + "JOIN ultimo_mensaje_leido uml ON m.grupo_id = uml.grupo_id "
                + "WHERE uml.nombre_usuario = ? AND m.grupo_id = ? AND m.mensaje_id > uml.ultimo_mensaje_id "
                + "ORDER BY m.timestamp ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreUsuario);
            pstmt.setInt(2, grupoId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                mensajes.add(new Mensaje(
                        rs.getLong("mensaje_id"),
                        rs.getString("emisor_nombre"),
                        rs.getString("contenido"),
                        rs.getTimestamp("timestamp")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
        return mensajes;
    }

    public static void actualizarUltimoLeido(String nombreUsuario, int grupoId, long ultimoMensajeId) {
        String sql = "UPDATE ultimo_mensaje_leido SET ultimo_mensaje_id = ? "
                + "WHERE nombre_usuario = ? AND grupo_id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, ultimoMensajeId);
            pstmt.setString(2, nombreUsuario);
            pstmt.setInt(3, grupoId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
    }
}