package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import fr.sannoxx.ubuwool.manager.MapConfig;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

class LolitaAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { return LolitaAbilities.madameSaucisse(p); }
    @Override public boolean useC2(Player p, Player t) {
        if (t == null) return false;
        LolitaAbilities.morsureDuChihuahua(p, t);
        return true;
    }
    @Override public boolean useUltimate(Player p) { return LolitaAbilities.unVsUnBanos(p); }
    @Override public long cooldownC1Ms() { return 600_000L; }
    @Override public long cooldownC2Ms() { return 40_000L; }
    @Override public void resetRound() { LolitaAbilities.resetRound(); }
}

public class LolitaAbilities {

    public static Map<String, Wolf> activeDogs = new HashMap<>();
    public static Set<String> dogUsedByPlayer = new HashSet<>();
    public static Set<String> playersInBanos = new HashSet<>();

    /**
     * Vérifie si un joueur est actuellement dans la zone Baños.
     * Utilisé pour bloquer l'utilisation des capacités.
     */
    public static boolean isInBanos(Player player) {
        return playersInBanos.contains(player.getName());
    }

    public static boolean madameSaucisse(Player player) {
        if (dogUsedByPlayer.contains(player.getName())) {
            player.sendMessage(Lang.get(player, Lang.Key.MSG_LOLI_C1));
            return false;
        }

        Wolf wolf = (Wolf) player.getWorld().spawnEntity(player.getLocation(), EntityType.WOLF);
        wolf.setCustomName("§8§lMadame Saucisse");
        wolf.setCustomNameVisible(true);
        wolf.setOwner(player);
        wolf.setTamed(true);

        // 5 coeurs = 10 HP max
        AttributeInstance maxHealthAttr = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(10.0);
        }
        wolf.setHealth(10.0);

        activeDogs.put(player.getName(), wolf);
        dogUsedByPlayer.add(player.getName());
        player.sendMessage(Lang.get(player, Lang.Key.MSG_LOLI_C1_1));

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    Wolf dog = activeDogs.get(player.getName());
                    if (dog != null && dog.isValid()) {
                        dog.remove();
                        activeDogs.remove(player.getName());
                        player.sendMessage(Lang.get(player, Lang.Key.MSG_LOLI_C1_2));
                    }
                }, 600L);
        return true;
    }

    public static void morsureDuChihuahua(Player attacker, Player target) {
        target.damage(4.0, attacker);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, true));
        attacker.sendMessage(Lang.get(attacker, Lang.Key.MSG_LOLI_C2));
        target.sendMessage(Lang.get(target, Lang.Key.MSG_LOLI_C2_1));
    }

    public static boolean unVsUnBanos(Player lolita) {
        GameManager gm = GameRegistry.getInstanceOf(lolita);
        if (gm == null) return false;
        MapConfig.UbuMap map = MapConfig.getSelectedMap();

        if (map == null || !map.hasBanos) {
            lolita.sendMessage(Lang.get(lolita, Lang.Key.MSG_LOLI_NO_BANOS));
            return false;
        }

        boolean isRed = gm.isRedTeam(lolita);
        Vector dir = lolita.getEyeLocation().getDirection();

        Player target = null;
        double closest = Double.MAX_VALUE;
        for (Player enemy : new ArrayList<>(isRed
                ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
            RayTraceResult r = enemy.getBoundingBox().expand(2)
                    .rayTrace(lolita.getEyeLocation().toVector(), dir, 40);
            if (r != null) {
                double d = enemy.getLocation().distanceSquared(lolita.getLocation());
                if (d < closest) { closest = d; target = enemy; }
            }
        }

        if (target == null) {
            lolita.sendMessage(Lang.get(lolita, Lang.Key.MSG_LOLI_C2_2));
            return false;
        }

        Location lolitaOld = lolita.getLocation().clone();
        Location targetOld = target.getLocation().clone();
        final Player finalTarget = target;

        World world = lolita.getWorld();
        Location banosLoc1 = new Location(world,
                map.banosX1 + 0.5, map.banosY1, map.banosZ1 + 0.5, 180f, 0f);
        Location banosLoc2 = new Location(world,
                map.banosX2 + 0.5, map.banosY2, map.banosZ2 + 0.5, 0f, 0f);

        lolita.teleport(banosLoc1);
        finalTarget.teleport(banosLoc2);

        playersInBanos.add(lolita.getName());
        playersInBanos.add(finalTarget.getName());

        lolita.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 0, false, true));
        gm.banosPlayers.put(lolita.getName(), lolitaOld);
        gm.banosPlayers.put(finalTarget.getName(), targetOld);

        lolita.sendMessage(Lang.get(lolita, Lang.Key.MSG_LOLI_UBULT) + finalTarget.getName() + "§7 !");
        finalTarget.sendMessage(Lang.get(finalTarget, Lang.Key.MSG_LOLI_UBULT_1));

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    if (gm.banosPlayers.containsKey(lolita.getName()))
                        returnFromBanos(lolita, lolitaOld, gm);
                    if (gm.banosPlayers.containsKey(finalTarget.getName()))
                        returnFromBanos(finalTarget, targetOld, gm);
                }, 1200L);
        return true;
    }

    public static void returnFromBanos(Player player, Location oldPos, GameManager gm) {
        if (!player.isOnline()) return;
        playersInBanos.remove(player.getName());
        player.teleport(oldPos);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        gm.banosPlayers.remove(player.getName());
        player.sendMessage(Lang.get(player, Lang.Key.MSG_LOLI_UBULT_2));
    }

    public static void returnFromBanos(Player player, Location oldPos) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        returnFromBanos(player, oldPos, gm);
    }

    public static void resetRound() {
        dogUsedByPlayer.clear();
        activeDogs.values().forEach(w -> { if (w != null && w.isValid()) w.remove(); });
        activeDogs.clear();
        playersInBanos.clear();
    }
}