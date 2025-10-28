package servidormulti;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class EstadisticasManager {

    public static String getStatsPersonalesFormateado(String nombre) {
        Stats stats = DataBaseManager.getEstadisticas(nombre);
        if (stats == null) {
            return "--> No se encontraron estadísticas para " + nombre;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--- Estadísticas de ").append(stats.nombre).append(" ---\n");
        sb.append(String.format("Puntos Totales: %d\n", stats.puntos));
        sb.append(String.format("Victorias: %d\n", stats.victorias));
        sb.append(String.format("Empates: %d\n", stats.empates));
        sb.append(String.format("Derrotas: %d\n", stats.derrotas));
        sb.append(String.format(Locale.US, "Winrate: %.2f%%\n", stats.winrate));

        return sb.toString();
    }

    public static String getRankingGeneralFormateado() {
        List<Stats> ranking = DataBaseManager.getRanking();
        if (ranking.isEmpty()) {
            return "--> Aún no hay estadísticas en el ranking.";
        }

        Collections.sort(ranking, new Comparator<Stats>() {
            @Override
            public int compare(Stats s1, Stats s2) {

                return Integer.compare(s2.puntos, s1.puntos);
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("--- Ranking General (Top 10) ---\n");

        int count = 1;
        for (Stats stats : ranking) {
            if (count > 10) break;

            sb.append(String.format(Locale.US, "%d. %s - %d Puntos (V:%d, E:%d, D:%d) - %.1f%% Winrate\n",
                    count,
                    stats.nombre,
                    stats.puntos,
                    stats.victorias,
                    stats.empates,
                    stats.derrotas,
                    stats.winrate
            ));
            count++;
        }
        return sb.toString();
    }
}