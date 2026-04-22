package fr.sannoxx.ubuwool;

import fr.sannoxx.ubuwool.agent.Agent;
import fr.sannoxx.ubuwool.manager.GameManager;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerData {
    public final UUID uuid;
    public Agent agent;
    public int ubus;
    public int kills;
    public int deaths;
    public int ultimateKills;

    public PlayerData(Player player) {
        this.uuid = player.getUniqueId();
        this.agent = null;
        this.ubus = GameManager.getStartUbus();
        this.kills = 0;
        this.deaths = 0;
        this.ultimateKills = 0;
    }

    /**
     * Enregistre un kill et ajoute les Ubus correspondants.
     * @param killUbus nombre d'Ubus gagnés pour ce kill (lu depuis config)
     */
    public void addKill(int killUbus) {
        kills++;
        ultimateKills++;
        ubus += killUbus;
    }

    /** Enregistre une mort en session. */
    public void addDeath() {
        deaths++;
    }

    /** @deprecated Utiliser addKill(int killUbus) */
    @Deprecated
    public void addKill() {
        addKill(GameManager.getKillUbus());
    }

    public boolean isUltimateReady() {
        int required = (agent != null) ? agent.getUltimateKillsRequired() : 5;
        return ultimateKills >= required;
    }

    public void consumeUltimate() {
        ultimateKills = 0;
    }
}