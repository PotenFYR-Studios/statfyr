package in.potenfyr.statfyr.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * A point-in-time snapshot of a player's canonical metrics.
 */
public final class Snapshot {

    public long ts;

    public Map<String, Long> metrics =
            new HashMap<>();

    public Snapshot() {
    }

    public Snapshot(long ts, Map<String, Long> metrics) {

        this.ts = ts;
        this.metrics = metrics;
    }

    public long get(String key) {

        Long value =
                metrics.get(key);

        return value == null ? 0L : value;
    }
}
