package fr.sannoxx.ubutag.placeholder;

import fr.sannoxx.ubutag.UbuTag;
import fr.sannoxx.ubutag.arena.Arena;
import fr.sannoxx.ubutag.stats.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Hook PlaceholderAPI (softdepend). Ne s'enregistre que si PlaceholderAPI
 * est présent au démarrage. Placeholders supportés (préfixe ubutag) :
 *
 *  ── État courant du joueur ──
 *  %ubutag_arena%        : nom de l'arène du joueur, ou "—"
 *  %ubutag_state%        : état de l'arène (WAITING, STARTING, IN_GAME, ENDING, ...)
 *  %ubutag_round%        : numéro du round courant, ou 0
 *  %ubutag_alive%        : nombre de joueurs encore en vie dans l'arène
 *  %ubutag_tagged%       : nombre de porteurs de TNT dans l'arène
 *  %ubutag_round_left%   : secondes restantes au round courant
 *  %ubutag_is_tagged%    : "yes" / "no"
 *
 *  ── Stats du joueur ──
 *  %ubutag_wins%, %ubutag_kills%, %ubutag_games%, %ubutag_tags%,
 *  %ubutag_streak_best%, %ubutag_streak_current%, %ubutag_survival%
 *
 *  ── Leaderboard global ──
 *  %ubutag_top_name_<n>%  : pseudo du n-ième joueur (n=1..)
 *  %ubutag_top_wins_<n>%  : wins du n-ième joueur
 */
public class UbuTagPlaceholders extends PlaceholderExpansion {

    private final UbuTag plugin;

    public UbuTagPlaceholders(UbuTag plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "ubutag"; }
    @Override public @NotNull String getAuthor()      { return "Sannoxx"; }
    @Override public @NotNull String getVersion()     { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()                { return true; }

    @Override
    public String onRequest(OfflinePlayer p, @NotNull String params) {
        return onPlaceholderRequest(p instanceof Player pl ? pl : null, params);
    }

    @Override
    public String onPlaceholderRequest(Player p, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);

        if (key.startsWith("top_")) {
            return handleTop(key);
        }

        if (p == null) return "";
        Arena arena = plugin.arenas().findArenaOf(p);
        PlayerStats stats = plugin.stats().get(p.getUniqueId());

        switch (key) {
            case "arena":      return arena == null ? "—" : arena.name;
            case "state":      return arena == null ? "NONE" : arena.getState().name();
            case "round":      return arena == null ? "0" : String.valueOf(arena.getRound());
            case "round_left": return arena == null ? "0" : String.valueOf(arena.getRoundSecondsLeft());
            case "alive":      return arena == null ? "0" : String.valueOf(arena.getAlive().size());
            case "tagged":     return arena == null ? "0" : String.valueOf(arena.getTagged().size());
            case "is_tagged":  return arena != null && arena.isTagged(p.getUniqueId()) ? "yes" : "no";

            case "wins":            return String.valueOf(stats == null ? 0 : stats.wins);
            case "kills":           return String.valueOf(stats == null ? 0 : stats.kills);
            case "games":           return String.valueOf(stats == null ? 0 : stats.gamesPlayed);
            case "tags":            return String.valueOf(stats == null ? 0 : stats.tagsPassed);
            case "streak_best":     return String.valueOf(stats == null ? 0 : stats.bestStreak);
            case "streak_current":  return String.valueOf(stats == null ? 0 : stats.currentStreak);
            case "survival":        return String.valueOf(stats == null ? 0 : stats.survivalSeconds);
            default: return null;
        }
    }

    private String handleTop(String key) {
        // top_name_<n> ou top_wins_<n>
        String[] parts = key.split("_");
        if (parts.length < 3) return null;
        String field = parts[1];
        int rank;
        try { rank = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { return null; }
        if (rank < 1) return null;
        List<PlayerStats> top = plugin.stats().top(Math.max(rank, plugin.getConfig().getInt("stats.top-size", 10)));
        if (rank > top.size()) return "—";
        PlayerStats ps = top.get(rank - 1);
        switch (field) {
            case "name": return ps.name == null || ps.name.isEmpty() ? "—" : ps.name;
            case "wins": return String.valueOf(ps.wins);
            default: return null;
        }
    }

    public void safeUnregister() {
        try { super.unregister(); } catch (Throwable ignored) {}
    }
}
