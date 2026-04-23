package fr.sannoxx.ubuwool.listener;

import fr.sannoxx.ubuwool.*;
import fr.sannoxx.ubuwool.ability.*;
import fr.sannoxx.ubuwool.manager.*;
import fr.sannoxx.ubuwool.menu.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class PlayerListener implements Listener {

    private final JavaPlugin plugin;

    private static final Set<UUID> chihuahuaDamage = new HashSet<>();

    public PlayerListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        MatchmakingQueue.dequeue(player);

        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm != null) {
            if (gm.state == GameManager.GameState.ROUND_ACTIVE
                    && !gm.deadPlayers.contains(player.getUniqueId())) {
                gm.deadPlayers.add(player.getUniqueId());
                gm.checkTeamEliminationPublic();
            }
            gm.teamRed.remove(player.getUniqueId());
            gm.teamBlue.remove(player.getUniqueId());
            gm.playerDataMap.remove(player.getUniqueId());

            refreshTeamMenus(gm);
        }

        MascordAbilities.stopCompassTracker(player);
        PlayerProfile.clearCache(player);
        PlayerStats.evict(player.getUniqueId());
        AbilityStateManager.resetPlayer(player.getUniqueId());
        AbilityManager.clearCooldowns(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof AgentMenu)
            AgentMenu.handleClick(event);
        else if (event.getInventory().getHolder() instanceof BuyMenu)
            BuyMenu.handleClick(event);
        else if (event.getInventory().getHolder() instanceof MapVoteMenu)
            MapVoteMenu.handleClick(event);
        else if (event.getInventory().getHolder() instanceof TeamMenu)
            TeamMenu.handleClick(event);
        else if (event.getInventory().getHolder() instanceof ProfileMenu)
            ProfileMenu.handleClick(event);
        else if (event.getInventory().getHolder() instanceof LeaderboardMenu)
            LeaderboardMenu.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TeamMenu) {
            TeamMenu.onClose(event);
        } else if (event.getInventory().getHolder() instanceof MapVoteMenu) {
            MapVoteMenu.onClose(event);
        } else if (event.getInventory().getHolder() instanceof BuyMenu) {
            // Nouveau : blocage fermeture BuyMenu
            BuyMenu.onClose(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        if (gm.state != GameManager.GameState.ROUND_ACTIVE) return;

        ItemStack stack = event.getItem().getItemStack();
        if (!stack.hasItemMeta() || !stack.getItemMeta().hasDisplayName()) return;
        String name = stack.getItemMeta().getDisplayName();

        if (name.contains("C1") || name.contains("C2") || name.contains("Ubultimate")
                || name.contains("Head Hunter") || name.contains("Chasseur de Tête")
                || name.contains("Rendez") || name.contains("Raze Rocket")
                || name.contains("Tour de Force")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        GameManager.GameState st = gm.state;
        if (st == GameManager.GameState.ROUND_ACTIVE) {
            gm.onPlayerDeath(player);
        } else if (st == GameManager.GameState.BUY_PHASE
                || st == GameManager.GameState.ROUND_END
                || st == GameManager.GameState.AGENT_SELECT) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.spigot().respawn();
                gm.tpToSpawnPublic(player);
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        MapConfig.UbuMap map = MapConfig.getSelectedMap();
        if (map != null) {
            event.setRespawnLocation(gm.isRedTeam(player)
                    ? new Location(Bukkit.getWorlds().getFirst(), map.spawnRedX, map.spawnRedY, map.spawnRedZ)
                    : new Location(Bukkit.getWorlds().getFirst(), map.spawnBlueX, map.spawnBlueY, map.spawnBlueZ));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        GameManager gm = GameRegistry.getInstanceOf(target);
        if (gm == null) return;

        if (gm.state != GameManager.GameState.ROUND_ACTIVE) {
            if (event.getDamager() instanceof Player || event.getDamager() instanceof Projectile) {
                event.setCancelled(true);
                return;
            }
        }

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker instanceof Player doma) {
            PlayerData d = gm.playerDataMap.get(doma.getUniqueId());
            if (d != null && d.agent != null
                    && d.agent.getName().equalsIgnoreCase("doma")) {
                DomaAbilities.onHit(doma, target);
            }
        }

        if (attacker != null && gm.playerDataMap.containsKey(attacker.getUniqueId())) {
            gm.lastDamager.put(target.getUniqueId(), attacker.getUniqueId());

            PlayerData atkData = gm.playerDataMap.get(attacker.getUniqueId());

            if (atkData != null && atkData.agent != null
                    && atkData.agent.getName().equalsIgnoreCase("hijab")) {
                HijabAbilities.showTargetHealth(attacker, target);
                if (HijabAbilities.hasFireAspect(attacker)) target.setFireTicks(80);
            }

            if (event.getDamager() instanceof Arrow) {
                HorcusAbilities.onArrowHit(event);
            }

            if (event.getDamager() instanceof Arrow arrow
                    && arrow.getShooter() instanceof Player shooter
                    && !UbuWool.processingHorcusDamage) {
                PlayerData sd = gm.playerDataMap.get(shooter.getUniqueId());
                if (sd != null && sd.agent != null && sd.agent.getName().equalsIgnoreCase("horcus")) {
                    UbuWool.processingHorcusDamage = true;
                    event.setDamage(event.getDamage() * 0.6);
                    UbuWool.processingHorcusDamage = false;
                }
            }
        }

        PlayerData vData = gm.playerDataMap.get(target.getUniqueId());
        if (vData != null && vData.agent != null
                && vData.agent.getName().equalsIgnoreCase("ticksuspicious")
                && event.getFinalDamage() > 0) {
            TicksuspiciousAbilities.onHitReceived(target);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        GameManager gm = GameRegistry.getInstanceOf(shooter);
        if (gm == null) return;
        if (gm.state != GameManager.GameState.ROUND_ACTIVE) return;

        PlayerData data = gm.playerDataMap.get(shooter.getUniqueId());
        if (data == null || data.agent == null) return;
        if (!data.agent.getName().equalsIgnoreCase("horcus")) return;

        ItemStack held = shooter.getInventory().getItemInMainHand();
        if (!held.hasItemMeta() || !held.getItemMeta().hasDisplayName()) return;
        String name = held.getItemMeta().getDisplayName();
        if (!name.contains("Head Hunter") && !name.contains("Chasseur de Tête")) return;

        HorcusAbilities.onHeadHunterShoot(arrow);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRazeRocketHitBlock(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof SmallFireball fireball)) return;
        for (GameManager gm : GameRegistry.getAllInstances()) {
            if (gm.activeRockets.containsKey(fireball.getUniqueId())) {
                event.setCancelled(true);
                fireball.remove();
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;

        org.bukkit.block.Block block = event.getBlock();
        Material mat = block.getType();

        if (mat == Material.SPONGE) {
            Location loc = block.getLocation();
            String ownerName = null;
            for (Map.Entry<String, Location> entry : HorcusAbilities.spongePositions.entrySet()) {
                if (entry.getValue().getBlockX() == loc.getBlockX()
                        && entry.getValue().getBlockY() == loc.getBlockY()
                        && entry.getValue().getBlockZ() == loc.getBlockZ()) {
                    ownerName = entry.getKey();
                    break;
                }
            }
            if (ownerName != null) {
                boolean isOwner = player.getName().equals(ownerName);
                Player horcus = Bukkit.getPlayerExact(ownerName);

                HorcusAbilities.spongePositions.remove(ownerName);

                event.setCancelled(true);
                block.setType(Material.AIR);

                if (isOwner) {
                    if (horcus != null) {
                        HorcusAbilities.swapRendezVousItem(horcus, true);
                    }
                } else {
                    if (horcus != null) {
                        HorcusAbilities.removeRendezVousItem(horcus);
                        horcus.sendMessage(Lang.get(horcus, Lang.Key.HORCUS_SPONGE_BROKEN));
                    }
                }
                return;
            }
        }

        if (gm.state != GameManager.GameState.ROUND_ACTIVE) {
            if (isAllowedBreak(mat)) event.setCancelled(true);
            return;
        }

        if (isAllowedBreak(mat)) {
            event.setCancelled(true);
        } else {
            if (isWoolBlock(mat)) {
                event.setDropItems(false);
            }
        }
    }

    private boolean isAllowedBreak(Material mat) {
        return isWoolBlock(mat)
                && mat != Material.DIORITE && mat != Material.IRON_BLOCK
                && mat != Material.CHERRY_LEAVES && mat != Material.SPONGE
                && mat != Material.JUKEBOX && mat != Material.BAMBOO_PLANKS;
    }

    private boolean isWoolBlock(Material mat) {
        return mat != Material.RED_WOOL && mat != Material.BLUE_WOOL
                && mat != Material.WHITE_WOOL && mat != Material.ORANGE_WOOL
                && mat != Material.MAGENTA_WOOL && mat != Material.LIGHT_BLUE_WOOL
                && mat != Material.YELLOW_WOOL && mat != Material.LIME_WOOL
                && mat != Material.PINK_WOOL && mat != Material.GRAY_WOOL
                && mat != Material.LIGHT_GRAY_WOOL && mat != Material.CYAN_WOOL
                && mat != Material.PURPLE_WOOL && mat != Material.GREEN_WOOL
                && mat != Material.BROWN_WOOL && mat != Material.BLACK_WOOL;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;

        if (gm.state != GameManager.GameState.ROUND_ACTIVE) {
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType().isBlock()) { event.setCancelled(true); return; }
            }
            return;
        }

        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack.getType().isAir()) return;

        org.bukkit.event.block.Action action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        if (stack.getType() == Material.NETHER_STAR) {
            event.setCancelled(true);
            handleUltimate(player, gm);
            return;
        }

        if (stack.getType() == Material.FIREWORK_ROCKET
                && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
                && stack.getItemMeta().getDisplayName().contains("Raze Rocket")) {
            if (LolitaAbilities.isInBanos(player)) {
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            launchRazeRocket(player, gm);
            return;
        }

        if (AbilityManager.isAbilityItem(stack)) {
            event.setCancelled(true);
            int slot = player.getInventory().getHeldItemSlot();
            if (AbilityManager.isOnCooldown(player, slot)) {
                AbilityManager.sendCooldownMessage(player, slot);
                return;
            }
            AbilityDispatcher.dispatch(player, stack, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttackEntity(EntityDamageByEntityEvent event) {
        if (chihuahuaDamage.contains(event.getEntity().getUniqueId())) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        GameManager gm = GameRegistry.getInstanceOf(attacker);
        if (gm == null) return;

        if (gm.state != GameManager.GameState.ROUND_ACTIVE) {
            event.setCancelled(true);
            return;
        }

        PlayerData data = gm.playerDataMap.get(attacker.getUniqueId());
        if (data == null || data.agent == null) return;

        ItemStack stack = attacker.getInventory().getItemInMainHand();
        if (!stack.hasItemMeta() || !stack.getItemMeta().hasDisplayName()) return;

        if (data.agent.getName().equalsIgnoreCase("lolita")
                && stack.getItemMeta().getDisplayName().contains("C2")) {

            if (gm.isRedTeam(attacker) == gm.isRedTeam(target)) return;

            if (LolitaAbilities.isInBanos(attacker)) {
                event.setCancelled(true);
                return;
            }

            int slot = attacker.getInventory().getHeldItemSlot();

            if (!AbilityManager.isOnCooldown(attacker, slot)) {
                chihuahuaDamage.add(target.getUniqueId());
                try {
                    LolitaAbilities.morsureDuChihuahua(attacker, target);
                } finally {
                    chihuahuaDamage.remove(target.getUniqueId());
                }
                event.setCancelled(true);
                AbilityManager.setCooldown(attacker, slot, stack, 40_000L);
            } else {
                AbilityManager.sendCooldownMessage(attacker, slot);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;

        ItemStack stack = event.getItemDrop().getItemStack();
        if (!stack.hasItemMeta() || !stack.getItemMeta().hasDisplayName()) return;
        String name = stack.getItemMeta().getDisplayName();
        if (name.contains("C1") || name.contains("C2") || name.contains("Ubultimate")
                || name.contains("Head Hunter") || name.contains("Chasseur de Tête")
                || name.contains("Rendez") || name.contains("Raze Rocket")
                || name.contains("Tour de Force")) {
            event.setCancelled(true);
        }
    }

    private void handleUltimate(Player player, GameManager gm) {
        if (LolitaAbilities.playersInBanos.contains(player.getName())) {
            return;
        }
        PlayerData data = gm.playerDataMap.get(player.getUniqueId());
        if (data == null || data.agent == null) return;

        if (!data.isUltimateReady()) {
            int required = data.agent.getUltimateKillsRequired();
            String stars = "§e★".repeat(Math.min(data.ultimateKills, required))
                    + "§7☆".repeat(Math.max(0, required - data.ultimateKills));
            player.sendMessage(Lang.get(player, Lang.Key.ULTIMATE_NOT_READY, stars));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        boolean success = AbilityDispatcher.dispatchUltimate(player);
        if (!success) return;

        announceUltimate(player, gm, getUltKey(data.agent.getName()));
        PlayerStats.recordUltimate(player.getUniqueId(), data.agent.getName());
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        data.consumeUltimate();
        gm.updateUltimateItem(player, data);

        TabManager.updateTab(gm);
    }

    private Lang.Key getUltKey(String agentName) {
        return switch (agentName.toLowerCase()) {
            case "sembol"         -> Lang.Key.ULT_SEMBOL;
            case "fantom"         -> Lang.Key.ULT_FANTOM;
            case "gargamel"       -> Lang.Key.ULT_GARGAMEL;
            case "horcus"         -> Lang.Key.ULT_HORCUS;
            case "bambouvore"     -> Lang.Key.ULT_BAMBOUVORE;
            case "lolita"         -> Lang.Key.ULT_LOLITA;
            case "asky"           -> Lang.Key.ULT_ASKY;
            case "carlos"         -> Lang.Key.ULT_CARLOS;
            case "larok"          -> Lang.Key.ULT_LAROK;
            case "ticksuspicious" -> Lang.Key.ULT_TICKSUSPICIOUS;
            case "mascord"        -> Lang.Key.ULT_MASCORD;
            case "hijab"          -> Lang.Key.ULT_HIJAB;
            case "ilargia"        -> Lang.Key.ULT_ILARGIA;
            case "gekko"          -> Lang.Key.ULT_GEKKO;
            case "doma"          -> Lang.Key.ULT_DOMA;
            default               -> Lang.Key.ABILITY_NOT_IMPLEMENTED;
        };
    }

    private void announceUltimate(Player player, GameManager gm, Lang.Key nameKey) {
        PlayerData data = gm.playerDataMap.get(player.getUniqueId());
        if (data == null || data.agent == null) return;
        String agentColor = data.agent.getColor();
        for (Player p : gm.getAllPlayers()) {
            String ultName = Lang.get(p, nameKey);
            String msg = agentColor + "[" + data.agent.getName().toUpperCase() + "] §f"
                    + player.getName() + " → §e§l★ " + ultName + " ★";
            p.sendMessage(msg);
            p.sendTitle("§e§l★ " + ultName + " ★", agentColor + player.getName(), 10, 40, 10);
        }
    }

    private void launchRazeRocket(Player player, GameManager gm) {
        World world = player.getWorld();
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Location spawnPos = player.getEyeLocation().add(dir.clone().multiply(1.5));

        ItemStack held = player.getInventory().getItemInMainHand();
        held.setAmount(held.getAmount() - 1);

        SmallFireball fireball = world.spawn(spawnPos, SmallFireball.class, fb -> {
            fb.setShooter(player);
            fb.setDirection(dir.clone().multiply(2));
            fb.setIsIncendiary(false);
            fb.setYield(0f);
        });

        gm.activeRockets.put(fireball.getUniqueId(), player.getUniqueId());
    }

    public static void refreshTeamMenus(GameManager gm) {
        for (Player p : gm.getAllPlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof TeamMenu) {
                TeamMenu.refresh(p, gm);
            }
        }
    }
}