package fr.sannoxx.ubuwool.command;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.PlayerData;
import fr.sannoxx.ubuwool.PlayerStats;
import fr.sannoxx.ubuwool.manager.*;
import fr.sannoxx.ubuwool.menu.*;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class UwCommand implements CommandExecutor, TabCompleter {

    public UwCommand(org.bukkit.plugin.java.JavaPlugin plugin) {}

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command,
                             @NonNull String label, String[] args) {

        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            case "join", "queue" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cJoueurs uniquement."); return true; }

                if (args.length >= 2) {
                    handleJoinDirect(p, args[1]);
                } else {
                    handleJoinQueue(p);
                }
            }

            case "leave" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cJoueurs uniquement."); return true; }
                handleLeave(p);
            }

            case "buy" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cJoueurs uniquement."); return true; }
                GameManager gm = GameRegistry.getInstanceOf(p);
                if (gm == null) { p.sendMessage("§cTu n'es pas dans une partie !"); return true; }
                if (gm.state != GameManager.GameState.BUY_PHASE) {
                    p.sendMessage(Lang.get(p, Lang.Key.SHOP_ONLY_BUY_PHASE)); return true;
                }
                BuyMenu.open(p, gm);
            }

            case "vote" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cJoueurs uniquement."); return true; }
                MapVoteMenu.open(p);
            }

            case "profile" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cJoueurs uniquement."); return true; }
                ProfileMenu.open(p);
            }

            case "agent" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cJoueurs uniquement."); return true; }
                if (args.length < 2) { p.sendMessage("§cUsage : /uw agent <nom>"); return true; }
                GameManager gm = GameRegistry.getInstanceOf(p);
                if (gm == null) { p.sendMessage(Lang.get(p, Lang.Key.NOT_IN_GAME)); return true; }
                gm.setAgent(p, args[1]);
            }

            case "agentlist" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cJoueurs uniquement."); return true; }
                GameManager gm = GameRegistry.getInstanceOf(p);
                if (gm == null) { p.sendMessage(Lang.get(p, Lang.Key.NOT_IN_GAME)); return true; }
                p.sendMessage("§6§l--- Agents (Instance #" + gm.getInstanceId() + ") ---");
                for (Map.Entry<java.util.UUID, PlayerData> entry : gm.playerDataMap.entrySet()) {
                    String agentName = entry.getValue().agent != null
                            ? entry.getValue().agent.getColor() + entry.getValue().agent.getName() : "§7Aucun";
                    String team = gm.teamRed.contains(entry.getKey()) ? "§cRouge" : "§9Bleu";
                    Player op = Bukkit.getPlayer(entry.getKey());
                    String pName = op != null ? op.getName() : entry.getKey().toString();
                    p.sendMessage(team + " §f" + pName + " §7→ " + agentName);
                }
            }

            case "stats" -> {
                if (!(sender instanceof Player viewer)) { sender.sendMessage("§cJoueurs uniquement."); return true; }
                Player target = viewer;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        viewer.sendMessage(Lang.get(viewer, Lang.Key.ADMIN_PLAYER_NOT_FOUND, args[1]));
                        return true;
                    }
                }
                ProfileMenu.openStatsPage(viewer, target);
            }

            case "top", "leaderboard" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cJoueurs uniquement."); return true; }
                LeaderboardMenu.open(p);
            }

            case "start" -> {
                if (!sender.hasPermission("ubuwool.admin")) {
                    sender.sendMessage("§cPermission refusée. §7(ubuwool.admin requis)");
                    return true;
                }
                if (args.length >= 2) {
                    handleAdminStart(sender, args[1]);
                } else {
                    handleAdminStartFromQueue(sender);
                }
            }

            case "stop" -> {
                if (!sender.hasPermission("ubuwool.admin")) {
                    sender.sendMessage("§cPermission refusée. §7(ubuwool.admin requis)");
                    return true;
                }
                if (args.length >= 2) {
                    handleAdminStop(sender, args[1]);
                } else if (sender instanceof Player p) {
                    GameManager gm = GameRegistry.getInstanceOf(p);
                    if (gm == null) {
                        sender.sendMessage("§cTu n'es pas dans une instance ! Utilise /uw stop <id>");
                        listInstances(sender);
                        return true;
                    }
                    gm.stopGame();
                    sender.sendMessage("§aInstance #" + gm.getInstanceId() + " arrêtée.");
                } else {
                    sender.sendMessage("§cUsage (console) : /uw stop <id>");
                    listInstances(sender);
                }
            }

            case "admin" -> {
                if (!sender.hasPermission("ubuwool.admin")) {
                    sender.sendMessage("§cPermission refusée. §7(ubuwool.admin requis)");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage : /uw admin <instances|getubultimate|reloadmaps|setmap|reloadabilities|forceend|stats|stopall>");
                    return true;
                }
                handleAdmin(sender, args);
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleJoinQueue(Player player) {
        if (GameRegistry.isInAnyGame(player)) {
            player.sendMessage("§cTu es déjà dans une partie ! (/uw leave pour quitter)");
            return;
        }
        if (MatchmakingQueue.isInQueue(player)) {
            player.sendMessage("§eTu es déjà en file d'attente (position " + MatchmakingQueue.getPosition(player) + ").");
            return;
        }
        MatchmakingQueue.enqueue(player);
    }

    private void handleJoinDirect(Player player, String idStr) {
        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            player.sendMessage("§cID d'instance invalide : §f" + idStr);
            return;
        }

        GameManager gm = GameRegistry.getInstance(id);
        if (gm == null) {
            player.sendMessage("§cInstance #" + id + " introuvable.");
            listInstances(player);
            return;
        }

        if (gm.isPlayerInGame(player)) {
            player.sendMessage("§cTu es déjà dans cette instance !");
            return;
        }

        if (GameRegistry.isInAnyGame(player)) {
            player.sendMessage("§cQuitte ta partie actuelle d'abord. (/uw leave)");
            return;
        }

        if (gm.state != GameManager.GameState.WAITING && gm.state != GameManager.GameState.AGENT_SELECT) {
            player.sendMessage("§cL'instance #" + id + " est en cours de jeu (phase : " + gm.state + ").");
            return;
        }

        gm.joinTeam(player);
        player.sendMessage("§a§l✔ §7Rejoint l'instance §6#" + id + "§7 !");
    }

    private void handleLeave(Player player) {
        if (MatchmakingQueue.isInQueue(player)) {
            MatchmakingQueue.dequeue(player);
            return;
        }
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) {
            player.sendMessage("§cTu n'es ni en file d'attente, ni dans une partie.");
            return;
        }
        gm.leaveTeam(player);
    }

    private void handleAdminStartFromQueue(CommandSender sender) {
        if (MatchmakingQueue.size() < MatchmakingQueue.MIN_PLAYERS) {
            sender.sendMessage("§cPas assez de joueurs en file d'attente ! §7("
                    + MatchmakingQueue.size() + "/" + MatchmakingQueue.MIN_PLAYERS + " min)");
            return;
        }

        GameManager gm = GameRegistry.createInstance();
        if (gm == null) {
            sender.sendMessage("§cNombre maximum d'instances atteint ("
                    + GameRegistry.MAX_INSTANCES + ").");
            return;
        }

        List<Player> players = MatchmakingQueue.drainOnlinePlayers();
        if (players.size() < MatchmakingQueue.MIN_PLAYERS) {
            GameRegistry.removeInstance(gm.getInstanceId());
            sender.sendMessage("§cPas assez de joueurs connectés ! §7("
                    + players.size() + "/" + MatchmakingQueue.MIN_PLAYERS + " min)");
            return;
        }

        sender.sendMessage("§a§l✔ §7Instance §6#" + gm.getInstanceId()
                + " §7créée avec §e" + players.size() + " §7joueur(s).");
        for (Player p : players) {
            gm.playerDataMap.computeIfAbsent(p.getUniqueId(),
                    k -> new fr.sannoxx.ubuwool.PlayerData(p));
            p.sendMessage("§6§l⚔ §7La partie commence ! Choisis ton équipe.");
            fr.sannoxx.ubuwool.menu.TeamMenu.open(p, gm);
        }
    }

    private void handleAdminStart(CommandSender sender, String idStr) {
        int id;
        try { id = Integer.parseInt(idStr); }
        catch (NumberFormatException e) { sender.sendMessage("§cID invalide : " + idStr); return; }
        GameManager gm = GameRegistry.getInstance(id);
        if (gm == null) { sender.sendMessage("§cInstance #" + id + " introuvable."); return; }
        gm.startGame();
        sender.sendMessage("§aInstance #" + id + " démarrée.");
    }

    private void handleAdminStop(CommandSender sender, String idStr) {
        int id;
        try { id = Integer.parseInt(idStr); }
        catch (NumberFormatException e) { sender.sendMessage("§cID invalide : " + idStr); return; }
        GameManager gm = GameRegistry.getInstance(id);
        if (gm == null) { sender.sendMessage("§cInstance #" + id + " introuvable."); return; }
        gm.stopGame();
        sender.sendMessage("§aInstance #" + id + " arrêtée.");
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        switch (args[1].toLowerCase()) {

            case "instances", "list" -> {
                sender.sendMessage("§6§l--- Instances UbuWool ---");
                for (String line : GameRegistry.getStatusLines()) sender.sendMessage(line);
                sender.sendMessage("§7File d'attente : §e" + MatchmakingQueue.size() + " §7joueur(s)");
            }

            case "createinstance" -> {
                GameManager gm = GameRegistry.createInstance();
                if (gm == null) {
                    sender.sendMessage("§cNombre maximum d'instances atteint (" + GameRegistry.MAX_INSTANCES + ").");
                } else {
                    sender.sendMessage("§aInstance #" + gm.getInstanceId() + " créée.");
                }
            }

            case "stopall" -> {
                int count = GameRegistry.count();
                GameRegistry.resetAll();
                MatchmakingQueue.reset();
                sender.sendMessage("§a" + count + " instance(s) arrêtée(s).");
            }

            case "forceend" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage : /uw admin forceend <id> [red|blue]"); return; }
                int id;
                try { id = Integer.parseInt(args[2]); }
                catch (NumberFormatException e) { sender.sendMessage("§cID invalide."); return; }
                GameManager gm = GameRegistry.getInstance(id);
                if (gm == null) { sender.sendMessage("§cInstance #" + id + " introuvable."); return; }
                boolean redWins = args.length < 4 || args[3].equalsIgnoreCase("red");
                gm.forceEndRound(redWins);
                sender.sendMessage("§aRound forcé (victoire " + (redWins ? "rouge" : "bleue") + ").");
            }

            case "getubultimate" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cJoueurs uniquement."); return; }
                GameManager gm = GameRegistry.getInstanceOf(p);
                if (gm == null) { p.sendMessage(Lang.get(p, Lang.Key.NOT_IN_GAME)); return; }
                PlayerData data = gm.playerDataMap.get(p.getUniqueId());
                if (data == null) { p.sendMessage(Lang.get(p, Lang.Key.NOT_IN_GAME)); return; }
                data.ultimateKills = data.agent != null ? data.agent.getUltimateKillsRequired() : 5;
                gm.updateUltimateItem(p, data);
                p.sendMessage(Lang.get(p, Lang.Key.ULTIMATE_ADMIN_UNLOCKED));
            }

            case "reloadmaps" -> {
                MapConfig.reload();
                if (sender instanceof Player p) sender.sendMessage(Lang.get(p, Lang.Key.MAP_RELOAD));
                else sender.sendMessage("§aMaps rechargées !");
            }

            case "reloadabilities" -> {
                AbilityConfig.reload();
                sender.sendMessage("§aabilities.yml rechargé !");
            }

            case "setmap" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage : /uw admin setmap <nom>"); return; }
                String nom = args[2];
                MapConfig.UbuMap map = MapConfig.getMap(nom);
                if (map == null) {
                    if (sender instanceof Player p) sender.sendMessage(Lang.get(p, Lang.Key.MAP_NOT_FOUND, nom));
                    else sender.sendMessage("§cMap introuvable : " + nom);
                    return;
                }
                MapConfig.setSelectedMap(nom);
                if (sender instanceof Player p) sender.sendMessage(Lang.get(p, Lang.Key.MAP_FORCED, nom));
                else sender.sendMessage("§aMap forcée : §f" + nom);
            }

            case "stats" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage : /uw admin stats <joueur>"); return; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage("§cJoueur non connecté."); return; }
                PlayerStats.Stats s = PlayerStats.get(target.getUniqueId());
                sender.sendMessage("§6§l--- Stats de " + args[2] + " ---");
                sender.sendMessage("§7Kills : §e" + s.totalKills + " §7| Morts : §c" + s.totalDeaths);
                sender.sendMessage("§7Victoires : §a" + s.totalWins + " §7| Défaites : §c" + s.totalLosses);
                sender.sendMessage("§7Parties : §f" + s.gamesPlayed + " §7| Win rate : §e" + String.format("%.1f%%", s.getWinRate()));
                sender.sendMessage("§7Agent favori : §f" + s.getFavoriteAgent());
            }

            case "tp" -> {
                if (args.length < 4) { sender.sendMessage("§cUsage : /uw admin tp <joueur> <instanceId>"); return; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage("§cJoueur introuvable."); return; }
                int id;
                try { id = Integer.parseInt(args[3]); }
                catch (NumberFormatException e) { sender.sendMessage("§cID invalide."); return; }
                GameManager gm = GameRegistry.getInstance(id);
                if (gm == null) { sender.sendMessage("§cInstance introuvable."); return; }
                GameManager old = GameRegistry.getInstanceOf(target);
                if (old != null) old.leaveTeam(target);
                gm.joinTeam(target);
                sender.sendMessage("§a" + target.getName() + " téléporté dans l'instance #" + id);
            }

            default -> sender.sendMessage("§cSous-commande admin inconnue : " + args[1]);
        }
    }

    private void listInstances(CommandSender sender) {
        sender.sendMessage("§6§l--- Instances disponibles ---");
        for (String line : GameRegistry.getStatusLines()) sender.sendMessage(line);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l--- UbuWool ---");
        sender.sendMessage("§e/uw join §7- Rejoindre la file d'attente (matchmaking)");
        sender.sendMessage("§e/uw join <id> §7- Rejoindre directement une instance");
        sender.sendMessage("§e/uw leave §7- Quitter la file / sa partie");
        sender.sendMessage("§e/uw buy §7- Ouvrir le shop (phase d'achat)");
        sender.sendMessage("§e/uw vote §7- Voter pour une map");
        sender.sendMessage("§e/uw profile §7- Profil (langue, stats...)");
        sender.sendMessage("§e/uw agentlist §7- Voir les agents choisis dans ta partie");
        sender.sendMessage("§e/uw stats [joueur] §7- Voir les statistiques");
        sender.sendMessage("§e/uw top §7- Classement global");
        if (sender.hasPermission("ubuwool.admin")) {
            sender.sendMessage("§c§l[ADMIN]");
            sender.sendMessage("§c/uw start [id] §7- Lancer une partie");
            sender.sendMessage("§c/uw stop [id] §7- Arrêter une partie");
            sender.sendMessage("§c/uw admin instances §7- Voir toutes les instances");
            sender.sendMessage("§c/uw admin createinstance §7- Créer une instance");
            sender.sendMessage("§c/uw admin stopall §7- Arrêter toutes les instances");
            sender.sendMessage("§c/uw admin forceend <id> [red|blue] §7- Forcer la fin d'un round");
            sender.sendMessage("§c/uw admin tp <joueur> <id> §7- Téléporter dans une instance");
            sender.sendMessage("§c/uw admin getubultimate §7- Débloquer l'ubultimate");
            sender.sendMessage("§c/uw admin reloadmaps §7- Recharger maps.yml");
            sender.sendMessage("§c/uw admin reloadabilities §7- Recharger abilities.yml");
            sender.sendMessage("§c/uw admin setmap <nom> §7- Forcer une map");
            sender.sendMessage("§c/uw admin stats <joueur> §7- Stats d'un joueur");
        }
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command,
                                      @NonNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList(
                    "join", "leave", "queue", "buy", "vote", "profile", "agent", "agentlist",
                    "stats", "top", "leaderboard"));
            if (sender.hasPermission("ubuwool.admin"))
                base.addAll(Arrays.asList("start", "stop", "admin"));
            return filterStart(base, args[0]);
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "agent" -> { return filterStart(AGENTS, args[1]); }
                case "stats" -> {
                    List<String> names = new ArrayList<>();
                    for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                    return filterStart(names, args[1]);
                }
                case "join" -> {
                    List<String> ids = new ArrayList<>();
                    for (GameManager gm : GameRegistry.getAllInstances())
                        ids.add(String.valueOf(gm.getInstanceId()));
                    return filterStart(ids, args[1]);
                }
                case "start", "stop" -> {
                    if (!sender.hasPermission("ubuwool.admin")) return Collections.emptyList();
                    List<String> ids = new ArrayList<>();
                    for (GameManager gm : GameRegistry.getAllInstances())
                        ids.add(String.valueOf(gm.getInstanceId()));
                    return filterStart(ids, args[1]);
                }
                case "admin" -> {
                    if (!sender.hasPermission("ubuwool.admin")) return Collections.emptyList();
                    return filterStart(Arrays.asList(
                            "instances", "list", "createinstance", "stopall",
                            "forceend", "getubultimate", "reloadmaps", "reloadabilities",
                            "setmap", "stats", "tp"), args[1]);
                }
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("admin")) {
                switch (args[1].toLowerCase()) {
                    case "setmap" -> {
                        List<String> mapNames = new ArrayList<>();
                        for (MapConfig.UbuMap m : MapConfig.getEnabledMaps()) mapNames.add(m.name);
                        return filterStart(mapNames, args[2]);
                    }
                    case "forceend", "stop" -> {
                        List<String> ids = new ArrayList<>();
                        for (GameManager gm : GameRegistry.getAllInstances())
                            ids.add(String.valueOf(gm.getInstanceId()));
                        return filterStart(ids, args[2]);
                    }
                    case "stats", "tp" -> {
                        List<String> names = new ArrayList<>();
                        for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                        return filterStart(names, args[2]);
                    }
                }
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("tp")) {
            List<String> ids = new ArrayList<>();
            for (GameManager gm : GameRegistry.getAllInstances())
                ids.add(String.valueOf(gm.getInstanceId()));
            return filterStart(ids, args[3]);
        }

        return Collections.emptyList();
    }

    private static final List<String> AGENTS = Arrays.asList(
            "sembol", "fantom", "gargamel", "horcus", "bambouvore", "lolita", "asky",
            "carlos", "larok", "ticksuspicious", "mascord", "hijab", "ilargia", "gekko");

    private List<String> filterStart(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(prefix.toLowerCase())) result.add(s);
        return result;
    }
}