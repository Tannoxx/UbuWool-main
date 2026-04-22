package fr.sannoxx.ubuwool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PlayerProfile {

    private static Path PROFILES_DIR;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Profile> cache = new HashMap<>();
    private static final Set<String> VALID_LANGS = Set.of("FR", "EN");

    public static void init(UbuWool plugin) {
        PROFILES_DIR = plugin.getDataFolder().toPath().resolve("profiles");
    }

    public static class Profile {
        public String language = "FR";
    }

    public static Profile get(Player player) {
        UUID uuid = player.getUniqueId();
        if (cache.containsKey(uuid)) return cache.get(uuid);
        Profile profile = load(uuid);
        cache.put(uuid, profile);
        return profile;
    }

    public static String getLanguage(Player player) {
        return get(player).language;
    }

    public static void setLanguage(Player player, String lang) {
        String sanitized = (lang != null && VALID_LANGS.contains(lang.toUpperCase()))
                ? lang.toUpperCase() : "FR";
        Profile profile = get(player);
        profile.language = sanitized;
        save(player.getUniqueId(), profile);
    }

    private static Profile load(UUID uuid) {
        if (PROFILES_DIR == null) return new Profile();
        Path file = PROFILES_DIR.resolve(uuid + ".json");
        if (!Files.exists(file)) return new Profile();
        try (Reader r = Files.newBufferedReader(file)) {
            Profile p = GSON.fromJson(r, Profile.class);
            if (p == null) return new Profile();
            if (p.language == null || !VALID_LANGS.contains(p.language.toUpperCase())) {
                p.language = "FR";
            } else {
                p.language = p.language.toUpperCase();
            }
            return p;
        } catch (Exception e) {
            return new Profile();
        }
    }

    private static void save(UUID uuid, Profile profile) {
        try {
            if (PROFILES_DIR == null) return;
            Files.createDirectories(PROFILES_DIR);
            Path file = PROFILES_DIR.resolve(uuid + ".json");
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(profile, w);
            }
        } catch (Exception e) {
            UbuWool.getInstance().getLogger()
                    .warning("Erreur sauvegarde profil " + uuid + " : " + e.getMessage());
        }
    }

    public static void clearCache(Player player) {
        cache.remove(player.getUniqueId());
    }
}