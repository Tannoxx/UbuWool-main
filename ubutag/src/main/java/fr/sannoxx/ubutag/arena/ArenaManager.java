package fr.sannoxx.ubutag.arena;

import fr.sannoxx.ubutag.UbuTag;
import fr.sannoxx.ubutag.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gère la liste des arènes : chargement / sauvegarde dans <plugin>/arenas.yml,
 * accesseurs, et le lobby principal commun.
 */
public class ArenaManager {

    private final UbuTag plugin;
    private final File file;
    private YamlConfiguration cfg;
    private final Map<String, Arena> arenas = new HashMap<>();
    private Location mainLobby;

    public ArenaManager(UbuTag plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void loadAll() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { plugin.getLogger().warning("Impossible de créer arenas.yml: " + e.getMessage()); }
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);

        // Main lobby
        ConfigurationSection mlSec = cfg.getConfigurationSection("main-lobby");
        if (mlSec == null) mlSec = cfg.createSection("main-lobby");
        this.mainLobby = LocationUtil.load(mlSec);

        arenas.clear();
        ConfigurationSection root = cfg.getConfigurationSection("arenas");
        if (root != null) {
            for (String name : root.getKeys(false)) {
                ConfigurationSection a = root.getConfigurationSection(name);
                if (a == null) continue;
                Arena arena = new Arena(plugin, name);
                arena.minPlayers = a.getInt("min-players", arena.minPlayers);
                arena.maxPlayers = a.getInt("max-players", arena.maxPlayers);
                arena.lobbyCountdownSeconds = a.getInt("lobby-countdown", arena.lobbyCountdownSeconds);
                arena.shortCountdownSeconds = a.getInt("short-countdown", arena.shortCountdownSeconds);
                arena.roundDurationSeconds = a.getInt("round-duration", arena.roundDurationSeconds);
                arena.roundsPerGame = a.getInt("rounds-per-game", arena.roundsPerGame);
                arena.initialTaggedRatio = a.getDouble("initial-tagged-ratio", arena.initialTaggedRatio);
                arena.speedAmp = a.getInt("speed-amp", arena.speedAmp);
                arena.jumpAmp = a.getInt("jump-amp", arena.jumpAmp);
                arena.blockDamage = a.getBoolean("block-damage", arena.blockDamage);
                arena.enabled = a.getBoolean("enabled", true);

                ConfigurationSection lobbySec = a.getConfigurationSection("lobby");
                if (lobbySec != null) arena.setLobby(LocationUtil.load(lobbySec));
                List<?> spawnsRaw = a.getList("spawns");
                if (spawnsRaw != null) {
                    for (Object o : spawnsRaw) {
                        if (o instanceof Map<?, ?> map) {
                            YamlConfiguration sub = new YamlConfiguration();
                            for (Map.Entry<?, ?> e : map.entrySet()) {
                                sub.set(String.valueOf(e.getKey()), e.getValue());
                            }
                            Location loc = LocationUtil.load(sub);
                            if (loc != null) arena.addSpawn(loc);
                        }
                    }
                }
                arenas.put(name.toLowerCase(), arena);
            }
        }
    }

    public void saveAll() {
        if (cfg == null) cfg = new YamlConfiguration();

        ConfigurationSection mlSec = cfg.getConfigurationSection("main-lobby");
        if (mlSec == null) mlSec = cfg.createSection("main-lobby");
        LocationUtil.save(mlSec, mainLobby);

        cfg.set("arenas", null);
        for (Arena arena : arenas.values()) {
            String base = "arenas." + arena.name;
            cfg.set(base + ".min-players", arena.minPlayers);
            cfg.set(base + ".max-players", arena.maxPlayers);
            cfg.set(base + ".lobby-countdown", arena.lobbyCountdownSeconds);
            cfg.set(base + ".short-countdown", arena.shortCountdownSeconds);
            cfg.set(base + ".round-duration", arena.roundDurationSeconds);
            cfg.set(base + ".rounds-per-game", arena.roundsPerGame);
            cfg.set(base + ".initial-tagged-ratio", arena.initialTaggedRatio);
            cfg.set(base + ".speed-amp", arena.speedAmp);
            cfg.set(base + ".jump-amp", arena.jumpAmp);
            cfg.set(base + ".block-damage", arena.blockDamage);
            cfg.set(base + ".enabled", arena.enabled);

            ConfigurationSection lobbySec = cfg.createSection(base + ".lobby");
            LocationUtil.save(lobbySec, arena.getLobby());

            List<Map<String, Object>> list = new ArrayList<>();
            for (Location s : arena.getSpawns()) {
                YamlConfiguration sub = new YamlConfiguration();
                LocationUtil.save(sub, s);
                Map<String, Object> map = new HashMap<>();
                for (String k : sub.getKeys(false)) map.put(k, sub.get(k));
                list.add(map);
            }
            cfg.set(base + ".spawns", list);
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder arenas.yml: " + e.getMessage());
        }
    }

    public Arena get(String name) {
        if (name == null) return null;
        return arenas.get(name.toLowerCase());
    }

    public Arena create(String name) {
        if (arenas.containsKey(name.toLowerCase())) return null;
        Arena a = new Arena(plugin, name);
        arenas.put(name.toLowerCase(), a);
        saveAll();
        return a;
    }

    public boolean delete(String name) {
        Arena a = arenas.remove(name.toLowerCase());
        if (a == null) return false;
        a.shutdown();
        saveAll();
        return true;
    }

    public List<Arena> getAll() {
        return new ArrayList<>(arenas.values());
    }

    public Location getMainLobby() {
        return mainLobby == null ? null : mainLobby.clone();
    }

    public void setMainLobby(Location l) {
        this.mainLobby = l == null ? null : l.clone();
        saveAll();
    }

    /** Recherche l'arène (lobby ou en jeu) contenant un joueur donné. */
    public Arena findArenaOf(UUID id) {
        for (Arena a : arenas.values()) {
            if (a.isInArena(id)) return a;
        }
        return null;
    }

    public Arena findArenaOf(Player p) {
        return p == null ? null : findArenaOf(p.getUniqueId());
    }

    /** Renvoie une arène disponible pour le matchmaking automatique. */
    public Arena findJoinable() {
        Arena best = null;
        int bestScore = -1;
        for (Arena a : arenas.values()) {
            if (!a.enabled) continue;
            if (!a.isReady()) continue;
            if (a.isFull()) continue;
            ArenaState s = a.getState();
            if (s == ArenaState.IN_GAME || s == ArenaState.ENDING) continue;
            // priorise les arènes les plus remplies
            int score = a.getPlayers().size();
            if (score > bestScore) {
                bestScore = score;
                best = a;
            }
        }
        return best;
    }

    public void shutdownAll() {
        for (Arena a : arenas.values()) a.shutdown();
    }

    /** Permet d'invoquer un Bukkit task wrapper si besoin (utilisé par d'autres classes). */
    public void schedule(Runnable r) {
        Bukkit.getScheduler().runTask(plugin, r);
    }
}
