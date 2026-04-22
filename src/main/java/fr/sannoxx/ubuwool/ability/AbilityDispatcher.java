package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.PlayerData;
import fr.sannoxx.ubuwool.manager.AbilityManager;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Remplace le switch géant de 200+ lignes dans PlayerListener.handleAbilityUse().
 *
 * AJOUT : les capacités (C1, C2, Ultimate) sont bloquées si le joueur se trouve
 * actuellement dans la zone Baños de Lolita.
 */
public class AbilityDispatcher {

    private static final Map<String, AgentAbility> registry = new HashMap<>();

    static {
        register("sembol",         new SembolAbilityImpl());
        register("fantom",         new FantomAbilityImpl());
        register("gargamel",       new GargamelAbilityImpl());
        register("horcus",         new HorcusAbilityImpl());
        register("bambouvore",     new BambouvoreAbilityImpl());
        register("lolita",         new LolitaAbilityImpl());
        register("asky",           new AskyAbilityImpl());
        register("carlos",         new CarlosAbilityImpl());
        register("larok",          new LarokAbilityImpl());
        register("ticksuspicious", new TicksuspiciousAbilityImpl());
        register("mascord",        new MascordAbilityImpl());
        register("hijab",          new HijabAbilityImpl());
        register("ilargia",        new IlargiaAbilityImpl());
        register("gekko",          new GekkoAbilityImpl());
        register("doma",          new DomaAbilityImpl());
    }

    public static void register(String agentName, AgentAbility impl) {
        registry.put(agentName.toLowerCase(), impl);
    }

    public static AgentAbility get(String agentName) {
        return registry.get(agentName.toLowerCase());
    }

    /**
     * Point d'entrée principal : dispatch C1 ou C2 selon le nom de l'item.
     * Les capacités sont bloquées dans la zone Baños.
     */
    public static boolean dispatch(Player player, ItemStack stack, int slot) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return false;
        PlayerData data = gm.playerDataMap.get(player.getUniqueId());
        if (data == null || data.agent == null) return false;

        // Blocage dans les Baños
        if (LolitaAbilities.isInBanos(player)) {
            player.sendMessage("§c§lBaños §7— Vous ne pouvez pas utiliser vos capacités ici !");
            return false;
        }

        String agentName = data.agent.getName().toLowerCase();
        AgentAbility ability = registry.get(agentName);
        if (ability == null) {
            player.sendMessage(Lang.get(player, Lang.Key.ABILITY_NOT_IMPLEMENTED));
            return false;
        }

        String itemName = (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName())
                ? stack.getItemMeta().getDisplayName() : "";

        boolean isC1 = itemName.contains("C1");
        boolean isC2 = itemName.contains("C2");
        if (!isC1 && !isC2) return false;

        // Horcus C2 gère lui-même son cooldown
        if (agentName.equals("horcus") && isC2) {
            HorcusAbilities.setLastRendezVousTp(player.getUniqueId(), false);
            HorcusAbilities.rendezVous(player);
            if (HorcusAbilities.wasLastRendezVousTp(player.getUniqueId())) {
                AbilityManager.setCooldown(player, slot, stack, 30_000L);
            }
            return false;
        }

        // Lolita C2 (morsure chihuahua) est géré dans onAttackEntity
        if (agentName.equals("lolita") && isC2) return false;

        boolean success;
        long cooldown;
        if (isC1) {
            success = ability.useC1(player);
            cooldown = ability.cooldownC1Ms();
        } else {
            success = ability.useC2(player, null);
            cooldown = ability.cooldownC2Ms();
        }

        if (success) {
            AbilityManager.setCooldown(player, slot, stack, cooldown);
        }
        return success;
    }

    /**
     * Dispatch de l'ultimate pour un agent.
     * Bloqué dans les Baños.
     */
    public static boolean dispatchUltimate(Player player) {
        // Blocage dans les Baños (déjà géré dans PlayerListener mais double sécurité)
        if (LolitaAbilities.isInBanos(player)) {
            return false;
        }

        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return false;
        PlayerData data = gm.playerDataMap.get(player.getUniqueId());
        if (data == null || data.agent == null) return false;
        AgentAbility ability = registry.get(data.agent.getName().toLowerCase());
        if (ability == null) {
            player.sendMessage(Lang.get(player, Lang.Key.ABILITY_NOT_IMPLEMENTED));
            return false;
        }
        return ability.useUltimate(player);
    }

    /** Réinitialisation de tous les states d'abilities en fin de round. */
    public static void resetAllRounds() {
        for (AgentAbility ability : registry.values()) {
            try {
                ability.resetRound();
            } catch (Exception e) {
                // On continue même si un reset échoue
            }
        }
    }
}