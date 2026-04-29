package fr.sannoxx.ubuwool;

import fr.sannoxx.ubuwool.ability.*;
import fr.sannoxx.ubuwool.command.UwCommand;
import fr.sannoxx.ubuwool.listener.PlayerListener;
import fr.sannoxx.ubuwool.manager.*;
import fr.sannoxx.ubuwool.menu.*;
import fr.sannoxx.ubuwool.placeholder.UbuPlaceholders;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class UbuWool extends JavaPlugin {

    private static UbuWool instance;

    public static boolean processingHorcusDamage = false;

    private int agentSelectTask  = -1;
    private int ilargiaTask      = -1;
    private int voidCheckTask    = -1;
    private int razeRocketTask   = -1;
    private int teamMenuRefreshTask = -1;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        Lang.load(this);
        PlayerProfile.init(this);
        MapConfig.load(this);
        AbilityConfig.load(this);
        PlayerStats.init(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        UwCommand cmd = new UwCommand(this);
        Objects.requireNonNull(getCommand("uw")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("uw")).setTabCompleter(cmd);

        MatchmakingQueue.startChecker();

        agentSelectTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (GameManager gm : GameRegistry.getAllInstances()) {
                if (gm.state != GameManager.GameState.AGENT_SELECT) continue;
                for (org.bukkit.entity.Player p : gm.getAllPlayers()) {
                    PlayerData d = gm.playerDataMap.get(p.getUniqueId());
                    if (d == null || d.agent == null) {
                        if (p.getOpenInventory().getType() == org.bukkit.event.inventory.InventoryType.CRAFTING) {
                            AgentMenu.open(p);
                        }
                    }
                }
            }
        }, 20L, 20L).getTaskId();

        ilargiaTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (GameManager gm : GameRegistry.getAllInstances()) {
                if (gm.state != GameManager.GameState.ROUND_ACTIVE) continue;
                for (org.bukkit.entity.Player p : gm.getAllPlayers()) {
                    PlayerData d = gm.playerDataMap.get(p.getUniqueId());
                    if (d == null || d.agent == null) continue;
                    if (!d.agent.getName().equalsIgnoreCase("ilargia")) continue;

                    if (IlargiaAbilities.hasNoArmor(p)) {
                        org.bukkit.potion.PotionEffect invis = p.getPotionEffect(
                                org.bukkit.potion.PotionEffectType.INVISIBILITY);
                        if (invis == null || invis.getDuration() <= 10) {
                            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.INVISIBILITY, 60, 0, false, false));
                        }
                        org.bukkit.potion.PotionEffect weak = p.getPotionEffect(
                                org.bukkit.potion.PotionEffectType.WEAKNESS);
                        if (weak == null || weak.getDuration() <= 10) {
                            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.WEAKNESS, 60, 0, false, false));
                        }
                    } else {
                        p.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
                        p.removePotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS);
                    }
                }
            }
        }, 1L, 1L).getTaskId();

        voidCheckTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (GameManager gm : GameRegistry.getAllInstances()) {
                if (gm.state != GameManager.GameState.ROUND_ACTIVE
                        && gm.state != GameManager.GameState.BUY_PHASE) continue;
                MapConfig.UbuMap map = MapConfig.getSelectedMap();
                if (map == null) continue;
                for (org.bukkit.entity.Player p : gm.getAllPlayers()) {
                    if (gm.deadPlayers.contains(p.getUniqueId())) continue;
                    if (p.getLocation().getY() < map.voidY) {
                        gm.tpToSpawnPublic(p);
                        p.sendMessage(Lang.get(p, Lang.Key.VOID_TP));
                    }
                }
            }
        }, 1L, 1L).getTaskId();

        razeRocketTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (GameManager gm : new ArrayList<>(GameRegistry.getAllInstances())) {
                processRazeRocketsForInstance(gm);
            }
        }, 1L, 1L).getTaskId();

        teamMenuRefreshTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (GameManager gm : GameRegistry.getAllInstances()) {
                if (gm.state != GameManager.GameState.WAITING) continue;
                for (org.bukkit.entity.Player p : gm.getAllPlayers()) {
                    if (p.getOpenInventory().getTopInventory().getHolder()
                            instanceof fr.sannoxx.ubuwool.menu.TeamMenu) {
                        fr.sannoxx.ubuwool.menu.TeamMenu.refresh(p, gm);
                    }
                }
            }
        }, 1L, 1L).getTaskId();

        if (UbuPlaceholders.tryRegister(this)) {
            getLogger().info("[UbuWool] Hook PlaceholderAPI enregistré (%ubuwool_...%).");
        }

        getLogger().info(getConfig().getString("startup-message", "UbuWool activé !"));
        getLogger().info("[UbuWool] Multi-instance activé (max " + GameRegistry.MAX_INSTANCES + " instances).");
    }

    private void processRazeRocketsForInstance(GameManager gm) {
        if (gm.activeRockets.isEmpty()) return;

        java.util.List<UUID> toDetonate = new java.util.ArrayList<>();

        for (Map.Entry<UUID, UUID> entry : new HashMap<>(gm.activeRockets).entrySet()) {
            UUID rocketUUID = entry.getKey();
            UUID ownerUUID  = entry.getValue();

            org.bukkit.entity.Entity rocket = null;
            for (org.bukkit.World w : getServer().getWorlds()) {
                Entity e = w.getEntity(rocketUUID);
                if (e != null) { rocket = e; break; }
            }

            if (rocket != null && rocket.isValid()) {
                org.bukkit.Location loc = rocket.getLocation();
                gm.rocketLastPos.put(rocketUUID, loc);

                org.bukkit.entity.Player owner = getServer().getPlayer(ownerUUID);
                if (owner == null) { toDetonate.add(rocketUUID); continue; }

                boolean hit = false;

                for (org.bukkit.entity.Player target : getServer().getOnlinePlayers()) {
                    if (target.getUniqueId().equals(ownerUUID)) continue;
                    if (!gm.playerDataMap.containsKey(target.getUniqueId())) continue;
                    if (gm.deadPlayers.contains(target.getUniqueId())) continue;
                    if (gm.isRedTeam(target) == gm.isRedTeam(owner)) continue;
                    if (target.getLocation().distanceSquared(loc) <= 1.44) {
                        hit = true;
                        rocket.remove();
                        break;
                    }
                }

                if (!hit) {
                    org.bukkit.block.Block block = loc.getBlock();
                    if (!block.getType().isAir()) {
                        hit = true;
                        rocket.remove();
                    }
                }

                if (hit) toDetonate.add(rocketUUID);
            } else {
                toDetonate.add(rocketUUID);
            }
        }

        for (UUID rocketUUID : toDetonate) {
            UUID ownerUuid = gm.activeRockets.remove(rocketUUID);
            org.bukkit.Location lastPos = gm.rocketLastPos.remove(rocketUUID);
            if (ownerUuid != null && lastPos != null) {
                org.bukkit.entity.Player owner = getServer().getPlayer(ownerUuid);
                if (owner != null) LarokAbilities.detonateRocket(owner, lastPos);
            }
        }
    }

    @Override
    public void onDisable() {
        if (agentSelectTask != -1) getServer().getScheduler().cancelTask(agentSelectTask);
        if (ilargiaTask     != -1) getServer().getScheduler().cancelTask(ilargiaTask);
        if (voidCheckTask   != -1) getServer().getScheduler().cancelTask(voidCheckTask);
        if (razeRocketTask  != -1) getServer().getScheduler().cancelTask(razeRocketTask);
        if (teamMenuRefreshTask != -1) getServer().getScheduler().cancelTask(teamMenuRefreshTask);

        MatchmakingQueue.stopChecker();
        MatchmakingQueue.reset();

        PlayerStats.saveAll();
        GameRegistry.resetAll();

        getLogger().info("UbuWool désactivé.");
    }

    public static UbuWool getInstance() { return instance; }
}