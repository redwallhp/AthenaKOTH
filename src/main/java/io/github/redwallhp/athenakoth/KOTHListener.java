package io.github.redwallhp.athenakoth;


import io.github.redwallhp.athenagm.arenas.Arena;
import io.github.redwallhp.athenagm.events.MatchCreateEvent;
import io.github.redwallhp.athenagm.matches.Match;
import io.github.redwallhp.athenagm.matches.Team;
import io.github.redwallhp.athenagm.utilities.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

public class KOTHListener implements Listener {


    private AthenaKOTH plugin;


    public KOTHListener(AthenaKOTH plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    /**
     * When a KOTH match starts, build a CapturePoint object and save it.
     * Also, update the beacon glass to ensure it's present and accurate.
     */
    @EventHandler
    public void onMatchCreate(MatchCreateEvent event) {
        if (!isKOTH(event.getMatch())) return;
        try {
            CapturePoint cap = new CapturePoint(event.getMatch());
            plugin.getCapturePoints().put(event.getMatch(), cap);
            cap.updateBeaconGlass();
        } catch (Exception ex) {
            plugin.getLogger().warning(ex.getMessage());
        }
    }


    /**
     * Initiate the capture process when a player interacts with the beacon or its glass.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isKOTH(event.getPlayer())) return;
        if (event.getClickedBlock() == null) return;
        CapturePoint cap = plugin.getCapturePointForPlayer(event.getPlayer());
        Team playerTeam = PlayerUtil.getTeamForPlayer(plugin.getAthena().getArenaHandler(), event.getPlayer());
        if (cap != null && playerTeam != null && !playerTeam.isSpectator()) {
            event.setCancelled(true);
            Vector beacon = cap.getBeacon();
            Vector glass = cap.getBeacon().clone().setY(cap.getBeacon().getY() + 1);
            Vector clicked = event.getClickedBlock().getLocation().toVector();
            if ( (clicked.equals(beacon) || clicked.equals(glass)) && !playerTeam.equals(cap.getOwner()) ) {
                cap.startCapture(plugin, playerTeam, event.getPlayer());
            }
        }
    }


    /**
     * Check if a given Match/Map is calling for a KOTH gamemode
     * @param match The Match to check
     */
    private boolean isKOTH(Match match) {
        return (match.getMap().getGameMode().equalsIgnoreCase("koth"));
    }


    /**
     * Check if a given Player is in a Match/Map calling for a KOTH gamemode
     * @param player The Player to check
     */
    private boolean isKOTH(Player player) {
        Arena arena = PlayerUtil.getArenaForPlayer(plugin.getAthena().getArenaHandler(), player);
        return (arena != null && isKOTH(arena.getMatch()));
    }


}
