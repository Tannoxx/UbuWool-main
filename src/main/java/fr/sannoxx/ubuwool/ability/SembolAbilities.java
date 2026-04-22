package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import java.util.ArrayList;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

class SembolAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { return SembolAbilities.cramageDust(p); }
    @Override public boolean useC2(Player p, Player t) { SembolAbilities.sembolFirebol(p); return true; }
    @Override public boolean useUltimate(Player p) { SembolAbilities.galopaFeuVite(p); return true; }
    @Override public long cooldownC1Ms() { return 50_000L; }
    @Override public long cooldownC2Ms() { return 30_000L; }
    @Override public void resetRound() { }
}

public class SembolAbilities {

    public static boolean cramageDust(Player player) {
        RayTraceResult hit = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 50);
        if (hit == null || hit.getHitBlock() == null) { player.sendMessage(Lang.get(player, Lang.Key.MSG_SEMB_C1)); return false; }
        Location center = hit.getHitBlock().getLocation();
        int r = 5;
        for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
            if (x*x+y*y+z*z > r*r) continue;
            Block b = center.clone().add(x,y,z).getBlock();
            if (b.getType() == Material.RED_WOOL || b.getType() == Material.BLUE_WOOL) b.setType(Material.GRAVEL);
        }
        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () -> {
            for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
                if (x*x+y*y+z*z > r*r) continue;
                Block b = center.clone().add(x,y,z).getBlock();
                if (b.getType() == Material.GRAVEL) b.setType(Material.AIR);
            }
        }, 60L);
        player.sendMessage(Lang.get(player, Lang.Key.MSG_SEMB_C1_1));
        return true;
    }

    public static void sembolFirebol(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        boolean isRed = gm.isRedTeam(player);
        Vector dir = player.getEyeLocation().getDirection().normalize();

        SmallFireball fb = player.getWorld().spawn(
                player.getEyeLocation().add(dir.clone().multiply(1.5)), SmallFireball.class);
        fb.setShooter(player);
        fb.setDirection(dir.clone().multiply(1.5));
        fb.setIsIncendiary(false);
        fb.setYield(0f);

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), task -> {
                    if (!fb.isValid()) { task.cancel(); return; }

                    Location loc = fb.getLocation();
                    Block block = loc.getBlock();
                    if (!block.getType().isAir()) {
                        fb.remove();
                        task.cancel();
                        return;
                    }

                    for (Player enemy : new ArrayList<>(isRed
                            ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
                        if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
                        if (enemy.getLocation().distanceSquared(loc) < 1.5) {
                            enemy.setFireTicks(100);
                            gm.lastDamager.put(enemy.getUniqueId(), player.getUniqueId());
                            loc.getWorld().spawnParticle(Particle.FLAME, loc, 10, 0.3, 0.3, 0.3, 0.1);
                            loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
                            fb.remove();
                            task.cancel();
                            return;
                        }
                    }
                }, 1L, 1L);

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    if (fb.isValid()) fb.remove();
                }, 100L);

        player.sendMessage(Lang.get(player, Lang.Key.MSG_SEMB_C2));
    }

    public static void galopaFeuVite(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 2, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0, false, true));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_SEMB_UBULT));

        final long[] elapsed = {0L};
        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), task -> {
                    elapsed[0] += 6;
                    if (elapsed[0] > 400) { task.cancel(); return; }
                    if (!player.isOnline() || !player.isValid()) { task.cancel(); return; }

                    Block feet = player.getLocation().getBlock();
                    if (feet.getType().isAir() && !player.getLocation().subtract(0, 1, 0).getBlock().getType().isAir()) {
                        feet.setType(Material.FIRE);
                        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () -> {
                            if (feet.getType() == Material.FIRE) feet.setType(Material.AIR);
                        }, 100L);
                    }
                }, 0L, 6L);

        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () ->
                player.sendMessage(Lang.get(player, Lang.Key.MSG_SEMB_UBULT_1)), 400L);
    }
}