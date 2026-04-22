package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.UbuWool;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class AbilityConfig {

    private static YamlConfiguration config;

    public static void load(UbuWool plugin) {
        plugin.saveResource("abilities.yml", false);
        File file = new File(plugin.getDataFolder(), "abilities.yml");
        config = YamlConfiguration.loadConfiguration(file);
        plugin.getLogger().info("[AbilityConfig] abilities.yml chargé.");
    }

    public static void reload() {
        File file = new File(UbuWool.getInstance().getDataFolder(), "abilities.yml");
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
            UbuWool.getInstance().getLogger().info("[AbilityConfig] abilities.yml rechargé.");
        }
    }

    public static long cooldown(String agent, String slot) {
        if (config == null) return 30_000L;
        return config.getLong("cooldowns." + agent.toLowerCase() + "." + slot, 30_000L);
    }

    public static double damage(String key) {
        if (config == null) return 4.0;
        return config.getDouble("damage." + key, 4.0);
    }

    public static double damageMin(String key) {
        if (config == null) return 1.0;
        return config.getDouble("damage." + key + "_min", 1.0);
    }

    public static long duration(String key) {
        if (config == null) return 100L;
        return config.getLong("durations." + key, 100L);
    }

    public static long durationMs(String key) {
        return duration(key) * 50L;
    }

    public static double range(String key) {
        if (config == null) return 5.0;
        return config.getDouble("ranges." + key, 5.0);
    }

    public static int rangeInt(String key) {
        return (int) range(key);
    }
}