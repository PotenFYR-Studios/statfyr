package in.potenfyr.statfyr.storage;

/**
 * A lightweight, meaningful player activity event.
 */
public final class ActivityEvent {

    public long ts;
    public String type;
    public String detail;
    public long value;

    public ActivityEvent() {
    }

    public ActivityEvent(long ts, String type, String detail, long value) {

        this.ts = ts;
        this.type = type;
        this.detail = detail;
        this.value = value;
    }
}
