package fr.sannoxx.ubutag.stats;

import fr.sannoxx.ubutag.UbuTag;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Charge / sauvegarde les stats des joueurs dans <plugin>/stats.yml. */
public class StatsManager {

    private final UbuTag plugin;
    private final File file;
    private YamlConfiguration cfg;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();

    public StatsManager(UbuTag plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void loadAll() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { plugin.getLogger().warning("Impossible de créer stats.yml: " + e.getMessage()); }
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
        cache.clear();
        ConfigurationSection root = cfg.getConfigurationSection("players");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    ConfigurationSection s = root.getConfigurationSection(key);
                    if (s == null) continue;
                    PlayerStats ps = new PlayerStats(id);
                    ps.name = s.getString("name", "");
                    ps.wins = s.getInt("wins", 0);
                    ps.gamesPlayed = s.getInt("gamesPlayed", 0);
                    ps.kills = s.getInt("kills", 0);
                    ps.tagsPassed = s.getInt("tagsPassed", 0);
                    ps.bestStreak = s.getInt("bestStreak", 0);
                    ps.currentStreak = s.getInt("currentStreak", 0);
                    ps.survivalSeconds = s.getLong("survivalSeconds", 0L);
                    cache.put(id, ps);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveAll() {
        if (cfg == null) cfg = new YamlConfiguration();
        cfg.set("players", null);
        for (PlayerStats ps : cache.values()) {
            String base = "players." + ps.uuid;
            cfg.set(base + ".name", ps.name);
            cfg.set(base + ".wins", ps.wins);
            cfg.set(base + ".gamesPlayed", ps.gamesPlayed);
            cfg.set(base + ".kills", ps.kills);
            cfg.set(base + ".tagsPassed", ps.tagsPassed);
            cfg.set(base + ".bestStreak", ps.bestStreak);
            cfg.set(base + ".currentStreak", ps.currentStreak);
            cfg.set(base + ".survivalSeconds", ps.survivalSeconds);
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder stats.yml: " + e.getMessage());
        }
    }

    public PlayerStats getOrCreate(UUID id, String name) {
        PlayerStats ps = cache.computeIfAbsent(id, PlayerStats::new);
        if (name != null && !name.isEmpty()) ps.name = name;
        return ps;
    }

    public PlayerStats get(UUID id) {
        return cache.get(id);
    }

    public PlayerStats lookup(String name) {
        for (PlayerStats ps : cache.values()) {
            if (ps.name != null && ps.name.equalsIgnoreCase(name)) return ps;
        }
        return null;
    }

    public List<PlayerStats> top(int size) {
        List<PlayerStats> all = new ArrayList<>(cache.values());
        all.sort(Comparator.comparingInt((PlayerStats p) -> p.wins).reversed());
        if (all.size() > size) return all.subList(0, size);
        return all;
    }

    /** Met à jour le pseudo enregistré (utile après un changement de nom). */
    public void touchName(OfflinePlayer p) {
        if (p == null || p.getUniqueId() == null) return;
        if (p.getName() == null) return;
        PlayerStats ps = getOrCreate(p.getUniqueId(), p.getName());
        ps.name = p.getName();
    }
}
