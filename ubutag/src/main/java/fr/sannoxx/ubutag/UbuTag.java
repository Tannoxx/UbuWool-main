package fr.sannoxx.ubutag;

import fr.sannoxx.ubutag.arena.ArenaManager;
import fr.sannoxx.ubutag.command.UbuTagCommand;
import fr.sannoxx.ubutag.listener.GameListener;
import fr.sannoxx.ubutag.placeholder.UbuTagPlaceholders;
import fr.sannoxx.ubutag.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Plugin TNT Tag (UbuTag) inspiré du mode TNT Tag d'Hypixel.
 *
 * Fonctionnalités principales :
 *  - Multi-arènes avec lobby d'attente, countdown, plusieurs rounds
 *  - Au démarrage, un pourcentage des joueurs reçoit la TNT (porteur "It")
 *  - Les porteurs doivent passer la TNT en frappant ou en cliquant droit sur
 *    un autre joueur. À la fin du round, tous les porteurs explosent et sont
 *    éliminés. Le dernier joueur en vie remporte la partie.
 *  - Stats persistantes (wins, kills, tags passés, parties jouées, etc.)
 *  - Hook PlaceholderAPI (softdepend) pour exposer les stats / état de jeu
 *  - Commandes joueurs (/ubutag join|leave|stats|top|list|help)
 *  - Commandes admin (création / édition d'arènes, force-start/stop, reload)
 */
public class UbuTag extends JavaPlugin {

    private static UbuTag instance;

    private Messages messages;
    private ArenaManager arenaManager;
    private StatsManager statsManager;
    private UbuTagPlaceholders placeholders;

    public static UbuTag get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.messages = new Messages(this);
        this.statsManager = new StatsManager(this);
        this.arenaManager = new ArenaManager(this);

        this.arenaManager.loadAll();
        this.statsManager.loadAll();

        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);

        UbuTagCommand cmd = new UbuTagCommand(this);
        Objects.requireNonNull(getCommand("ubutag")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("ubutag")).setTabCompleter(cmd);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholders = new UbuTagPlaceholders(this);
            this.placeholders.register();
            getLogger().info("Hook PlaceholderAPI activé.");
        }

        getLogger().info("UbuTag activé : " + this.arenaManager.getAll().size() + " arène(s) chargée(s).");
    }

    @Override
    public void onDisable() {
        if (this.arenaManager != null) {
            this.arenaManager.shutdownAll();
            this.arenaManager.saveAll();
        }
        if (this.statsManager != null) {
            this.statsManager.saveAll();
        }
        if (this.placeholders != null) {
            this.placeholders.safeUnregister();
        }
        getLogger().info("UbuTag désactivé.");
    }

    public Messages messages()      { return messages; }
    public ArenaManager arenas()    { return arenaManager; }
    public StatsManager stats()     { return statsManager; }
}
