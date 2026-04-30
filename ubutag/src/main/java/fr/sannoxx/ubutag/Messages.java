package fr.sannoxx.ubutag;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Charge messages.yml et fournit les helpers de formatage / envoi.
 *
 * Les messages supportent les codes couleurs '&' et un système de
 * placeholders {key} remplacés par les arguments fournis.
 */
public final class Messages {

    private final UbuTag plugin;
    private YamlConfiguration cfg;
    private String prefix = "";

    public Messages(UbuTag plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
        this.prefix = ChatColor.translateAlternateColorCodes('&', cfg.getString("prefix", ""));
    }

    public String prefix() { return prefix; }

    public String raw(String key) {
        String v = cfg.getString(key);
        return v == null ? key : v;
    }

    public List<String> rawList(String key) {
        List<String> v = cfg.getStringList(key);
        return v == null ? new ArrayList<>() : v;
    }

    public String get(String key, Object... args) {
        return color(applyPlaceholders(raw(key), args));
    }

    public List<String> getList(String key, Object... args) {
        List<String> out = new ArrayList<>();
        for (String s : rawList(key)) {
            out.add(color(applyPlaceholders(s, args)));
        }
        return out;
    }

    public void send(CommandSender to, String key, Object... args) {
        if (to == null) return;
        to.sendMessage(get(key, args));
    }

    public void sendList(CommandSender to, String key, Object... args) {
        if (to == null) return;
        for (String line : getList(key, args)) {
            to.sendMessage(line);
        }
    }

    /**
     * Remplace les placeholders {key} dans un message. Les arguments sont
     * passés sous forme de paires (clé, valeur). Le placeholder {prefix} est
     * automatiquement injecté.
     */
    public String applyPlaceholders(String input, Object... args) {
        if (input == null) return "";
        String s = input.replace("{prefix}", prefix);
        if (args == null || args.length == 0) return s;
        // args = [k1, v1, k2, v2, ...]
        for (int i = 0; i + 1 < args.length; i += 2) {
            String k = String.valueOf(args[i]);
            String v = String.valueOf(args[i + 1]);
            s = s.replace("{" + k + "}", v);
        }
        return s;
    }

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String strip(String s) {
        return ChatColor.stripColor(color(s));
    }

    /** Helper pour construire les paires de placeholders. */
    public static Object[] vars(Object... pairs) {
        return pairs;
    }

    /** Convertit une Map<String,String> en tableau (k,v,k,v,...). */
    public static Object[] vars(Map<String, ?> map) {
        Object[] out = new Object[map.size() * 2];
        int i = 0;
        for (Map.Entry<String, ?> e : map.entrySet()) {
            out[i++] = e.getKey();
            out[i++] = e.getValue();
        }
        return out;
    }
}
