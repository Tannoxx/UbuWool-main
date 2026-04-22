package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

class HijabAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { HijabAbilities.avecOuSansChapeau(p); return true; }
    @Override public boolean useC2(Player p, Player t) { HijabAbilities.danKyojur(p); return true; }
    @Override public boolean useUltimate(Player p) { HijabAbilities.villageDesPoteau(p); return true; }
    @Override public long cooldownC1Ms() { return 1_000L; }
    @Override public long cooldownC2Ms() { return 50_000L; }
    @Override public void resetRound() { HijabAbilities.resetRound(); }
}

public class HijabAbilities {

    public static Map<String, Boolean> hatMode = new HashMap<>();
    public static Map<String, Long> fireAspectActive = new HashMap<>();
    public static Map<String, List<Entity>> villageZombies = new HashMap<>();
    public static Map<String, List<Entity>> villageVillagers = new HashMap<>();
    public static Map<String, List<Location>> villagePosts = new HashMap<>();
    private static final Map<String, Integer> villageZombieTimers = new HashMap<>();
    private static final Map<String, Integer> villageRetargetTimers = new HashMap<>();

    private static final String[] BABY_NAMES = {
            "Antiguaetvaloran2", "Antiguaetopenfron3", "Darryl", "Andgelo", "Arha Chakrabarti",
            "Dorkan Van Dorkan", "Bus?", "Inkiete", "Leon", "Daijoubou", "Stéphane Bouille",
            "Nicky Minaj Anaconda", "Samuel Étienne", "Brakav", "Drixav", "Lucas Van Stylo",
            "Fonetaine", "Cactus", "Lakol", "Karl Marx", "Marshal petit bouclier", "M. Osirb",
            "Mme. Uchiwa", "Le Mage", "Pipikiwi", "Godgift", "Sebastian", "Nibag", "Yrogerg",
            "Tomas De la Cruz"
    };

    public static void showTargetHealth(Player hijab, Player target) {
        String bar = "§c❤ " + String.format("%.1f", target.getHealth()) + " / " + target.getMaxHealth();
        hijab.sendActionBar(Component.text(bar));
    }

    public static void avecOuSansChapeau(Player player) {
        String name = player.getName();
        boolean cur = hatMode.getOrDefault(name, false);
        boolean newMode = !cur;
        hatMode.put(name, newMode);

        if (newMode) {
            player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 99999, 0, false, false));
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.sendMessage(Lang.get(player, Lang.Key.MSG_HIJA_C1));
        } else {
            player.getInventory().setHelmet(null);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 99999, 0, false, false));
            player.sendMessage(Lang.get(player, Lang.Key.MSG_HIJA_C1_1));
        }
    }

    public static void danKyojur(Player player) {
        player.getWorld().playSound(player.getLocation(),
                Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 0.3f, 2.0f);
        fireAspectActive.put(player.getName(), System.currentTimeMillis() + 4000);
        player.sendMessage(Lang.get(player, Lang.Key.MSG_HIJA_C2));
    }

    public static boolean hasFireAspect(Player player) {
        Long exp = fireAspectActive.get(player.getName());
        return exp != null && System.currentTimeMillis() < exp;
    }

    public static void villageDesPoteau(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        World world = player.getWorld();
        String name = player.getName();
        boolean isRed = gm.isRedTeam(player);

        List<Entity> villagers = new ArrayList<>();
        List<Location> posts = new ArrayList<>();
        Location center = player.getLocation();
        Random rng = new Random();

        for (int a = 0; a < 80; a++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = 5 + rng.nextDouble() * 25;
            int bx = (int)(center.getX() + Math.cos(angle) * dist);
            int bz = (int)(center.getZ() + Math.sin(angle) * dist);

            Block ground = null;
            for (int y = (int)center.getY(); y > (int)center.getY() - 10; y--) {
                Block check = world.getBlockAt(bx, y, bz);
                Block above = world.getBlockAt(bx, y + 1, bz);
                if (check.getType().isSolid() && above.getType().isAir()) { ground = check; break; }
            }
            if (ground == null) continue;

            int by = ground.getY();
            for (int h = 1; h <= 3; h++) {
                Block fp = world.getBlockAt(bx, by + h, bz);
                if (fp.getType().isAir()) { fp.setType(Material.JUNGLE_FENCE); posts.add(fp.getLocation()); }
            }
            Villager v = (Villager) world.spawnEntity(
                    new Location(world, bx + 0.5, by + 1, bz + 0.5), EntityType.VILLAGER);
            v.setBaby(); v.setPersistent(true); villagers.add(v);
        }

        villagePosts.put(name, posts);
        villageVillagers.put(name, villagers);

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 0, false, true));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_HIJA_UBULT));

        List<Entity> zombies = new ArrayList<>();
        villageZombies.put(name, zombies);

        final int[] counter = {0};
        final BukkitTask[] taskHolder = {null};

        taskHolder[0] = UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), () -> {
                    if (counter[0] >= 10 || gm.state != GameManager.GameState.ROUND_ACTIVE) {
                        if (taskHolder[0] != null) taskHolder[0].cancel();
                        villageZombieTimers.remove(name);
                        UbuWool.getInstance().getServer().getScheduler()
                                .runTaskLater(UbuWool.getInstance(), () -> cleanVillage(name, world), 60L);
                        return;
                    }
                    counter[0]++;

                    List<Player> enemies = new ArrayList<>(isRed
                            ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers());
                    enemies.removeIf(e -> gm.deadPlayers.contains(e.getUniqueId()));

                    Location spawnLoc = player.getLocation().clone();
                    Player targetEnemy = null;
                    double closestDist = Double.MAX_VALUE;
                    for (Player e : enemies) {
                        double d = e.getLocation().distanceSquared(player.getLocation());
                        if (d < closestDist) { closestDist = d; targetEnemy = e; }
                    }
                    if (targetEnemy != null) {
                        spawnLoc = targetEnemy.getLocation().clone()
                                .add((rng.nextDouble() - 0.5) * 2, 0, (rng.nextDouble() - 0.5) * 2);
                    }

                    Zombie z = (Zombie) world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
                    z.setBaby(true);
                    z.setCustomName("§5" + BABY_NAMES[rng.nextInt(BABY_NAMES.length)]);
                    z.setCustomNameVisible(true);
                    z.setPersistent(true);
                    z.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));

                    if (targetEnemy != null) z.setTarget(targetEnemy);

                    zombies.add(z);
                }, 60L, 60L);

        villageZombieTimers.put(name, taskHolder[0].getTaskId());

        BukkitTask retargetTask = UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), () -> {
                    if (gm.state != GameManager.GameState.ROUND_ACTIVE) return;
                    List<Entity> currentZombies = villageZombies.get(name);
                    if (currentZombies == null || currentZombies.isEmpty()) return;

                    List<Player> aliveEnemies = new ArrayList<>(isRed
                            ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers());
                    aliveEnemies.removeIf(e -> gm.deadPlayers.contains(e.getUniqueId()));
                    if (aliveEnemies.isEmpty()) return;

                    for (Entity entity : new ArrayList<>(currentZombies)) {
                        if (!entity.isValid()) continue;
                        Zombie z = (Zombie) entity;
                        Player nearest = null;
                        double minDist = Double.MAX_VALUE;
                        for (Player enemy : aliveEnemies) {
                            double d = z.getLocation().distanceSquared(enemy.getLocation());
                            if (d < minDist) { minDist = d; nearest = enemy; }
                        }
                        if (nearest != null) z.setTarget(nearest);
                    }
                }, 20L, 20L);

        villageRetargetTimers.put(name, retargetTask.getTaskId());
    }

    public static void cleanVillage(String playerName, World world) {
        Integer retargetId = villageRetargetTimers.remove(playerName);
        if (retargetId != null) Bukkit.getScheduler().cancelTask(retargetId);

        List<Entity> zs = villageZombies.remove(playerName);
        if (zs != null) zs.forEach(z -> { if (z.isValid()) z.remove(); });
        List<Entity> vs = villageVillagers.remove(playerName);
        if (vs != null) vs.forEach(v -> { if (v.isValid()) v.remove(); });
        List<Location> ps = villagePosts.remove(playerName);
        if (ps != null) ps.forEach(l -> {
            if (l.getBlock().getType() == Material.JUNGLE_FENCE) l.getBlock().setType(Material.AIR);
        });
    }

    public static void resetRound() {
        hatMode.clear();
        fireAspectActive.clear();
        villageZombieTimers.values().forEach(id -> Bukkit.getScheduler().cancelTask(id));
        villageZombieTimers.clear();
        villageRetargetTimers.values().forEach(id -> Bukkit.getScheduler().cancelTask(id));
        villageRetargetTimers.clear();
        villageZombies.values().forEach(l -> l.forEach(z -> { if (z.isValid()) z.remove(); }));
        villageZombies.clear();
        villagePosts.clear();
        villageVillagers.values().forEach(l -> l.forEach(v -> { if (v.isValid()) v.remove(); }));
        villageVillagers.clear();
    }
}