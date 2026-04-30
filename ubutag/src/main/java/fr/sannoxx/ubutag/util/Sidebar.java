package fr.sannoxx.ubutag.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * Sidebar par joueur : un Scoreboard avec une Objective en SIDEBAR et des
 * teams "u0", "u1"... pour permettre des contenus dynamiques avec lignes
 * dupliquées (chaque entry est invisible et unique).
 */
public final class Sidebar {

    /** Nombre maximum de lignes supportées (limite des "couleurs" disponibles). */
    public static final int MAX_LINES = 16;

    private final Player player;
    private final Scoreboard board;
    private final Objective sidebar;
    private final List<String> currentEntries = new ArrayList<>();

    public Sidebar(Player player, String title) {
        this.player = player;
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        this.sidebar = board.registerNewObjective("ubutag", "dummy", color(title));
        this.sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
    }

    public void setTitle(String title) {
        this.sidebar.setDisplayName(color(title));
    }

    /**
     * Met à jour les lignes de la sidebar (du haut vers le bas).
     * Les codes couleur '&' sont supportés.
     */
    public void setLines(List<String> lines) {
        // Tronque si > MAX_LINES
        if (lines.size() > MAX_LINES) {
            lines = lines.subList(0, MAX_LINES);
        }
        int n = lines.size();

        // Reset des entries devenues inutiles
        for (String e : new ArrayList<>(currentEntries)) {
            int idx = currentEntries.indexOf(e);
            if (idx >= n) {
                board.resetScores(e);
                Team t = board.getTeam("u" + idx);
                if (t != null) t.unregister();
            }
        }
        if (n < currentEntries.size()) {
            currentEntries.subList(n, currentEntries.size()).clear();
        }

        for (int i = 0; i < n; i++) {
            String entry = entryFor(i);
            String content = color(lines.get(i));

            // Ensure team
            Team team = board.getTeam("u" + i);
            if (team == null) {
                team = board.registerNewTeam("u" + i);
                team.addEntry(entry);
            }

            // Découpe prefix/suffix (max 64 chars chacun en 1.21)
            String prefix;
            String suffix;
            if (content.length() <= 64) {
                prefix = content;
                suffix = "";
            } else {
                prefix = content.substring(0, 64);
                String last = ChatColor.getLastColors(prefix);
                suffix = (last + content.substring(64));
                if (suffix.length() > 64) suffix = suffix.substring(0, 64);
            }
            team.setPrefix(prefix);
            team.setSuffix(suffix);

            sidebar.getScore(entry).setScore(n - i);

            if (i >= currentEntries.size()) currentEntries.add(entry);
            else currentEntries.set(i, entry);
        }
    }

    public void destroy() {
        try {
            for (Team t : new ArrayList<>(board.getTeams())) t.unregister();
            sidebar.unregister();
        } catch (IllegalStateException ignored) {}
        // Restaurer le scoreboard principal du serveur
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        if (player.isOnline()) player.setScoreboard(main);
    }

    /**
     * Génère une entry unique et invisible (utilise les codes couleur en
     * combinaison) pour un index de ligne donné.
     */
    private static String entryFor(int i) {
        ChatColor[] all = ChatColor.values();
        if (i < 0) i = 0;
        ChatColor a = all[i % all.length];
        ChatColor b = all[(i / all.length + i) % all.length];
        return a.toString() + b.toString();
    }

    private static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
