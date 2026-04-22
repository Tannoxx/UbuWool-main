package fr.sannoxx.ubuwool;

import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Lang {

    public enum Key {
        ALREADY_REGISTERED, JOIN_RED, JOIN_BLUE, LEFT_TEAM, SPECTATOR,
        NEED_PLAYERS, CHOOSE_AGENT, AGENT_CHOSEN, AGENT_UNKNOWN, AGENT_TAKEN,
        BUY_PHASE_START, ROUND_START, ROUND_WIN_RED, ROUND_WIN_BLUE, GAME_WIN_RED, GAME_WIN_BLUE,
        ELIMINATED, KILL_MESSAGE, STAR_EARNED_DEATH,
        ULTIMATE_NOT_READY, ULTIMATE_UNLOCKED,
        SHOP_ONLY_BUY_PHASE, SHOP_MAX, SHOP_ALREADY_BOUGHT, SHOP_NOT_ENOUGH_UBUS,
        SHOP_BUY_APPLE, SHOP_BUY_PICKAXE_IRON, SHOP_BUY_PICKAXE_DIAMOND,
        SHOP_BUY_PROT, SHOP_BUY_SHARP, SHOP_BUY_WOOL, SHOP_BUY_ABSORB, SHOP_BUY_SHEARS,
        SHOP_NEED_IRON_PICKAXE,
        STAR_CAPTURE_PROGRESS, STAR_CAPTURE_INTERRUPTED, STAR_CAPTURED, STAR_RESPAWNED,
        SUMMARY_TITLE, SUMMARY_ROUNDS, SUMMARY_TEAM_RED, SUMMARY_TEAM_BLUE,
        UBUS_GAINED,
        ABILITY_NOT_IMPLEMENTED, COOLDOWN_MESSAGE, NO_ENEMY_AIMED,
        MAP_SELECTED, MAP_NOT_FOUND, MAP_VOTE_REGISTERED, MAP_VOTE_BROADCAST,
        MAP_RELOAD, MAP_FORCED, NO_VALID_MAP,
        NOT_IN_GAME, ULTIMATE_ADMIN_UNLOCKED, GAME_STOPPED,
        VOID_TP,
        BANOS_START_LOLITA, BANOS_START_TARGET, BANOS_RETURN, BANOS_RETURN_END,
        CARLOS_REVIVE, CARLOS_SHIFT_ACTIVE,
        HORCUS_SPONGE_BROKEN, HORCUS_RDVGIFT, HORCUS_TPRDV,
        CENTRE_CAPTURE_RED, CENTRE_CAPTURE_BLUE,
        PROFILE_LANGUAGE_CHANGED,
        ROUND_TIMEOUT, ROUND_FORCE_END,
        ADMIN_STATE_HEADER, ADMIN_STATE_PHASE, ADMIN_STATE_SCORE, ADMIN_STATE_ROUND,
        ADMIN_STATE_PLAYERS, ADMIN_TP_PLAYER, ADMIN_PLAYER_NOT_FOUND, ADMIN_ROUND_FORCED,
        // Items
        ITEM_SEMBOL_C1, ITEM_SEMBOL_C2, ITEM_FANTOM_C1, ITEM_FANTOM_C2,
        ITEM_GARGAMEL_C1, ITEM_GARGAMEL_C2, ITEM_HORCUS_C1, ITEM_HORCUS_C2,
        ITEM_BAMBOUVORE_C1, ITEM_BAMBOUVORE_C2, ITEM_LOLITA_C1, ITEM_LOLITA_C2,
        ITEM_ASKY_C1, ITEM_ASKY_C2, ITEM_CARLOS_C1, ITEM_CARLOS_C2,
        ITEM_LAROK_C1, ITEM_LAROK_C2, ITEM_TICKSUSPICIOUS_C1, ITEM_TICKSUSPICIOUS_C2,
        ITEM_MASCORD_C1, ITEM_MASCORD_C2, ITEM_HIJAB_C1, ITEM_HIJAB_C2,
        ITEM_ILARGIA_C1, ITEM_ILARGIA_C2, ITEM_GEKKO_PASSIVE, ITEM_GEKKO_C1, ITEM_GEKKO_C2,
        ITEM_ULTIMATE,
        // HUD
        HUD_WAITING, HUD_AGENT_SELECT, HUD_BUY_PHASE, HUD_ROUND, HUD_ROUND_END, HUD_PAUSE,
        // Résumé
        SUMMARY_NO_AGENT, SUMMARY_KILL_SINGULAR, SUMMARY_KILL_PLURAL,
        // Shop UI
        SHOP_TITLE, SHOP_UBUS_DISPLAY, SHOP_UBUS_LORE, SHOP_ITEM_MAX,
        SHOP_ITEM_COUNT, SHOP_ITEM_NOT_BOUGHT, SHOP_ITEM_UPGRADE_AVAILABLE,
        SHOP_ITEM_APPLE_NAME, SHOP_ITEM_APPLE_DESC,
        SHOP_ITEM_PICKAXE_IRON_NAME, SHOP_ITEM_PICKAXE_IRON_DESC,
        SHOP_ITEM_PICKAXE_DIAMOND_NAME, SHOP_ITEM_PICKAXE_DIAMOND_DESC,
        SHOP_ITEM_PROT_NAME, SHOP_ITEM_PROT_DESC,
        SHOP_ITEM_SHARP_NAME, SHOP_ITEM_SHARP_DESC,
        SHOP_ITEM_WOOL_NAME, SHOP_ITEM_WOOL_DESC,
        SHOP_ITEM_ABSORB_NAME, SHOP_ITEM_ABSORB_DESC,
        SHOP_ITEM_SHEARS_NAME, SHOP_ITEM_SHEARS_DESC,
        // Map vote UI
        MAP_VOTE_TITLE, MAP_VOTE_MENU_TITLE, MAP_VOTE_LORE_LINE,
        MAP_VOTE_COUNT, MAP_VOTE_CURRENT, MAP_VOTE_CLICK,
        // Team menu
        TEAM_MENU_TITLE, TEAM_RED_NAME, TEAM_BLUE_NAME,
        TEAM_SPECTATOR_NAME, TEAM_SPECTATOR_LORE, TEAM_NO_PLAYER, TEAM_CLICK_JOIN, TEAM_CONFIRM,
        // Messages capacités
        MSG_ASKY_C1, MSG_ASKY_C2, MSG_ASKY_C2_1, MSG_ASKY_UBULT,
        MSG_BAMB_C1, MSG_BAMB_C1_1, MSG_BAMB_C2, MSG_BAMB_C2_1, MSG_BAMB_C2_2,
        MSG_BAMB_UBULT, MSG_BAMB_UBULT_1,
        MSG_CARL_C1, MSG_CARL_C1_1, MSG_CARL_C2, MSG_CARL_C2_1,
        MSG_CARL_UBULT, MSG_CARL_UBULT_1,
        MSG_FANT_C1, MSG_FANT_C1_1, MSG_FANT_C2, MSG_FANT_UBULT, MSG_FANT_UBULT_1,
        MSG_GARG_C1, MSG_GARG_C2, MSG_GARG_C2_1, MSG_GARG_C2_2, MSG_GARG_UBULT, MSG_GARG_UBULT_1,
        MSG_GEKK_C1, MSG_GEKK_C2, MSG_GEKK_C2_1,
        MSG_GEKK_UBULT, MSG_GEKK_UBULT_1, MSG_GEKK_UBULT_2, MSG_GEKK_UBULT_3,
        MSG_HIJA_C1, MSG_HIJA_C1_1, MSG_HIJA_C2, MSG_HIJA_UBULT,
        MSG_HORC_C2, MSG_HORC_C2_1, MSG_HORC_C2_2, MSG_HORC_C2_3, MSG_HORC_C2_4, MSG_HORC_UBULT,
        MSG_LARO_C1, MSG_LARO_C2, MSG_LARO_C2_1,
        MSG_LARO_UBULT, MSG_LARO_UBULT_1, MSG_LARO_UBULT_2,
        MSG_LOLI_C1, MSG_LOLI_C1_1, MSG_LOLI_C1_2, MSG_LOLI_C2, MSG_LOLI_C2_1, MSG_LOLI_C2_2,
        MSG_LOLI_UBULT, MSG_LOLI_UBULT_1, MSG_LOLI_UBULT_2,
        MSG_MASC_C1, MSG_MASC_C2, MSG_MASC_C2_1,
        MSG_MASC_UBULT, MSG_MASC_UBULT_1, MSG_MASC_UBULT_2,
        MSG_SEMB_C1, MSG_SEMB_C1_1, MSG_SEMB_C2, MSG_SEMB_UBULT, MSG_SEMB_UBULT_1,
        MSG_TICK_C1, MSG_TICK_C1_1, MSG_TICK_C1_2, MSG_TICK_C2, MSG_TICK_C2_1,
        MSG_TICK_UBULT, MSG_TICK_MINE_HIT, MSG_TICK_SEEKER_HIT, MSG_TICK_PERNICIOUS_HIT,
        MSG_LOLI_NO_BANOS, MSG_HORC_HEADSHOT, MSG_CARL_REVIVE_TIMER, MSG_ILAR_C1_NO_TARGET, MSG_ILAR_C1_CAST, MSG_ILAR_C1_TARGET_START,
        MSG_ILAR_C1_RELEASED, MSG_ILAR_C1_FREED, MSG_ILAR_C1_FREED_CASTER_DEAD,
        MSG_ILAR_C2_CAST, MSG_ILAR_C2_END,
        MSG_ILAR_UBULT, MSG_ILAR_UBULT_BROADCAST, MSG_ILAR_UBULT_HIT,
        MSG_DOMA_C1_CAST, MSG_DOMA_C1_NO_HIT, MSG_DOMA_C1_HIT,
        MSG_DOMA_C2_CAST, MSG_DOMA_C2_NO_TARGET, MSG_DOMA_C2_FROZEN, MSG_DOMA_C2_RELEASED,
        MSG_DOMA_UBULT,
        ITEM_DOMA_C1, ITEM_DOMA_C2,
        ULT_DOMA,
        // Ultimates
        ULT_SEMBOL, ULT_FANTOM, ULT_GARGAMEL, ULT_HORCUS, ULT_BAMBOUVORE,
        ULT_LOLITA, ULT_ASKY, ULT_CARLOS, ULT_LAROK, ULT_TICKSUSPICIOUS,
        ULT_MASCORD, ULT_HIJAB, ULT_ILARGIA, ULT_GEKKO,


        // ====================================================
        // NOUVELLES CLÉS — Statistiques
        // ====================================================
        STATS_MENU_TITLE, STATS_NO_DATA, STATS_KILLED_BY, STATS_WITH, STATS_KILLER_HP,

        // ====================================================
        // NOUVELLES CLÉS — Death Recap (traduction)
        // ====================================================
        RECAP_HEADER,
        RECAP_KILLED_BY,
        RECAP_AGENT,
        RECAP_WEAPON,
        RECAP_KILLER_HP,
        RECAP_ENV_CAUSE,
        RECAP_NO_INFO,
        RECAP_ENV_KILLER,       // valeur textuelle utilisée comme clé interne (ex: "Environnement")
        RECAP_DEFAULT_WEAPON,   // arme par défaut (ex: "épée" / "sword")

        // ====================================================
        // NOUVELLES CLÉS — File d'attente (MatchmakingQueue)
        // ====================================================
        QUEUE_JOINED,           // "Tu es en file d'attente (position %d)."
        QUEUE_ALREADY_IN,       // "Tu es déjà en file d'attente ! (position %d)"
        QUEUE_ALREADY_IN_GAME,  // "Tu es déjà dans une partie !"
        QUEUE_LEFT,             // "Tu as quitté la file d'attente."
        QUEUE_SIZE_BROADCAST,   // "%d joueur(s) en attente."
        QUEUE_ADMIN_HINT,       // "Un admin lancera la partie avec /uw start."

        // ====================================================
        // NOUVELLES CLÉS — ProfileMenu / Leaderboard / Stats (traduction)
        // ====================================================
        PROFILE_TITLE,
        PROFILE_LANGUAGE_LABEL,
        PROFILE_LANGUAGE_CHANGE_HINT,
        PROFILE_STATS_SHORTCUT,
        PROFILE_STATS_SHORTCUT_LORE,
        PROFILE_CLICK_TO_SEE,
        PROFILE_CURRENT_LANG,
        PROFILE_SELECT_LANG,

        STATS_GAMES_PLAYED,
        STATS_WIN_RATE,
        STATS_KDA,
        STATS_KILLS_DEATHS,
        STATS_KILLS_LABEL,
        STATS_DEATHS_LABEL,
        STATS_WINS_LOSSES,
        STATS_WINS_LABEL,
        STATS_LOSSES_LABEL,
        STATS_ROUNDS_LABEL,
        STATS_ROUNDS_WON,
        STATS_ROUNDS_LOST,
        STATS_AGENTS_LABEL,
        STATS_MOST_PLAYED,
        STATS_BEST_KILLS_AGENT,
        STATS_AGENT_KILLS,
        STATS_AGENT_GAMES,
        STATS_AGENT_WINRATE,

        LB_TITLE,
        LB_NO_DATA,
        LB_NO_DATA_LORE,
        LB_KILLS_LABEL,
        LB_KDA_LABEL,
        LB_WINRATE_LABEL,
        LB_FAV_AGENT_LABEL,

        NAV_PROFILE,
        NAV_STATS,
        NAV_LEADERBOARD,
        NAV_CURRENT_PAGE,
        NAV_CLICK_TO_OPEN,
        NAV_CLOSE,
    }

    private static final Map<Key, String> FR = new HashMap<>();
    private static final Map<Key, String> EN = new HashMap<>();

    public static void load(UbuWool plugin) {
        loadLocale(plugin, "messages_fr.yml", FR, "FR");
        loadLocale(plugin, "messages_en.yml", EN, "EN");
    }

    private static void loadLocale(UbuWool plugin, String filename,
                                   Map<Key, String> target, String localeName) {
        plugin.saveResource(filename, false);

        java.io.File file = new java.io.File(plugin.getDataFolder(), filename);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        InputStream defaultStream = plugin.getResource(filename);
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            yaml.setDefaults(defaults);
        }

        int loaded = 0, missing = 0;
        Logger log = plugin.getLogger();

        for (Key key : Key.values()) {
            String val = yaml.getString(key.name());
            if (val != null) {
                target.put(key, val);
                loaded++;
            } else {
                target.put(key, "§c[MISSING:" + key.name() + "]");
                missing++;
                log.warning("[Lang/" + localeName + "] Clé manquante : " + key.name());
            }
        }
        log.info("[Lang] " + localeName + " chargé — " + loaded + " clés, " + missing + " manquantes.");
    }

    // ---- API publique ----

    public static String get(Player player, Key key) {
        String lang = PlayerProfile.getLanguage(player);
        Map<Key, String> map = "EN".equals(lang) ? EN : FR;
        return map.getOrDefault(key, FR.getOrDefault(key, key.name()));
    }

    public static String get(Player player, Key key, Object... args) {
        return String.format(get(player, key), args);
    }

    public static void broadcast(Server server, Key key, Object... args) {
        for (Player p : server.getOnlinePlayers()) {
            p.sendMessage(get(p, key, args));
        }
    }

    public static String getRaw(Key key) {
        return FR.getOrDefault(key, key.name());
    }
}