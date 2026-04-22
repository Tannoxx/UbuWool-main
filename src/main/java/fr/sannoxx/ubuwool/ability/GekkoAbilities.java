package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

class GekkoAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { GekkoAbilities.altegoWingman(p); return true; }
    @Override public boolean useC2(Player p, Player t) { GekkoAbilities.vertiFlash(p); return true; }
    @Override public boolean useUltimate(Player p) { GekkoAbilities.monstreEnLiberte(p); return true; }
    @Override public long cooldownC1Ms() { return 40_000L; }
    @Override public long cooldownC2Ms() { return 35_000L; }
    @Override public void resetRound() { GekkoAbilities.resetRound(); }
}

public class GekkoAbilities {

    public static Map<String, Fox> activeFoxes = new HashMap<>();
    private static final Set<UUID> recentlyStolen = new HashSet<>();

    public static void altegoWingman(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        Vector dir = player.getEyeLocation().getDirection().normalize();

        Fireball fb = (Fireball) player.getWorld().spawnEntity(
                player.getEyeLocation().add(dir.clone().multiply(2)), EntityType.FIREBALL);
        fb.setShooter(player);
        fb.setIsIncendiary(false);
        fb.setYield(0);
        fb.setDirection(dir.clone().multiply(3));

        Item display = player.getWorld().dropItem(fb.getLocation(),
                new ItemStack(Material.TNT_MINECART));
        display.setPickupDelay(Integer.MAX_VALUE);
        display.setGravity(false);
        display.setVelocity(new Vector(0, 0, 0));

        boolean isRed = gm.isRedTeam(player);

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), task -> {
                    if (!fb.isValid()) { display.remove(); task.cancel(); return; }
                    display.teleport(fb.getLocation());
                    display.setVelocity(new Vector(0, 0, 0));

                    for (Player enemy : new ArrayList<>(isRed
                            ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
                        if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
                        if (enemy.getLocation().distanceSquared(fb.getLocation()) < 4) {
                            Location pos = fb.getLocation().clone();
                            display.remove(); fb.remove(); task.cancel();
                            explodeNoDamage(player, pos, gm);
                            return;
                        }
                    }
                    Block bp = fb.getLocation().getBlock();
                    if (!bp.getType().isAir()) {
                        Location pos = fb.getLocation().clone();
                        display.remove(); fb.remove(); task.cancel();
                        explodeNoDamage(player, pos, gm);
                    }
                }, 1L, 1L);

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    if (fb.isValid()) {
                        Location pos = fb.getLocation().clone();
                        display.remove(); fb.remove();
                        explodeNoDamage(player, pos, gm);
                    }
                }, 100L);

        player.sendMessage(Lang.get(player, Lang.Key.MSG_GEKK_C1));
    }

    private static void explodeNoDamage(Player owner, Location pos, GameManager gm) {
        pos.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, pos, 1);
        pos.getWorld().playSound(pos, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);

        boolean isRed = gm.isRedTeam(owner);
        for (Player enemy : new ArrayList<>(isRed
                ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
            if (enemy.getLocation().distanceSquared(pos) <= 9) {
                enemy.damage(4, owner);
                gm.lastDamager.put(enemy.getUniqueId(), owner.getUniqueId());
            }
        }
    }

    public static void vertiFlash(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        boolean isRed = gm.isRedTeam(player);
        Location gekkoPos = player.getEyeLocation();

        for (Player enemy : new ArrayList<>(isRed
                ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
            Vector toGekko = gekkoPos.toVector().subtract(enemy.getEyeLocation().toVector()).normalize();
            double dot = toGekko.dot(enemy.getEyeLocation().getDirection().normalize());
            if (dot > 0 && enemy.getLocation().distance(player.getLocation()) < 20) {
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, true));
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0, false, true));
                enemy.sendMessage(Lang.get(enemy, Lang.Key.MSG_GEKK_C2));
            }
        }
        player.sendMessage(Lang.get(player, Lang.Key.MSG_GEKK_C2_1));
    }

    public static void monstreEnLiberte(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        World world = player.getWorld();
        boolean isRed = gm.isRedTeam(player);
        String name = player.getName();

        Fox fox = (Fox) world.spawnEntity(player.getLocation(), EntityType.FOX);
        fox.setCustomName("§a§lMORDICUS");
        fox.setCustomNameVisible(true);
        fox.setPersistent(true);

        try {
            org.bukkit.attribute.AttributeInstance scaleAttr =
                    fox.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
            if (scaleAttr != null) scaleAttr.setBaseValue(3.5);
        } catch (Exception ignored) {}

        List<Player> enemies = new ArrayList<>(isRed
                ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers());
        enemies.removeIf(e -> gm.deadPlayers.contains(e.getUniqueId()));
        if (!enemies.isEmpty())
            fox.setTarget(enemies.get(new Random().nextInt(enemies.size())));

        activeFoxes.put(name, fox);
        player.sendMessage(Lang.get(player, Lang.Key.MSG_GEKK_UBULT));

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), task -> {
                    if (!fox.isValid()) { task.cancel(); activeFoxes.remove(name); return; }
                    if (!player.isOnline() || gm.deadPlayers.contains(player.getUniqueId())) {
                        task.cancel(); activeFoxes.remove(name);
                        if (fox.isValid()) fox.remove();
                        return;
                    }
                    for (Player enemy : new ArrayList<>(isRed
                            ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
                        if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
                        if (fox.getLocation().distanceSquared(enemy.getLocation()) < 12.25) {
                            if (!recentlyStolen.contains(enemy.getUniqueId())) {
                                stealSword(enemy, player);
                            }
                        }
                    }
                }, 4L, 4L);

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    activeFoxes.remove(name);
                    if (fox.isValid()) fox.remove();
                    player.sendMessage(Lang.get(player, Lang.Key.MSG_GEKK_UBULT_1));
                }, 400L);
    }

    private static void stealSword(Player victim, Player gekko) {
        for (int i = 0; i < victim.getInventory().getSize(); i++) {
            ItemStack s = victim.getInventory().getItem(i);
            if (s == null || !s.getType().name().endsWith("_SWORD")) continue;

            final ItemStack original = s.clone();
            victim.getInventory().setItem(i, null);
            victim.sendMessage(Lang.get(victim, Lang.Key.MSG_GEKK_UBULT_2));

            recentlyStolen.add(victim.getUniqueId());
            UbuWool.getInstance().getServer().getScheduler()
                    .runTaskLater(UbuWool.getInstance(),
                            () -> recentlyStolen.remove(victim.getUniqueId()), 200L);

            final String stealTag = "§0STOLEN_" + victim.getName() + "_" + System.currentTimeMillis();
            ItemStack tagged = original.clone();
            ItemMeta meta = tagged.getItemMeta();
            if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(tagged.getType());
            meta.setDisplayName(stealTag);
            tagged.setItemMeta(meta);
            gekko.getInventory().addItem(tagged);

            UbuWool.getInstance().getServer().getScheduler()
                    .runTaskLater(UbuWool.getInstance(), () -> {
                        for (int j = 0; j < gekko.getInventory().getSize(); j++) {
                            ItemStack gs = gekko.getInventory().getItem(j);
                            if (gs == null || !gs.hasItemMeta()) continue;
                            if (gs.getItemMeta().hasDisplayName()
                                    && gs.getItemMeta().getDisplayName().equals(stealTag)) {
                                gekko.getInventory().setItem(j, null);
                                break;
                            }
                        }
                        victim.getInventory().addItem(original);
                        victim.sendMessage(Lang.get(victim, Lang.Key.MSG_GEKK_UBULT_3));
                    }, 180L);
            break;
        }
    }

    public static void resetRound() {
        activeFoxes.values().forEach(f -> { if (f.isValid()) f.remove(); });
        activeFoxes.clear();
        recentlyStolen.clear();
    }
}