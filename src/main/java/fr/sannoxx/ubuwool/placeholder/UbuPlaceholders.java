package fr.sannoxx.ubuwool.placeholder;

import fr.sannoxx.ubuwool.PlayerStats;
import fr.sannoxx.ubuwool.UbuWool;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Hook PlaceholderAPI pour exposer les statistiques UbuWool.
 *
 * <p>Placeholders joueur (le joueur est passé par PAPI) :</p>
 * <ul>
 *   <li>%ubuwool_kills%, %ubuwool_deaths%, %ubuwool_kda%</li>
 *   <li>%ubuwool_wins%, %ubuwool_losses%, %ubuwool_games%, %ubuwool_winrate%</li>
 *   <li>%ubuwool_rounds_won%, %ubuwool_rounds_lost%</li>
 *   <li>%ubuwool_favorite_agent%, %ubuwool_best_agent%</li>
 *   <li>%ubuwool_kills_&lt;agent&gt;%, %ubuwool_games_&lt;agent&gt;%, %ubuwool_wins_&lt;agent&gt;%</li>
 *   <li>%ubuwool_kills_map_&lt;map&gt;%</li>
 *   <li>%ubuwool_ultimates_&lt;agent&gt;%</li>
 *   <li>%ubuwool_abilities_&lt;agent&gt;_&lt;slot&gt;%</li>
 * </ul>
 *
 * <p>Placeholders globaux (leaderboard kills) :</p>
 * <ul>
 *   <li>%ubuwool_top_kills_&lt;n&gt;_name%</li>
 *   <li>%ubuwool_top_kills_&lt;n&gt;_value%</li>
 * </ul>
 */
public class UbuPlaceholders extends PlaceholderExpansion {

    private final UbuWool plugin;

    public UbuPlaceholders(UbuWool plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "ubuwool";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) return "";
        String key = params.toLowerCase(Locale.ROOT);

        String top = handleTop(key);
        if (top != null) return top;

        if (player == null) return "";
        UUID uuid = player.getUniqueId();
        PlayerStats.Stats stats = PlayerStats.get(uuid);

        switch (key) {
            case "kills":         return String.valueOf(stats.totalKills);
            case "deaths":        return String.valueOf(stats.totalDeaths);
            case "kda":           return formatDouble(stats.getKDA());
            case "wins":          return String.valueOf(stats.totalWins);
            case "losses":        return String.valueOf(stats.totalLosses);
            case "games":
            case "games_played":  return String.valueOf(stats.gamesPlayed);
            case "winrate":       return formatDouble(stats.getWinRate());
            case "rounds_won":    return String.valueOf(stats.roundsWon);
            case "rounds_lost":   return String.valueOf(stats.roundsLost);
            case "favorite_agent":return stripColors(stats.getFavoriteAgent());
            case "best_agent":    return stripColors(stats.getBestAgent());
            default: break;
        }

        if (key.startsWith("kills_map_")) {
            return mapValue(stats.killsByMap, key.substring("kills_map_".length()));
        }
        if (key.startsWith("kills_")) {
            return mapValue(stats.killsByAgent, key.substring("kills_".length()));
        }
        if (key.startsWith("games_")) {
            return mapValue(stats.gamesByAgent, key.substring("games_".length()));
        }
        if (key.startsWith("wins_")) {
            return mapValue(stats.winsByAgent, key.substring("wins_".length()));
        }
        if (key.startsWith("ultimates_")) {
            return mapValue(stats.ultimatesUsed, key.substring("ultimates_".length()));
        }
        if (key.startsWith("abilities_")) {
            String rest = key.substring("abilities_".length());
            int sep = rest.lastIndexOf('_');
            if (sep <= 0 || sep == rest.length() - 1) return "0";
            String agent = rest.substring(0, sep);
            String slot = rest.substring(sep + 1);
            return mapValue(stats.abilitiesUsed, agent + "." + slot);
        }

        return null;
    }

    private String handleTop(String key) {
        if (!key.startsWith("top_kills_")) return null;
        String rest = key.substring("top_kills_".length());
        int sep = rest.lastIndexOf('_');
        if (sep <= 0 || sep == rest.length() - 1) return "";
        String idxStr = rest.substring(0, sep);
        String field = rest.substring(sep + 1);
        int rank;
        try {
            rank = Integer.parseInt(idxStr);
        } catch (NumberFormatException e) {
            return "";
        }
        if (rank <= 0) return "";

        List<PlayerStats.LeaderboardEntry> board = PlayerStats.buildLeaderboard();
        if (rank > board.size()) {
            return field.equals("value") ? "0" : "";
        }
        PlayerStats.LeaderboardEntry entry = board.get(rank - 1);
        switch (field) {
            case "name":  return entry.playerName;
            case "value":
            case "kills": return String.valueOf(entry.stats.totalKills);
            case "uuid":  return entry.uuid.toString();
            default:      return "";
        }
    }

    private String mapValue(Map<String, Integer> map, String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) return "0";
        Integer direct = map.get(rawKey);
        if (direct != null) return String.valueOf(direct);
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(rawKey)) {
                return String.valueOf(entry.getValue());
            }
        }
        return "0";
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String stripColors(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?i)§[0-9a-fk-or]", "").trim();
    }

    /** Enregistre l'expansion si PlaceholderAPI est présent. */
    public static boolean tryRegister(UbuWool plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return false;
        }
        try {
            return new UbuPlaceholders(plugin).register();
        } catch (Throwable t) {
            plugin.getLogger().warning("[UbuWool] Échec d'enregistrement PlaceholderAPI : " + t.getMessage());
            return false;
        }
    }
}
