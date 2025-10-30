package servidormulti.grupos;

import java.sql.Timestamp;

public class Mensaje {
    public final long id;
    public final String emisor;
    public final String contenido;
    public final Timestamp timestamp;

    public Mensaje(long id, String emisor, String contenido, Timestamp timestamp) {
        this.id = id;
        this.emisor = emisor;
        this.contenido = contenido;
        this.timestamp = timestamp;
    }
}