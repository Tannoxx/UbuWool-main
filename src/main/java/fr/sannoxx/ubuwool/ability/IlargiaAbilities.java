package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

class IlargiaAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { return IlargiaAbilities.rigAndWa(p); }
    @Override public boolean useC2(Player p, Player t) { return IlargiaAbilities.bowserBreathe(p); }
    @Override public boolean useUltimate(Player p) { IlargiaAbilities.pedaniPedalo(p); return true; }
    @Override public long cooldownC1Ms() { return 40_000L; }
    @Override public long cooldownC2Ms() { return 33_000L; }
    @Override public void resetRound() { IlargiaAbilities.resetRound(); }
}

public class IlargiaAbilities {

    public static Map<String, List<Entity>> activeClones = new HashMap<>();

    private static final Set<String> breathingPlayers = new HashSet<>();

    public static boolean hasNoArmor(Player player) {
        for (ItemStack s : player.getInventory().getArmorContents())
            if (s != null && s.getType() != Material.AIR) return false;
        return true;
    }

    public static boolean rigAndWa(Player ilargia) {
        GameManager gm = GameRegistry.getInstanceOf(ilargia);
        if (gm == null) return false;
        boolean isRed = gm.isRedTeam(ilargia);
        Vector dir = ilargia.getEyeLocation().getDirection();

        Player target = null;
        double closest = Double.MAX_VALUE;
        for (Player enemy : new ArrayList<>(isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
            RayTraceResult r = enemy.getBoundingBox().expand(1.5)
                    .rayTrace(ilargia.getEyeLocation().toVector(), dir, 20);
            if (r != null) {
                double d = enemy.getLocation().distanceSquared(ilargia.getLocation());
                if (d < closest) { closest = d; target = enemy; }
            }
        }

        if (target == null) {
            ilargia.sendMessage(Lang.get(ilargia, Lang.Key.MSG_ILAR_C1_NO_TARGET));
            return false;
        }

        final Player finalTarget = target;

        final int castSlot = ilargia.getInventory().getHeldItemSlot();

        ilargia.sendMessage(Lang.get(ilargia, Lang.Key.MSG_ILAR_C1_CAST, finalTarget.getName()));
        finalTarget.sendMessage(Lang.get(finalTarget, Lang.Key.MSG_ILAR_C1_TARGET_START));

        final long[] elapsed = {0L};
        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), task -> {
                    elapsed[0]++;

                    if (ilargia.isOnline()
                            && ilargia.getInventory().getHeldItemSlot() != castSlot) {
                        task.cancel();
                        if (finalTarget.isOnline()) {
                            finalTarget.setVelocity(new Vector(0, 0, 0));
                            finalTarget.sendMessage(Lang.get(finalTarget, Lang.Key.MSG_ILAR_C1_FREED));
                        }
                        ilargia.sendMessage(Lang.get(ilargia, Lang.Key.MSG_ILAR_C1_RELEASED));
                        return;
                    }

                    if (elapsed[0] >= 80) {
                        task.cancel();
                        if (finalTarget.isOnline()) {
                            finalTarget.setVelocity(new Vector(0, 0, 0));
                            finalTarget.sendMessage(Lang.get(finalTarget, Lang.Key.MSG_ILAR_C1_FREED));
                        }
                        return;
                    }

                    if (!finalTarget.isOnline() || gm.deadPlayers.contains(finalTarget.getUniqueId())) {
                        task.cancel(); return;
                    }
                    if (!ilargia.isOnline() || gm.deadPlayers.contains(ilargia.getUniqueId())) {
                        task.cancel();
                        if (finalTarget.isOnline()) {
                            finalTarget.setVelocity(new Vector(0, 0, 0));
                            finalTarget.sendMessage(Lang.get(finalTarget, Lang.Key.MSG_ILAR_C1_FREED_CASTER_DEAD));
                        }
                        return;
                    }

                    Location eyeLoc = ilargia.getEyeLocation();
                    RayTraceResult ray = ilargia.getWorld().rayTraceBlocks(
                            eyeLoc, eyeLoc.getDirection(), 50,
                            FluidCollisionMode.NEVER, true);

                    Location destFeet;
                    if (ray != null && ray.getHitBlock() != null) {
                        assert ray.getHitBlockFace() != null;
                        org.bukkit.block.Block hitFace = ray.getHitBlock().getRelative(ray.getHitBlockFace());
                        destFeet = hitFace.getLocation().add(0.5, 0, 0.5);
                    } else {
                        destFeet = eyeLoc.clone().add(eyeLoc.getDirection().multiply(50));
                        destFeet.setX(Math.floor(destFeet.getX()) + 0.5);
                        destFeet.setZ(Math.floor(destFeet.getZ()) + 0.5);
                    }

                    Location resolved = findFreeSpace(destFeet);
                    if (resolved == null) {
                        finalTarget.setVelocity(new Vector(0, 0, 0));
                        return;
                    }

                    resolved.setYaw(finalTarget.getLocation().getYaw());
                    resolved.setPitch(finalTarget.getLocation().getPitch());
                    resolved.setWorld(finalTarget.getWorld());
                    finalTarget.teleport(resolved);
                    finalTarget.setVelocity(new Vector(0, 0, 0));

                }, 0L, 1L);
        return true;
    }

    private static Location findFreeSpace(Location loc) {
        if (isFree(loc)) return loc;
        for (int i = 1; i <= 5; i++) {
            Location c = loc.clone().add(0, -i, 0);
            if (isFree(c)) return c;
        }
        for (int i = 1; i <= 5; i++) {
            Location c = loc.clone().add(0, i, 0);
            if (isFree(c)) return c;
        }
        return null;
    }

    private static boolean isFree(Location loc) {
        org.bukkit.block.Block feet = loc.getBlock();
        org.bukkit.block.Block head = feet.getRelative(0, 1, 0);
        return !feet.getType().isSolid() && !head.getType().isSolid();
    }

    public static boolean bowserBreathe(Player ilargia) {
        GameManager gm = GameRegistry.getInstanceOf(ilargia);
        if (gm == null) return false;

        String name = ilargia.getName();
        if (breathingPlayers.contains(name)) return false;
        breathingPlayers.add(name);

        boolean isRed = gm.isRedTeam(ilargia);

        ilargia.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
        ilargia.sendMessage(Lang.get(ilargia, Lang.Key.MSG_ILAR_C2_CAST));

        final int[] tick = {0};

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), task -> {
                    tick[0]++;

                    if (tick[0] > 60) {
                        task.cancel();
                        breathingPlayers.remove(name);
                        if (ilargia.isOnline())
                            ilargia.sendMessage(Lang.get(ilargia, Lang.Key.MSG_ILAR_C2_END));
                        return;
                    }

                    if (!ilargia.isOnline() || gm.deadPlayers.contains(ilargia.getUniqueId())) {
                        task.cancel();
                        breathingPlayers.remove(name);
                        return;
                    }

                    Location eye = ilargia.getEyeLocation();
                    Vector forward = eye.getDirection().normalize();

                    spawnFireConeParticles(ilargia.getWorld(), eye, forward);

                    if (tick[0] % 6 == 0) {
                        List<Player> enemies = new ArrayList<>(
                                isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers());
                        for (Player enemy : enemies) {
                            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
                            if (isInCone(eye, forward, enemy)) {
                                double newHp = enemy.getHealth() - 1.0;
                                gm.lastDamager.put(enemy.getUniqueId(), ilargia.getUniqueId());
                                if (newHp <= 0) {
                                    enemy.setHealth(0);
                                } else {
                                    enemy.setHealth(newHp);
                                    enemy.getWorld().playSound(enemy.getLocation(),
                                            Sound.ENTITY_PLAYER_HURT, 0.6f, 1.2f);
                                }
                            }
                        }
                    }
                }, 0L, 1L);

        return true;
    }

    private static boolean isInCone(Location eye, Vector forward, Player target) {
        Vector toTarget = target.getLocation().add(0, 1, 0)
                .toVector().subtract(eye.toVector());
        double dist = toTarget.length();
        if (dist > 8.0) return false;
        double dot = toTarget.normalize().dot(forward);
        return dot >= 0.82;
    }

    private static void spawnFireConeParticles(World world, Location origin,
                                               Vector forward) {
        Random rng = new Random();
        for (int i = 0; i < 12; i++) {
            double t      = 1.0 + rng.nextDouble() * (8.0 - 1.0);
            double spread = t * 0.35;
            double ox     = (rng.nextDouble() - 0.5) * spread;
            double oy     = (rng.nextDouble() - 0.5) * spread;
            double oz     = (rng.nextDouble() - 0.5) * spread;
            Vector perp   = new Vector(ox, oy, oz);
            Location pLoc = origin.clone().add(forward.clone().multiply(t)).add(perp);
            world.spawnParticle(Particle.FLAME, pLoc, 1, 0, 0, 0, 0.01);
            if (rng.nextInt(3) == 0)
                world.spawnParticle(Particle.LAVA, pLoc, 1, 0.1, 0.1, 0.1, 0);
        }
    }

    public static void pedaniPedalo(Player ilargia) {
        GameManager gm = GameRegistry.getInstanceOf(ilargia);
        if (gm == null) return;

        boolean isRed = gm.isRedTeam(ilargia);
        World world   = ilargia.getWorld();
        Vector dir    = ilargia.getEyeLocation().getDirection().clone().normalize();
        Location waveOrigin = ilargia.getLocation().clone().add(0, 1, 0);

        ilargia.sendMessage(Lang.get(ilargia, Lang.Key.MSG_ILAR_UBULT));

        Set<UUID> alreadyHit = new HashSet<>();
        final int[] tick = {0};

        final double WAVE_SPEED  = 1.0;

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), task -> {
                    tick[0]++;

                    if (tick[0] > 20) { task.cancel(); return; }

                    if (!ilargia.isOnline() || gm.deadPlayers.contains(ilargia.getUniqueId())) {
                        task.cancel(); return;
                    }

                    double distAlongAxis = tick[0] * WAVE_SPEED;
                    Location sliceCenter = waveOrigin.clone().add(dir.clone().multiply(distAlongAxis));

                    spawnWaveParticles(world, sliceCenter, dir);

                    List<Player> enemies = new ArrayList<>(
                            isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers());

                    for (Player enemy : enemies) {
                        if (alreadyHit.contains(enemy.getUniqueId())) continue;
                        if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;

                        if (isInWaveSlice(enemy.getLocation().add(0, 1, 0),
                                sliceCenter, dir)) {

                            alreadyHit.add(enemy.getUniqueId());
                            gm.lastDamager.put(enemy.getUniqueId(), ilargia.getUniqueId());
                            double newHp = enemy.getHealth() - 4.0;
                            if (newHp <= 0) {
                                enemy.setHealth(0);
                            } else {
                                enemy.setHealth(newHp);
                            }

                            enemy.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOW_FALLING, 100, 0, false, true));

                            Vector push = dir.clone();
                            push.setY(1.2);
                            push.normalize().multiply(10);
                            push.setY(0.9);
                            enemy.setVelocity(push);

                            enemy.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOWNESS, 100, 2, false, true));

                            enemy.sendMessage(Lang.get(enemy, Lang.Key.MSG_ILAR_UBULT_HIT));
                            world.playSound(enemy.getLocation(),
                                    Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.5f);
                        }
                    }

                }, 0L, 1L);
    }

    private static boolean isInWaveSlice(Location point, Location center,
                                         Vector dir) {
        Vector toPoint = point.toVector().subtract(center.toVector());
        double axial   = toPoint.dot(dir);
        if (Math.abs(axial) > 1.0) return false;
        Vector lateral = toPoint.clone().subtract(dir.clone().multiply(axial));
        return lateral.length() <= 7.5;
    }

    private static void spawnWaveParticles(World world, Location center,
                                           Vector dir) {
        Random rng = new Random();
        Vector up    = new Vector(0, 1, 0);
        Vector perp1 = dir.clone().crossProduct(up).normalize();
        if (perp1.lengthSquared() < 0.01) perp1 = new Vector(1, 0, 0);
        Vector perp2 = dir.clone().crossProduct(perp1).normalize();

        for (int i = 0; i < 20; i++) {
            double a    = rng.nextDouble() * Math.PI * 2;
            double r    = rng.nextDouble() * 7.5;
            Location pLoc = center.clone()
                    .add(perp1.clone().multiply(Math.cos(a) * r))
                    .add(perp2.clone().multiply(Math.sin(a) * r));
            world.spawnParticle(Particle.CLOUD, pLoc, 1, 0.1, 0.1, 0.1, 0.05);
            if (rng.nextInt(4) == 0)
                world.spawnParticle(Particle.SWEEP_ATTACK, pLoc, 1, 0, 0, 0, 0);
        }
    }

    public static void resetRound() {
        breathingPlayers.clear();
        activeClones.values().forEach(l -> l.forEach(c -> { if (c.isValid()) c.remove(); }));
        activeClones.clear();
    }
}