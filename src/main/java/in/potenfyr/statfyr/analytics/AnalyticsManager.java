package in.potenfyr.statfyr.analytics;

import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.compat.SchedulerCompat;
import in.potenfyr.statfyr.model.PlayerStats;
import in.potenfyr.statfyr.storage.ActivityEvent;
import in.potenfyr.statfyr.storage.FileStorage;
import in.potenfyr.statfyr.storage.PeriodArchive;
import in.potenfyr.statfyr.storage.ServerSnapshot;
import in.potenfyr.statfyr.storage.Snapshot;
import in.potenfyr.statfyr.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central analytics engine.
 *
 * <p>Owns player profiles, session tracking, periodic snapshots, historical
 * storage, leaderboards, retention, segmentation and server analytics.
 *
 * <p>All heavy work runs off the main thread. The engine is deliberately
 * storage-agnostic: the default backend is an append-only JSONL store, but any
 * {@link Storage} implementation can be plugged in.
 */
public final class AnalyticsManager {

    /**
     * Optional economy provider (Vault).
     */
    public interface BalanceProvider {

        double balance(UUID uuid, String name);
    }

    private static final long DAY_MS =
            24L * 60L * 60L * 1000L;

    private final Statfyr plugin;
    private final Storage storage;

    private final Map<UUID, PlayerProfile> profiles =
            new ConcurrentHashMap<>();

    private final AtomicLong totalSessionSeconds =
            new AtomicLong();

    private volatile ServerState serverState =
            new ServerState();

    private volatile boolean running;

    private volatile BalanceProvider balanceProvider;
    private volatile MilestoneListener milestoneListener;

    private final Set<String> announcedServerMilestones =
            ConcurrentHashMap.newKeySet();

    // -- cached settings -----------------------------------------------------
    private volatile boolean collectionEnabled = true;
    private volatile boolean sessionTracking = true;
    private volatile boolean activityTimeline = true;
    private volatile boolean afkEnabled = true;
    private volatile long afkThresholdMs = 300_000L;
    private volatile long retentionDays = 365L;
    private volatile int snapshotIntervalSeconds = 300;
    private volatile DayOfWeek weeklyResetDay = DayOfWeek.MONDAY;
    private volatile int weeklyResetHour = 0;
    private volatile int monthlyResetDay = 1;
    private volatile int kdrMinKills = 10;
    private volatile String serverId = "server-1";
    private volatile String serverName = "Survival";
    private volatile Set<String> disabledMetrics =
            new HashSet<>();
    private volatile List<Integer> milestonePlaytimeHours =
            new ArrayList<>();
    private volatile List<Long> milestoneKills =
            new ArrayList<>();
    private volatile List<Long> milestoneBlocks =
            new ArrayList<>();
    private volatile List<Integer> milestoneUniquePlayers =
            new ArrayList<>();
    private volatile List<Integer> milestonePeakPlayers =
            new ArrayList<>();

    public AnalyticsManager(Statfyr plugin) {

        this.plugin = plugin;
        this.storage =
                new FileStorage(
                        plugin.getDataFolder(),
                        plugin.getLogger()
                );
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start() throws Exception {

        reloadSettings();

        storage.init();

        this.serverState =
                storage.loadServerState();

        serverState.serverId = serverId;
        serverState.serverName = serverName;

        loadAllProfiles();

        running = true;

        long snapshotTicks =
                Math.max(
                        20L,
                        (long) snapshotIntervalSeconds * 20L
                );

        SchedulerCompat.runAsyncRepeating(
                plugin,
                this::snapshot,
                snapshotTicks,
                snapshotTicks
        );

        /*
         * Live accrual: keeps playtime, active/AFK buckets and metric totals
         * fresh while players are ONLINE. Without this, those numbers only
         * moved when a player quit, so dashboards showed stale stats for
         * anyone still playing.
         */
        SchedulerCompat.runAsyncRepeating(
                plugin,
                this::accrueLive,
                20L * 30L,
                20L * 30L
        );

        SchedulerCompat.runAsyncRepeating(
                plugin,
                this::maintenance,
                20L * 60L * 60L,
                20L * 60L * 60L
        );

        SchedulerCompat.runAsyncRepeating(
                plugin,
                this::archivePeriods,
                20L * 300L,
                20L * 300L
        );

        plugin.getLogger().info(
                "Analytics engine ready ("
                        + profiles.size()
                        + " profiles, retention "
                        + (retentionDays <= 0
                        ? "unlimited"
                        : retentionDays + "d")
                        + ")."
        );
    }

    public void shutdown() {

        running = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            onQuit(player);
        }

        saveServerState();
        storage.close();
    }

    public void reloadSettings() {

        collectionEnabled =
                cfgBool("collection.enabled", true);

        sessionTracking =
                cfgBool("collection.session-tracking", true);

        activityTimeline =
                cfgBool("collection.activity-timeline", true);

        afkEnabled =
                cfgBool("collection.afk.enabled", true);

        afkThresholdMs =
                Math.max(
                        5L,
                        cfgLong("collection.afk.threshold-seconds", 300L)
                ) * 1000L;

        retentionDays =
                cfgLong("history.retention-days", 365L);

        snapshotIntervalSeconds =
                Math.max(
                        30,
                        cfgInt("collection.snapshot-interval-seconds", 300)
                );

        weeklyResetHour =
                cfgInt("leaderboards.weekly-reset-hour", 0);

        monthlyResetDay =
                cfgInt("leaderboards.monthly-reset-day", 1);

        kdrMinKills =
                cfgInt("leaderboards.minimums.kdr.kills", 10);

        serverId =
                cfgString("network.server-id", "server-1");

        serverName =
                cfgString("network.server-name", "Survival");

        try {

            weeklyResetDay =
                    DayOfWeek.valueOf(
                            cfgString(
                                    "leaderboards.weekly-reset-day",
                                    "MONDAY"
                            ).toUpperCase(Locale.ROOT)
                    );

        } catch (Exception ignored) {

            weeklyResetDay = DayOfWeek.MONDAY;
        }

        disabledMetrics =
                new HashSet<>(
                        cfgStringList("collection.disabled-metrics")
                );

        milestonePlaytimeHours =
                cfgIntList("milestones.playtime-hours");

        milestoneKills =
                cfgLongList("milestones.kills");

        milestoneBlocks =
                cfgLongList("milestones.blocks-mined");

        milestoneUniquePlayers =
                cfgIntList("milestones.unique-players");

        milestonePeakPlayers =
                cfgIntList("milestones.peak-players");

        if (serverState != null) {
            serverState.serverId = serverId;
            serverState.serverName = serverName;
        }
    }

    public void setBalanceProvider(BalanceProvider provider) {

        this.balanceProvider = provider;
    }

    public void setMilestoneListener(MilestoneListener listener) {

        this.milestoneListener = listener;
    }

    // -------------------------------------------------------------------------
    // Session tracking
    // -------------------------------------------------------------------------

    public void onJoin(Player player) {

        if (!collectionEnabled || !sessionTracking) {
            return;
        }

        long now =
                System.currentTimeMillis();

        PlayerProfile profile =
                profile(player.getUniqueId(), player.getName());

        boolean isNew =
                profile.firstSeen <= 0L || profile.totalSessions == 0;

        if (profile.firstSeen <= 0L) {
            profile.firstSeen = now;
        }

        profile.name = player.getName();
        profile.beginSession(now, null);
        profile.sessionStartMetrics = currentMetricMap(profile);

        serverState.knownPlayers.add(profile.uuid);
        serverState.recordSession(
                isoDay(now),
                ZonedDateTime.now().getHour(),
                ZonedDateTime.now().getDayOfWeek().name(),
                profile.uuid,
                isNew
        );
        serverState.recordPeak(isoDay(now), Bukkit.getOnlinePlayers().size());

        appendActivity(
                player.getUniqueId(),
                "join",
                player.getName(),
                0L
        );

        saveProfile(profile);

        if (isNew && cfgBool("integrations.discord.events.new-player", false)) {
            fireMilestone(
                    player.getUniqueId(),
                    "new_player",
                    player.getName() + " joined for the first time"
            );
        }
    }

    public void onQuit(Player player) {

        if (!collectionEnabled || !sessionTracking) {
            return;
        }

        PlayerProfile profile =
                profiles.get(player.getUniqueId());

        if (profile == null) {
            return;
        }

        long now =
                System.currentTimeMillis();

        boolean isAfk =
                afkEnabled
                        && (now - profile.lastActivity) > afkThresholdMs;

        profile.accrue(now, isAfk, true);

        long sessionSeconds =
                profile.endSession(now);

        Map<String, Long> end =
                currentMetricMap(profile);

        Map<String, Long> start =
                profile.sessionStartMetrics;

        long mined =
                delta(end, start, Metrics.BLOCKS_MINED);

        long kills =
                delta(end, start, Metrics.KILLS);

        long crafted =
                delta(end, start, Metrics.ITEMS_CRAFTED);

        long deaths =
                delta(end, start, Metrics.DEATHS);

        if (activityTimeline) {

            if (mined >= 50) {
                appendActivity(
                        player.getUniqueId(),
                        "mined",
                        null,
                        mined
                );
            }

            if (crafted >= 10) {
                appendActivity(
                        player.getUniqueId(),
                        "crafted",
                        null,
                        crafted
                );
            }

            if (kills > 0) {
                appendActivity(
                        player.getUniqueId(),
                        "kills",
                        null,
                        kills
                );
            }

            if (deaths > 0) {
                appendActivity(
                        player.getUniqueId(),
                        "deaths",
                        null,
                        deaths
                );
            }

            appendActivity(
                    player.getUniqueId(),
                    "session",
                    null,
                    sessionSeconds
            );

            appendActivity(
                    player.getUniqueId(),
                    "leave",
                    player.getName(),
                    sessionSeconds
            );
        }

        totalSessionSeconds.addAndGet(sessionSeconds);

        checkMilestones(profile, now);

        saveProfile(profile);
        saveServerState();
    }

    /**
     * Marks a player as active (AFK detection input).
     *
     * @param player player that performed an action
     */
    public void touch(Player player) {

        if (!afkEnabled) {
            return;
        }

        PlayerProfile profile =
                profiles.get(player.getUniqueId());

        if (profile != null) {
            profile.lastActivity =
                    System.currentTimeMillis();
            profile.afk = false;
        }
    }

    // -------------------------------------------------------------------------
    // Snapshots
    // -------------------------------------------------------------------------

    private void snapshot() {

        if (!collectionEnabled || !running) {
            return;
        }

        long now =
                System.currentTimeMillis();

        String day =
                isoDay(now);

        for (Player player : Bukkit.getOnlinePlayers()) {

            try {

                PlayerProfile profile =
                        profile(player.getUniqueId(), player.getName());

                PlayerStats stats =
                        plugin.getStatsManager()
                                .getPlayerStats(player.getUniqueId());

                if (stats == null) {
                    continue;
                }

                Map<String, Long> metrics =
                        Metrics.extract(stats);

                applyMetrics(profile, metrics, now);

                if (balanceProvider != null) {

                    double balance =
                            balanceProvider.balance(
                                    player.getUniqueId(),
                                    player.getName()
                            );

                    profile.allTime.put(
                            Metrics.BALANCE,
                            (long) balance
                    );
                }

                boolean isAfk =
                        afkEnabled
                                && (now - profile.lastActivity) > afkThresholdMs;

                profile.afk = isAfk;
                profile.accrue(now, isAfk, true);
                profile.lastSeen = now;
                profile.lastSnapshotAt = now;

                storage.appendSnapshot(
                        player.getUniqueId(),
                        new Snapshot(now, metrics)
                );

                checkMilestones(profile, now);
                saveProfile(profile);

            } catch (Throwable throwable) {

                if (plugin.getConfigManager() != null
                        && plugin.getConfigManager().isDebug()) {

                    plugin.getLogger().warning(
                            "Snapshot failed for "
                                    + player.getName()
                                    + ": "
                                    + throwable.getMessage()
                    );
                }
            }
        }

        int online =
                Bukkit.getOnlinePlayers().size();

        serverState.recordPeak(day, online);

        Set<String> uniqueToday =
                uniqueForDays(lastDays(now, 1));

        storage.appendServerSnapshot(
                new ServerSnapshot(
                        now,
                        online,
                        uniqueToday.size()
                )
        );

        checkServerMilestones(online);

        saveServerState();
    }

    private void applyMetrics(
            PlayerProfile profile,
            Map<String, Long> metrics,
            long now
    ) {

        for (Map.Entry<String, Long> entry : metrics.entrySet()) {

            String key =
                    entry.getKey();

            if (disabledMetrics.contains(key)) {
                continue;
            }

            long value =
                    entry.getValue();

            long existing =
                    profile.allTime(key);

            if (value > existing) {
                profile.allTime.put(key, value);
            }
        }
    }

    /**
     * Lightweight pass over online players: accrues active/AFK time, applies
     * current metric totals and refreshes lastSeen without writing history
     * snapshots. Runs every 30 seconds so playtime and derived stats advance
     * live while players are still on the server.
     */
    private void accrueLive() {

        if (!collectionEnabled || !running) {
            return;
        }

        long now =
                System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {

            try {

                PlayerProfile profile =
                        profiles.get(player.getUniqueId());

                if (profile == null) {

                    profile =
                            profile(player.getUniqueId(), player.getName());
                }

                boolean isAfk =
                        afkEnabled
                                && (now - profile.lastActivity) > afkThresholdMs;

                profile.afk = isAfk;
                profile.accrue(now, isAfk, true);
                profile.lastSeen = now;

                PlayerStats stats =
                        plugin.getStatsManager()
                                .getPlayerStats(player.getUniqueId());

                if (stats != null) {
                    applyMetrics(
                            profile,
                            Metrics.extract(stats),
                            now
                    );
                }

                saveProfile(profile);

            } catch (Throwable ignored) {
                // A failed live tick must never break the loop; the next one
                // and the deeper snapshot pass will catch up.
            }
        }
    }

    // -------------------------------------------------------------------------
    // Maintenance
    // -------------------------------------------------------------------------

    private void maintenance() {

        if (!running) {
            return;
        }

        try {

            storage.prune(retentionDays);

            if (retentionDays > 0) {

                long cutoff =
                        System.currentTimeMillis()
                                - retentionDays * DAY_MS;

                String cutoffDay =
                        isoDay(cutoff);

                serverState.playersByDay
                        .keySet()
                        .removeIf(day -> day.compareTo(cutoffDay) < 0);

                serverState.peakByDay
                        .keySet()
                        .removeIf(day -> day.compareTo(cutoffDay) < 0);

                serverState.sessionsByDay
                        .keySet()
                        .removeIf(day -> day.compareTo(cutoffDay) < 0);

                serverState.newPlayersByDay
                        .keySet()
                        .removeIf(day -> day.compareTo(cutoffDay) < 0);
            }

            saveServerState();

        } catch (Throwable throwable) {

            plugin.getLogger().warning(
                    "Analytics maintenance failed: " + throwable.getMessage()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Profiles
    // -------------------------------------------------------------------------

    private void loadAllProfiles() {

        for (UUID uuid : storage.knownProfileIds()) {

            try {

                PlayerProfile profile =
                        storage.loadProfile(uuid);

                if (profile != null) {
                    profiles.put(uuid, profile);
                }

            } catch (Exception ignored) {
            }
        }
    }

    public PlayerProfile profile(UUID uuid, String name) {

        PlayerProfile profile =
                profiles.get(uuid);

        if (profile == null) {

            profile =
                    storage.loadProfile(uuid);

            if (profile == null) {

                profile =
                        new PlayerProfile(
                                uuid,
                                name,
                                System.currentTimeMillis()
                        );

            } else if (name != null && !name.isEmpty()) {

                profile.name = name;
            }

            profiles.put(uuid, profile);
        }

        return profile;
    }

    public PlayerProfile profileIfPresent(UUID uuid) {

        return profiles.get(uuid);
    }

    public Collection<PlayerProfile> allProfiles() {

        return profiles.values();
    }

    public void deleteProfile(UUID uuid) {

        profiles.remove(uuid);
        storage.deleteProfile(uuid);
    }

    public void purgeHistory() {

        storage.purgeHistory();
    }

    public int profileCount() {

        return profiles.size();
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    public long metric(UUID uuid, String metric) {

        PlayerProfile profile =
                profiles.get(uuid);

        if (profile == null) {
            return 0L;
        }

        return profile.total(Metrics.canonical(metric));
    }

    public double kdr(UUID uuid) {

        PlayerProfile profile =
                profiles.get(uuid);

        if (profile == null) {
            return 0.0;
        }

        long kills =
                profile.total(Metrics.KILLS);

        long deaths =
                profile.total(Metrics.DEATHS);

        return deaths == 0L
                ? kills
                : (double) kills / (double) deaths;
    }

    public Map<String, Long> metrics(UUID uuid) {

        Map<String, Long> result =
                new LinkedHashMap<>();

        PlayerProfile profile =
                profiles.get(uuid);

        if (profile == null) {
            return result;
        }

        for (String key : Metrics.leaderboardKeys()) {
            result.put(key, profile.total(key));
        }

        result.put(Metrics.ACTIVE_TIME, profile.activeSeconds);
        result.put(Metrics.AFK_TIME, profile.afkSeconds);
        result.put(Metrics.SESSIONS, (long) profile.totalSessions);

        return result;
    }

    public boolean isAfk(UUID uuid) {

        PlayerProfile profile =
                profiles.get(uuid);

        if (profile == null || !afkEnabled) {
            return false;
        }

        if (Bukkit.getPlayer(uuid) == null) {
            return false;
        }

        return (System.currentTimeMillis() - profile.lastActivity)
                > afkThresholdMs;
    }

    // -------------------------------------------------------------------------
    // Custom metrics
    // -------------------------------------------------------------------------

    /**
     * Registers a custom metric value for a player.
     *
     * @param metric metric name (for example {@code "economy.balance"})
     * @param uuid   player UUID
     * @param value  metric value
     */
    public void setCustomMetric(
            String metric,
            UUID uuid,
            double value
    ) {

        if (metric == null || uuid == null) {
            return;
        }

        PlayerProfile profile =
                profile(uuid, null);

        profile.customMetrics.put(metric, value);
    }

    public Map<String, Double> customMetrics(UUID uuid) {

        PlayerProfile profile =
                profiles.get(uuid);

        return profile == null
                ? new LinkedHashMap<>()
                : profile.customMetrics;
    }

    /**
     * @param metric custom metric name
     * @return every player's value for the metric, keyed by UUID
     */
    public Map<String, Double> customMetricValues(String metric) {

        Map<String, Double> result =
                new LinkedHashMap<>();

        for (PlayerProfile profile : profiles.values()) {

            Double value =
                    profile.customMetrics.get(metric);

            if (value != null) {
                result.put(profile.uuid, value);
            }
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Leaderboards
    // -------------------------------------------------------------------------

    public List<LeaderboardEntry> leaderboard(
            String stat,
            Period period,
            int limit
    ) {

        String metric =
                Metrics.canonical(stat);

        long now =
                System.currentTimeMillis();

        List<LeaderboardEntry> entries =
                new ArrayList<>();

        for (PlayerProfile profile : profiles.values()) {

            long value;
            long decimalScaled;

            if (Metrics.KDR.equals(metric)) {

                long kills =
                        periodValue(profile, Metrics.KILLS, period, now);

                long deaths =
                        periodValue(profile, Metrics.DEATHS, period, now);

                if (kills < kdrMinKills) {
                    continue;
                }

                double kdr =
                        deaths == 0L
                                ? kills
                                : (double) kills / (double) deaths;

                value = Math.round(kdr * 1000.0);
                decimalScaled = value;
                entries.add(
                        new LeaderboardEntry(
                                profile.uuid,
                                displayName(profile),
                                value,
                                kdr,
                                isOnline(profile)
                        )
                );

                continue;
            }

            value =
                    periodValue(profile, metric, period, now);

            if (value <= 0L) {
                continue;
            }

            entries.add(
                    new LeaderboardEntry(
                            profile.uuid,
                            displayName(profile),
                            value,
                            (double) value,
                            isOnline(profile)
                    )
            );
        }

        entries.sort(
                Comparator
                        .comparingDouble(
                                (LeaderboardEntry entry) -> entry.decimalValue
                        )
                        .reversed()
                        .thenComparing(
                                entry -> entry.name,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        if (limit > 0 && entries.size() > limit) {
            return new ArrayList<>(entries.subList(0, limit));
        }

        return entries;
    }

    /**
     * Computes a player's 1-based rank for a metric/period.
     *
     * @param uuid   player UUID
     * @param stat   stat alias
     * @param period aggregation window
     * @return 1-based rank, or {@code -1} when unranked
     */
    public int rank(UUID uuid, String stat, Period period) {

        String metric =
                Metrics.canonical(stat);

        PlayerProfile mine =
                profiles.get(uuid);

        if (mine == null) {
            return -1;
        }

        long now =
                System.currentTimeMillis();

        double myValue;

        if (Metrics.KDR.equals(metric)) {

            long kills =
                    periodValue(mine, Metrics.KILLS, period, now);

            if (kills < kdrMinKills) {
                return -1;
            }

            long deaths =
                    periodValue(mine, Metrics.DEATHS, period, now);

            myValue =
                    deaths == 0L
                            ? kills
                            : (double) kills / (double) deaths;

        } else {

            myValue =
                    periodValue(mine, metric, period, now);
        }

        if (myValue <= 0.0) {
            return -1;
        }

        int better =
                0;

        for (PlayerProfile other : profiles.values()) {

            if (other == mine) {
                continue;
            }

            double otherValue;

            if (Metrics.KDR.equals(metric)) {

                long kills =
                        periodValue(other, Metrics.KILLS, period, now);

                if (kills < kdrMinKills) {
                    continue;
                }

                long deaths =
                        periodValue(other, Metrics.DEATHS, period, now);

                otherValue =
                        deaths == 0L
                                ? kills
                                : (double) kills / (double) deaths;

            } else {

                otherValue =
                        periodValue(other, metric, period, now);
            }

            if (otherValue > myValue) {
                better++;
            }
        }

        return better + 1;
    }

    private long periodValue(
            PlayerProfile profile,
            String metric,
            Period period,
            long now
    ) {

        long current =
                profile.total(metric);

        if (period == Period.ALL_TIME) {
            return current;
        }

        String key =
                period.token();

        long windowStart =
                period.start(
                        now,
                        weeklyResetDay,
                        weeklyResetHour,
                        monthlyResetDay
                );

        Map<String, Long> baseline =
                profile.periodBaselines.get(key);

        Long recorded =
                profile.periodStarts.get(key);

        if (baseline == null || recorded == null) {

            profile.periodBaselines.put(
                    key,
                    currentMetricMap(profile)
            );

            profile.periodStarts.put(key, windowStart);

            return 0L;
        }

        /*
         * If the window has rolled over, report zero for the new window but
         * leave the baseline intact so the archive task can still compute the
         * finished window's values. The archive task resets the baseline.
         */
        if (recorded < windowStart) {
            return 0L;
        }

        Long startValue =
                baseline.get(metric);

        return Math.max(
                0L,
                current - (startValue == null ? 0L : startValue)
        );
    }

    // -------------------------------------------------------------------------
    // Period archival
    // -------------------------------------------------------------------------

    private static final String[] ARCHIVE_METRICS = {
            Metrics.KILLS,
            Metrics.DEATHS,
            Metrics.PLAYER_KILLS,
            Metrics.MOB_KILLS,
            Metrics.KDR,
            Metrics.PLAYTIME,
            Metrics.BLOCKS_MINED,
            Metrics.ITEMS_CRAFTED,
            Metrics.DISTANCE_TRAVELED,
            Metrics.SESSIONS
    };

    private void archivePeriods() {

        if (!running) {
            return;
        }

        long now =
                System.currentTimeMillis();

        Period[] periods = {
                Period.DAILY,
                Period.WEEKLY,
                Period.MONTHLY
        };

        for (Period period : periods) {

            try {

                long start =
                        period.start(
                                now,
                                weeklyResetDay,
                                weeklyResetHour,
                                monthlyResetDay
                        );

                Long previous =
                        serverState.periodWindowStarts.get(period.token());

                if (previous != null && previous < start) {

                    archiveWindow(period, previous, start, now);

                    for (PlayerProfile profile : profiles.values()) {

                        profile.periodBaselines.put(
                                period.token(),
                                currentMetricMap(profile)
                        );

                        profile.periodStarts.put(
                                period.token(),
                                start
                        );
                    }
                }

                serverState.periodWindowStarts.put(
                        period.token(),
                        start
                );

            } catch (Throwable ignored) {
            }
        }

        saveServerState();
    }

    private void archiveWindow(
            Period period,
            long start,
            long end,
            long now
    ) {

        String key =
                period.token();

        for (String metric : ARCHIVE_METRICS) {

            List<LeaderboardEntry> ranking =
                    windowLeaderboard(metric, key);

            if (ranking.isEmpty()) {
                continue;
            }

            List<PeriodArchive.Entry> entries =
                    new ArrayList<>();

            int rank = 0;

            for (LeaderboardEntry entry : ranking) {

                entries.add(
                        new PeriodArchive.Entry(
                                entry.uuid,
                                entry.name,
                                entry.value,
                                entry.decimalValue
                        )
                );

                if (++rank >= 100) {
                    break;
                }
            }

            storage.appendPeriodArchive(
                    new PeriodArchive(
                            key,
                            metric,
                            start,
                            end,
                            now,
                            entries
                    )
            );
        }
    }

    private List<LeaderboardEntry> windowLeaderboard(
            String metric,
            String periodKey
    ) {

        List<LeaderboardEntry> entries =
                new ArrayList<>();

        for (PlayerProfile profile : profiles.values()) {

            Map<String, Long> baseline =
                    profile.periodBaselines.get(periodKey);

            if (baseline == null) {
                continue;
            }

            if (Metrics.KDR.equals(metric)) {

                long kills =
                        windowValue(profile, baseline, Metrics.KILLS);

                if (kills < kdrMinKills) {
                    continue;
                }

                long deaths =
                        windowValue(profile, baseline, Metrics.DEATHS);

                double kdr =
                        deaths == 0L
                                ? kills
                                : (double) kills / (double) deaths;

                entries.add(
                        new LeaderboardEntry(
                                profile.uuid,
                                displayName(profile),
                                Math.round(kdr * 1000.0),
                                kdr,
                                false
                        )
                );

            } else {

                long value =
                        windowValue(profile, baseline, metric);

                if (value <= 0L) {
                    continue;
                }

                entries.add(
                        new LeaderboardEntry(
                                profile.uuid,
                                displayName(profile),
                                value,
                                (double) value,
                                false
                        )
                );
            }
        }

        entries.sort(
                Comparator
                        .comparingDouble(
                                (LeaderboardEntry entry) -> entry.decimalValue
                        )
                        .reversed()
                        .thenComparing(
                                entry -> entry.name,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        return entries;
    }

    private static long windowValue(
            PlayerProfile profile,
            Map<String, Long> baseline,
            String metric
    ) {

        long current =
                profile.total(metric);

        Long base =
                baseline.get(metric);

        return Math.max(
                0L,
                current - (base == null ? 0L : base)
        );
    }

    /**
     * @param period daily/weekly/monthly (or {@code null} for all)
     * @param from   earliest generation time
     * @param to     latest generation time
     * @param limit  maximum archives
     * @return archived leaderboard results
     */
    public List<PeriodArchive> archives(
            String period,
            long from,
            long to,
            int limit
    ) {

        return storage.readPeriodArchives(period, from, to, limit);
    }

    // -------------------------------------------------------------------------
    // Server analytics
    // -------------------------------------------------------------------------

    public ServerStats serverStats() {

        long now =
                System.currentTimeMillis();

        ServerStats stats =
                new ServerStats();

        int online =
                Bukkit.getOnlinePlayers().size();

        stats.online = online;

        String today =
                isoDay(now);

        List<String> last7 =
                lastDays(now, 7);

        List<String> last30 =
                lastDays(now, 30);

        stats.peakToday =
                Math.max(
                        online,
                        peakFor(today)
                );

        stats.peakWeek =
                peakForDays(last7, online);

        stats.peakMonth =
                peakForDays(last30, online);

        stats.peakAllTime =
                Math.max(online, serverState.peakAllTime);

        Set<String> uniqueToday =
                uniqueForDays(lastDays(now, 1));

        Set<String> uniqueWeek =
                uniqueForDays(last7);

        Set<String> uniqueMonth =
                uniqueForDays(last30);

        stats.uniqueToday = uniqueToday.size();
        stats.uniqueWeek = uniqueWeek.size();
        stats.uniqueMonth = uniqueMonth.size();
        stats.totalPlayers = profiles.size();

        long playtime =
                0L;

        for (PlayerProfile profile : profiles.values()) {
            playtime += profile.totalPlaytimeSeconds;
        }

        stats.totalPlaytimeSeconds = playtime;
        stats.sessionsTotal = serverState.totalSessions;

        stats.averageSessionSeconds =
                serverState.totalSessions == 0L
                        ? 0.0
                        : (double) playtime
                        / (double) serverState.totalSessions;

        long firstSeen =
                Long.MAX_VALUE;

        for (PlayerProfile profile : profiles.values()) {

            if (profile.firstSeen > 0L
                    && profile.firstSeen < firstSeen) {

                firstSeen = profile.firstSeen;
            }
        }

        double days =
                firstSeen == Long.MAX_VALUE
                        ? 1.0
                        : Math.max(
                        1.0,
                        (now - firstSeen) / (double) DAY_MS
                );

        stats.sessionsPerDay =
                serverState.totalSessions / days;

        Integer newToday =
                serverState.newPlayersByDay.get(today);

        stats.newPlayersToday =
                newToday == null ? 0 : newToday;

        stats.returningPlayersToday =
                Math.max(
                        0,
                        stats.uniqueToday - stats.newPlayersToday
                );

        stats.averageConcurrent =
                averageConcurrent(now);

        return stats;
    }

    private double averageConcurrent(long now) {

        List<ServerSnapshot> recent =
                storage.readServerHistory(
                        now - DAY_MS,
                        now,
                        4096
                );

        if (recent.isEmpty()) {
            return Bukkit.getOnlinePlayers().size();
        }

        long total =
                0L;

        for (ServerSnapshot snapshot : recent) {
            total += snapshot.online;
        }

        return (double) total / (double) recent.size();
    }

    /**
     * @return read-only economy analytics derived from Vault balances, or an
     *         {@code enabled:false} map when Vault is not present
     */
    public Map<String, Object> economyStats() {

        Map<String, Object> result =
                new LinkedHashMap<>();

        if (balanceProvider == null) {

            result.put("enabled", false);
            return result;
        }

        result.put("enabled", true);

        List<Double> balances =
                new ArrayList<>();

        double total =
                0.0;

        double highest =
                0.0;

        double lowest =
                Double.MAX_VALUE;

        for (PlayerProfile profile : profiles.values()) {

            double balance =
                    profile.allTime(Metrics.BALANCE);

            balances.add(balance);
            total += balance;

            if (balance > highest) {
                highest = balance;
            }

            if (balance < lowest) {
                lowest = balance;
            }
        }

        if (balances.isEmpty()) {
            lowest = 0.0;
        }

        java.util.Collections.sort(balances);

        double median =
                0.0;

        if (!balances.isEmpty()) {

            int middle =
                    balances.size() / 2;

            median =
                    balances.size() % 2 == 0
                            ? (balances.get(middle - 1)
                            + balances.get(middle)) / 2.0
                            : balances.get(middle);
        }

        result.put("players", balances.size());
        result.put("total_in_circulation", total);
        result.put(
                "average",
                balances.isEmpty() ? 0.0 : total / balances.size()
        );
        result.put("median", median);
        result.put("highest", highest);
        result.put("lowest", lowest);

        return result;
    }

    public Map<String, Object> heatmap() {

        Map<String, Object> result =
                new LinkedHashMap<>();
        result.put("sessions_by_hour", serverState.sessionsByHour);
        result.put("sessions_by_weekday", serverState.sessionsByWeekday);
        result.put("peak_by_day", serverState.peakByDay);
        result.put("sessions_by_day", serverState.sessionsByDay);
        result.put("new_players_by_day", serverState.newPlayersByDay);

        return result;
    }

    // -------------------------------------------------------------------------
    // Retention & segmentation
    // -------------------------------------------------------------------------

    public Map<String, Object> retention() {

        Map<String, Set<String>> dayPlayers =
                new HashMap<>();

        for (Map.Entry<String, List<String>> entry
                : serverState.playersByDay.entrySet()) {

            dayPlayers.put(
                    entry.getKey(),
                    new HashSet<>(entry.getValue())
            );
        }

        int[] windows = {1, 7, 14, 30};

        Map<String, Integer> retained =
                new LinkedHashMap<>();

        Map<String, Integer> cohorts =
                new LinkedHashMap<>();

        for (int window : windows) {
            retained.put("d" + window, 0);
            cohorts.put("d" + window, 0);
        }

        long now =
                System.currentTimeMillis();

        for (PlayerProfile profile : profiles.values()) {

            if (profile.firstSeen <= 0L) {
                continue;
            }

            String cohort =
                    isoDay(profile.firstSeen);

            for (int window : windows) {

                if (now - profile.firstSeen
                        < (long) window * DAY_MS) {
                    continue;
                }

                cohorts.put("d" + window, cohorts.get("d" + window) + 1);

                if (playedWithin(cohort, window, dayPlayers)) {
                    retained.put(
                            "d" + window,
                            retained.get("d" + window) + 1
                    );
                }
            }
        }

        Map<String, Object> result =
                new LinkedHashMap<>();

        for (int window : windows) {

            int cohort = cohorts.get("d" + window);
            int kept = retained.get("d" + window);

            result.put(
                    "d" + window,
                    cohort == 0 ? 0.0 : (double) kept / (double) cohort
            );
        }

        result.put("cohorts", cohorts);
        result.put("retained", retained);

        return result;
    }

    private boolean playedWithin(
            String cohortDay,
            int window,
            Map<String, Set<String>> dayPlayers
    ) {

        LocalDate cohort =
                LocalDate.parse(cohortDay);

        for (int offset = 1; offset <= window; offset++) {

            String day =
                    cohort.plusDays(offset).toString();

            Set<String> players =
                    dayPlayers.get(day);

            if (players != null && !players.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public String segment(PlayerProfile profile) {

        long now =
                System.currentTimeMillis();

        long sinceLast =
                (now - profile.lastSeen) / DAY_MS;

        long sinceFirst =
                (now - profile.firstSeen) / DAY_MS;

        if (sinceLast > 60) {
            return "churned";
        }

        if (sinceLast > 30) {
            return "inactive";
        }

        if (sinceFirst <= 7) {
            return "new";
        }

        if (sinceLast > 14) {
            return "at_risk";
        }

        if (sinceLast < 3 && profile.totalSessions >= 20) {
            return "highly_active";
        }

        return "active";
    }

    public Map<String, Integer> segments() {

        Map<String, Integer> counts =
                new LinkedHashMap<>();

        for (PlayerProfile profile : profiles.values()) {

            String segment =
                    segment(profile);

            counts.merge(segment, 1, Integer::sum);
        }

        return counts;
    }

    // -------------------------------------------------------------------------
    // History access
    // -------------------------------------------------------------------------

    public List<Snapshot> playerHistory(
            UUID uuid,
            long from,
            long to,
            int limit
    ) {

        return storage.readPlayerHistory(uuid, from, to, limit);
    }

    public List<ServerSnapshot> serverHistory(
            long from,
            long to,
            int limit
    ) {

        return storage.readServerHistory(from, to, limit);
    }

    public List<ActivityEvent> activity(
            UUID uuid,
            long from,
            long to,
            int limit
    ) {

        return storage.readActivity(uuid, from, to, limit);
    }

    public void appendActivity(
            UUID uuid,
            String type,
            String detail,
            long value
    ) {

        if (!activityTimeline || uuid == null) {
            return;
        }

        final ActivityEvent event =
                new ActivityEvent(
                        System.currentTimeMillis(),
                        type,
                        detail,
                        value
                );

        try {

            plugin.getExecutorService().execute(
                    () -> storage.appendActivity(uuid, event)
            );

        } catch (Throwable ignored) {

            storage.appendActivity(uuid, event);
        }
    }

    // -------------------------------------------------------------------------
    // Milestones
    // -------------------------------------------------------------------------

    private void checkMilestones(
            PlayerProfile profile,
            long now
    ) {

        if (milestoneListener == null
                || !plugin.getConfig().getBoolean("milestones.enabled", true)) {

            return;
        }

        long hours =
                profile.totalPlaytimeSeconds / 3600L;

        for (Integer milestone : milestonePlaytimeHours) {

            if (hours >= milestone) {

                String key =
                        "playtime_" + milestone + "h";

                if (profile.milestones.put(key, now) == null) {

                    fireMilestone(
                            profile.id(),
                            key,
                            displayName(profile)
                                    + " reached "
                                    + milestone
                                    + " hours of playtime"
                    );
                }
            }
        }

        for (Long milestone : milestoneKills) {

            if (profile.total(Metrics.KILLS) >= milestone) {

                String key =
                        "kills_" + milestone;

                if (profile.milestones.put(key, now) == null) {

                    fireMilestone(
                            profile.id(),
                            key,
                            displayName(profile)
                                    + " reached "
                                    + milestone
                                    + " kills"
                    );
                }
            }
        }

        for (Long milestone : milestoneBlocks) {

            if (profile.total(Metrics.BLOCKS_MINED) >= milestone) {

                String key =
                        "blocks_" + milestone;

                if (profile.milestones.put(key, now) == null) {

                    fireMilestone(
                            profile.id(),
                            key,
                            displayName(profile)
                                    + " mined "
                                    + milestone
                                    + " blocks"
                    );
                }
            }
        }
    }

    private void checkServerMilestones(int online) {

        if (milestoneListener == null) {
            return;
        }

        for (Integer milestone : milestonePeakPlayers) {

            if (online >= milestone) {

                String key =
                        "peak_" + milestone;

                if (announcedServerMilestones.add(key)) {

                    fireMilestone(
                            null,
                            key,
                            "Server reached "
                                    + online
                                    + " concurrent players"
                    );
                }
            }
        }

        int unique =
                serverState.knownPlayers.size();

        for (Integer milestone : milestoneUniquePlayers) {

            if (unique >= milestone) {

                String key =
                        "unique_" + milestone;

                if (announcedServerMilestones.add(key)) {

                    fireMilestone(
                            null,
                            key,
                            "Server reached "
                                    + unique
                                    + " unique players"
                    );
                }
            }
        }
    }

    private void fireMilestone(
            UUID uuid,
            String key,
            String message
    ) {

        try {

            MilestoneListener listener =
                    milestoneListener;

            if (listener != null) {
                listener.onMilestone(uuid, key, message);
            }

        } catch (Throwable ignored) {
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Long> currentMetricMap(PlayerProfile profile) {

        Map<String, Long> map =
                new HashMap<>();

        for (String key : Metrics.leaderboardKeys()) {
            map.put(key, profile.total(key));
        }

        map.put(Metrics.PLAYTIME, profile.total(Metrics.PLAYTIME));
        map.put(Metrics.ACTIVE_TIME, profile.activeSeconds);
        map.put(Metrics.AFK_TIME, profile.afkSeconds);
        map.put(Metrics.SESSIONS, (long) profile.totalSessions);

        return map;
    }

    private static long delta(
            Map<String, Long> end,
            Map<String, Long> start,
            String key
    ) {

        long endValue =
                end.get(key) == null ? 0L : end.get(key);

        long startValue =
                start == null || start.get(key) == null
                        ? 0L
                        : start.get(key);

        return Math.max(0L, endValue - startValue);
    }

    private String displayName(PlayerProfile profile) {

        if (profile.name != null && !profile.name.isEmpty()) {
            return profile.name;
        }

        return "unknown";
    }

    private boolean isOnline(PlayerProfile profile) {

        UUID uuid =
                profile.id();

        return uuid != null && Bukkit.getPlayer(uuid) != null;
    }

    public String serverId() {
        return serverId;
    }

    public String serverName() {
        return serverName;
    }

    public ServerState serverState() {
        return serverState;
    }

    private int peakFor(String day) {

        Integer peak =
                serverState.peakByDay.get(day);

        return peak == null ? 0 : peak;
    }

    private int peakForDays(List<String> days, int fallback) {

        int peak =
                fallback;

        for (String day : days) {

            Integer value =
                    serverState.peakByDay.get(day);

            if (value != null && value > peak) {
                peak = value;
            }
        }

        return peak;
    }

    private Set<String> uniqueForDays(List<String> days) {

        return serverState.uniqueAcross(days);
    }

    // -------------------------------------------------------------------------
    // Config helpers
    // -------------------------------------------------------------------------

    private boolean cfgBool(String path, boolean def) {

        try {
            return plugin.getConfig().getBoolean(path, def);
        } catch (Exception ignored) {
            return def;
        }
    }

    private int cfgInt(String path, int def) {

        try {
            return plugin.getConfig().getInt(path, def);
        } catch (Exception ignored) {
            return def;
        }
    }

    private long cfgLong(String path, long def) {

        try {
            return plugin.getConfig().getLong(path, def);
        } catch (Exception ignored) {
            return def;
        }
    }

    private String cfgString(String path, String def) {

        try {

            String value =
                    plugin.getConfig().getString(path, def);

            return value == null ? def : value;

        } catch (Exception ignored) {
            return def;
        }
    }

    private List<String> cfgStringList(String path) {

        try {

            List<?> raw =
                    plugin.getConfig().getList(path);

            List<String> result =
                    new ArrayList<>();

            if (raw != null) {

                for (Object value : raw) {

                    if (value != null) {
                        result.add(String.valueOf(value));
                    }
                }
            }

            return result;

        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private List<Integer> cfgIntList(String path) {

        List<Integer> result =
                new ArrayList<>();

        for (Object value : cfgStringList(path)) {

            try {
                result.add(Integer.parseInt(String.valueOf(value)));

            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private List<Long> cfgLongList(String path) {

        List<Long> result =
                new ArrayList<>();

        for (Object value : cfgStringList(path)) {

            try {
                result.add(Long.parseLong(String.valueOf(value)));

            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private static String isoDay(long epochMillis) {

        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString();
    }

    private static List<String> lastDays(long now, int count) {

        List<String> days =
                new ArrayList<>();

        LocalDate today =
                Instant.ofEpochMilli(now)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

        for (int i = 0; i < count; i++) {
            days.add(today.minusDays(i).toString());
        }

        return days;
    }

    private void saveProfile(PlayerProfile profile) {

        try {

            plugin.getExecutorService().execute(
                    () -> storage.saveProfile(profile)
            );

        } catch (Throwable ignored) {

            storage.saveProfile(profile);
        }
    }

    private void saveServerState() {

        try {

            plugin.getExecutorService().execute(
                    () -> storage.saveServerState(serverState)
            );

        } catch (Throwable ignored) {

            storage.saveServerState(serverState);
        }
    }

    public void setServerName(String name) {

        this.serverName = name;
        serverState.serverName = name;
    }

    /** @return the current week key, e.g. {@code 2026-W41}. */
    public String currentWeekKey() {

        ZonedDateTime now =
                ZonedDateTime.now();

        WeekFields fields =
                WeekFields.ISO;

        return now.get(fields.weekBasedYear())
                + "-W"
                + String.format(
                "%02d",
                now.get(fields.weekOfWeekBasedYear())
        );
    }

    /** @return the current month key, e.g. {@code 2026-10}. */
    public String currentMonthKey() {

        LocalDate today =
                LocalDate.now();

        return String.format(
                "%04d-%02d",
                today.getYear(),
                today.getMonthValue()
        );
    }

    /** @return a snapshot of in-memory profile metric values */
    public Map<String, Object> debugSnapshot() {

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put("profiles", profiles.size());
        map.put("retention_days", retentionDays);
        map.put("snapshot_interval_seconds", snapshotIntervalSeconds);
        map.put("known_players", serverState.knownPlayers.size());
        map.put("total_sessions", serverState.totalSessions);
        map.put("server_id", serverId);
        map.put("server_name", serverName);

        return map;
    }
}
