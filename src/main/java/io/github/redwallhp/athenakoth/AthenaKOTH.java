package io.github.redwallhp.athenakoth;

import io.github.redwallhp.athenagm.AthenaGM;
import io.github.redwallhp.athenagm.arenas.Arena;
import io.github.redwallhp.athenagm.matches.Match;
import io.github.redwallhp.athenagm.utilities.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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
        }
    }


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


    public AthenaGM getAthena() {
        return athena;
    }


    public HashMap<Match, CapturePoint> getCapturePoints() {
        return capturePoints;
    }


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


}
