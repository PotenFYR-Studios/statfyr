package in.potenfyr.statfyr.analytics;

import java.util.UUID;

/**
 * Optional callback invoked when a player or server milestone is reached.
 */
public interface MilestoneListener {

    /**
     * @param uuid    player UUID, or {@code null} for server milestones
     * @param key     stable milestone key
     * @param message human readable description
     */
    void onMilestone(UUID uuid, String key, String message);
}
