package me.uc_hussein.ultraschat.death;

import org.bukkit.entity.Entity;

/** Result of classifying a death. {@code killer} may be null. */
public record DeathInfo(DeathType type, Entity killer) {
}
