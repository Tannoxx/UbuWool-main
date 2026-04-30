package fr.sannoxx.ubutag.command;

import fr.sannoxx.ubutag.Messages;
import fr.sannoxx.ubutag.UbuTag;
import fr.sannoxx.ubutag.arena.Arena;
import fr.sannoxx.ubutag.arena.ArenaState;
import fr.sannoxx.ubutag.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Commande /ubutag — joueurs et admin. */
public class UbuTagCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PLAYER_SUBS = Arrays.asList(
            "join", "leave", "stats", "top", "list", "help");

    private static final List<String> ADMIN_SUBS = Arrays.asList(
            "create", "delete", "setlobby", "setmainlobby", "addspawn",
            "clearspawns", "setmin", "setmax", "setduration", "enable",
            "disable", "forcestart", "forcestop", "reload");

    private final UbuTag plugin;

    public UbuTagCommand(UbuTag plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        Messages M = plugin.messages();
        if (args.length == 0) {
            return showHelp(sender);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help":
                return showHelp(sender);
            case "join":
                return cmdJoin(sender, args);
            case "leave":
                return cmdLeave(sender);
            case "stats":
                return cmdStats(sender, args);
            case "top":
                return cmdTop(sender);
            case "list":
                return cmdList(sender);
            case "admin":
                return cmdAdmin(sender, args);
            default:
                M.send(sender, "unknown-command");
                return true;
        }
    }

    private boolean showHelp(CommandSender sender) {
        Messages M = plugin.messages();
        sender.sendMessage(M.get("help-header"));
        for (String l : M.getList("help-player")) sender.sendMessage(l);
        if (sender.hasPermission("ubutag.admin")) {
            for (String l : M.getList("help-admin")) sender.sendMessage(l);
        }
        return true;
    }

    private boolean cmdJoin(CommandSender sender, String[] args) {
        Messages M = plugin.messages();
        if (!(sender instanceof Player p)) { M.send(sender, "player-only"); return true; }
        Arena current = plugin.arenas().findArenaOf(p);
        if (current != null) { M.send(p, "already-in-game"); return true; }

        Arena target;
        if (args.length >= 2) {
            target = plugin.arenas().get(args[1]);
            if (target == null) { M.send(p, "arena-not-found", "arena", args[1]); return true; }
            if (!target.enabled) { M.send(p, "arena-disabled"); return true; }
            if (!target.isReady()) { M.send(p, "arena-not-ready"); return true; }
            if (target.isFull()) { M.send(p, "arena-full"); return true; }
            ArenaState s = target.getState();
            if (s == ArenaState.IN_GAME || s == ArenaState.ENDING) { M.send(p, "game-already-running"); return true; }
        } else {
            target = plugin.arenas().findJoinable();
            if (target == null) { M.send(p, "arena-not-found", "arena", "(auto)"); return true; }
        }
        target.addPlayer(p);
        return true;
    }

    private boolean cmdLeave(CommandSender sender) {
        Messages M = plugin.messages();
        if (!(sender instanceof Player p)) { M.send(sender, "player-only"); return true; }
        Arena a = plugin.arenas().findArenaOf(p);
        if (a == null) { M.send(p, "not-in-game"); return true; }
        a.removePlayer(p);
        return true;
    }

    private boolean cmdStats(CommandSender sender, String[] args) {
        Messages M = plugin.messages();
        String targetName = args.length >= 2 ? args[1] : (sender instanceof Player ? sender.getName() : null);
        if (targetName == null) { M.send(sender, "player-only"); return true; }

        PlayerStats ps = plugin.stats().lookup(targetName);
        if (ps == null) {
            // tente via OfflinePlayer
            OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
            if (op.getUniqueId() != null) {
                ps = plugin.stats().get(op.getUniqueId());
            }
        }
        if (ps == null) { M.send(sender, "stats-no-data"); return true; }

        sender.sendMessage(M.get("stats-header", "player", ps.name == null ? targetName : ps.name));
        sender.sendMessage(M.get("stats-line-wins", "value", ps.wins));
        sender.sendMessage(M.get("stats-line-games", "value", ps.gamesPlayed));
        sender.sendMessage(M.get("stats-line-kills", "value", ps.kills));
        sender.sendMessage(M.get("stats-line-tags", "value", ps.tagsPassed));
        sender.sendMessage(M.get("stats-line-streak", "value", ps.bestStreak));
        sender.sendMessage(M.get("stats-line-survival", "value", ps.survivalSeconds));
        return true;
    }

    private boolean cmdTop(CommandSender sender) {
        Messages M = plugin.messages();
        int size = plugin.getConfig().getInt("stats.top-size", 10);
        List<PlayerStats> top = plugin.stats().top(size);
        if (top.isEmpty()) { M.send(sender, "top-empty"); return true; }
        sender.sendMessage(M.get("top-header", "value", size));
        int rank = 1;
        for (PlayerStats ps : top) {
            sender.sendMessage(M.get("top-line",
                    "value", rank++,
                    "player", ps.name == null || ps.name.isEmpty() ? ps.uuid.toString().substring(0, 8) : ps.name,
                    "count", ps.wins));
        }
        return true;
    }

    private boolean cmdList(CommandSender sender) {
        Messages M = plugin.messages();
        sender.sendMessage(M.get("arena-list-header"));
        List<Arena> all = plugin.arenas().getAll();
        if (all.isEmpty()) {
            sender.sendMessage(Messages.color("&7Aucune arène. &eUtilise &c/ubutag admin create <nom>"));
            return true;
        }
        for (Arena a : all) {
            String state;
            if (!a.enabled) state = "DISABLED";
            else if (!a.isReady()) state = "INCOMPLETE";
            else state = a.getState().name();
            sender.sendMessage(M.get("arena-list-line",
                    "arena", a.name,
                    "count", a.getPlayers().size(),
                    "max", a.maxPlayers,
                    "state", state));
        }
        return true;
    }

    /* ───────────────────────── ADMIN ───────────────────────── */

    private boolean cmdAdmin(CommandSender sender, String[] args) {
        Messages M = plugin.messages();
        if (!sender.hasPermission("ubutag.admin")) { M.send(sender, "no-permission"); return true; }
        if (args.length < 2) { showHelp(sender); return true; }
        String s = args[1].toLowerCase(Locale.ROOT);
        switch (s) {
            case "create":          return adminCreate(sender, args);
            case "delete":          return adminDelete(sender, args);
            case "setlobby":        return adminSetLobby(sender, args);
            case "setmainlobby":    return adminSetMainLobby(sender);
            case "addspawn":        return adminAddSpawn(sender, args);
            case "clearspawns":     return adminClearSpawns(sender, args);
            case "setmin":          return adminSetIntField(sender, args, "min");
            case "setmax":          return adminSetIntField(sender, args, "max");
            case "setduration":     return adminSetIntField(sender, args, "duration");
            case "enable":          return adminToggle(sender, args, true);
            case "disable":         return adminToggle(sender, args, false);
            case "forcestart":      return adminForce(sender, args, true);
            case "forcestop":       return adminForce(sender, args, false);
            case "reload":          return adminReload(sender);
            default:                showHelp(sender); return true;
        }
    }

    private boolean adminCreate(CommandSender sender, String[] args) {
        Messages M = plugin.messages();
        if (args.length < 3) { sender.sendMessage(Messages.color("&c/ubutag admin create <arena>")); return true; }
        String name = args[2];
        if (plugin.arenas().get(name) != null) { M.send(sender, "arena-already-exists", "arena", name); return true; }
        plugin.arenas().create(name);
        M.send(sender, "admin-created", "arena", name);
        return true;
    }

    private boolean adminDelete(CommandSender sender, String[] args) {
        Messages M = plugin.messages();
        if (args.length < 3) { sender.sendMessage(Messages.color("&c/ubutag admin delete <arena>")); return true; }
        String name = args[2];
        if (!plugin.arenas().delete(name)) { M.send(sender, "arena-not-found", "arena", name); return true; }
        M.send(sender, "admin-deleted", "arena", name);
        return true;
    }

    private boolean adminSetLobby(CommandSender sender, String[] args) {
        Messages M = plugin.messages();
        if (!(sender instanceof Player p)) { M.send(sender, "player-only"); return true; }
        if (args.length < 3) { sender.sendMessage(Messages.color("&c/ubutag admin setlobby <arena>")); return true; }
        Arena a = plugin.arenas().get(args[2]);
        if (a == null) { M.send(p, "arena-not-found", "arena", args[2]); return true; }
        a.setLobby(p.getLocation());
        plugin.arenas().saveAll();
        M.send(p, "admin-set-lobby", "arena", a.name);
        return true;
    }

    private boolean adminSetMainLobby(CommandSender sender) {
        Messages M = plugin.messages();
        if (!(sender instanceof Player p)) { M.send(sender, "player-only"); return true; }
        plugin.arenas().setMainLobby(p.getLocation());
        M.send(p, "admin-set-mainlobby");
        return true;
    }

    private boolean adminAddSpawn(CommandSender sender, String[] args) {
        Messages M = plugin.messages();
        if (!(sender instanceof Player p)) { M.send(sender, "player-only"); return true; }
        if (args.length < 3) { sender.sendMessage(Messages.color("&c/ubutag admin addspawn <arena>")); return true; }
        Arena a = plugin.arenas().get(args[2]);
        if (a == null) { M.send(p, "arena-not-found", "arena", args[2]); return true; }
        a.addSpawn(p.getLocation());
        plugin.arenas().saveAll();
        M.send(p, "admin-add-spawn", "arena", a.name, "count", a.getSpawns().size());
        return true;
    }

    private boolean adminClearSpawns(CommandSender sender, String[] args) {
        Messages M = plugin.messages();
        if (args.length < 3) { sender.sendMessage(Messages.color("&c/ubutag admin clearspawns <arena>")); return true; }
        Arena a = plugin.arenas().get(args[2]);
        if (a == null) { M.send(sender, "arena-not-found", "arena", args[2]); return true; }
        a.clearSpawns();
        plugin.arenas().saveAll();
        M.send(sender, "admin-clear-spawns", "arena", a.name);
        return true;
    }

    private boolean adminSetIntField(CommandSender sender, String[] args, String field) {
        Messages M = plugin.messages();
        if (args.length < 4) { sender.sendMessage(Messages.color("&c/ubutag admin set" + field + " <arena> <value>")); return true; }
        Arena a = plugin.arenas().get(args[2]);
        if (a == null) { M.send(sender, "arena-not-found", "arena", args[2]); return true; }
        int value;
        try { value = Integer.parseInt(args[3]); }
        catch (NumberFormatException e) { sender.sendMessage(Messages.color("&cValeur invalide.")); return true; }
        switch (field) {
            case "min":      a.minPlayers = Math.max(2, value); M.send(sender, "admin-set-min", "arena", a.name, "value", a.minPlayers); break;
            case "max":      a.maxPlayers = Math.max(a.minPlayers, value); M.send(sender, "admin-set-max", "arena", a.name, "value", a.maxPlayers); break;
            case "duration": a.roundDurationSeconds = Math.max(5, value); M.send(sender, "admin-set-duration", "arena", a.name, "value", a.roundDurationSeconds); break;
        }
        plugin.arenas().saveAll();
        return true;
    }

    private boolean adminToggle(CommandSender sender, String[] args, boolean enable) {
        Messages M = plugin.messages();
        if (args.length < 3) { sender.sendMessage(Messages.color("&c/ubutag admin " + (enable ? "enable" : "disable") + " <arena>")); return true; }
        Arena a = plugin.arenas().get(args[2]);
        if (a == null) { M.send(sender, "arena-not-found", "arena", args[2]); return true; }
        a.enabled = enable;
        if (!enable) a.shutdown();
        plugin.arenas().saveAll();
        M.send(sender, enable ? "admin-enabled" : "admin-disabled", "arena", a.name);
        return true;
    }

    private boolean adminForce(CommandSender sender, String[] args, boolean start) {
        Messages M = plugin.messages();
        if (args.length < 3) { sender.sendMessage(Messages.color("&c/ubutag admin " + (start ? "forcestart" : "forcestop") + " <arena>")); return true; }
        Arena a = plugin.arenas().get(args[2]);
        if (a == null) { M.send(sender, "arena-not-found", "arena", args[2]); return true; }
        if (start) {
            if (a.getPlayers().isEmpty()) { sender.sendMessage(Messages.color("&cAucun joueur dans cette arène.")); return true; }
            a.startLobbyCountdown(3);
            M.send(sender, "admin-force-start");
        } else {
            a.shutdown();
            M.send(sender, "admin-force-stop");
        }
        return true;
    }

    private boolean adminReload(CommandSender sender) {
        Messages M = plugin.messages();
        plugin.reloadConfig();
        plugin.messages().reload();
        plugin.arenas().loadAll();
        plugin.stats().loadAll();
        M.send(sender, "admin-reload");
        return true;
    }

    /* ───────────────────────── TAB COMPLETION ───────────────────────── */

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>(PLAYER_SUBS);
            if (sender.hasPermission("ubutag.admin")) out.add("admin");
            return filter(out, args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "join":
                case "list":
                    return filter(arenaNames(), args[1]);
                case "stats":
                    List<String> names = new ArrayList<>();
                    for (Player pl : Bukkit.getOnlinePlayers()) names.add(pl.getName());
                    return filter(names, args[1]);
                case "admin":
                    if (sender.hasPermission("ubutag.admin")) return filter(ADMIN_SUBS, args[1]);
                    break;
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            String s = args[1].toLowerCase(Locale.ROOT);
            if (s.equals("create")) return new ArrayList<>();
            if (Arrays.asList("delete", "setlobby", "addspawn", "clearspawns",
                    "setmin", "setmax", "setduration", "enable", "disable",
                    "forcestart", "forcestop").contains(s)) {
                return filter(arenaNames(), args[2]);
            }
        }
        return new ArrayList<>();
    }

    private List<String> arenaNames() {
        List<String> out = new ArrayList<>();
        for (Arena a : plugin.arenas().getAll()) out.add(a.name);
        return out;
    }

    private List<String> filter(List<String> source, String token) {
        List<String> out = new ArrayList<>();
        String t = token == null ? "" : token.toLowerCase(Locale.ROOT);
        for (String s : source) if (s.toLowerCase(Locale.ROOT).startsWith(t)) out.add(s);
        return out;
    }

    /* Pour éviter un avertissement "unused" sur UUID. */
    @SuppressWarnings("unused")
    private static final Class<?> UUID_REF = UUID.class;
}
