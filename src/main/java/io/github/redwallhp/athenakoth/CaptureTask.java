package io.github.redwallhp.athenakoth;


import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

public class CaptureTask extends BukkitRunnable {


    private AthenaKOTH plugin;
    private CapturePoint cap;
    int ticks;
    int missed;


    public CaptureTask(AthenaKOTH plugin, CapturePoint cap) {
        this.cap = cap;
        this.ticks = cap.getCaptureTime();
        this.missed = 0;
        this.runTaskTimer(plugin, 0L, 20L);
    }


    public void run() {

        ticks--;

        if (ticks <= 0) {
            cap.capture();
            return;
        }

        cap.getMatch().playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        // If the capturing team doesn't stay near the point, the capture will stop after
        // two ticks of being outside the radius.
        if (!cap.hasTeamPlayersNearby(cap.getCapturingTeam())) {
            if (missed == 2) {
                cap.stopCaptureTask();
            }
            missed++;
        }

    }


}
