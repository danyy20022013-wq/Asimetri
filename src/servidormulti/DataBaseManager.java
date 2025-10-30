package servidormulti;

import servidormulti.grupos.Grupo;
import servidormulti.grupos.Mensaje;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.sql.*;

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


        String sqlGrupos = "CREATE TABLE IF NOT EXISTS grupos ("
                + " grupo_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " nombre TEXT NOT NULL UNIQUE"
                + ");";

        String sqlMiembrosGrupo = "CREATE TABLE IF NOT EXISTS miembros_grupo ("
                + " grupo_id INTEGER NOT NULL,"
                + " nombre_usuario TEXT NOT NULL,"
                + " PRIMARY KEY (grupo_id, nombre_usuario),"
                + " FOREIGN KEY (grupo_id) REFERENCES grupos(grupo_id) ON DELETE CASCADE,"
                + " FOREIGN KEY (nombre_usuario) REFERENCES usuarios(nombre)"
                + ");";

        String sqlMensajes = "CREATE TABLE IF NOT EXISTS mensajes ("
                + " mensaje_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " grupo_id INTEGER NOT NULL,"
                + " emisor_nombre TEXT NOT NULL,"
                + " contenido TEXT NOT NULL,"
                + " timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + " FOREIGN KEY (grupo_id) REFERENCES grupos(grupo_id) ON DELETE CASCADE,"
                + " FOREIGN KEY (emisor_nombre) REFERENCES usuarios(nombre)"
                + ");";

        String sqlUltimoLeido = "CREATE TABLE IF NOT EXISTS ultimo_mensaje_leido ("
                + " nombre_usuario TEXT NOT NULL,"
                + " grupo_id INTEGER NOT NULL,"
                + " ultimo_mensaje_id INTEGER DEFAULT 0,"
                + " PRIMARY KEY (nombre_usuario, grupo_id),"
                + " FOREIGN KEY (nombre_usuario) REFERENCES usuarios(nombre),"
                + " FOREIGN KEY (grupo_id) REFERENCES grupos(grupo_id) ON DELETE CASCADE"
                + ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlBloqueados);
            stmt.execute(sqlEstadisticas);
            stmt.execute(sqlGrupos);
            stmt.execute(sqlMiembrosGrupo);
            stmt.execute(sqlMensajes);
            stmt.execute(sqlUltimoLeido);

            System.out.println("Base de datos inicializada correctamente.");

            crearGrupoPorDefecto("Todos");

        } catch (SQLException e) {
            System.out.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    private static void crearGrupoPorDefecto(String nombre) {
        if (getGrupoPorNombre(nombre) == null) {
            String sql = "INSERT INTO grupos (nombre) VALUES (?)";
            try (Connection conn = DriverManager.getConnection(URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nombre);
                pstmt.executeUpdate();
                System.out.println("Grupo por defecto 'Todos' creado.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized void registrarUsuario(String nombre, String password) {
        String sqlUsuario = "INSERT INTO usuarios(nombre, password) VALUES(?,?)";
        String sqlStats = "INSERT INTO estadisticas(nombre, victorias, empates, derrotas) VALUES(?, 0, 0, 0)";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
            conn.setAutoCommit(false);

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

            Grupo grupoTodos = getGrupoPorNombre("Todos");
            if (grupoTodos != null) {
                unirUsuarioAGrupo(nombre, grupoTodos.id);
            }

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


    public static Grupo getGrupoPorNombre(String nombre) {
        String sql = "SELECT grupo_id FROM grupos WHERE nombre = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Grupo(rs.getInt("grupo_id"), nombre);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean crearGrupo(String nombre) {
        String sql = "INSERT INTO grupos (nombre) VALUES (?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean borrarGrupo(String nombre) {
        if (nombre.equalsIgnoreCase("Todos")) {
            return false;
        }
        String sql = "DELETE FROM grupos WHERE nombre = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            int filasAfectadas = pstmt.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean unirUsuarioAGrupo(String nombreUsuario, int grupoId) {
        String sqlMiembro = "INSERT OR IGNORE INTO miembros_grupo (grupo_id, nombre_usuario) VALUES (?, ?)";
        String sqlUltimoLeido = "INSERT OR IGNORE INTO ultimo_mensaje_leido (nombre_usuario, grupo_id, ultimo_mensaje_id) VALUES (?, ?, 0)";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
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
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static List<Grupo> getGruposDeUsuario(String nombreUsuario) {
        List<Grupo> grupos = new ArrayList<>();
        String sql = "SELECT g.grupo_id, g.nombre FROM grupos g "
                + "JOIN miembros_grupo mg ON g.grupo_id = mg.grupo_id "
                + "WHERE mg.nombre_usuario = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreUsuario);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                grupos.add(new Grupo(rs.getInt("grupo_id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return grupos;
    }

    public static List<String> getNombresMiembrosDeGrupo(int grupoId) {
        List<String> miembros = new ArrayList<>();
        String sql = "SELECT nombre_usuario FROM miembros_grupo WHERE grupo_id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, grupoId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                miembros.add(rs.getString("nombre_usuario"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return miembros;
    }

    public static long almacenarMensaje(int grupoId, String emisor, String contenido) {
        String sql = "INSERT INTO mensajes (grupo_id, emisor_nombre, contenido) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
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
            e.printStackTrace();
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

        try (Connection conn = DriverManager.getConnection(URL);
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
            e.printStackTrace();
        }
        return mensajes;
    }

    public static void actualizarUltimoLeido(String nombreUsuario, int grupoId, long ultimoMensajeId) {
        String sql = "UPDATE ultimo_mensaje_leido SET ultimo_mensaje_id = ? "
                + "WHERE nombre_usuario = ? AND grupo_id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, ultimoMensajeId);
            pstmt.setString(2, nombreUsuario);
            pstmt.setInt(3, grupoId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            pstmt.setString(1, j1);
            pstmt.addBatch();

            pstmt.setString(1, j2);
            pstmt.addBatch();

            pstmt.executeBatch();
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
}