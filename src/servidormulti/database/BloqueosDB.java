package servidormulti.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class BloqueosDB {

    public static Set<String> cargarListaDeBloqueados(String nombreUsuario) {
        Set<String> bloqueados = new HashSet<>();
        String sql = "SELECT bloqueado FROM bloqueados WHERE bloqueador = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreUsuario);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                bloqueados.add(rs.getString("bloqueado"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bloqueados;
    }

    public static synchronized void bloquearUsuario(String bloqueador, String bloqueado) {
        String sql = "INSERT OR IGNORE INTO bloqueados(bloqueador, bloqueado) VALUES(?,?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void desbloquearUsuario(String bloqueador, String bloqueado) {
        String sql = "DELETE FROM bloqueados WHERE bloqueador = ? AND bloqueado = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}