package fr.sannoxx.ubuwool;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathRecap {

    private static final Map<UUID, RecapData> lastRecap = new HashMap<>();

    public static class RecapData {
        public final String killerName;
        public final String killerTeamColor;
        public final String agentName;
        public final String agentColor;
        public final String weaponName;
        public final double killerHealth;
        public final long timestamp;

        public RecapData(String killerName, String killerTeamColor,
                         String agentName, String agentColor,
                         String weaponName, double killerHealth) {
            this.killerName      = killerName;
            this.killerTeamColor = killerTeamColor;
            this.agentName       = agentName;
            this.agentColor      = agentColor;
            this.weaponName      = weaponName;
            this.killerHealth    = killerHealth;
            this.timestamp       = System.currentTimeMillis();
        }
    }

    public static void record(Player victim, Player killer,
                              String killerAgentName, String killerAgentColor,
                              boolean killerIsRed) {
        String teamColor = killerIsRed ? "§c" : "§9";
        String weapon = "§f" + Lang.getRaw(Lang.Key.RECAP_DEFAULT_WEAPON);
        if (killer != null) {
            org.bukkit.inventory.ItemStack held = killer.getInventory().getItemInMainHand();
            if (held.hasItemMeta() && held.getItemMeta().hasDisplayName()) {
                weapon = held.getItemMeta().getDisplayName();
            } else if (!held.getType().isAir()) {
                weapon = "§f" + formatMaterial(held.getType().name());
            }
        }

        lastRecap.put(victim.getUniqueId(), new RecapData(
                killer != null ? killer.getName() : Lang.getRaw(Lang.Key.RECAP_ENV_KILLER),
                teamColor,
                killerAgentName != null ? killerAgentName : "?",
                killerAgentColor != null ? killerAgentColor : "§7",
                weapon,
                killer != null ? killer.getHealth() : 0
        ));
    }

    public static RecapData getSnapshot(UUID victimUUID) {
        return lastRecap.get(victimUUID);
    }

    public static void sendFromSnapshot(Player victim, RecapData snapshot) {
        if (snapshot == null) {
            sendNoInfo(victim);
            return;
        }
        sendRecap(victim, snapshot);
    }

    public static void send(Player victim) {
        RecapData data = lastRecap.get(victim.getUniqueId());
        if (data == null) {
            sendNoInfo(victim);
            return;
        }
        sendRecap(victim, data);
    }


    private static void sendNoInfo(Player victim) {
        victim.sendMessage("§7" + Lang.get(victim, Lang.Key.RECAP_NO_INFO));
    }

    private static void sendRecap(Player victim, RecapData data) {
        String envKillerKey = Lang.getRaw(Lang.Key.RECAP_ENV_KILLER);

        if (data.killerName.equals(envKillerKey)) {
            victim.sendMessage("§7" + Lang.get(victim, Lang.Key.RECAP_ENV_CAUSE));
        } else {
            victim.sendMessage("§7" + Lang.get(victim, Lang.Key.RECAP_KILLED_BY)
                    + " " + data.killerTeamColor + "§l" + data.killerName);
            victim.sendMessage("§7" + Lang.get(victim, Lang.Key.RECAP_AGENT)
                    + " " + data.agentColor + "§l" + data.agentName);
            victim.sendMessage("§7" + Lang.get(victim, Lang.Key.RECAP_WEAPON)
                    + " " + data.weaponName);
            int killerHearts = (int) Math.ceil(data.killerHealth / 2.0);
            String hearts = "§c" + "❤".repeat(Math.min(killerHearts, 10));
            victim.sendMessage("§7" + Lang.get(victim, Lang.Key.RECAP_KILLER_HP)
                    + " " + hearts
                    + " §7(" + String.format("%.1f", data.killerHealth / 2.0) + "♥)");
        }
    }

    public static void clear(UUID uuid) {
        lastRecap.remove(uuid);
    }

    public static void clearAll() {
        lastRecap.clear();
    }

    private static String formatMaterial(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}