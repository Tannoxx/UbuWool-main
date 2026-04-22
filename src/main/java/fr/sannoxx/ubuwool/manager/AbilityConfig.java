package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.UbuWool;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Lecteur centralisé de abilities.yml.
 *
 * Toutes les valeurs de gameplay (cooldowns, dégâts, durées, portées) sont
 * lues ici au lieu d'être hardcodées dans les *Abilities.java.
 *
 * Usage : AbilityConfig.cooldown("sembol", "c1")  → 50000L (ms)
 *         AbilityConfig.damage("larok_c2")         → 4.0
 *         AbilityConfig.duration("sembol_ult_speed") → 400L (ticks)
 */
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

    /** Cooldown en ms pour un agent et un slot (c1, c2, c2_tp). */
    public static long cooldown(String agent, String slot) {
        if (config == null) return 30_000L;
        return config.getLong("cooldowns." + agent.toLowerCase() + "." + slot, 30_000L);
    }

    /** Dégâts pour une ability. */
    public static double damage(String key) {
        if (config == null) return 4.0;
        return config.getDouble("damage." + key, 4.0);
    }

    /** HP minimum laissé par la Raze Rocket. */
    public static double damageMin(String key) {
        if (config == null) return 1.0;
        return config.getDouble("damage." + key + "_min", 1.0);
    }

    /** Durée en ticks pour une ability. */
    public static long duration(String key) {
        if (config == null) return 100L;
        return config.getLong("durations." + key, 100L);
    }

    /** Durée en millisecondes (converti depuis ticks × 50). */
    public static long durationMs(String key) {
        return duration(key) * 50L;
    }

    /** Rayon / distance pour une ability. */
    public static double range(String key) {
        if (config == null) return 5.0;
        return config.getDouble("ranges." + key, 5.0);
    }

    public static int rangeInt(String key) {
        return (int) range(key);
    }
}