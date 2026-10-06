package in.potenfyr.statfyr.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * An archived leaderboard result for a completed period window.
 *
 * <p>Written when a daily/weekly/monthly window rolls over so historical
 * leaderboard results stay accessible after their counters reset.
 */
public final class PeriodArchive {

    /** daily / weekly / monthly. */
    public String period;

    /** canonical metric key. */
    public String metric;

    /** window start (epoch millis). */
    public long start;

    /** window end (epoch millis). */
    public long end;

    /** when the archive was generated (epoch millis). */
    public long generatedAt;

    public List<Entry> entries =
            new ArrayList<>();

    public PeriodArchive() {
    }

    public PeriodArchive(
            String period,
            String metric,
            long start,
            long end,
            long generatedAt,
            List<Entry> entries
    ) {

        this.period = period;
        this.metric = metric;
        this.start = start;
        this.end = end;
        this.generatedAt = generatedAt;
        this.entries = entries;
    }

    /**
     * A single archived leaderboard row.
     */
    public static final class Entry {

        public String uuid;
        public String name;
        public long value;
        public double decimal;

        public Entry() {
        }

        public Entry(
                String uuid,
                String name,
                long value,
                double decimal
        ) {

            this.uuid = uuid;
            this.name = name;
            this.value = value;
            this.decimal = decimal;
        }
    }
}
