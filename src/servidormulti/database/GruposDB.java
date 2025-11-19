package servidormulti.database;

import servidormulti.grupos.Grupo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GruposDB {

    public static Grupo getGrupoPorNombre(String nombre) {
        String sql = "SELECT grupo_id FROM grupos WHERE nombre = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Grupo(rs.getInt("grupo_id"), nombre);
            }
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
        return null;
    }

    public static boolean crearGrupo(String nombre) {
        String sql = "INSERT INTO grupos (nombre) VALUES (?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
            return false;
        }
    }

    public static boolean borrarGrupo(String nombre) {
        if (nombre.equalsIgnoreCase("Todos")) {
            return false;
        }
        String sql = "DELETE FROM grupos WHERE nombre = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            int filasAfectadas = pstmt.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
            return false;
        }
    }

    public static boolean unirUsuarioAGrupo(String nombreUsuario, int grupoId) {
        String sqlMiembro = "INSERT OR IGNORE INTO miembros_grupo (grupo_id, nombre_usuario) VALUES (?, ?)";
        String sqlUltimoLeido = "INSERT OR IGNORE INTO ultimo_mensaje_leido (nombre_usuario, grupo_id, ultimo_mensaje_id) VALUES (?, ?, 0)";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlMiembro)) {
                pstmt.setInt(1, grupoId);
                pstmt.setString(2, nombreUsuario);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUltimoLeido)) {
                pstmt.setString(1, nombreUsuario);
                pstmt.setInt(2, grupoId);
                pstmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { System.err.println("Error en la base de datos: " + e.getMessage()); }
        }
    }

    public static List<Grupo> getGruposDeUsuario(String nombreUsuario) {
        List<Grupo> grupos = new ArrayList<>();
        String sql = "SELECT g.grupo_id, g.nombre FROM grupos g "
                + "JOIN miembros_grupo mg ON g.grupo_id = mg.grupo_id "
                + "WHERE mg.nombre_usuario = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreUsuario);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                grupos.add(new Grupo(rs.getInt("grupo_id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
        return grupos;
    }

    public static List<String> getNombresMiembrosDeGrupo(int grupoId) {
        List<String> miembros = new ArrayList<>();
        String sql = "SELECT nombre_usuario FROM miembros_grupo WHERE grupo_id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, grupoId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                miembros.add(rs.getString("nombre_usuario"));
            }
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
        return miembros;
    }
}