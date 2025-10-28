package servidormulti;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class DataBaseManager {
    private static final String URL = "jdbc:sqlite:chat_database.db";

    public static void inicializar() {
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios ("
                + " nombre TEXT PRIMARY KEY NOT NULL UNIQUE,"
                + " password TEXT NOT NULL"
                + ");";

        String sqlBloqueados = "CREATE TABLE IF NOT EXISTS bloqueados ("
                + " bloqueador TEXT NOT NULL,"
                + " bloqueado TEXT NOT NULL,"
                + " PRIMARY KEY (bloqueador, bloqueado),"
                + " FOREIGN KEY (bloqueador) REFERENCES usuarios(nombre),"
                + " FOREIGN KEY (bloqueado) REFERENCES usuarios(nombre)"
                + ");";


        String sqlEstadisticas = "CREATE TABLE IF NOT EXISTS estadisticas ("
                + " nombre TEXT PRIMARY KEY NOT NULL UNIQUE,"
                + " victorias INTEGER DEFAULT 0,"
                + " empates INTEGER DEFAULT 0,"
                + " derrotas INTEGER DEFAULT 0,"
                + " FOREIGN KEY (nombre) REFERENCES usuarios(nombre)"
                + ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlBloqueados);
            stmt.execute(sqlEstadisticas); // <-- Ejecutar la creación
            System.out.println("Base de datos inicializada correctamente.");
        } catch (SQLException e) {
            System.out.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    // --- MÉTODO MODIFICADO ---
    public static synchronized void registrarUsuario(String nombre, String password) {
        String sqlUsuario = "INSERT INTO usuarios(nombre, password) VALUES(?,?)";
        // Al registrar un usuario, creamos su fila de estadísticas en 0
        String sqlStats = "INSERT INTO estadisticas(nombre, victorias, empates, derrotas) VALUES(?, 0, 0, 0)";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
            conn.setAutoCommit(false); // Iniciar transacción

            try (PreparedStatement pstmt = conn.prepareStatement(sqlUsuario)) {
                pstmt.setString(1, nombre);
                pstmt.setString(2, password);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlStats)) {
                pstmt.setString(1, nombre);
                pstmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public static synchronized void registrarResultado(String ganador, String perdedor) {
        String sqlGanador = "UPDATE estadisticas SET victorias = victorias + 1 WHERE nombre = ?";
        String sqlPerdedor = "UPDATE estadisticas SET derrotas = derrotas + 1 WHERE nombre = ?";

        try (Connection conn = DriverManager.getConnection(URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtG = conn.prepareStatement(sqlGanador);
                 PreparedStatement pstmtP = conn.prepareStatement(sqlPerdedor)) {

                pstmtG.setString(1, ganador);
                pstmtG.executeUpdate();

                pstmtP.setString(1, perdedor);
                pstmtP.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void registrarEmpate(String j1, String j2) {
        String sql = "UPDATE estadisticas SET empates = empates + 1 WHERE nombre = ?";
        try (Connection conn = DriverManager.getConnection(URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, j1);
                pstmt.executeUpdate();

                pstmt.setString(1, j2);
                pstmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Stats getEstadisticas(String nombre) {
        String sql = "SELECT victorias, empates, derrotas FROM estadisticas WHERE nombre = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Stats(nombre, rs.getInt("victorias"), rs.getInt("empates"), rs.getInt("derrotas"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Stats> getRanking() {
        List<Stats> ranking = new ArrayList<>();
        String sql = "SELECT nombre, victorias, empates, derrotas FROM estadisticas";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ranking.add(new Stats(rs.getString("nombre"), rs.getInt("victorias"), rs.getInt("empates"), rs.getInt("derrotas")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ranking;
    }

    public static synchronized boolean validarLogin(String nombre, String password) {
        String sql = "SELECT password FROM usuarios WHERE nombre = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return storedPassword.equals(password);
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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