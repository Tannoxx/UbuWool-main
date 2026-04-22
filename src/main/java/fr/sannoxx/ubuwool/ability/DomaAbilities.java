package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

class DomaAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { return DomaAbilities.frozenSlash(p); }
    @Override public boolean useC2(Player p, Player t) { return DomaAbilities.blizziBlizzaroi(p); }
    @Override public boolean useUltimate(Player p) { DomaAbilities.sherbetLand(p); return true; }
    @Override public long cooldownC1Ms() { return 40_000L; }
    @Override public long cooldownC2Ms() { return 40_000L; }
    @Override public void resetRound() { DomaAbilities.resetRound(); }
}

public class DomaAbilities {

    public static final Map<String, Integer> hitCounter = new HashMap<>();
    private static final Set<String>             ultActive = new HashSet<>();
    private static final Map<String, BukkitTask> ultTasks  = new HashMap<>();

    private static final NamespacedKey SIZE_KEY =
            new NamespacedKey("ubuwool", "doma_ult_scale");
    private static final NamespacedKey HEALTH_KEY =
            new NamespacedKey("ubuwool", "doma_ult_health");

    private static final Map<String, List<org.bukkit.block.Block>> iceBlocks = new HashMap<>();

    public static boolean isUltActive(Player p) {
        return ultActive.contains(p.getName());
    }

    public static int passiveThreshold(Player p) {
        return isUltActive(p) ? 5 : 10;
    }

    public static void onHit(Player doma, Player victim) {
        String name  = doma.getName();
        int    count = hitCounter.getOrDefault(name, 0) + 1;

        if (count >= passiveThreshold(doma)) {
            hitCounter.put(name, 0);
            applyFrozenHit(victim);
            spawnFrozenHitParticles(victim.getLocation());
        } else {
            hitCounter.put(name, count);
        }
        updatePassiveBar(doma);
    }

    private static void applyFrozenHit(Player target) {
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.MINING_FATIGUE, 60, 1, false, true));
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 60, 1, false, true));
        target.getWorld().playSound(target.getLocation(),
                Sound.BLOCK_GLASS_BREAK, 0.8f, 1.4f);
    }

    private static void spawnFrozenHitParticles(Location loc) {
        loc.getWorld().spawnParticle(
                Particle.SNOWFLAKE, loc.clone().add(0, 1, 0),
                20, 0.4, 0.4, 0.4, 0.05);
    }

    public static void updatePassiveBar(Player doma) {
        int count     = hitCounter.getOrDefault(doma.getName(), 0);
        int threshold = passiveThreshold(doma);
        int filled    = (int) Math.round((double) count / threshold * 10);

        StringBuilder bar = new StringBuilder("§b❄ §f[");
        for (int i = 0; i < 10; i++)
            bar.append(i < filled ? "§b◎" : "§8◉");
        bar.append("§f] §7").append(count).append("/").append(threshold);

        doma.sendActionBar(net.kyori.adventure.text.Component.text(bar.toString()));
    }

    public static boolean frozenSlash(Player doma) {
        GameManager gm = GameRegistry.getInstanceOf(doma);
        if (gm == null) return false;

        boolean isRed = gm.isRedTeam(doma);
        Location eye  = doma.getEyeLocation();

        float yawRad    = (float) Math.toRadians(eye.getYaw());
        Vector hForward = new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();

        final double SLASH_DIST   = 4.0;
        final double SLASH_RADIUS = 3.0;

        Location slashCenter = doma.getLocation().clone()
                .add(hForward.clone().multiply(SLASH_DIST))
                .add(0, 1.0, 0);

        spawnFrozenSlashParticles(doma.getWorld(), slashCenter, hForward, SLASH_RADIUS);

        doma.getWorld().playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.7f);
        doma.getWorld().playSound(eye, Sound.BLOCK_POWDER_SNOW_BREAK, 0.8f, 1.2f);

        List<Player> enemies = new ArrayList<>(
                isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers());
        boolean hit = false;

        for (Player enemy : enemies) {
            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;

            double dx = enemy.getLocation().getX() - slashCenter.getX();
            double dz = enemy.getLocation().getZ() - slashCenter.getZ();
            if (Math.sqrt(dx * dx + dz * dz) > SLASH_RADIUS) continue;

            hit = true;
            gm.lastDamager.put(enemy.getUniqueId(), doma.getUniqueId());
            double newHp = enemy.getHealth() - 4.0;
            if (newHp <= 0) {
                enemy.setHealth(0);
            } else {
                enemy.setHealth(newHp);
                enemy.addPotionEffect(new PotionEffect(
                        PotionEffectType.POISON, 100, 0, false, true));
                enemy.getWorld().playSound(enemy.getLocation(),
                        Sound.BLOCK_GLASS_BREAK, 0.6f, 1.6f);
                enemy.sendMessage(Lang.get(enemy, Lang.Key.MSG_DOMA_C1_HIT));
            }
        }

        doma.sendMessage(hit
                ? Lang.get(doma, Lang.Key.MSG_DOMA_C1_CAST)
                : Lang.get(doma, Lang.Key.MSG_DOMA_C1_NO_HIT));
        return true;
    }

    private static void spawnFrozenSlashParticles(World world, Location center,
                                                  Vector hForward, double radius) {
        Random rng = new Random();

        Vector up   = new Vector(0, 1, 0);
        Vector perp = hForward.clone().crossProduct(up).normalize();

        Particle.DustOptions coldDust = new Particle.DustOptions(
                Color.fromRGB(140, 215, 255), 1.0f);

        double[] heights = { -0.6, 0.0, 0.6 };

        for (double dy : heights) {
            Location rowCenter = center.clone().add(0, dy, 0);
            int linePoints = 24;

            for (int i = 0; i <= linePoints; i++) {
                double t = -radius + i * (2.0 * radius / linePoints);
                Location pLoc = rowCenter.clone().add(perp.clone().multiply(t));
                pLoc.add(hForward.clone().multiply(rng.nextGaussian() * 0.15));

                world.spawnParticle(Particle.SNOWFLAKE, pLoc, 1, 0.03, 0.04, 0.03, 0.01);

                if (i == 0 || i == linePoints / 2 || i == linePoints)
                    world.spawnParticle(Particle.SWEEP_ATTACK, pLoc, 1, 0, 0, 0, 0);
            }
        }

        for (int i = 0; i < 14; i++) {
            double t  = (rng.nextDouble() * 2 - 1) * radius;
            double dy = (rng.nextDouble() - 0.5) * 1.2;
            Location pLoc = center.clone()
                    .add(perp.clone().multiply(t))
                    .add(0, dy, 0);
            world.spawnParticle(Particle.ITEM_SNOWBALL, pLoc, 1,
                    hForward.getX() * 0.15, 0.08, hForward.getZ() * 0.15, 0.12);
        }

        for (int i = 0; i < 12; i++) {
            double t  = (rng.nextDouble() * 2 - 1) * radius;
            double dy = (rng.nextDouble() - 0.5) * 1.0;
            Location pLoc = center.clone()
                    .add(perp.clone().multiply(t))
                    .add(hForward.clone().multiply(rng.nextGaussian() * 0.2))
                    .add(0, dy, 0);
            world.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, coldDust);
        }
    }

    public static boolean blizziBlizzaroi(Player doma) {
        GameManager gm = GameRegistry.getInstanceOf(doma);
        if (gm == null) return false;

        boolean isRed = gm.isRedTeam(doma);
        Vector  dir   = doma.getEyeLocation().getDirection();

        Player target  = null;
        double closest = Double.MAX_VALUE;
        for (Player enemy : new ArrayList<>(
                isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
            org.bukkit.util.RayTraceResult r = enemy.getBoundingBox().expand(1.0)
                    .rayTrace(doma.getEyeLocation().toVector(), dir, 20);
            if (r != null) {
                double d = enemy.getLocation().distanceSquared(doma.getLocation());
                if (d < closest) { closest = d; target = enemy; }
            }
        }

        if (target == null) {
            doma.sendMessage(Lang.get(doma, Lang.Key.MSG_DOMA_C2_NO_TARGET));
            return false;
        }

        final Player finalTarget = target;
        final String targetName  = finalTarget.getName();

        doma.sendMessage(Lang.get(doma, Lang.Key.MSG_DOMA_C2_CAST, finalTarget.getName()));
        finalTarget.sendMessage(Lang.get(finalTarget, Lang.Key.MSG_DOMA_C2_FROZEN));

        int bx = finalTarget.getLocation().getBlockX();
        int by = finalTarget.getLocation().getBlockY();
        int bz = finalTarget.getLocation().getBlockZ();
        World w = finalTarget.getWorld();

        Location cageCenter = new Location(w, bx + 0.5, by, bz + 0.5,
                finalTarget.getLocation().getYaw(), finalTarget.getLocation().getPitch());
        finalTarget.teleport(cageCenter);

        int[][] cageOffsets = {
                { 1, 0,  0}, {-1, 0,  0}, { 0, 0,  1}, { 0, 0, -1},
                { 1, 1,  0}, {-1, 1,  0}, { 0, 1,  1}, { 0, 1, -1},
                { 0, 2,  0}, { 0, -1,  0}
        };
        int[][] suffocateOffsets = {
                { 0, 0, 0},  // pieds
                { 0, 1, 0}   // tête
        };

        List<org.bukkit.block.Block> placed = new ArrayList<>();

        for (int[] off : cageOffsets) {
            org.bukkit.block.Block b = w.getBlockAt(bx + off[0], by + off[1], bz + off[2]);
            if (b.isEmpty() || b.isPassable()) {
                b.setType(Material.BARRIER);
                placed.add(b);
            }
        }
        for (int[] off : suffocateOffsets) {
            org.bukkit.block.Block b = w.getBlockAt(bx + off[0], by + off[1], bz + off[2]);
            if (b.isEmpty() || b.isPassable()) {
                b.setType(Material.ICE);
                placed.add(b);
            }
        }
        iceBlocks.put(targetName, placed);

        finalTarget.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 60, 254, false, false));
        finalTarget.addPotionEffect(new PotionEffect(
                PotionEffectType.MINING_FATIGUE, 60, 254, false, false));
        finalTarget.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP_BOOST, 60, 254, false, false));

        doma.getWorld().playSound(finalTarget.getLocation(),
                Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
        finalTarget.getWorld().spawnParticle(Particle.SNOWFLAKE,
                finalTarget.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.1);

        final double centerX = bx + 0.5;
        final double centerZ = bz + 0.5;

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), recenterTask -> {
                    if (!finalTarget.isOnline()
                            || gm.deadPlayers.contains(finalTarget.getUniqueId())
                            || !iceBlocks.containsKey(targetName)) {
                        recenterTask.cancel();
                        return;
                    }
                    Location loc = finalTarget.getLocation();
                    if (Math.abs(loc.getX() - centerX) > 0.3 || Math.abs(loc.getZ() - centerZ) > 0.3) {
                        Location corrected = new Location(w, centerX, loc.getY(), centerZ,
                                loc.getYaw(), loc.getPitch());
                        finalTarget.teleport(corrected);
                    }
                    finalTarget.setVelocity(new org.bukkit.util.Vector(0, -0.1, 0));
                }, 0L, 1L);

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(),
                        () -> releaseIce(targetName, finalTarget, gm), 60L);

        return true;
    }

    private static void releaseIce(String targetName, Player target, GameManager gm) {
        List<org.bukkit.block.Block> blocks = iceBlocks.remove(targetName);
        if (blocks != null)
            blocks.forEach(b -> {
                if (b.getType() == Material.BARRIER || b.getType() == Material.ICE)
                    b.setType(Material.AIR);
            });

        if (target.isOnline() && !gm.deadPlayers.contains(target.getUniqueId())) {
            target.removePotionEffect(PotionEffectType.SLOWNESS);
            target.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            target.removePotionEffect(PotionEffectType.JUMP_BOOST);
            target.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            target.sendMessage(Lang.get(target, Lang.Key.MSG_DOMA_C2_RELEASED));
            target.getWorld().playSound(target.getLocation(),
                    Sound.BLOCK_GLASS_BREAK, 0.8f, 1.8f);
        }
    }

    public static void sherbetLand(Player doma) {
        GameManager gm = GameRegistry.getInstanceOf(doma);
        if (gm == null) return;

        String name = doma.getName();
        if (ultActive.contains(name)) return;
        ultActive.add(name);

        var healthAttr = doma.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.getModifiers().stream()
                    .filter(m -> m.getKey().equals(HEALTH_KEY))
                    .forEach(healthAttr::removeModifier);
            healthAttr.addModifier(new AttributeModifier(
                    HEALTH_KEY, 10.0, AttributeModifier.Operation.ADD_NUMBER));
        }
        double newMax = doma.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        doma.setHealth(Math.min(doma.getHealth() + 10.0, newMax));

        var scaleAttr = doma.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttr != null) {
            scaleAttr.getModifiers().stream()
                    .filter(m -> m.getKey().equals(SIZE_KEY))
                    .forEach(scaleAttr::removeModifier);
            scaleAttr.addModifier(new AttributeModifier(
                    SIZE_KEY, 0.2, AttributeModifier.Operation.ADD_SCALAR));
        }

        doma.sendMessage(Lang.get(doma, Lang.Key.MSG_DOMA_UBULT));

        doma.getWorld().playSound(doma.getLocation(),
                Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.8f);
        doma.getWorld().spawnParticle(Particle.SNOWFLAKE,
                doma.getLocation().add(0, 1, 0), 80, 1, 1, 1, 0.2);
    }

    public static void resetRound() {
        hitCounter.clear();

        ultTasks.values().forEach(BukkitTask::cancel);
        ultTasks.clear();

        for (String name : new HashSet<>(ultActive)) {
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) {
                var scaleAttr = p.getAttribute(Attribute.GENERIC_SCALE);
                if (scaleAttr != null) {
                    scaleAttr.getModifiers().stream()
                            .filter(m -> m.getKey().equals(SIZE_KEY))
                            .forEach(scaleAttr::removeModifier);
                }
                var healthAttr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (healthAttr != null) {
                    healthAttr.getModifiers().stream()
                            .filter(m -> m.getKey().equals(HEALTH_KEY))
                            .forEach(healthAttr::removeModifier);
                }
                double naturalMax = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                if (p.getHealth() > naturalMax) p.setHealth(naturalMax);
            }
        }
        ultActive.clear();

        iceBlocks.forEach((n, blocks) ->
                blocks.forEach(b -> { if (b.getType() == Material.ICE) b.setType(Material.AIR); }));
        iceBlocks.clear();
    }
}