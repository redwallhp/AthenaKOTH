package io.github.redwallhp.athenakoth;


import io.github.redwallhp.athenagm.events.PlayerScorePointEvent;
import io.github.redwallhp.athenagm.matches.Match;
import io.github.redwallhp.athenagm.matches.Team;
import io.github.redwallhp.athenagm.utilities.ItemUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Representation of the capture point, which should consist of a beacon with a glass block on top.
 */
public class CapturePoint {


    private Match match;
    private Vector beacon;
    private int captureTime;
    private Team owner;
    private Team capturingTeam;
    private Player capturingPlayer;
    private CaptureTask captureTask;


    public CapturePoint(Match match) throws IOException, ConfigurationException {

        File file = new File(match.getMap().getPath(), "koth.yml");
        if (!file.exists()) throw new IOException(String.format("No koth.yml found at path %s", file.getPath()));
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        this.match = match;
        this.owner = null;
        this.capturingTeam = null;
        this.capturingPlayer = null;
        this.captureTask = null;

        this.beacon = new Vector(yaml.getInt("beacon.x"),  yaml.getInt("beacon.y"), yaml.getInt("beacon.z"));
        this.captureTime = yaml.getInt("capture_time", 6);

        if (yaml.getConfigurationSection("beacon").getKeys(false).size() < 1) {
            throw new ConfigurationException("koth.yml must specify beacon coordinates!");
        }

    }


    /**
     * Start a timer and then change over the owner if it's not interrupted.
     * If called while a capture is in progress, cancel the timer if the calling player is on the other team.
     * Silently ignore duplicate calls from the same team.
     * @param team The team the capturing player is taking the point for
     * @param player The player who interacted with the block
     */
    public void startCapture(AthenaKOTH plugin, Team team, Player player) {
        if (capturingTeam != null) {
            if (!capturingTeam.equals(team)) {
                captureTask.cancel();
                captureTask = null;
                capturingTeam = null;
                capturingPlayer = null;
                match.playSound(Sound.ENTITY_ITEM_BREAK);
            }
            return;
        }
        match.playSound(Sound.UI_BUTTON_CLICK);
        this.capturingTeam = team;
        this.capturingPlayer = player;
        this.captureTask = new CaptureTask(plugin, this);
    }


    /**
     * Capture the point
     */
    public void capture() {
        this.owner = capturingTeam;
        updateBeaconGlass();
        match.playSound(Sound.ENTITY_WITHER_SPAWN);
        PlayerScorePointEvent event = new PlayerScorePointEvent(capturingPlayer, capturingTeam, 1);
        Bukkit.getPluginManager().callEvent(event);
        stopCaptureTask();
    }


    public void stopCaptureTask() {
        this.captureTask.cancel();
        this.capturingTeam = null;
        this.capturingPlayer = null;
        this.captureTask = null;
    }


    /**
     * Set the glass block atop the beacon to be the appropriate color.
     * If there is no current owner, the glass block will be white. Otherwise, it will be dynamically changed to the
     * owner team's color.
     */
    public void updateBeaconGlass() {
        Vector glassVector = beacon.clone().setY(beacon.getY() + 1);
        Block glass = glassVector.toLocation(match.getWorld()).getBlock();
        glass.setType(Material.STAINED_GLASS);
        if (owner == null) {
            // set the color to be white
            DyeColor dye = DyeColor.getByColor(Color.WHITE);
            glass.setData(dye.getData());
        } else {
            // set the color to be the owner team's color
            byte data = ItemUtil.getDyeColorByte(owner.getColor());
            glass.setData(data);
        }
        glass.getState().update();
    }


    /**
     * Check if there are any team players within an 8 block radius (+/- 3 blocks vertical) from
     * the capture point.
     * @param team The team to check
     * @return true if there are players present
     */
    public boolean hasTeamPlayersNearby(Team team) {
        for (Player player : team.getPlayers()) {
            Location loc = player.getLocation();
            double distance = Math.pow(beacon.getX() - loc.getX(), 2) + Math.pow(beacon.getZ() - loc.getZ(), 2);
            if (distance < Math.pow(8, 2)) {
                double maxY = beacon.getY() + 3;
                double minY = beacon.getY() - 3;
                if (loc.getY() <= maxY && loc.getY() >= minY) return true;
            }
        }
        return false;
    }


    public Vector getBeacon() {
        return beacon;
    }


    public int getCaptureTime() {
        return captureTime;
    }


    public Team getOwner() {
        return owner;
    }


    public void setOwner(Team owner) {
        this.owner = owner;
    }


    public Match getMatch() {
        return match;
    }


    public Team getCapturingTeam() {
        return capturingTeam;
    }


    public Player getCapturingPlayer() {
        return capturingPlayer;
    }


}
