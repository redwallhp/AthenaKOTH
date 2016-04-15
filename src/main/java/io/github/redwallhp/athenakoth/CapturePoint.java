package io.github.redwallhp.athenakoth;


import io.github.redwallhp.athenagm.events.PlayerScorePointEvent;
import io.github.redwallhp.athenagm.matches.Match;
import io.github.redwallhp.athenagm.matches.Team;
import io.github.redwallhp.athenagm.utilities.ItemUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
    private int captureDistance;
    private Team owner;
    private Team capturingTeam;
    private Player capturingPlayer;
    private CaptureTask captureTask;
    private BossBar captureBar;


    public CapturePoint(Match match) throws IOException, ConfigurationException {

        File file = new File(match.getMap().getPath(), "koth.yml");
        if (!file.exists()) throw new IOException(String.format("No koth.yml found at path %s", file.getPath()));
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        this.match = match;
        this.owner = null;
        this.capturingTeam = null;
        this.capturingPlayer = null;
        this.captureTask = null;
        this.captureBar = null;

        this.beacon = new Vector(yaml.getInt("beacon.x"),  yaml.getInt("beacon.y"), yaml.getInt("beacon.z"));
        this.captureTime = yaml.getInt("capture_time", 6);
        this.captureDistance = yaml.getInt("capture_distance", 8);

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
                stopCaptureTask();
                match.playSound(Sound.ENTITY_ITEM_BREAK);
            }
        }
        else if (!team.equals(getOwner())) {
            match.playSound(Sound.UI_BUTTON_CLICK);
            this.capturingTeam = team;
            this.capturingPlayer = player;
            this.captureTask = new CaptureTask(plugin, this);
            createCaptureBar(team);
        }
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


    /**
     * Clear the capture timer and associated data
     */
    public void stopCaptureTask() {
        if (this.captureTask != null) this.captureTask.cancel();
        if (this.captureBar != null) this.captureBar.removeAll();
        this.capturingTeam = null;
        this.capturingPlayer = null;
        this.captureTask = null;
        this.captureBar = null;
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
     * Check if there are any team players within an x block radius (+/- 1 blocks vertical) from
     * the capture point.
     * @param team The team to check
     * @return true if there are players present
     */
    public boolean hasTeamPlayersNearby(Team team) {
        for (Player player : team.getPlayers()) {
            Location loc = player.getLocation();
            double distance = Math.pow(beacon.getX() - loc.getX(), 2) + Math.pow(beacon.getZ() - loc.getZ(), 2);
            if (distance < Math.pow(captureDistance, 2)) {
                double maxY = beacon.getY() + 1;
                double minY = beacon.getY() - 1;
                if (loc.getY() <= maxY && loc.getY() >= minY) return true;
            }
        }
        return false;
    }


    /**
     * Create a new capture bar at the top of the screen when a team starts capturing.
     * @param team The team capturing
     */
    private void createCaptureBar(Team team) {
        String title = String.format("%s%s is capturing...", team.getColoredName(), ChatColor.RESET);
        captureBar = match.getPlugin().getServer().createBossBar(title, BarColor.PURPLE, BarStyle.SOLID);
        for (Player p : team.getMatch().getAllPlayers()) {
            captureBar.addPlayer(p);
        }
    }


    /**
     * Calculate and update the capture bar percentage
     * @param seconds Seconds progressed toward the capture limit
     */
    public void setBarProgress(int seconds) {
        double progress;
        try {
            progress = (double)seconds / (double)captureTime;
        } catch (ArithmeticException ex) {
            progress = 1.0;
        }
        captureBar.setProgress(progress);
    }


    /**
     * Get the coordinates of the beacon block
     */
    public Vector getBeacon() {
        return beacon;
    }


    /**
     * Get the time, in seconds, it takes to capture this point
     */
    public int getCaptureTime() {
        return captureTime;
    }


    /**
     * Get the Team that currently holds the capture point
     * @return Team object, or null if nobody owns the point
     */
    public Team getOwner() {
        return owner;
    }


    /**
     * Set the Team that holds the capture point
     */
    public void setOwner(Team owner) {
        this.owner = owner;
    }


    /**
     * Get the Match this capture point is associated with
     */
    public Match getMatch() {
        return match;
    }


    /**
     * Get the Team that is currently capturing the point
     */
    public Team getCapturingTeam() {
        return capturingTeam;
    }


    /**
     * Get the Player that is currently capturing the point
     */
    public Player getCapturingPlayer() {
        return capturingPlayer;
    }


}
