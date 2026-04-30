package fr.sannoxx.ubutag.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/** Helpers pour (dé)sérialiser une Location depuis/vers un ConfigurationSection. */
public final class LocationUtil {

    private LocationUtil() {}

    public static void save(ConfigurationSection sec, Location loc) {
        if (loc == null || loc.getWorld() == null) {
            sec.set("set", false);
            return;
        }
        sec.set("world", loc.getWorld().getName());
        sec.set("x", loc.getX());
        sec.set("y", loc.getY());
        sec.set("z", loc.getZ());
        sec.set("yaw", loc.getYaw());
        sec.set("pitch", loc.getPitch());
        sec.set("set", true);
    }

    public static Location load(ConfigurationSection sec) {
        if (sec == null) return null;
        if (!sec.getBoolean("set", false)) return null;
        String worldName = sec.getString("world", "");
        if (worldName == null || worldName.isEmpty()) return null;
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw");
        float pitch = (float) sec.getDouble("pitch");
        return new Location(w, x, y, z, yaw, pitch);
    }
}
