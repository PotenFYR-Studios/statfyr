package in.potenfyr.statfyr.stats;

/**
 * Minecraft namespaced stat keys used in the stats JSON files.
 * These are stable across 1.16.5 – 1.21.x. (hopefully :>)
 */
public final class StatKeys {

    private StatKeys() {}

    // Top-level categories in the stats JSON
    public static final String CATEGORY_CUSTOM  = "minecraft:custom";
    public static final String CATEGORY_KILLED  = "minecraft:killed";
    public static final String CATEGORY_KILLED_BY = "minecraft:killed_by";
    public static final String CATEGORY_CRAFTED = "minecraft:crafted";
    public static final String CATEGORY_USED    = "minecraft:used";
    public static final String CATEGORY_BROKEN  = "minecraft:broken";
    public static final String CATEGORY_PICKED_UP = "minecraft:picked_up";
    public static final String CATEGORY_DROPPED = "minecraft:dropped";
    public static final String CATEGORY_MINED   = "minecraft:mined";

    // Custom stat keys (inside minecraft:custom)
    public static final String PLAY_TIME           = "minecraft:play_time";
    public static final String DEATHS              = "minecraft:deaths";
    public static final String DAMAGE_DEALT        = "minecraft:damage_dealt";
    public static final String DAMAGE_TAKEN        = "minecraft:damage_taken";
    public static final String MOB_KILLS           = "minecraft:mob_kills";
    public static final String PLAYER_KILLS        = "minecraft:player_kills";
    public static final String JUMPS               = "minecraft:jump";
    public static final String LEAVE_GAME          = "minecraft:leave_game";

    // Distance stats (stored in centimetres by the game)
    public static final String WALK_ONE_CM         = "minecraft:walk_one_cm";
    public static final String SPRINT_ONE_CM       = "minecraft:sprint_one_cm";
    public static final String FLY_ONE_CM          = "minecraft:fly_one_cm";
    public static final String SWIM_ONE_CM         = "minecraft:swim_one_cm";
    public static final String WALK_UNDER_WATER_ONE_CM = "minecraft:walk_under_water_one_cm";
    public static final String FALL_ONE_CM         = "minecraft:fall_one_cm";
    public static final String CLIMB_ONE_CM        = "minecraft:climb_one_cm";
    public static final String HORSE_ONE_CM        = "minecraft:horse_one_cm";
    public static final String BOAT_ONE_CM         = "minecraft:boat_one_cm";
    public static final String MINECART_ONE_CM     = "minecraft:minecart_one_cm";
    public static final String PIG_ONE_CM          = "minecraft:pig_one_cm";
    public static final String STRIDER_ONE_CM      = "minecraft:strider_one_cm";

    // Interaction stats
    public static final String OPEN_CHEST          = "minecraft:open_chest";
    public static final String OPEN_SHULKER_BOX    = "minecraft:open_shulker_box";
    public static final String TALKED_TO_VILLAGER  = "minecraft:talked_to_villager";
    public static final String TRADED_WITH_VILLAGER = "minecraft:traded_with_villager";
    public static final String FISH_CAUGHT         = "minecraft:fish_caught";
    public static final String ANIMALS_BRED        = "minecraft:animals_bred";
    public static final String SLEEP_IN_BED        = "minecraft:sleep_in_bed";
    public static final String ENCHANT_ITEM        = "minecraft:enchant_item";

    // Killed entity keys (inside minecraft:killed)
    public static final String ENTITY_PLAYER       = "minecraft:player";
}
