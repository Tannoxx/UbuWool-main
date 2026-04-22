package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

class MascordAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { MascordAbilities.upDog(p); return true; }
    @Override public boolean useC2(Player p, Player target) {
        GameManager gm = GameRegistry.getInstanceOf(p);
        if (gm == null) return false;
        boolean isRed = gm.isRedTeam(p);
        Player aimed = findAimed(p, isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers(), gm);
        if (aimed == null) { p.sendMessage(fr.sannoxx.ubuwool.Lang.get(p, fr.sannoxx.ubuwool.Lang.Key.NO_ENEMY_AIMED)); return false; }
        MascordAbilities.syndromeApiculteur(p, aimed);
        return true;
    }
    @Override public boolean useUltimate(Player p) { MascordAbilities.sigmaPouleur(p); return true; }
    @Override public long cooldownC1Ms() { return 40_000L; }
    @Override public long cooldownC2Ms() { return 50_000L; }
    @Override public void resetRound() { MascordAbilities.resetRound(); }

    private Player findAimed(Player player, java.util.List<Player> candidates, GameManager gm) {
        org.bukkit.util.Vector dir = player.getEyeLocation().getDirection();
        Player target = null;
        double closest = Double.MAX_VALUE;
        for (Player enemy : candidates) {
            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
            org.bukkit.util.RayTraceResult r = enemy.getBoundingBox().expand(0.8)
                    .rayTrace(player.getEyeLocation().toVector(), dir, 5.0);
            if (r != null) {
                double d = enemy.getLocation().distanceSquared(player.getLocation());
                if (d < closest) { closest = d; target = enemy; }
            }
        }
        return target;
    }
}

public class MascordAbilities {

    private static final Map<UUID, Integer> compassTasks = new HashMap<>();
    public static final Map<UUID, Integer> apiculteurTasks = new HashMap<>();
    private static final Map<Block, BlockData> originalBlockData = new HashMap<>();

    public static void startCompassTracker(Player player) {
        stopCompassTracker(player);
        BukkitTask task = UbuWool.getInstance().getServer().getScheduler().runTaskTimer(
                UbuWool.getInstance(), () -> {
                    GameManager gm = GameRegistry.getInstanceOf(player);
                    if (gm == null) return;
                    if (gm.state != GameManager.GameState.ROUND_ACTIVE
                            && gm.state != GameManager.GameState.BUY_PHASE) return;

                    boolean isRed = gm.isRedTeam(player);
                    List<Player> enemies = new ArrayList<>(isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers());

                    Player weakest = null;
                    double minHealth = Double.MAX_VALUE;
                    for (Player e : enemies) {
                        if (gm.deadPlayers.contains(e.getUniqueId())) continue;
                        if (e.getHealth() < minHealth) { minHealth = e.getHealth(); weakest = e; }
                    }
                    if (weakest == null) return;

                    ItemStack compass = new ItemStack(Material.COMPASS);
                    CompassMeta meta = (CompassMeta) compass.getItemMeta();
                    meta.setDisplayName("§9Boussole ? §7→ §c" + weakest.getName()
                            + " §7(§c" + (int)(weakest.getHealth() / 2) + "♥§7)");
                    meta.setLodestone(weakest.getLocation());
                    meta.setLodestoneTracked(false);
                    compass.setItemMeta(meta);
                    player.getInventory().setItemInOffHand(compass);
                }, 20L, 20L);

        compassTasks.put(player.getUniqueId(), task.getTaskId());
    }

    public static void stopCompassTracker(Player player) {
        Integer taskId = compassTasks.remove(player.getUniqueId());
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
    }

    public static void upDog(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        Location pos = player.getLocation();

        for (Player nearby : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (nearby == player) continue;
            if (!gm.playerDataMap.containsKey(nearby.getUniqueId())) continue;
            if (gm.deadPlayers.contains(nearby.getUniqueId())) continue;
            if (nearby.getLocation().distance(pos) > 5) continue;
            Vector dir = nearby.getLocation().subtract(pos).toVector().normalize();
            nearby.setVelocity(new Vector(dir.getX() * 4, 1.8, dir.getZ() * 4));
        }

        player.setVelocity(new Vector(0, 2.5, 0));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_MASC_C1));
    }

    public static void syndromeApiculteur(Player attacker, Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 160, 0, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 160, 0, false, true));
        target.sendMessage(Lang.get(target, Lang.Key.MSG_MASC_C2));
        attacker.sendMessage(Lang.get(attacker, Lang.Key.MSG_MASC_C2_1));

        BukkitTask task = UbuWool.getInstance().getServer().getScheduler().runTaskTimer(
                UbuWool.getInstance(), () -> {
                    Block feet = target.getLocation().subtract(0, 1, 0).getBlock();
                    Material currentType = feet.getType();

                    if (!currentType.equals(Material.HONEY_BLOCK)
                            && !currentType.isAir()
                            && !originalBlockData.containsKey(feet)) {

                        final BlockData savedData = feet.getBlockData().clone();
                        originalBlockData.put(feet, savedData);
                        feet.setType(Material.HONEY_BLOCK);

                        UbuWool.getInstance().getServer().getScheduler().runTaskLater(
                                UbuWool.getInstance(), () -> {
                                    if (feet.getType() == Material.HONEY_BLOCK) {
                                        feet.setBlockData(savedData);
                                    }
                                    originalBlockData.remove(feet);
                                }, 100L);
                    }
                }, 0L, 4L);

        apiculteurTasks.put(target.getUniqueId(), task.getTaskId());

        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () -> {
            Integer id = apiculteurTasks.remove(target.getUniqueId());
            if (id != null) Bukkit.getScheduler().cancelTask(id);
        }, 160L);
    }

    public static void sigmaPouleur(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;

        for (Player p : gm.getAllPlayers()) {
            if (p == player) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 600, 5, false, true));
            p.sendMessage(Lang.get(p, Lang.Key.MSG_MASC_UBULT));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 0, false, true));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_MASC_UBULT_1));

        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () -> {
            for (Player p : gm.getAllPlayers()) {
                if (p == player) continue;
                p.removePotionEffect(PotionEffectType.JUMP_BOOST);
                p.sendMessage(Lang.get(p, Lang.Key.MSG_MASC_UBULT_2));
            }
            player.removePotionEffect(PotionEffectType.STRENGTH);
        }, 600L);
    }

    public static void resetRound() {
        apiculteurTasks.values().forEach(Bukkit.getScheduler()::cancelTask);
        apiculteurTasks.clear();
        originalBlockData.clear();
        compassTasks.values().forEach(Bukkit.getScheduler()::cancelTask);
        compassTasks.clear();
    }
}