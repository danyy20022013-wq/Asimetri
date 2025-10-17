package servidormulti;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class DataBaseManager {
    private static final String URL = "jdbc:sqlite:chat_database.db";


    public static void inicializar() {
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios ("
                + " nombre TEXT PRIMARY KEY NOT NULL UNIQUE"
                + ");";

        String sqlBloqueados = "CREATE TABLE IF NOT EXISTS bloqueados ("
                + " bloqueador TEXT NOT NULL,"
                + " bloqueado TEXT NOT NULL,"
                + " PRIMARY KEY (bloqueador, bloqueado),"
                + " FOREIGN KEY (bloqueador) REFERENCES usuarios(nombre),"
                + " FOREIGN KEY (bloqueado) REFERENCES usuarios(nombre)"
                + ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlBloqueados);
            System.out.println("Base de datos inicializada correctamente.");
        } catch (SQLException e) {
            System.out.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    public static synchronized boolean usuarioExiste(String nombre) {
        String sql = "SELECT nombre FROM usuarios WHERE nombre = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized void registrarUsuario(String nombre) {
        String sql = "INSERT INTO usuarios(nombre) VALUES(?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Set<String> cargarUsuariosRegistrados() {
        Set<String> usuarios = new HashSet<>();
        String sql = "SELECT nombre FROM usuarios";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usuarios.add(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usuarios;
    }
    public static Set<String> cargarListaDeBloqueados(String nombreUsuario) {
        Set<String> bloqueados = new HashSet<>();
        String sql = "SELECT bloqueado FROM bloqueados WHERE bloqueador = ?";
        try (Connection conn = DriverManager.getConnection(URL);
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
        String sql = "INSERT INTO bloqueados(bloqueador, bloqueado) VALUES(?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
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
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}