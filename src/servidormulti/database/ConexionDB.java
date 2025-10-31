package servidormulti.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {

    private static final String URL = "jdbc:sqlite:chat_database.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void inicializar() {
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios ( nombre TEXT PRIMARY KEY NOT NULL UNIQUE, password TEXT NOT NULL );";
        String sqlBloqueados = "CREATE TABLE IF NOT EXISTS bloqueados ( bloqueador TEXT NOT NULL, bloqueado TEXT NOT NULL, PRIMARY KEY (bloqueador, bloqueado), FOREIGN KEY (bloqueador) REFERENCES usuarios(nombre), FOREIGN KEY (bloqueado) REFERENCES usuarios(nombre) );";
        String sqlEstadisticas = "CREATE TABLE IF NOT EXISTS estadisticas ( nombre TEXT PRIMARY KEY NOT NULL UNIQUE, victorias INTEGER DEFAULT 0, empates INTEGER DEFAULT 0, derrotas INTEGER DEFAULT 0, FOREIGN KEY (nombre) REFERENCES usuarios(nombre) );";
        String sqlGrupos = "CREATE TABLE IF NOT EXISTS grupos ( grupo_id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL UNIQUE );";
        String sqlMiembrosGrupo = "CREATE TABLE IF NOT EXISTS miembros_grupo ( grupo_id INTEGER NOT NULL, nombre_usuario TEXT NOT NULL, PRIMARY KEY (grupo_id, nombre_usuario), FOREIGN KEY (grupo_id) REFERENCES grupos(grupo_id) ON DELETE CASCADE, FOREIGN KEY (nombre_usuario) REFERENCES usuarios(nombre) );";
        String sqlMensajes = "CREATE TABLE IF NOT EXISTS mensajes ( mensaje_id INTEGER PRIMARY KEY AUTOINCREMENT, grupo_id INTEGER NOT NULL, emisor_nombre TEXT NOT NULL, contenido TEXT NOT NULL, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (grupo_id) REFERENCES grupos(grupo_id) ON DELETE CASCADE, FOREIGN KEY (emisor_nombre) REFERENCES usuarios(nombre) );";
        String sqlUltimoLeido = "CREATE TABLE IF NOT EXISTS ultimo_mensaje_leido ( nombre_usuario TEXT NOT NULL, grupo_id INTEGER NOT NULL, ultimo_mensaje_id INTEGER DEFAULT 0, PRIMARY KEY (nombre_usuario, grupo_id), FOREIGN KEY (nombre_usuario) REFERENCES usuarios(nombre), FOREIGN KEY (grupo_id) REFERENCES grupos(grupo_id) ON DELETE CASCADE );";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlBloqueados);
            stmt.execute(sqlEstadisticas);
            stmt.execute(sqlGrupos);
            stmt.execute(sqlMiembrosGrupo);
            stmt.execute(sqlMensajes);
            stmt.execute(sqlUltimoLeido);

            System.out.println("Base de datos inicializada correctamente.");
            crearGrupoPorDefecto();

        } catch (SQLException e) {
            System.out.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    private static void crearGrupoPorDefecto() {
        if (GruposDB.getGrupoPorNombre("Todos") == null) {
            String sql = "INSERT INTO grupos (nombre) VALUES (?)";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "Todos");
                pstmt.executeUpdate();
                System.out.println("Grupo por defecto 'Todos' creado.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}