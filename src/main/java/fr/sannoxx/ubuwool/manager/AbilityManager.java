package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityManager {

    // UUID → (slot → expiry timestamp ms)
    private static final Map<UUID, Map<Integer, Long>> cooldownMap = new HashMap<>();

    /** Durée maximale de cooldown autorisée en ms (10 minutes). */
    private static final long MAX_COOLDOWN_MS = 600_000L;

    public static boolean isAbilityItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        if (stack.getType() == org.bukkit.Material.BOW) return false;
        if (!stack.getItemMeta().hasDisplayName()) return false;
        String name = stack.getItemMeta().getDisplayName();
        return name.contains("C1") || name.contains("C2");
    }

    public static boolean isOnCooldown(Player player, int slot) {
        Map<Integer, Long> map = cooldownMap.get(player.getUniqueId());
        if (map == null) return false;
        Long until = map.get(slot);
        if (until == null) return false;
        return System.currentTimeMillis() < until;
    }

    public static void sendCooldownMessage(Player player, int slot) {
        Map<Integer, Long> map = cooldownMap.get(player.getUniqueId());
        if (map == null) return;
        Long until = map.get(slot);
        if (until == null) return;
        long remaining = Math.max(0, (until - System.currentTimeMillis()) / 1000);
        player.sendMessage(Lang.get(player, Lang.Key.COOLDOWN_MESSAGE, remaining));
    }

    public static void setCooldown(Player player, int slot, ItemStack stack, long durationMs) {
        // FIX : si un cooldown est déjà actif sur ce slot, ne pas le redémarrer.
        // Cela évite le double déclenchement (son en double, scheduler en double)
        // qui survenait quand onInteract ET onAttackEntity appelaient tous deux setCooldown.
        if (isOnCooldown(player, slot)) return;

        // Plafonnement pour éviter les overflows avec des valeurs comme 999_999_999L
        long safeDuration = Math.min(durationMs, MAX_COOLDOWN_MS);
        cooldownMap.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(slot, System.currentTimeMillis() + safeDuration);

        removeGlow(stack);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.2f);

        // safeDuration / 50 = ticks
        long ticks = safeDuration / 50;
        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () -> {
            if (!player.isOnline()) return;
            ItemStack current = player.getInventory().getItem(slot);
            if (isAbilityItem(current)) {
                addGlow(current);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            }
        }, ticks);
    }

    public static void addGlow(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
    }

    public static void removeGlow(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        meta.removeEnchant(Enchantment.UNBREAKING);
        stack.setItemMeta(meta);
    }

    public static void clearAllCooldowns(Player player) {
        cooldownMap.remove(player.getUniqueId());
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isAbilityItem(s)) addGlow(s);
        }
    }

    /** Nettoyage par UUID — appelé à la déconnexion ou en fin de partie. */
    public static void clearCooldowns(UUID uuid) {
        cooldownMap.remove(uuid);
    }

    /** @deprecated Utiliser clearCooldowns(UUID) */
    @Deprecated
    public static void clearCooldowns(String playerName) {
        cooldownMap.keySet().removeIf(uuid -> {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
            return p != null && p.getName().equals(playerName);
        });
    }
}