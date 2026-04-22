package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TabManager {

    private static Component text(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }

    public static void updateTab(GameManager gm) {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        cleanTeams(sb, gm.getInstanceId());

        for (Player p : gm.getAllPlayers()) {
            applyNametag(sb, gm, p);
            applyTabName(gm, p);
        }

        for (Player p : gm.getAllPlayers()) {
            p.sendPlayerListHeaderAndFooter(
                    text("§6§lUbuWool"),
                    Component.empty()
            );
        }
    }

    public static void updatePlayerTab(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();

        String teamId = buildTeamId(gm, player);
        Team existing = sb.getTeam(teamId);
        if (existing != null) existing.unregister();

        applyNametag(sb, gm, player);
        applyTabName(gm, player);
    }

    public static void resetTab(GameManager gm) {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Player p : gm.getAllPlayers()) {
            String teamId = buildTeamId(gm, p);
            Team team = sb.getTeam(teamId);
            if (team != null) {
                team.removeEntry(p.getName());
            }
        }

        cleanTeams(sb, gm.getInstanceId());

        for (Player p : gm.getAllPlayers()) {
            p.playerListName(null);
            p.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        }
    }

    public static void resetAllTabs() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        sb.getTeams().stream()
                .filter(t -> t.getName().startsWith("uw_"))
                .toList()
                .forEach(Team::unregister);
    }

    private static void applyNametag(Scoreboard sb, GameManager gm, Player p) {
        PlayerData data = gm.playerDataMap.get(p.getUniqueId());
        boolean isRed = gm.isRedTeam(p);

        String teamColor  = isRed ? "§c" : "§9";
        String agentColor = (data != null && data.agent != null) ? data.agent.getColor() : "§7";
        String agentName  = (data != null && data.agent != null) ? data.agent.getName() : "?";

        String teamId = buildTeamId(gm, p);
        Team team = sb.getTeam(teamId);
        if (team == null) team = sb.registerNewTeam(teamId);

        team.setColor(isRed ? ChatColor.RED : ChatColor.BLUE);
        team.setPrefix(teamColor);
        team.setSuffix(" " + agentColor + agentName);

        if (!team.hasEntry(p.getName())) {
            team.addEntry(p.getName());
        }
    }

    private static void applyTabName(GameManager gm, Player p) {
        PlayerData data = gm.playerDataMap.get(p.getUniqueId());
        boolean isRed = gm.isRedTeam(p);

        String teamColor  = isRed ? "§c" : "§9";
        String agentColor = (data != null && data.agent != null) ? data.agent.getColor() : "§7";
        String agentName  = (data != null && data.agent != null) ? data.agent.getName() : "?";

        String ultBar;
        if (data != null && data.agent != null) {
            int required = data.agent.getUltimateKillsRequired();
            int current  = Math.min(data.ultimateKills, required);
            if (data.isUltimateReady()) {
                ultBar = " §e§l★";
            } else {
                ultBar = " §e" + "★".repeat(current) + "§7" + "☆".repeat(required - current);
            }
        } else {
            ultBar = "";
        }

        int kills  = (data != null) ? data.kills  : 0;
        int deaths = (data != null) ? data.deaths : 0;
        String kdStr = " §e" + kills + "§7/§c" + deaths;

        String tabName = teamColor + p.getName() + " " + agentColor + agentName + ultBar + kdStr;
        p.playerListName(text(tabName));
    }

    private static void cleanTeams(Scoreboard sb, int instanceId) {
        String prefix = "uw_" + instanceId + "_";
        sb.getTeams().stream()
                .filter(t -> t.getName().startsWith(prefix))
                .toList()
                .forEach(Team::unregister);
    }

    private static String buildTeamId(GameManager gm, Player p) {
        String namePrefix = p.getName().substring(0, Math.min(p.getName().length(), 10));
        return "uw_" + gm.getInstanceId() + "_" + namePrefix;
    }
}