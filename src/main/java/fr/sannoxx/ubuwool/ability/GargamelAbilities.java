package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.util.*;

class GargamelAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { GargamelAbilities.goldenRadar(p); return true; }
    @Override public boolean useC2(Player p, Player t) { return GargamelAbilities.rustyCurse(p); }
    @Override public boolean useUltimate(Player p) { GargamelAbilities.blingBling(p); return true; }
    @Override public long cooldownC1Ms() { return 40_000L; }
    @Override public long cooldownC2Ms() { return 40_000L; }
    @Override public void resetRound() { }
}

public class GargamelAbilities {

    public static void goldenRadar(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;
        boolean isRed = gm.isRedTeam(player);
        for (Player enemy : (isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0, false, false));
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 120, 0, false, false));
        }
        player.sendMessage(Lang.get(player, Lang.Key.MSG_GARG_C1));
    }

    public static boolean rustyCurse(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return false;
        boolean isRed = gm.isRedTeam(player);
        Vector dir = player.getEyeLocation().getDirection();
        Player target = null; double closest = Double.MAX_VALUE;
        for (Player enemy : (isRed ? gm.getTeamBluePlayers() : gm.getTeamRedPlayers())) {
            if (gm.deadPlayers.contains(enemy.getUniqueId())) continue;
            RayTraceResult r = enemy.getBoundingBox().expand(1.5).rayTrace(
                    player.getEyeLocation().toVector(), dir, 25);
            if (r != null) {
                double d = enemy.getLocation().distanceSquared(player.getLocation());
                if (d < closest) { closest = d; target = enemy; }
            }
        }
        if (target == null) { player.sendMessage(Lang.get(player, Lang.Key.MSG_GARG_C2)); return false; }
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 0, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0, false, true));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_GARG_C2_1) + target.getName() + "§7 !");
        target.sendMessage(Lang.get(target, Lang.Key.MSG_GARG_C2_2));
        return true;
    }

    public static void blingBling(Player player) {
        ItemStack goldSword = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta meta = goldSword.getItemMeta();
        meta.setDisplayName("§6§l★ Bling Bling ★");
        meta.addEnchant(Enchantment.SHARPNESS, 3, true);
        goldSword.setItemMeta(meta);
        int swordSlot = -1; ItemStack origSword = null;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s == null) continue;
            if (s.getType().name().endsWith("_SWORD")) { swordSlot = i; origSword = s.clone(); break; }
        }
        if (swordSlot != -1) player.getInventory().setItem(swordSlot, goldSword);
        else player.getInventory().addItem(goldSword);
        final ItemStack finalOrig = origSword; final int finalSlot = swordSlot;
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 300, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 1, false, true));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_GARG_UBULT));
        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () -> {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s != null && s.hasItemMeta() && s.getItemMeta().hasDisplayName()
                        && s.getItemMeta().getDisplayName().contains("Bling Bling")) {
                    player.getInventory().setItem(i, finalOrig != null ? finalOrig : new ItemStack(Material.WOODEN_SWORD));
                    break;
                }
            }
            player.sendMessage(Lang.get(player, Lang.Key.MSG_GARG_UBULT_1));
        }, 300L);
    }
}