package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.AbilityManager;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.util.*;

class LarokAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { LarokAbilities.jettDash(p); return true; }
    @Override public boolean useC2(Player p, Player t) { return LarokAbilities.neonFulgure(p); }
    @Override public boolean useUltimate(Player p) { LarokAbilities.razeRocket(p); return true; }
    @Override public long cooldownC1Ms() { return 40_000L; }
    @Override public long cooldownC2Ms() { return 40_000L; }
    @Override public void resetRound() { }
}

public class LarokAbilities {

    public static void jettDash(Player player) {
        Vector dir = player.getEyeLocation().getDirection().normalize().multiply(12.5);
        player.setVelocity(new Vector(dir.getX(), Math.max(dir.getY(), 0.3), dir.getZ()));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, true));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_LARO_C1));
    }

    public static boolean neonFulgure(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return false;
        boolean isRed = gm.isRedTeam(player);
        Vector dir = player.getEyeLocation().getDirection();
        Player target = null;
        double closest = Double.MAX_VALUE;
        for (Player enemy : new ArrayList<>(isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
            RayTraceResult r = enemy.getBoundingBox().expand(1.2).rayTrace(player.getEyeLocation().toVector(), dir, 15);
            if (r != null) {
                double d = enemy.getLocation().distanceSquared(player.getLocation());
                if (d < closest) { closest = d; target = enemy; }
            }
        }
        if (target == null) { player.sendMessage(Lang.get(player, Lang.Key.MSG_LARO_C2)); return false; }
        player.getWorld().strikeLightningEffect(target.getLocation());
        final Player finalTarget = target;
        UbuWool.getInstance().getServer().getScheduler().runTask(UbuWool.getInstance(), () -> {
            finalTarget.damage(4, player);
            finalTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 254, false, false));
        });
        player.sendMessage(Lang.get(player, Lang.Key.MSG_LARO_C2_1));
        return true;
    }

    public static void razeRocket(Player player) {
        ItemStack rocket = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta meta = rocket.getItemMeta();
        meta.setDisplayName("§c§l★ Raze Rocket ★");
        rocket.setItemMeta(meta);
        AbilityManager.addGlow(rocket);

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s != null && s.getType() == Material.NETHER_STAR) {
                player.getInventory().setItem(i, rocket);
                break;
            }
        }
        player.sendMessage(Lang.get(player, Lang.Key.MSG_LARO_UBULT));
    }

    public static void detonateRocket(Player shooter, Location pos) {
        GameManager gm = GameRegistry.getInstanceOf(shooter);
        if (gm == null) return;

        pos.getWorld().createExplosion(pos.getX(), pos.getY(), pos.getZ(), 0f, false, false);
        pos.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, pos, 3);
        pos.getWorld().playSound(pos, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1f);

        boolean shooterIsRed = gm.isRedTeam(shooter);
        for (Player target : new ArrayList<>(shooter.getServer().getOnlinePlayers())) {
            if (!gm.playerDataMap.containsKey(target.getUniqueId())) continue;
            if (gm.deadPlayers.contains(target.getUniqueId())) continue;
            if (target.equals(shooter)) continue;
            if (gm.isRedTeam(target) == shooterIsRed) continue;
            if (target.getLocation().distanceSquared(pos) > 36) continue;

            gm.lastDamager.put(target.getUniqueId(), shooter.getUniqueId());

            UbuWool.getInstance().getServer().getScheduler().runTask(UbuWool.getInstance(), () -> {
                if (gm.deadPlayers.contains(target.getUniqueId())) return;
                double newHealth = Math.max(0, target.getHealth() - 10.0);
                target.setHealth(newHealth);

                target.removePotionEffect(PotionEffectType.SLOWNESS);
                UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () ->
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 0, false, true)),
                        1L);

                target.sendMessage(Lang.get(target, Lang.Key.MSG_LARO_UBULT_2));
            });
        }
    }
}