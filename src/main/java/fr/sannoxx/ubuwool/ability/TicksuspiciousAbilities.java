package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

class TicksuspiciousAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { return TicksuspiciousAbilities.mineExplosive(p); }
    @Override public boolean useC2(Player p, Player t) { return TicksuspiciousAbilities.teteChercheusse(p); }
    @Override public boolean useUltimate(Player p) { TicksuspiciousAbilities.operationPernicious(p); return true; }
    @Override public long cooldownC1Ms() { return 20_000L; }
    @Override public long cooldownC2Ms() { return 50_000L; }
    @Override public void resetRound() { TicksuspiciousAbilities.resetRound(); }
}

public class TicksuspiciousAbilities {

    public static Map<UUID, Integer>  hitCounters      = new HashMap<>();
    public static Map<UUID, Location> mines            = new HashMap<>();
    public static Map<Integer, UUID>  activeSilverfish = new HashMap<>();

    public static void initHitCounter(Player player)  { hitCounters.put(player.getUniqueId(), 0); }
    public static void clearHitCounter(Player player) { hitCounters.remove(player.getUniqueId()); }

    public static void onHitReceived(Player player) {
        if (!hitCounters.containsKey(player.getUniqueId())) return;
        int count = hitCounters.merge(player.getUniqueId(), 1, Integer::sum);
        if (count >= 10) {
            hitCounters.put(player.getUniqueId(), 0);
            triggerPassiveExplosion(player);
        }
    }

    private static void triggerPassiveExplosion(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        Location loc = player.getLocation();
        boolean isRed = gm.isRedTeam(player);
        for (Player nearby : new ArrayList<>(player.getServer().getOnlinePlayers())) {
            if (nearby == player) continue;
            if (!gm.playerDataMap.containsKey(nearby.getUniqueId())) continue;
            if (gm.deadPlayers.contains(nearby.getUniqueId())) continue;
            if (gm.isRedTeam(nearby) == isRed) continue;
            if (nearby.getLocation().distance(loc) > 3) continue;
            nearby.damage(2, player);
            gm.lastDamager.put(nearby.getUniqueId(), player.getUniqueId());
        }
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
    }

    public static boolean mineExplosive(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return false;

        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), 5);
        Location target;
        if (result != null && result.getHitBlock() != null) {
            assert result.getHitBlockFace() != null;
            target = result.getHitBlock().getRelative(result.getHitBlockFace()).getLocation();
        } else {
            player.sendMessage(Lang.get(player, Lang.Key.MSG_TICK_C1));
            return false;
        }
        Block block = target.getBlock();
        if (!block.getType().isAir()) {
            player.sendMessage(Lang.get(player, Lang.Key.MSG_TICK_C1_1));
            return false;
        }

        final UUID mineId = UUID.randomUUID();
        final Location mineLocation = target.clone();
        mines.put(mineId, mineLocation);
        player.sendMessage(Lang.get(player, Lang.Key.MSG_TICK_C1_2));

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), particleTask -> {
                    if (!mines.containsKey(mineId)) { particleTask.cancel(); return; }
                    if (!player.isOnline()) { particleTask.cancel(); return; }
                    Location center = mineLocation.clone().add(0.5, 0.1, 0.5);
                    for (int deg = 0; deg < 360; deg += 30) {
                        double rad = Math.toRadians(deg);
                        double dx = Math.cos(rad) * 0.6;
                        double dz = Math.sin(rad) * 0.6;
                        player.spawnParticle(Particle.DUST,
                                center.clone().add(dx, 0, dz), 1, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.RED, 1.2f));
                    }
                    player.spawnParticle(Particle.DUST, center, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.ORANGE, 1.0f));
                }, 0L, 5L);

        boolean isRed = gm.isRedTeam(player);
        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), task -> {
                    if (!mines.containsKey(mineId)) { task.cancel(); return; }
                    for (Player enemy : new ArrayList<>(isRed
                            ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
                        if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
                        if (enemy.getLocation().distanceSquared(mineLocation) < 2.25) {
                            mines.remove(mineId);
                            task.cancel();
                            for (Player t : new ArrayList<>(Bukkit.getOnlinePlayers())) {
                                if (!gm.playerDataMap.containsKey(t.getUniqueId())) continue;
                                if (gm.deadPlayers.contains(t.getUniqueId())) continue;
                                if (t == player) continue;
                                if (gm.isRedTeam(t) == isRed) continue;
                                if (t.getLocation().distanceSquared(mineLocation) > 6.25) continue;
                                t.damage(6, player);
                                gm.lastDamager.put(t.getUniqueId(), player.getUniqueId());
                                t.sendMessage(Lang.get(t, Lang.Key.MSG_TICK_MINE_HIT));
                            }
                            mineLocation.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, mineLocation, 1);
                            mineLocation.getWorld().playSound(mineLocation, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1f);
                            return;
                        }
                    }
                }, 2L, 2L);

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> mines.remove(mineId), 6800L);
        return true;
    }

    public static boolean teteChercheusse(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return false;
        boolean isRed = gm.isRedTeam(player);
        List<Player> aliveEnemies = new ArrayList<>(isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers());
        aliveEnemies.removeIf(e -> gm.deadPlayers.contains(e.getUniqueId()));
        if (aliveEnemies.isEmpty()) {
            player.sendMessage(Lang.get(player, Lang.Key.MSG_TICK_C2));
            return false;
        }

        for (int i = 0; i < 3; i++) {
            Player finalTarget = aliveEnemies.get(i % aliveEnemies.size());

            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(
                    player.getLocation().add((Math.random() - 0.5), 0.5, (Math.random() - 0.5)),
                    EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setSmall(true);
            activeSilverfish.put(stand.getEntityId(), player.getUniqueId());

            UbuWool.getInstance().getServer().getScheduler()
                    .runTaskTimer(UbuWool.getInstance(), task -> {
                        if (!stand.isValid() || !activeSilverfish.containsKey(stand.getEntityId())) {
                            task.cancel(); return;
                        }
                        if (gm.deadPlayers.contains(finalTarget.getUniqueId()) || !finalTarget.isOnline()) {
                            stand.remove(); activeSilverfish.remove(stand.getEntityId()); task.cancel(); return;
                        }

                        Location targetLoc = finalTarget.getLocation().add(0, 0.5, 0);
                        Location standLoc  = stand.getLocation();
                        Vector dir = targetLoc.toVector().subtract(standLoc.toVector()).normalize();
                        stand.teleport(standLoc.add(dir.multiply(0.5)));
                        stand.getWorld().spawnParticle(Particle.ITEM_SLIME, stand.getLocation(), 2, 0.1, 0.1, 0.1, 0.01);

                        if (finalTarget.getLocation().distanceSquared(stand.getLocation()) < 2.25) {
                            stand.remove(); activeSilverfish.remove(stand.getEntityId()); task.cancel();
                            finalTarget.damage(4.0, player);
                            finalTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
                            gm.lastDamager.put(finalTarget.getUniqueId(), player.getUniqueId());
                            finalTarget.sendMessage(Lang.get(finalTarget, Lang.Key.MSG_TICK_SEEKER_HIT));
                            stand.getWorld().spawnParticle(Particle.EXPLOSION, stand.getLocation(), 1);
                            stand.getWorld().playSound(stand.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
                            return;
                        }

                        for (Player otherEnemy : new ArrayList<>(isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
                            if (otherEnemy == finalTarget) continue;
                            if (gm.deadPlayers.contains(otherEnemy.getUniqueId())) continue;
                            if (otherEnemy.getLocation().distanceSquared(stand.getLocation()) < 2.25) {
                                stand.remove(); activeSilverfish.remove(stand.getEntityId()); task.cancel();
                                otherEnemy.damage(4.0, player);
                                otherEnemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
                                gm.lastDamager.put(otherEnemy.getUniqueId(), player.getUniqueId());
                                otherEnemy.sendMessage(Lang.get(otherEnemy, Lang.Key.MSG_TICK_SEEKER_HIT));
                                stand.getWorld().spawnParticle(Particle.EXPLOSION, stand.getLocation(), 1);
                                stand.getWorld().playSound(stand.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
                                return;
                            }
                        }
                    }, 1L, 1L);
        }
        player.sendMessage(Lang.get(player, Lang.Key.MSG_TICK_C2_1));
        return true;
    }

    public static void operationPernicious(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,   200, 2, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0, false, true));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_TICK_UBULT));

        final int[] secondes = {10};
        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), countdownTask -> {
                    if (secondes[0] <= 0) { countdownTask.cancel(); return; }
                    int s = secondes[0];
                    String couleur = s > 6 ? "§e" : s > 3 ? "§6" : "§c";
                    player.sendActionBar(Component.text("§c§l💥 " + couleur + "§l" + s + "s §c§l💥"));
                    float pitch = 0.5f + (10 - s) * 0.08f;
                    int beepsThisSec = (s <= 3) ? 3 : (s <= 6) ? 2 : 1;
                    Location playerLoc = player.getLocation();
                    for (Player nearby : Bukkit.getOnlinePlayers()) {
                        if (nearby.getLocation().distanceSquared(playerLoc) > 256) continue;
                        for (int b = 0; b < beepsThisSec; b++)
                            nearby.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, pitch + b * 0.1f);
                    }
                    secondes[0]--;
                }, 0L, 20L);

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    Location pos = player.getLocation();
                    pos.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, pos, 3, 1, 1, 1, 0);
                    pos.getWorld().playSound(pos, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.8f);

                    int r = 15;
                    for (int x = (int)pos.getX() - r; x <= (int)pos.getX() + r; x++) {
                        for (int y = (int)pos.getY() - r; y <= (int)pos.getY() + r; y++) {
                            for (int z = (int)pos.getZ() - r; z <= (int)pos.getZ() + r; z++) {
                                Block b = pos.getWorld().getBlockAt(x, y, z);
                                if (b.getType() == Material.RED_WOOL || b.getType() == Material.BLUE_WOOL
                                        || b.getType() == Material.WHITE_WOOL || b.getType() == Material.PINK_WOOL)
                                    b.setType(Material.AIR);
                            }
                        }
                    }

                    boolean playerIsRed = gm.isRedTeam(player);
                    for (Player target : new ArrayList<>(Bukkit.getOnlinePlayers())) {
                        if (target == player) continue;
                        if (!gm.playerDataMap.containsKey(target.getUniqueId())) continue;
                        if (gm.deadPlayers.contains(target.getUniqueId())) continue;
                        if (gm.isRedTeam(target) == playerIsRed) continue;
                        if (target.getLocation().distanceSquared(pos) > 36) continue;
                        gm.lastDamager.put(target.getUniqueId(), player.getUniqueId());
                        target.sendMessage(Lang.get(target, Lang.Key.MSG_TICK_PERNICIOUS_HIT));
                        target.setHealth(0);
                    }
                }, 200L);
    }

    public static void resetRound() {
        mines.clear();
        for (int entityId : new ArrayList<>(activeSilverfish.keySet())) {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                for (org.bukkit.entity.Entity e : w.getEntities()) {
                    if (e.getEntityId() == entityId && e instanceof ArmorStand) {
                        e.remove();
                        break;
                    }
                }
            }
        }
        activeSilverfish.clear();
        hitCounters.clear();
    }
}