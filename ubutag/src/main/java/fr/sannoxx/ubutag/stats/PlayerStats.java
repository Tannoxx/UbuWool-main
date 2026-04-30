package fr.sannoxx.ubutag.stats;

import java.util.UUID;

/** Stats persistantes d'un joueur. */
public class PlayerStats {

    public final UUID uuid;
    public String name = "";

    public int wins;
    public int gamesPlayed;
    /** Nombre d'éliminations (joueurs explosés à cause d'une TNT que tu leur as passée). */
    public int kills;
    /** Nombre total de TNT passées avec succès à un autre joueur. */
    public int tagsPassed;
    /** Meilleure série de victoires consécutives. */
    public int bestStreak;
    /** Série de victoires en cours. */
    public int currentStreak;
    /** Temps total survécu (en secondes). */
    public long survivalSeconds;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }

    public double winRate() {
        if (gamesPlayed <= 0) return 0;
        return (double) wins / (double) gamesPlayed;
    }
}
