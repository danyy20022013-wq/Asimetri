package servidormulti.database;

import servidormulti.grupos.Grupo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class UsuariosDB {

    public static synchronized void registrarUsuario(String nombre, String password) {
        String sqlUsuario = "INSERT INTO usuarios(nombre, password) VALUES(?,?)";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlUsuario)) {
                pstmt.setString(1, nombre);
                pstmt.setString(2, password);
                pstmt.executeUpdate();
            }

            conn.commit();

            EstadisticasDB.crearStatsIniciales(nombre);

            Grupo grupoTodos = GruposDB.getGrupoPorNombre("Todos");
            if (grupoTodos != null) {
                GruposDB.unirUsuarioAGrupo(nombre, grupoTodos.id);
            }

        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println("Error en la base de datos: " + e.getMessage()); }
            }
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { System.err.println("Error en la base de datos: " + e.getMessage()); }
            }
        }
    }

    public static synchronized boolean validarLogin(String nombre, String password) {
        String sql = "SELECT password FROM usuarios WHERE nombre = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return storedPassword.equals(password);
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
            return false;
        }
    }

    public static synchronized boolean usuarioExiste(String nombre) {
        String sql = "SELECT nombre FROM usuarios WHERE nombre = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
            return false;
        }
    }

    public static Set<String> cargarUsuariosRegistrados() {
        Set<String> usuarios = new HashSet<>();
        String sql = "SELECT nombre FROM usuarios";
        try (Connection conn = ConexionDB.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usuarios.add(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
        return usuarios;
    }
}