package fr.sannoxx.ubuwool.menu;

import fr.sannoxx.ubuwool.PlayerData;
import fr.sannoxx.ubuwool.PlayerProfile;
import fr.sannoxx.ubuwool.manager.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import fr.sannoxx.ubuwool.manager.GameRegistry;

import java.util.Arrays;
import java.util.List;

public class AgentMenu implements InventoryHolder {

    private final Inventory inventory;

    private AgentMenu(Player player) {
        String title = PlayerProfile.getLanguage(player).equals("EN")
                ? "§6§lPICK YOUR AGENT"
                : "§6§lCHOISIS TON AGENT";
        this.inventory = Bukkit.createInventory(this, 27, title);
        buildMenu(player);
    }

    @Override
    public @NonNull Inventory getInventory() {
        return inventory;
    }

    public static void open(Player player) {
        AgentMenu menu = new AgentMenu(player);
        player.openInventory(menu.inventory);
    }

    private void buildMenu(Player player) {
        boolean en = PlayerProfile.getLanguage(player).equals("EN");

        // Remplir avec des panneaux noirs
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        // Sembol — slot 11
        inventory.setItem(11, makeItem(Material.RED_DYE, "§c§lSEMBOL", en ? Arrays.asList(
                "§7Passive: §fSpeed I",
                "§7Abilities:",
                "     §bScorching Dust: §fmelts targeted wool",
                "     §bSembol Firebol: §fthrows a fireball",
                "     §6§lUbultimate §r§6Rapidash Fast-fire: §fspeed + flame trail",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fVitesse I permanent",
                "§7Capacités:",
                "     §bCrâmage Dust: §ffait fondre la laine ciblée",
                "     §bSembol Firebol: §flance une boule de feu",
                "     §6§lUbultimate §r§6Galopa Feu-vite: §fvitesse + traînée de flammes",
                " ", "§eClique pour choisir !"
        )));

        // Fantom — slot 13
        inventory.setItem(13, makeItem(Material.LIGHT_BLUE_DYE, "§b§lFANTOM", en ? Arrays.asList(
                "§7Passive: §fPermanent shield",
                "§7Abilities:",
                "     §bTurtle Master: §fimproved resistance",
                "     §bDoor-to-Door: §fteleportation forward",
                "     §6§lUbultimate §r§6Anvil Rain: §frain of deadly anvils",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fBouclier permanent",
                "§7Capacités:",
                "     §bTurtle Master: §frésistance améliorée",
                "     §bPorte-à-Porte: §ftéléportation en avant",
                "     §6§lUbultimate §r§6Pluie d'Enclumes: §fpluie d'enclumes meurtrières",
                " ", "§eClique pour choisir !"
        )));

        // Gargamel — slot 15
        inventory.setItem(15, makeItem(Material.ORANGE_DYE, "§6§lGARGAMEL", en ? Arrays.asList(
                "§7Passive: §fHealing potion (2♥)",
                "§7Abilities:",
                "     §bGolden Radar: §freveals enemy positions",
                "     §bRusty Curse: §fcurses the targeted enemy",
                "     §6§lUbultimate §r§6Bling Bling: §fbetter sword + buffs",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fPotion de soin (2♥)",
                "§7Capacités:",
                "     §bRadar Doré: §frévèle la position des ennemis",
                "     §bMalédiction de la Rouille: §fmaudit l'ennemi ciblé",
                "     §6§lUbultimate §r§6Bling Bling: §féquipement amélioré",
                " ", "§eClique pour choisir !"
        )));

        // Horcus — slot 3
        inventory.setItem(3, makeItem(Material.NETHER_WART, "§4§lHORCUS", en ? Arrays.asList(
                "§7Passive: §f8 arrows",
                "§7Abilities:",
                "     §bHead Hunter: §fbow that can headshot",
                "     §bRendez-Vous: §fplaceable teleporter",
                "     §6§lUbultimate §r§6Tour de Force: §fdangerous crossbow",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §f8 flèches",
                "§7Capacités:",
                "     §bChasseur de Tête: §farc qui peut infliger des headshots",
                "     §bRendez-Vous: §ftéléporteur déposable",
                "     §6§lUbultimate §r§6Tour de Force: §farbalète dangereuse",
                " ", "§eClique pour choisir !"
        )));

        // Bambouvore — slot 5
        inventory.setItem(5, makeItem(Material.GREEN_DYE, "§2§lBAMBOUVORE", en ? Arrays.asList(
                "§7Passive: §fGolden apple",
                "§7Abilities:",
                "     §bBamboo Wall: §fplaces a wall",
                "     §bBribe: §fdrops a trap/gift on the ground",
                "     §6§lUbultimate §r§6Spring's Arrival: §fregenerate allies + clear enemies' wool",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fPomme dorée",
                "§7Capacités:",
                "     §bMuraille de Bambou: §fplace un mur",
                "     §bPot-de-Vin: §fdépose un piège/cadeau au sol",
                "     §6§lUbultimate §r§6Arrivée du Printemps: §frégénère les alliés + nettoie la laine ennemie",
                " ", "§eClique pour choisir !"
        )));

        // Lolita — slot 21
        inventory.setItem(21, makeItem(Material.BLACK_DYE, "§0§lLOLITA", en ? Arrays.asList(
                "§7Passive: §fHaste II potion (10s)",
                "§7Abilities:",
                "     §bMiss Sausage: §fsummons an enraged dog",
                "     §bChihuahua Bite: §fbite an enemy",
                "     §6§lUbultimate §r§61v1 Baños: §fduel the targeted player",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fPotion de Célérité 2 (10s)",
                "§7Capacités:",
                "     §bMadame Saucisse: §ffait apparaître un chien",
                "     §bMorsure du Chihuahua: §fcroquer l'ennemi",
                "     §6§lUbultimate §r§61v1 Baños: §fdéfier en duel",
                " ", "§eClique pour choisir !"
        )));

        // Asky — slot 23
        inventory.setItem(23, makeItem(Material.MAGENTA_DYE, "§d§lASKY", en ? Arrays.asList(
                "§7Passive: §fLeather helmet",
                "§7Abilities:",
                "     §bDisgusting Phenerwin: §fheal at cost of debuffs",
                "     §bReggaeton Remix: §flevitation ray",
                "     §6§lUbultimate §r§6Wanted Pétasse: §fjukebox weakening nearby enemies",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fCasque en cuir",
                "§7Capacités:",
                "     §bPhenerwin Dégueulasse: §fsoin + malus",
                "     §bVersion Reggaeton: §frayon de lévitation",
                "     §6§lUbultimate §r§6Wanted Pétasse: §fjukebox affaiblissant les ennemis",
                " ", "§eClique pour choisir !"
        )));

        // Carlos — slot 12
        inventory.setItem(12, makeItem(Material.YELLOW_DYE, "§e§lCARLOS", en ? Arrays.asList(
                "§7Passive: §fFire resistance",
                "§7Abilities:",
                "     §bAxe Maniac: §ftemporary iron axe",
                "     §bKnight Sacrifice: §ftemporary iron armor",
                "     §6§lUbultimate §r§6Last Shift: §fsecond life",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fRésistance au feu",
                "§7Capacités:",
                "     §bFou à la Hache: §fhache en fer temporaire",
                "     §bSacrifice Chevalier: §farmure en fer temporaire",
                "     §6§lUbultimate §r§6Dernier Shift: §fdeuxième vie",
                " ", "§eClique pour choisir !"
        )));

        // Larok — slot 14
        inventory.setItem(14, makeItem(Material.WHITE_DYE, "§f§lLAROK", en ? Arrays.asList(
                "§7Passive: §f7 wind charges",
                "§7Abilities:",
                "     §bJett Dash: §fforward propulsion",
                "     §bNeon Thunderbolt: §fstrikes lightning on target",
                "     §6§lUbultimate §r§6Raze Rocket: §fdeadly missile",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §f7 wind charges",
                "§7Capacités:",
                "     §bJett Dash: §fpropulsion en avant",
                "     §bNeon Fulgurance: §féleclair sur la cible",
                "     §6§lUbultimate §r§6Raze Rocket: §fmissile léthal",
                " ", "§eClique pour choisir !"
        )));

        // Ticksuspicious — slot 4
        inventory.setItem(4, makeItem(Material.LIGHT_GRAY_DYE, "§7§lTICKSUSPICIOUS", en ? Arrays.asList(
                "§7Passive: §fexplodes every 10 hits received",
                "§7Abilities:",
                "     §bExplosive Mine: §fplaceable mine",
                "     §bSeeking Heads: §fhoming bombs",
                "     §6§lUbultimate §r§6Pernicious Mission: §fkamikaze explosion",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fexplose tous les 10 coups reçus",
                "§7Capacités:",
                "     §bMine Explosive: §fmine piégée",
                "     §bTêtes Chercheuses: §fbombes à tête chercheuse",
                "     §6§lUbultimate §r§6Opération Pernicious: §fexplosion kamikaze",
                " ", "§eClique pour choisir !"
        )));

        // Mascord — slot 22
        inventory.setItem(22, makeItem(Material.BLUE_DYE, "§9§lMASCORD", en ? Arrays.asList(
                "§7Passive: §fcompass → weakest enemy",
                "§7Abilities:",
                "     §bUp Dog: §fpropulsion into the air",
                "     §bBeekeeper Syndrome: §fmakes target sick",
                "     §6§lUbultimate §r§6Sigma Pouleur: §flunar gravity on map",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fBoussole → ennemi le plus affaibli",
                "§7Capacités:",
                "     §bUp Dog: §fpropulsion dans les airs",
                "     §bSyndrome de l'Apiculteur: §frend la cible malade",
                "     §6§lUbultimate §r§6Sigma Pouleur: §fgravité lunaire",
                " ", "§eClique pour choisir !"
        )));

        // Gekko — slot 20
        inventory.setItem(20, makeItem(Material.LIME_DYE, "§a§lGEKKO", en ? Arrays.asList(
                "§7Passive: §flingering poison potion",
                "§7Abilities:",
                "     §bAltego Wingman: §fexplosive fireball",
                "     §bVerti Flash: §fblinds enemies in sight",
                "     §6§lUbultimate §r§6Freed Monster: §ffox that steals swords",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fPotion de poison persistante",
                "§7Capacités:",
                "     §bAltego Wingman: §fboule de feu explosive",
                "     §bVerti Flash: §faveugle les ennemis dans le champ de vision",
                "     §6§lUbultimate §r§6Monstre en Liberté: §frenard qui vole les épées",
                " ", "§eClique pour choisir !"
        )));

        // Hijab — slot 6
        inventory.setItem(6, makeItem(Material.PURPLE_DYE, "§5§lHIJAB", en ? Arrays.asList(
                "§7Passive: §fsees opponents' health",
                "§7Abilities:",
                "     §bHat or No Hat: §fswitch between two states",
                "     §bDan Kyojur: §ffire aspect for a short time",
                "     §6§lUbultimate §r§6Raised Posts Village: §fspeed + zombie village",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fVoit la vie des joueurs ennemis",
                "§7Capacités:",
                "     §bAvec ou Sans Chapeau: §fchange d'état pour des effets différents",
                "     §bDan Kyojur: §fcoups enflammés pour quelques secondes",
                "     §6§lUbultimate §r§6Village des Poteaux Levés: §fvitesse + village zombifié",
                " ", "§eClique pour choisir !"
        )));

        // Ilargia — slot 24
        inventory.setItem(24, makeItem(Material.GRAY_DYE, "§8§lILARGIA", en ? Arrays.asList(
                "§7Passive: §finvisibility without armor",
                "§7Abilities:",
                "     §bRig and Wa: §fcontrol an enemy's movement",
                "     §bBowser Breathe: §fspits fire",
                "     §6§lUbultimate §r§6Pedani-Pedalo: §fcasts a huge wind wave",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §finvisible sans armure",
                "§7Capacités:",
                "     §bRig and Wa: §fcontrôle les mouvements d'un ennemi",
                "     §bSouffle de Bowser: §fcrache des flammes",
                "     §6§lUbultimate §r§6Pédani-Pédalo: §fgénère une bourrasque de vent",
                " ", "§eClique pour choisir !"
        )));

        // Doma — slot 2
        inventory.setItem(2, makeItem(Material.CYAN_DYE, "§3§lDOMA", en ? Arrays.asList(
                "§7Passive: §ffreezing hit every 10 hits",
                "§7Abilities:",
                "     §bFrozen Slash: §fcreates a slash that freezes enemies hit",
                "     §bSnover Abomasnow: §ffreezes someone in an ice block",
                "     §6§lUbultimate §r§6Sherbet Land: §fbecomes overpowered",
                " ", "§eClick to pick!"
        ) : Arrays.asList(
                "§7Passif: §fcoup gelé tous les 10 coups",
                "§7Capacités:",
                "     §bTranche de Givre: §fcrée une coupe givrée",
                "     §bBlizzi Blizzaroi: §fenferme l'ennemi dans un bloc de glace",
                "     §6§lUbultimate §r§6Sherbet Land: §fdevient surpuissant",
                " ", "§eClique pour choisir !"
        )));

    }

    private static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Mapping slot → nom d'agent */
    public static String getAgentFromSlot(int slot) {
        return switch (slot) {
            case 11 -> "sembol";
            case 13 -> "fantom";
            case 15 -> "gargamel";
            case 3  -> "horcus";
            case 5  -> "bambouvore";
            case 21 -> "lolita";
            case 23 -> "asky";
            case 12 -> "carlos";
            case 14 -> "larok";
            case 4  -> "ticksuspicious";
            case 22 -> "mascord";
            case 20 -> "gekko";
            case 6  -> "hijab";
            case 24 -> "ilargia";
            case 2 -> "doma";
            default -> null;
        };
    }

    /** Appelé par le listener pour traiter un clic */
    public static void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String agentName = getAgentFromSlot(event.getRawSlot());
        if (agentName == null) return;

        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) {
            player.sendMessage("§cTu n'es pas dans une instance !");
            player.closeInventory();
            return;
        }
        if (!gm.teamRed.contains(player.getUniqueId()) && !gm.teamBlue.contains(player.getUniqueId())) {
            player.sendMessage("§cRejoins d'abord une équipe !");
            player.closeInventory();
            return;
        }
        gm.playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData(player));
        gm.setAgent(player, agentName);

        PlayerData d = gm.playerDataMap.get(player.getUniqueId());
        if (d != null && d.agent != null && d.agent.getName().equalsIgnoreCase(agentName)) {
            player.closeInventory();
        }
    }
}