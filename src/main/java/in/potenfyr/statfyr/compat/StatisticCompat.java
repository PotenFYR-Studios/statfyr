package in.potenfyr.statfyr.compat;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Version-agnostic helpers for reading live {@link Statistic} values.
 *
 * <p>The Bukkit statistics API changed shape several times between 1.8 and
 * 26.x:
 *
 * <ul>
 *     <li>1.8.x–1.16.x: {@code Statistic#isBlock()}, {@code isSubstatistic()};
 *         no {@code getType()} on some builds.</li>
 *     <li>1.17+: {@code Statistic#getType()} returning {@code Statistic.Type}.</li>
 *     <li>Play time was renamed {@code PLAY_ONE_TICK} → {@code PLAY_ONE_MINUTE}.</li>
 *     <li>{@code Material#isItem()} / {@code EntityType#isAlive()} were added
 *         after 1.8.</li>
 * </ul>
 *
 * <p>To keep a single JAR working everywhere, the statistic <em>category</em>
 * is derived from the stable enum constant name (which has not changed since
 * 1.8) and material/entity filtering degrades gracefully when the reflective
 * methods are missing.
 */
public final class StatisticCompat {

    private StatisticCompat() {
    }

    // Boolean predicate methods looked up once, reflectively.
    private static final Method MATERIAL_IS_ITEM =
            findBooleanMethod(Material.class, "isItem");

    private static final Method MATERIAL_IS_BLOCK =
            findBooleanMethod(Material.class, "isBlock");

    private static final Method ENTITY_TYPE_IS_ALIVE =
            findBooleanMethod(EntityType.class, "isAlive");

    // Lazily built, immutable snapshots.
    private static volatile Material[] itemMaterials;
    private static volatile Material[] blockMaterials;
    private static volatile EntityType[] livingEntityTypes;

    // -------------------------------------------------------------------------
    // Category detection
    // -------------------------------------------------------------------------

    public static final String UNTYPED = "UNTYPED";
    public static final String BLOCK = "BLOCK";
    public static final String ITEM = "ITEM";
    public static final String ENTITY = "ENTITY";

    /**
     * Determines the statistic category using the stable enum name.
     *
     * @param statistic statistic to classify
     * @return {@link #UNTYPED}, {@link #BLOCK}, {@link #ITEM} or {@link #ENTITY}
     */
    public static String categoryOf(Statistic statistic) {

        if (statistic == null) {
            return UNTYPED;
        }

        switch (statistic.name()) {

            case "MINE_BLOCK":
                return BLOCK;

            case "CRAFT_ITEM":
            case "USE_ITEM":
            case "BREAK_ITEM":
            case "PICKUP":
            case "DROP":
                return ITEM;

            case "KILL_ENTITY":
            case "ENTITY_KILLED_BY":
                return ENTITY;

            default:
                return UNTYPED;
        }
    }

    /**
     * @param statistic statistic to test
     * @return {@code true} when the statistic tracks increments over time
     *         rather than a single value
     */
    public static boolean isUntyped(Statistic statistic) {

        return UNTYPED.equals(categoryOf(statistic));
    }

    /**
     * @param statistic statistic to test
     * @return {@code true} for both the legacy {@code PLAY_ONE_TICK} and the
     *         modern {@code PLAY_ONE_MINUTE} constants
     */
    public static boolean isPlayTime(Statistic statistic) {

        if (statistic == null) {
            return false;
        }

        String name =
                statistic.name();

        return "PLAY_ONE_MINUTE".equals(name)
                || "PLAY_ONE_TICK".equals(name);
    }

    // -------------------------------------------------------------------------
    // Material / entity snapshots
    // -------------------------------------------------------------------------

    /**
     * @return all materials that represent an item on this server version
     */
    public static Material[] getItemMaterials() {

        Material[] cached =
                itemMaterials;

        if (cached == null) {

            synchronized (StatisticCompat.class) {

                cached = itemMaterials;

                if (cached == null) {

                    cached = buildMaterials(true);

                    itemMaterials = cached;
                }
            }
        }

        return cached;
    }

    /**
     * @return all materials that represent a block on this server version
     */
    public static Material[] getBlockMaterials() {

        Material[] cached =
                blockMaterials;

        if (cached == null) {

            synchronized (StatisticCompat.class) {

                cached = blockMaterials;

                if (cached == null) {

                    cached = buildMaterials(false);

                    blockMaterials = cached;
                }
            }
        }

        return cached;
    }

    /**
     * @return all living entity types on this server version
     */
    public static EntityType[] getLivingEntityTypes() {

        EntityType[] cached =
                livingEntityTypes;

        if (cached == null) {

            synchronized (StatisticCompat.class) {

                cached = livingEntityTypes;

                if (cached == null) {

                    cached = buildEntityTypes();

                    livingEntityTypes = cached;
                }
            }
        }

        return cached;
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private static Material[] buildMaterials(boolean wantItems) {

        Method predicate =
                wantItems
                        ? MATERIAL_IS_ITEM
                        : MATERIAL_IS_BLOCK;

        List<Material> result =
                new ArrayList<>();

        for (Material material : Material.values()) {

            if (material == null) {
                continue;
            }

            Boolean matches =
                    evaluate(predicate, material);

            /*
             * When the predicate method does not exist (very old servers)
             * include every material and let the caller catch the
             * IllegalArgumentException produced by unsupported pairings.
             */
            if (matches == null || matches.booleanValue()) {
                result.add(material);
            }
        }

        return result.toArray(new Material[0]);
    }

    private static EntityType[] buildEntityTypes() {

        List<EntityType> result =
                new ArrayList<>();

        for (EntityType type : EntityType.values()) {

            if (type == null) {
                continue;
            }

            Boolean alive =
                    evaluate(
                            ENTITY_TYPE_IS_ALIVE,
                            type
                    );

            if (alive == null || alive.booleanValue()) {
                result.add(type);
            }
        }

        return result.toArray(new EntityType[0]);
    }

    private static Boolean evaluate(
            Method method,
            Object target
    ) {

        if (method == null) {
            return null;
        }

        try {

            Object value =
                    method.invoke(target);

            if (value instanceof Boolean) {
                return (Boolean) value;
            }

        } catch (Throwable ignored) {
            // Treat unreadable predicates as "unknown".
        }

        return null;
    }

    private static Method findBooleanMethod(
            Class<?> owner,
            String name
    ) {

        try {

            return owner.getMethod(name);

        } catch (Throwable ignored) {

            return null;
        }
    }
}
