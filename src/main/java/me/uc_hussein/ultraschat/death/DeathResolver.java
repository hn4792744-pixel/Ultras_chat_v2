package me.uc_hussein.ultraschat.death;

import org.bukkit.Material;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Classifies a player's death from the last damage cause. Damage causes are matched by name so
 * causes added in newer versions simply fall through to {@link DeathType#OTHER}.
 */
public final class DeathResolver {

    public DeathInfo resolve(Player victim) {
        EntityDamageEvent last = victim.getLastDamageCause();
        if (last == null) {
            return new DeathInfo(DeathType.OTHER, null);
        }
        String cause = last.getCause().name();
        if (last instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            boolean projectile = false;
            if (damager instanceof Projectile pr) {
                projectile = true;
                if (pr.getShooter() instanceof Entity shooter) {
                    damager = shooter;
                }
            }
            if (damager instanceof Player killer && !killer.getUniqueId().equals(victim.getUniqueId())) {
                return new DeathInfo(DeathType.PLAYER_KILL, killer);
            }
            DeathType mob = mobType(damager);
            if (mob != null) {
                return new DeathInfo(mob, damager);
            }
            if (projectile) {
                return new DeathInfo(DeathType.PROJECTILE, damager);
            }
            if (cause.contains("EXPLOSION")) {
                return new DeathInfo(DeathType.EXPLOSION, damager);
            }
            if (damager instanceof LightningStrike) {
                return new DeathInfo(DeathType.LIGHTNING, null);
            }
            if (damager instanceof FallingBlock) {
                return new DeathInfo(DeathType.FALLING_BLOCK, null);
            }
            return new DeathInfo(DeathType.ENTITY_ATTACK, damager);
        }
        return new DeathInfo(fromCause(victim, cause), null);
    }

    private static DeathType mobType(Entity e) {
        if (e instanceof Drowned) {
            return DeathType.DROWNED;
        }
        if (e instanceof Zombie) {
            return DeathType.ZOMBIE;
        }
        if (e instanceof AbstractSkeleton) {
            return DeathType.SKELETON;
        }
        if (e instanceof Creeper) {
            return DeathType.CREEPER;
        }
        if (e instanceof Spider) {
            return DeathType.SPIDER;
        }
        if (e instanceof Enderman) {
            return DeathType.ENDERMAN;
        }
        if (e instanceof Witch) {
            return DeathType.WITCH;
        }
        return null;
    }

    private static DeathType fromCause(Player victim, String cause) {
        return switch (cause) {
            case "FALL" -> DeathType.FALL;
            case "FIRE", "FIRE_TICK", "MELTING", "HOT_FLOOR", "CAMPFIRE" -> DeathType.FIRE;
            case "LAVA" -> DeathType.LAVA;
            case "DROWNING" -> DeathType.DROWNING;
            case "SUFFOCATION", "CRAMMING" -> DeathType.SUFFOCATION;
            case "VOID" -> DeathType.VOID;
            case "BLOCK_EXPLOSION", "ENTITY_EXPLOSION" -> DeathType.EXPLOSION;
            case "PROJECTILE" -> DeathType.PROJECTILE;
            case "LIGHTNING" -> DeathType.LIGHTNING;
            case "POISON" -> DeathType.POISON;
            case "WITHER" -> DeathType.WITHER;
            case "STARVATION" -> DeathType.STARVATION;
            case "CONTACT" -> victim.getLocation().getBlock().getType() == Material.SWEET_BERRY_BUSH
                    ? DeathType.SWEET_BERRY_BUSH : DeathType.CACTUS;
            case "FALLING_BLOCK" -> DeathType.FALLING_BLOCK;
            case "MAGIC", "DRAGON_BREATH" -> DeathType.MAGIC;
            case "FREEZE" -> DeathType.FREEZE;
            case "ENTITY_ATTACK", "ENTITY_SWEEP_ATTACK", "THORNS" -> DeathType.ENTITY_ATTACK;
            default -> DeathType.OTHER;
        };
    }
}
