package servidormulti.database;

import servidormulti.Stats;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EstadisticasDB {

    public static synchronized void crearStatsIniciales(String nombre) {
        String sqlStats = "INSERT OR IGNORE INTO estadisticas(nombre, victorias, empates, derrotas) VALUES(?, 0, 0, 0)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlStats)) {
            pstmt.setString(1, nombre);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
    }

    public static synchronized void registrarResultado(String ganador, String perdedor) {
        String sqlGanador = "UPDATE estadisticas SET victorias = victorias + 1 WHERE nombre = ?";
        String sqlPerdedor = "UPDATE estadisticas SET derrotas = derrotas + 1 WHERE nombre = ?";

        try (Connection conn = ConexionDB.getConnection()) {
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
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
    }

    public static synchronized void registrarEmpate(String j1, String j2) {
        String sql = "UPDATE estadisticas SET empates = empates + 1 WHERE nombre = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            pstmt.setString(1, j1);
            pstmt.addBatch();
            pstmt.setString(1, j2);
            pstmt.addBatch();
            pstmt.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
    }

    public static Stats getEstadisticas(String nombre) {
        String sql = "SELECT victorias, empates, derrotas FROM estadisticas WHERE nombre = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Stats(nombre, rs.getInt("victorias"), rs.getInt("empates"), rs.getInt("derrotas"));
            }
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
        return null;
    }

    public static List<Stats> getRanking() {
        List<Stats> ranking = new ArrayList<>();
        String sql = "SELECT nombre, victorias, empates, derrotas FROM estadisticas";
        try (Connection conn = ConexionDB.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ranking.add(new Stats(rs.getString("nombre"), rs.getInt("victorias"), rs.getInt("empates"), rs.getInt("derrotas")));
            }
        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }
        return ranking;
    }
}