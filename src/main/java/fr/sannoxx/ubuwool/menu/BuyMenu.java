package fr.sannoxx.ubuwool.menu;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.PlayerData;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class BuyMenu implements InventoryHolder {

    public static final Map<String, Map<String, Integer>> roundPurchases = new HashMap<>();

    public static void resetPurchases(GameManager gm) {
        String prefix = gm.getInstanceId() + ":";
        roundPurchases.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public static void resetAllPurchases() {
        roundPurchases.clear();
    }

    private final Inventory inventory;
    private final Player player;
    private final GameManager gm;

    private BuyMenu(Player player, GameManager gm) {
        this.player = player;
        this.gm = gm;
        this.inventory = Bukkit.createInventory(this, 54, Lang.get(player, Lang.Key.SHOP_TITLE));
        refresh();
    }

    @Override
    public @NonNull Inventory getInventory() { return inventory; }

    public static void open(Player player, GameManager gm) {
        BuyMenu menu = new BuyMenu(player, gm);
        player.openInventory(menu.inventory);
    }

    public static void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof BuyMenu menu)) return;

        GameManager gm = menu.gm;

        if (gm.state != GameManager.GameState.BUY_PHASE) return;

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    if (!player.isOnline()) return;
                    if (gm.state != GameManager.GameState.BUY_PHASE) return;
                    open(player, gm);
                }, 1L);
    }

    private static int priceApple()     { return GameManager.getShopPrice("apple",           400); }
    private static int pricePickaxe()   { return GameManager.getShopPrice("pickaxe-iron",    300); }
    private static int pricePickaxe2()  { return GameManager.getShopPrice("pickaxe-diamond", 300); }
    private static int priceProt()      { return GameManager.getShopPrice("protection",      200); }
    private static int priceSharp()     { return GameManager.getShopPrice("sharpness",       400); }
    private static int priceWool()      { return GameManager.getShopPrice("wool",             50); }
    private static int priceAbsorb()    { return GameManager.getShopPrice("absorption",      250); }
    private static int priceShears()    { return GameManager.getShopPrice("shears",          200); }

    private String purchaseKey() {
        return gm.getInstanceId() + ":" + player.getName();
    }

    private int getCount(String key) {
        return roundPurchases.getOrDefault(purchaseKey(), Map.of()).getOrDefault(key, 0);
    }

    private void increment(String key) {
        roundPurchases.computeIfAbsent(purchaseKey(), k -> new HashMap<>()).merge(key, 1, Integer::sum);
    }

    public static Map<String, Integer> getPurchasesFor(int instanceId, String playerName) {
        return roundPurchases.getOrDefault(instanceId + ":" + playerName, Map.of());
    }

    public void refresh() {
        PlayerData data = gm.playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) inventory.setItem(i, filler);

        inventory.setItem(4, item(Material.SUNFLOWER,
                Lang.get(player, Lang.Key.SHOP_UBUS_DISPLAY, data.ubus),
                Collections.singletonList(Lang.get(player, Lang.Key.SHOP_UBUS_LORE))));

        int apples   = getCount("apple");
        int pickaxe  = getCount("pickaxe");
        int pickaxe2 = getCount("pickaxe2");
        int prot     = getCount("prot");
        int sharp    = getCount("sharp");
        int wool     = getCount("wool");
        int absorb   = getCount("absorb");
        int shears   = getCount("shears");

        inventory.setItem(10, shopItem(player, Material.GOLDEN_APPLE,
                Lang.Key.SHOP_ITEM_APPLE_NAME, Lang.Key.SHOP_ITEM_APPLE_DESC,
                apples >= 2
                        ? Lang.get(player, Lang.Key.SHOP_ITEM_MAX, 2, 2)
                        : Lang.get(player, Lang.Key.SHOP_ITEM_COUNT, apples, 2)));

        if (pickaxe < 1) {
            inventory.setItem(12, shopItem(player, Material.IRON_PICKAXE,
                    Lang.Key.SHOP_ITEM_PICKAXE_IRON_NAME, Lang.Key.SHOP_ITEM_PICKAXE_IRON_DESC,
                    Lang.get(player, Lang.Key.SHOP_ITEM_NOT_BOUGHT)));
        } else if (pickaxe2 < 1) {
            inventory.setItem(12, shopItem(player, Material.DIAMOND_PICKAXE,
                    Lang.Key.SHOP_ITEM_PICKAXE_DIAMOND_NAME, Lang.Key.SHOP_ITEM_PICKAXE_DIAMOND_DESC,
                    Lang.get(player, Lang.Key.SHOP_ITEM_UPGRADE_AVAILABLE)));
        } else {
            inventory.setItem(12, shopItem(player, Material.DIAMOND_PICKAXE,
                    Lang.Key.SHOP_ITEM_PICKAXE_DIAMOND_NAME, Lang.Key.SHOP_ITEM_PICKAXE_DIAMOND_DESC,
                    Lang.get(player, Lang.Key.SHOP_ITEM_MAX, 2, 2)));
        }

        inventory.setItem(14, shopItem(player, Material.LEATHER_CHESTPLATE,
                Lang.Key.SHOP_ITEM_PROT_NAME, Lang.Key.SHOP_ITEM_PROT_DESC,
                prot >= 3
                        ? Lang.get(player, Lang.Key.SHOP_ITEM_MAX, 3, 3)
                        : Lang.get(player, Lang.Key.SHOP_ITEM_COUNT, prot, 3)));

        inventory.setItem(16, shopItem(player, Material.WOODEN_SWORD,
                Lang.Key.SHOP_ITEM_SHARP_NAME, Lang.Key.SHOP_ITEM_SHARP_DESC,
                sharp >= 1
                        ? Lang.get(player, Lang.Key.SHOP_ITEM_MAX, 1, 1)
                        : Lang.get(player, Lang.Key.SHOP_ITEM_NOT_BOUGHT)));

        inventory.setItem(28, shopItem(player, Material.WHITE_WOOL,
                Lang.Key.SHOP_ITEM_WOOL_NAME, Lang.Key.SHOP_ITEM_WOOL_DESC,
                wool >= 12
                        ? Lang.get(player, Lang.Key.SHOP_ITEM_MAX, 12, 12)
                        : Lang.get(player, Lang.Key.SHOP_ITEM_COUNT, wool, 12)));

        inventory.setItem(30, shopItem(player, Material.TOTEM_OF_UNDYING,
                Lang.Key.SHOP_ITEM_ABSORB_NAME, Lang.Key.SHOP_ITEM_ABSORB_DESC,
                absorb >= 3
                        ? Lang.get(player, Lang.Key.SHOP_ITEM_MAX, 3, 3)
                        : Lang.get(player, Lang.Key.SHOP_ITEM_COUNT, absorb, 3)));

        inventory.setItem(32, shopItem(player, Material.SHEARS,
                Lang.Key.SHOP_ITEM_SHEARS_NAME, Lang.Key.SHOP_ITEM_SHEARS_DESC,
                shears >= 1
                        ? Lang.get(player, Lang.Key.SHOP_ITEM_MAX, 1, 1)
                        : Lang.get(player, Lang.Key.SHOP_ITEM_NOT_BOUGHT)));
    }

    public static void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof BuyMenu menu)) return;

        PlayerData data = menu.gm.playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        switch (slot) {
            case 10 -> {
                if (menu.getCount("apple") >= 2) { player.sendMessage(Lang.get(player, Lang.Key.SHOP_MAX)); return; }
                if (!charge(player, data, priceApple())) return;
                menu.increment("apple");
                player.sendMessage(Lang.get(player, Lang.Key.SHOP_BUY_APPLE, data.ubus));
            }
            case 12 -> {
                if (menu.getCount("pickaxe") >= 1) {
                    if (menu.getCount("pickaxe2") >= 1) { player.sendMessage(Lang.get(player, Lang.Key.SHOP_MAX)); return; }
                    if (!charge(player, data, pricePickaxe2())) return;
                    menu.increment("pickaxe2");
                    player.sendMessage(Lang.get(player, Lang.Key.SHOP_BUY_PICKAXE_DIAMOND, data.ubus));
                } else {
                    if (!charge(player, data, pricePickaxe())) return;
                    menu.increment("pickaxe");
                    player.sendMessage(Lang.get(player, Lang.Key.SHOP_BUY_PICKAXE_IRON, data.ubus));
                }
            }
            case 14 -> {
                int prot = menu.getCount("prot");
                if (prot >= 3) { player.sendMessage(Lang.get(player, Lang.Key.SHOP_MAX)); return; }
                if (!charge(player, data, priceProt())) return;
                menu.increment("prot");
                player.sendMessage(Lang.get(player, Lang.Key.SHOP_BUY_PROT, prot + 1, data.ubus));
            }
            case 16 -> {
                if (menu.getCount("sharp") >= 1) { player.sendMessage(Lang.get(player, Lang.Key.SHOP_ALREADY_BOUGHT)); return; }
                if (!charge(player, data, priceSharp())) return;
                menu.increment("sharp");
                player.sendMessage(Lang.get(player, Lang.Key.SHOP_BUY_SHARP, data.ubus));
            }
            case 28 -> {
                if (menu.getCount("wool") >= 12) { player.sendMessage(Lang.get(player, Lang.Key.SHOP_MAX)); return; }
                if (!charge(player, data, priceWool())) return;
                menu.increment("wool");
                player.sendMessage(Lang.get(player, Lang.Key.SHOP_BUY_WOOL, data.ubus));
            }
            case 30 -> {
                if (menu.getCount("absorb") >= 3) { player.sendMessage(Lang.get(player, Lang.Key.SHOP_MAX)); return; }
                if (!charge(player, data, priceAbsorb())) return;
                menu.increment("absorb");
                player.sendMessage(Lang.get(player, Lang.Key.SHOP_BUY_ABSORB, data.ubus));
            }
            case 32 -> {
                if (menu.getCount("shears") >= 1) { player.sendMessage(Lang.get(player, Lang.Key.SHOP_ALREADY_BOUGHT)); return; }
                if (!charge(player, data, priceShears())) return;
                menu.increment("shears");
                player.sendMessage(Lang.get(player, Lang.Key.SHOP_BUY_SHEARS, data.ubus));
            }
            default -> { return; }
        }
        menu.refresh();
    }

    private static boolean charge(Player player, PlayerData data, int cost) {
        if (data.ubus < cost) {
            player.sendMessage(Lang.get(player, Lang.Key.SHOP_NOT_ENOUGH_UBUS, data.ubus, cost));
            return false;
        }
        data.ubus -= cost;
        return true;
    }

    private static ItemStack shopItem(Player player, Material mat, Lang.Key nameKey, Lang.Key descKey, String status) {
        return item(mat, Lang.get(player, nameKey), Arrays.asList(Lang.get(player, descKey), status));
    }

    private static ItemStack item(Material mat, String name, java.util.List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}