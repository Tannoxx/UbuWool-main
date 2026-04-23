package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import fr.sannoxx.ubuwool.manager.MapConfig;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.util.*;

class BambouvoreAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { return BambouvoreAbilities.murailleDeBambou(p); }
    @Override public boolean useC2(Player p, Player t) { BambouvoreAbilities.potDeVin(p); return true; }
    @Override public boolean useUltimate(Player p) { BambouvoreAbilities.arriveeDuPrintemps(p); return true; }
    @Override public long cooldownC1Ms() { return 35_000L; }
    @Override public long cooldownC2Ms() { return 45_000L; }
    @Override public void resetRound() { }
}

public class BambouvoreAbilities {

    public static boolean murailleDeBambou(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return false;
        RayTraceResult hit = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 15);
        if (hit == null || hit.getHitBlock() == null) { player.sendMessage(Lang.get(player, Lang.Key.MSG_BAMB_C1)); return false; }
        Location target = hit.getHitBlock().getLocation();
        Vector dir = player.getEyeLocation().getDirection().normalize();
        boolean alongX = Math.abs(dir.getX()) < Math.abs(dir.getZ());
        List<Location> wallBlocks = new ArrayList<>();
        for (int col = -2; col <= 2; col++) for (int row = 0; row <= 4; row++) {
            Location bp = alongX ? target.clone().add(col, row, 0) : target.clone().add(0, row, col);
            if (bp.getBlock().getType().isAir()) {
                bp.getBlock().setType(Material.BAMBOO_PLANKS);
                wallBlocks.add(bp);
            }
        }
        gm.bambouvoreWalls.addAll(wallBlocks);
        player.sendMessage(Lang.get(player, Lang.Key.MSG_BAMB_C1_1));
        return true;
    }

    public static void potDeVin(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;

        RayTraceResult hit = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), 5);
        Location dropLoc;
        if (hit != null && hit.getHitBlock() != null) {
            assert hit.getHitBlockFace() != null;
            dropLoc = hit.getHitBlock().getRelative(hit.getHitBlockFace()).getLocation().add(0.5, 0, 0.5);
        } else {
            dropLoc = player.getEyeLocation().add(
                    player.getEyeLocation().getDirection().normalize().multiply(5));
        }

        Item item = player.getWorld().dropItem(dropLoc, new org.bukkit.inventory.ItemStack(Material.EMERALD));
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setVelocity(new Vector(0, 0, 0));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_BAMB_C2));

        UbuWool.getInstance().getServer().getScheduler().runTaskTimer(UbuWool.getInstance(), task -> {
            if (!item.isValid()) { task.cancel(); return; }
            for (Player p : gm.getAllPlayers()) {
                if (gm.deadPlayers.contains(p.getUniqueId())) continue;
                if (p.getLocation().distanceSquared(item.getLocation()) < 2.25) {
                    item.remove(); task.cancel();
                    boolean isEnemy = gm.isRedTeam(p) != gm.isRedTeam(player);
                    if (isEnemy) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true));
                        p.sendMessage(Lang.get(p, Lang.Key.MSG_BAMB_C2_1));
                        gm.lastDamager.put(p.getUniqueId(), player.getUniqueId());
                    } else {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 60, 1, false, true));
                        p.sendMessage(Lang.get(p, Lang.Key.MSG_BAMB_C2_2));
                    }
                    return;
                }
            }
        }, 2L, 2L);

        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(),
                () -> { if (item.isValid()) item.remove(); }, 600L);
    }

    public static void arriveeDuPrintemps(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;

        boolean isRed = gm.isRedTeam(player);
        MapConfig.UbuMap map = MapConfig.getSelectedMap();

        int cx = map != null ? map.woolCleanCenterX : 0;
        int cy = map != null ? map.woolCleanCenterY : 64;
        int cz = map != null ? map.woolCleanCenterZ : 0;
        int r  = map != null ? map.woolCleanRadius   : 50;

        World world = player.getWorld();

        Material enemyWool = isRed ? Material.BLUE_WOOL : Material.RED_WOOL;

        for (int x = cx - r; x <= cx + r; x++) {
            for (int y = cy - 10; y <= cy + 20; y++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    Material type = b.getType();

                    if (type == enemyWool) {
                        b.setType(Material.PINK_WOOL);
                    } else if (type == Material.DIORITE || type == Material.IRON_BLOCK) {
                        b.setType(Material.CHERRY_LEAVES);
                    }
                }
            }
        }

        List<Player> allies = isRed ? gm.getTeamRedPlayers() : gm.getTeamBluePlayers();
        for (Player ally : allies) {
            ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0, false, true));
            ally.sendMessage(Lang.get(ally, Lang.Key.MSG_BAMB_UBULT));
        }
        player.sendMessage(Lang.get(player, Lang.Key.MSG_BAMB_UBULT_1));
    }
}