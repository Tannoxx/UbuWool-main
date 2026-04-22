package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

class CarlosAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { CarlosAbilities.fouALaHache(p); return true; }
    @Override public boolean useC2(Player p, Player t) { CarlosAbilities.sacrificeChevalier(p); return true; }
    @Override public boolean useUltimate(Player p) { CarlosAbilities.dernierShift(p); return true; }
    @Override public long cooldownC1Ms() { return 40_000L; }
    @Override public long cooldownC2Ms() { return 40_000L; }
    @Override public void resetRound() { CarlosAbilities.resetRound(); }
}

public class CarlosAbilities {

    public static Map<String, Location> pendingRevive = new HashMap<>();
    public static Map<String, ItemStack[]> pendingReviveInventory = new HashMap<>();
    public static Map<String, ItemStack[]> pendingReviveArmor = new HashMap<>();

    private static final Set<UUID> activeReviveTimers = new HashSet<>();
    private static final Map<UUID, Integer> reviveTaskIds = new HashMap<>();  // AJOUT

    public static void fouALaHache(Player player) {
        int swordSlot = -1;
        ItemStack origSword = null;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s != null && s.getType().name().endsWith("_SWORD")) {
                swordSlot = i;
                origSword = s.clone();
                break;
            }
        }
        final int finalSlot = swordSlot == -1 ? player.getInventory().getHeldItemSlot() : swordSlot;
        final ItemStack finalOrig = origSword;
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        var m = axe.getItemMeta();
        m.setDisplayName("§e§lHache Ensanglantée");
        axe.setItemMeta(m);
        player.getInventory().setItem(finalSlot, axe);
        player.sendMessage(Lang.get(player, Lang.Key.MSG_CARL_C1));
        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () -> {
            ItemStack cur = player.getInventory().getItem(finalSlot);
            if (cur != null && cur.getType() == Material.IRON_AXE)
                player.getInventory().setItem(finalSlot, finalOrig);
            player.sendMessage(Lang.get(player, Lang.Key.MSG_CARL_C1_1));
        }, 100L);
    }

    public static void sacrificeChevalier(Player player) {
        ItemStack[] oldArmor = player.getInventory().getArmorContents().clone();
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 160, 2, false, true));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_CARL_C2));
        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () -> {
            player.getInventory().setArmorContents(oldArmor);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
            player.sendMessage(Lang.get(player, Lang.Key.MSG_CARL_C2_1));
        }, 160L);
    }

    public static void dernierShift(Player player) {
        String name = player.getName();
        pendingRevive.put(name, player.getLocation().clone());

        ItemStack[] inv = new ItemStack[player.getInventory().getSize()];
        for (int i = 0; i < inv.length; i++) {
            ItemStack s = player.getInventory().getItem(i);
            inv[i] = (s != null) ? s.clone() : null;
        }
        pendingReviveInventory.put(name, inv);

        ItemStack[] armorContents = player.getInventory().getArmorContents();
        ItemStack[] armorClone = new ItemStack[armorContents.length];
        for (int i = 0; i < armorContents.length; i++) {
            armorClone[i] = (armorContents[i] != null) ? armorContents[i].clone() : null;
        }
        pendingReviveArmor.put(name, armorClone);

        player.sendMessage(Lang.get(player, Lang.Key.MSG_CARL_UBULT));
    }

    public static void startReviveTimer(Player player) {
        String name = player.getName();
        UUID uuid = player.getUniqueId();

        if (activeReviveTimers.contains(uuid)) return;
        activeReviveTimers.add(uuid);

        final int[] secondesRestantes = {60};
        UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), countdownTask -> {
                    if (!player.isOnline()) { countdownTask.cancel(); activeReviveTimers.remove(uuid); return; }
                    if (secondesRestantes[0] <= 0) { countdownTask.cancel(); activeReviveTimers.remove(uuid); return; }
                    int s = secondesRestantes[0];
                    String couleur = s > 30 ? "§a" : s > 10 ? "§e" : "§c";
                    player.sendActionBar(Component.text(Lang.get(player, Lang.Key.MSG_CARL_REVIVE_TIMER, couleur, s)));
                    secondesRestantes[0]--;
                }, 0L, 20L);

        int taskId = UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    reviveTaskIds.remove(uuid);
                    activeReviveTimers.remove(uuid);
                    GameManager gm = GameRegistry.getInstanceOf(player);
                    if (gm == null) return;
                    if (gm.state != GameManager.GameState.ROUND_ACTIVE) return;
                    if (gm.deadPlayers.contains(uuid)) return;

                    player.sendMessage(Lang.get(player, Lang.Key.MSG_CARL_UBULT_1));

                    gm.deadPlayers.add(uuid);
                    UbuWool.getInstance().getServer().getScheduler().runTask(UbuWool.getInstance(), () -> {
                        player.getInventory().clear();
                        player.setGameMode(GameMode.SPECTATOR);
                        player.setHealth(player.getMaxHealth());
                    });
                    gm.checkTeamEliminationPublic();
                }, 1200L).getTaskId();

        reviveTaskIds.put(uuid, taskId);
    }

    public static void cancelReviveTimer(UUID uuid) {
        Integer taskId = reviveTaskIds.remove(uuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        activeReviveTimers.remove(uuid);
    }

    public static void clearRevive(String name) {
        pendingRevive.remove(name);
        pendingReviveInventory.remove(name);
        pendingReviveArmor.remove(name);
    }

    public static void resetRound() {
        for (Map.Entry<UUID, Integer> entry : reviveTaskIds.entrySet()) {
            Bukkit.getScheduler().cancelTask(entry.getValue());
        }
        reviveTaskIds.clear();

        pendingRevive.clear();
        pendingReviveInventory.clear();
        pendingReviveArmor.clear();
        activeReviveTimers.clear();
    }
}