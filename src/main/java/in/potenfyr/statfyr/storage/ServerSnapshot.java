package in.potenfyr.statfyr.storage;

/**
 * A point-in-time snapshot of server-level concurrency.
 */
public final class ServerSnapshot {

    public long ts;
    public int online;
    public int uniqueToday;

    public ServerSnapshot() {
    }

    public ServerSnapshot(long ts, int online, int uniqueToday) {

        this.ts = ts;
        this.online = online;
        this.uniqueToday = uniqueToday;
    }
}
