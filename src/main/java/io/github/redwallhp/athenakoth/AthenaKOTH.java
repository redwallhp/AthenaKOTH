package io.github.redwallhp.athenakoth;

import io.github.redwallhp.athenagm.AthenaGM;
import io.github.redwallhp.athenagm.arenas.Arena;
import io.github.redwallhp.athenagm.matches.Match;
import io.github.redwallhp.athenagm.utilities.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;


public class AthenaKOTH extends JavaPlugin {


    AthenaGM athena;
    KOTHListener listener;
    HashMap<Match, CapturePoint> capturePoints;


    @Override
    public void onEnable() {
        if (checkAthena()) {
            listener = new KOTHListener(this);
            capturePoints = new HashMap<Match, CapturePoint>();
            startScoreIncrementTask();
        }
    }


    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }


    /**
     * Load the server's AthenaGM instance, returning false if AthenaGM is not installed.
     * @return true if AthenaGM is installed and active, false otherwise
     */
    private boolean checkAthena() {
        Plugin plugin = getServer().getPluginManager().getPlugin("AthenaGM");
        if (plugin == null || !(plugin instanceof AthenaGM)) {
            this.setEnabled(false);
            return false;
        } else {
            athena = (AthenaGM) plugin;
            return true;
        }
    }


    /**
     * Get the server's AthenaGM instance
     * @return AthenaGM instance
     */
    public AthenaGM getAthena() {
        return athena;
    }


    /**
     * Get the active capture points on maps currently active
     */
    public HashMap<Match, CapturePoint> getCapturePoints() {
        return capturePoints;
    }


    /**
     * Get the capture point for the KOTH match the player is in
     * @param player The playe to check
     * @return CapturePoint object for the player's Match, or null
     */
    public CapturePoint getCapturePointForPlayer(Player player) {
        Arena arena = PlayerUtil.getArenaForPlayer(getAthena().getArenaHandler(), player);
        if (arena != null) {
            for (Map.Entry<Match, CapturePoint> entry : capturePoints.entrySet()) {
                if (arena.getMatch().equals(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }


    /**
     * Continually award one generic point to a team every minute a CapturePoint is held.
     */
    private void startScoreIncrementTask() {
        new BukkitRunnable() {
            public void run() {
                for (CapturePoint cap : getCapturePoints().values()) {
                    cap.awardTeamGenericPoint();
                }
            }
        }.runTaskTimer(this, 10L, 10L);
    }


}
