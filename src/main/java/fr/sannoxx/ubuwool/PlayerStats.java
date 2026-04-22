package fr.sannoxx.ubuwool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Statistiques persistantes par joueur.
 *
 * Structure JSON par joueur : stats/<uuid>.json
 * {
 *   "totalKills": 42,
 *   "totalDeaths": 15,
 *   "totalWins": 7,
 *   "totalLosses": 3,
 *   "gamesPlayed": 10,
 *   "killsByAgent": { "Sembol": 12, "Carlos": 8, ... },
 *   "gamesByAgent": { "Sembol": 5, ... },
 *   "winsByAgent":  { "Sembol": 3, ... },
 *   "killsByMap":   { "TestMap": 20, ... },
 *   "abilitiesUsed": { "sembol.C1": 42, ... },
 *   "ultimatesUsed": { "Sembol": 5, ... }
 * }
 *
 * Clés UUID → pas de problème si le joueur change de pseudo.
 */
public class PlayerStats {

    private static Path STATS_DIR;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Cache en mémoire UUID → Stats */
    private static final Map<UUID, Stats> cache = new ConcurrentHashMap<>();

    public static void init(UbuWool plugin) {
        STATS_DIR = plugin.getDataFolder().toPath().resolve("stats");
    }

    // =========================================================
    // Modèle de données
    // =========================================================

    public static class Stats {
        public int totalKills   = 0;
        public int totalDeaths  = 0;
        public int totalWins    = 0;
        public int totalLosses  = 0;
        public int gamesPlayed  = 0;
        public int roundsWon    = 0;
        public int roundsLost   = 0;
        public Map<String, Integer> killsByAgent   = new LinkedHashMap<>();
        public Map<String, Integer> gamesByAgent   = new LinkedHashMap<>();
        public Map<String, Integer> winsByAgent    = new LinkedHashMap<>();
        public Map<String, Integer> killsByMap     = new LinkedHashMap<>();
        public Map<String, Integer> abilitiesUsed  = new LinkedHashMap<>();
        public Map<String, Integer> ultimatesUsed  = new LinkedHashMap<>();

        public void addKill(String agentName, String mapName) {
            totalKills++;
            killsByAgent.merge(agentName, 1, Integer::sum);
            if (mapName != null) killsByMap.merge(mapName, 1, Integer::sum);
        }

        public void addDeath() { totalDeaths++; }

        public void addGame(String agentName, boolean won) {
            gamesPlayed++;
            if (won) { totalWins++; winsByAgent.merge(agentName, 1, Integer::sum); }
            else totalLosses++;
            gamesByAgent.merge(agentName, 1, Integer::sum);
        }

        public void addRound(boolean won) {
            if (won) roundsWon++; else roundsLost++;
        }

        public void addAbilityUse(String agentName, String slot) {
            abilitiesUsed.merge(agentName + "." + slot, 1, Integer::sum);
        }

        public void addUltimateUse(String agentName) {
            ultimatesUsed.merge(agentName, 1, Integer::sum);
        }

        /** KDA simplifié : kills / max(1, deaths) */
        public double getKDA() {
            return (double) totalKills / Math.max(1, totalDeaths);
        }

        /** Win rate en % */
        public double getWinRate() {
            return gamesPlayed == 0 ? 0.0 : (double) totalWins / gamesPlayed * 100.0;
        }

        /** Agent le plus joué */
        public String getFavoriteAgent() {
            return gamesByAgent.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("§7—");
        }

        /** Agent avec le meilleur KDA */
        public String getBestAgent() {
            return killsByAgent.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("§7—");
        }
    }

    // =========================================================
    // Accès public
    // =========================================================

    public static Stats get(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerStats::load);
    }

    public static void save(UUID uuid) {
        Stats stats = cache.get(uuid);
        if (stats == null) return;
        saveToDisk(uuid, stats);
    }

    public static void saveAll() {
        for (Map.Entry<UUID, Stats> entry : cache.entrySet()) {
            saveToDisk(entry.getKey(), entry.getValue());
        }
    }

    public static void evict(UUID uuid) {
        Stats stats = cache.remove(uuid);
        if (stats != null) saveToDisk(uuid, stats);
    }

    // =========================================================
    // Helpers de mutation (appelés depuis GameManager)
    // =========================================================

    public static void recordKill(UUID killerUUID, String agentName, String mapName) {
        get(killerUUID).addKill(agentName, mapName);
        save(killerUUID);
    }

    public static void recordDeath(UUID victimUUID) {
        get(victimUUID).addDeath();
        save(victimUUID);
    }

    public static void recordGameEnd(UUID playerUUID, String agentName, boolean won) {
        get(playerUUID).addGame(agentName, won);
        save(playerUUID);
    }

    public static void recordRoundEnd(UUID playerUUID, boolean won) {
        get(playerUUID).addRound(won);
        save(playerUUID);
    }

    public static void recordAbility(UUID playerUUID, String agentName, String slot) {
        get(playerUUID).addAbilityUse(agentName, slot);
        // Pas de save immédiat pour les abilities (trop fréquent) — sauvegardé à la fin du round
    }

    public static void recordUltimate(UUID playerUUID, String agentName) {
        get(playerUUID).addUltimateUse(agentName);
        save(playerUUID);
    }

    // =========================================================
    // I/O
    // =========================================================

    private static Stats load(UUID uuid) {
        if (STATS_DIR == null) return new Stats();
        Path file = STATS_DIR.resolve(uuid + ".json");
        if (!Files.exists(file)) return new Stats();
        try (Reader r = Files.newBufferedReader(file)) {
            Stats s = GSON.fromJson(r, Stats.class);
            return s != null ? s : new Stats();
        } catch (Exception e) {
            return new Stats();
        }
    }

    private static void saveToDisk(UUID uuid, Stats stats) {
        if (STATS_DIR == null) return;
        try {
            Files.createDirectories(STATS_DIR);
            Path file = STATS_DIR.resolve(uuid + ".json");
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(stats, w);
            }
        } catch (Exception e) {
            UbuWool.getInstance().getLogger()
                    .warning("[PlayerStats] Erreur sauvegarde stats " + uuid + " : " + e.getMessage());
        }
    }

    // =========================================================
    // Leaderboard global
    // =========================================================

    public static class LeaderboardEntry {
        public final UUID uuid;
        public final String playerName;
        public final Stats stats;
        LeaderboardEntry(UUID uuid, String playerName, Stats stats) {
            this.uuid = uuid; this.playerName = playerName; this.stats = stats;
        }
    }

    /**
     * Construit un leaderboard en lisant tous les fichiers stats du dossier.
     * Trié par kills décroissants.
     */
    public static List<LeaderboardEntry> buildLeaderboard() {
        if (STATS_DIR == null || !Files.exists(STATS_DIR)) return Collections.emptyList();
        List<LeaderboardEntry> entries = new ArrayList<>();
        try {
            Files.list(STATS_DIR).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    String uuidStr = p.getFileName().toString().replace(".json", "");
                    UUID uuid = UUID.fromString(uuidStr);
                    try (Reader r = Files.newBufferedReader(p)) {
                        Stats s = GSON.fromJson(r, Stats.class);
                        if (s == null) return;
                        org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                        String name = op.getName() != null ? op.getName() : uuidStr.substring(0, 8);
                        entries.add(new LeaderboardEntry(uuid, name, s));
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            UbuWool.getInstance().getLogger()
                    .warning("[PlayerStats] Erreur lecture leaderboard : " + e.getMessage());
        }
        entries.sort((a, b) -> Integer.compare(b.stats.totalKills, a.stats.totalKills));
        return entries;
    }
}