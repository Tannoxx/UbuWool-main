package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

class AskyAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { AskyAbilities.phenerwinDegueulasse(p); return true; }
    @Override public boolean useC2(Player p, Player t) { AskyAbilities.versionReggaeton(p); return true; }
    @Override public boolean useUltimate(Player p) { AskyAbilities.wantedPetasse(p); return true; }
    @Override public long cooldownC1Ms() { return 45_000L; }
    @Override public long cooldownC2Ms() { return 45_000L; }
    @Override public void resetRound() { }
}

public class AskyAbilities {

    public static void phenerwinDegueulasse(Player player) {
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 8));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,   80, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER,   80, 0, false, true));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_ASKY_C1));
    }

    public static void versionReggaeton(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Location start = player.getEyeLocation();
        boolean isRed = gm.isRedTeam(player);

        for (int i = 1; i <= 20; i++) {
            final int step = i;
            UbuWool.getInstance().getServer().getScheduler()
                    .runTaskLater(UbuWool.getInstance(), () -> {
                        Location pos = start.clone().add(dir.clone().multiply(step));
                        start.getWorld().spawnParticle(Particle.NOTE, pos, 3, 0.2, 0.2, 0.2, 0.1);
                        for (Player enemy : new ArrayList<>(isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
                            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
                            if (enemy.getBoundingBox().expand(1).contains(pos.toVector())) {
                                enemy.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 140, 0, false, true));
                                enemy.sendMessage(Lang.get(enemy, Lang.Key.MSG_ASKY_C2));
                            }
                        }
                    }, step);
        }
        player.sendMessage(Lang.get(player, Lang.Key.MSG_ASKY_C2_1));
    }

    public static void wantedPetasse(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;

        RayTraceResult hit = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), 15);

        Location jukeboxLoc;
        if (hit != null && hit.getHitBlock() != null) {
            assert hit.getHitBlockFace() != null;
            jukeboxLoc = hit.getHitBlock().getRelative(hit.getHitBlockFace()).getLocation();
        }
        else
            jukeboxLoc = player.getLocation();

        jukeboxLoc.getBlock().setType(Material.JUKEBOX);
        jukeboxLoc.getWorld().playSound(jukeboxLoc, Sound.MUSIC_DISC_MELLOHI, SoundCategory.RECORDS, 4.0f, 1.0f);

        gm.askyJukeboxes.add(jukeboxLoc);
        player.sendMessage(Lang.get(player, Lang.Key.MSG_ASKY_UBULT));

        boolean isRed = gm.isRedTeam(player);
        final Location jLoc = jukeboxLoc;

        runJukeboxPhase(jLoc, isRed, gm,
                Collections.singletonList(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false)),
                () -> runJukeboxPhase(jLoc, isRed, gm,
                        Arrays.asList(
                                new PotionEffect(PotionEffectType.SLOWNESS,       40, 1, false, false),
                                new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 0, false, false)
                        ),
                        () -> runJukeboxPhase(jLoc, isRed, gm,
                                Arrays.asList(
                                        new PotionEffect(PotionEffectType.SLOWNESS,       40, 2, false, false),
                                        new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 1, false, false),
                                        new PotionEffect(PotionEffectType.WEAKNESS,       40, 0, false, false)
                                ),
                                () -> cleanupJukebox(jLoc, gm))));
    }

    private static void runJukeboxPhase(Location jLoc, boolean isRed, GameManager gm,
                                        List<PotionEffect> effects, Runnable onDone) {
        final long[] elapsed = {0L};
        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), task -> {
                    elapsed[0] += 20;

                    if (!jLoc.getBlock().getType().equals(Material.JUKEBOX)) {
                        task.cancel();
                        cleanupJukebox(jLoc, gm);
                        return;
                    }

                    if (elapsed[0] > 300L) {
                        task.cancel();
                        onDone.run();
                        return;
                    }

                    for (Player enemy : new ArrayList<>(isRed
                            ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
                        if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
                        if (enemy.getLocation().distanceSquared(jLoc) <= 225) {
                            effects.forEach(e -> enemy.addPotionEffect(
                                    new PotionEffect(e.getType(), e.getDuration(),
                                            e.getAmplifier(), false, false)));
                            jLoc.getWorld().spawnParticle(Particle.NOTE,
                                    enemy.getLocation().add(0, 1, 0),
                                    1, 0.3, 0.3, 0.3, 0.1);
                        }
                    }
                }, 0L, 20L);
    }

    private static void cleanupJukebox(Location jLoc, GameManager gm) {
        if (jLoc.getBlock().getType() == Material.JUKEBOX)
            jLoc.getBlock().setType(Material.AIR);
        gm.askyJukeboxes.remove(jLoc);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.stopSound(SoundCategory.RECORDS);
        }
    }
}