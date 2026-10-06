package in.potenfyr.statfyr.compat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Scheduler abstraction that works on both the classic Bukkit scheduler
 * (Bukkit/Spigot/Paper/Purpur) and Folia's regionised scheduler.
 *
 * <p>Folia removed {@code BukkitScheduler}, so every scheduling call is routed
 * through here instead of touching {@code Bukkit.getScheduler()} directly.
 * Detection happens once and all reflective lookups fail closed.
 */
public final class SchedulerCompat {

    private SchedulerCompat() {
    }

    private static final Method GET_ASYNC_SCHEDULER =
            findBukkitMethod("getAsyncScheduler");

    private static final Method GET_GLOBAL_REGION_SCHEDULER =
            findBukkitMethod("getGlobalRegionScheduler");

    // -------------------------------------------------------------------------
    // Async repeating
    // -------------------------------------------------------------------------

    /**
     * Runs a task asynchronously on a fixed period. Converts ticks to
     * milliseconds for Folia's schedulers.
     *
     * @param plugin      owning plugin
     * @param task        task to run
     * @param delayTicks  initial delay in ticks
     * @param periodTicks period in ticks
     * @return {@code true} when the task was scheduled
     */
    public static boolean runAsyncRepeating(
            Plugin plugin,
            Runnable task,
            long delayTicks,
            long periodTicks
    ) {

        if (ServerVersion.isFolia()
                && scheduleFoliaAsyncRepeating(
                plugin,
                task,
                delayTicks,
                periodTicks
        )) {

            return true;
        }

        if (!ServerVersion.isFolia()) {

            try {

                Bukkit.getScheduler()
                        .runTaskTimerAsynchronously(
                                plugin,
                                task,
                                delayTicks,
                                periodTicks
                        );

                return true;

            } catch (Throwable ignored) {
                // Fall through to the Folia path / failure.
            }
        }

        return scheduleFoliaAsyncRepeating(
                plugin,
                task,
                delayTicks,
                periodTicks
        );
    }

    /**
     * Runs a task once, asynchronously.
     *
     * @param plugin owning plugin
     * @param task   task to run
     * @return {@code true} when the task was scheduled
     */
    public static boolean runAsync(
            Plugin plugin,
            Runnable task
    ) {

        if (!ServerVersion.isFolia()) {

            try {

                Bukkit.getScheduler()
                        .runTaskAsynchronously(
                                plugin,
                                task
                        );

                return true;

            } catch (Throwable ignored) {
                // Fall through.
            }
        }

        if (GET_ASYNC_SCHEDULER == null) {
            return false;
        }

        try {

            Object scheduler =
                    GET_ASYNC_SCHEDULER.invoke(null);

            if (scheduler == null) {
                return false;
            }

            Method run =
                    scheduler.getClass()
                            .getMethod(
                                    "run",
                                    Plugin.class,
                                    Consumer.class
                            );

            run.invoke(
                    scheduler,
                    plugin,
                    (Consumer<Object>) ignored -> task.run()
            );

            return true;

        } catch (Throwable ignored) {

            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Sync bridge
    // -------------------------------------------------------------------------

    /**
     * Runs a task on the server's main thread where one exists (Bukkit's
     * scheduler), or the global region scheduler on Folia.
     *
     * @param plugin owning plugin
     * @param task   task to run
     * @return {@code true} when the task was scheduled
     */
    public static boolean runGlobalSync(
            Plugin plugin,
            Runnable task
    ) {

        if (!ServerVersion.isFolia()) {

            try {

                Bukkit.getScheduler()
                        .runTask(plugin, task);

                return true;

            } catch (Throwable ignored) {
                // Fall through.
            }
        }

        if (GET_GLOBAL_REGION_SCHEDULER == null) {
            return false;
        }

        try {

            Object scheduler =
                    GET_GLOBAL_REGION_SCHEDULER.invoke(null);

            if (scheduler == null) {
                return false;
            }

            Method run =
                    scheduler.getClass()
                            .getMethod(
                                    "run",
                                    Plugin.class,
                                    Consumer.class
                            );

            run.invoke(
                    scheduler,
                    plugin,
                    (Consumer<Object>) ignored -> task.run()
            );

            return true;

        } catch (Throwable ignored) {

            return false;
        }
    }

    /**
     * Executes a callable on the server's main thread where required and waits
     * for the result.
     *
     * <p>On Folia there is no global main thread; offline-player lookups are
     * safe to resolve from the calling thread so the callable runs directly.
     *
     * @param plugin   owning plugin
     * @param callable work to execute
     * @param <T>      result type
     * @return the callable's result
     * @throws Exception when the work fails
     */
    public static <T> T callSync(
            Plugin plugin,
            Callable<T> callable
    ) throws Exception {

        if (ServerVersion.isFolia()) {
            return callable.call();
        }

        try {

            Future<T> future =
                    Bukkit.getScheduler()
                            .callSyncMethod(
                                    plugin,
                                    callable
                            );

            return future.get();

        } catch (Throwable throwable) {

            // If a fork lacks callSyncMethod, just run inline.
            return callable.call();
        }
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private static boolean scheduleFoliaAsyncRepeating(
            Plugin plugin,
            Runnable task,
            long delayTicks,
            long periodTicks
    ) {

        if (GET_ASYNC_SCHEDULER == null) {
            return false;
        }

        try {

            Object scheduler =
                    GET_ASYNC_SCHEDULER.invoke(null);

            if (scheduler == null) {
                return false;
            }

            Method runAtFixedRate =
                    scheduler.getClass()
                            .getMethod(
                                    "runAtFixedRate",
                                    Plugin.class,
                                    Consumer.class,
                                    long.class,
                                    long.class,
                                    TimeUnit.class
                            );

            long delayMillis =
                    Math.max(1L, delayTicks * 50L);

            long periodMillis =
                    Math.max(1L, periodTicks * 50L);

            runAtFixedRate.invoke(
                    scheduler,
                    plugin,
                    (Consumer<Object>) ignored -> task.run(),
                    delayMillis,
                    periodMillis,
                    TimeUnit.MILLISECONDS
            );

            return true;

        } catch (Throwable ignored) {

            return false;
        }
    }

    private static Method findBukkitMethod(String name) {

        try {

            return Bukkit.class.getMethod(name);

        } catch (Throwable ignored) {

            return null;
        }
    }

    /**
     * @return {@code true} when a Folia global-region scheduler is available
     */
    public static boolean hasGlobalRegionScheduler() {

        return GET_GLOBAL_REGION_SCHEDULER != null;
    }
}
