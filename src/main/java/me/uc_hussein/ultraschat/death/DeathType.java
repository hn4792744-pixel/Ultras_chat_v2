package me.uc_hussein.ultraschat.death;

import java.util.Locale;

/** Death categories; the key matches config.yml death.types.* and death_xx.yml. */
public enum DeathType {
    PLAYER_KILL, ZOMBIE, SKELETON, CREEPER, SPIDER, ENDERMAN, WITCH, DROWNED,
    FALL, FIRE, LAVA, DROWNING, SUFFOCATION, VOID, EXPLOSION, PROJECTILE, LIGHTNING,
    POISON, WITHER, STARVATION, CACTUS, SWEET_BERRY_BUSH, FALLING_BLOCK, ENTITY_ATTACK,
    MAGIC, FREEZE, OTHER;

    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
