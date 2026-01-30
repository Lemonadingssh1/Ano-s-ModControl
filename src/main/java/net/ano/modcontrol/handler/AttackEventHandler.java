package net.ano.modcontrol.handler;

import net.ano.modcontrol.AnosModControl;
import net.ano.modcontrol.data.ModControlData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Explosion;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Iterator;
import java.util.Set;

public class AttackEventHandler {
    
    private int tickCounter = 0;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (shouldBlockAttack(event.getSource(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (shouldBlockAttack(event.getSource(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDamage(LivingDamageEvent event) {
        if (shouldBlockAttack(event.getSource(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /**
     * Block negative potion effects from entities with attack disabled
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPotionApply(MobEffectEvent.Applicable event) {
        LivingEntity target = event.getEntity();
        MobEffectInstance effectInstance = event.getEffectInstance();
        
        if (effectInstance == null || target == null) return;
        
        // Only block negative effects
        if (effectInstance.getEffect().isBeneficial()) return;
        
        // Try to find the source entity from the effect
        // Unfortunately, Minecraft doesn't directly track who applied the effect
        // We need to check nearby entities that might have caused it
        if (target.level() instanceof ServerLevel serverLevel) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof ThrownPotion potion) {
                    Entity owner = potion.getOwner();
                    if (owner != null && potion.distanceToSqr(target) < 64) { // Within 8 blocks
                        ResourceLocation attackerId = ForgeRegistries.ENTITY_TYPES.getKey(owner.getType());
                        if (attackerId != null) {
                            ResourceLocation targetId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
                            String targetPlayerName = (target instanceof Player) ? target.getName().getString() : null;
                            
                            if (ModControlData.isAttackDisabled(attackerId, targetId, targetPlayerName)) {
                                event.setResult(Event.Result.DENY);
                                AnosModControl.LOGGER.debug("Blocked potion effect from {} to {}", attackerId, target.getName().getString());
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Explosion explosion = event.getExplosion();
        
        Entity directSource = explosion.getDirectSourceEntity();
        LivingEntity indirectSource = explosion.getIndirectSourceEntity();
        Entity causingEntity = directSource != null ? directSource : indirectSource;
        
        if (causingEntity == null) return;
        
        ResourceLocation attackerId = ForgeRegistries.ENTITY_TYPES.getKey(causingEntity.getType());
        if (attackerId == null) return;
        
        Iterator<Entity> iterator = event.getAffectedEntities().iterator();
        while (iterator.hasNext()) {
            Entity affectedEntity = iterator.next();
            if (!(affectedEntity instanceof LivingEntity target)) continue;
            
            ResourceLocation targetId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            String targetPlayerName = (target instanceof Player) ? target.getName().getString() : null;
            
            if (ModControlData.isAttackDisabled(attackerId, targetId, targetPlayerName)) {
                iterator.remove();
                AnosModControl.LOGGER.debug("Protected {} from explosion by {}", target.getName().getString(), attackerId);
            }
        }
    }

    /**
     * Handle forced attacks - make mobs attack targets they normally wouldn't
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        if (tickCounter < 20) return; // Check every second
        tickCounter = 0;
        
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob)) continue;
                
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
                if (entityId == null) continue;
                
                Set<String> forcedTargets = ModControlData.getForcedAttackTargets(entityId);
                if (forcedTargets.isEmpty()) continue;
                
                // Find a valid target to attack
                LivingEntity currentTarget = mob.getTarget();
                if (currentTarget != null) {
                    ResourceLocation currentTargetId = ForgeRegistries.ENTITY_TYPES.getKey(currentTarget.getType());
                    String currentTargetName = (currentTarget instanceof Player) ? currentTarget.getName().getString() : null;
                    
                    // Check if current target is a forced target
                    boolean isForced = false;
                    for (String forced : forcedTargets) {
                        if (ModControlData.matchesSingleTarget(forced, currentTargetId, currentTargetName)) {
                            isForced = true;
                            break;
                        }
                    }
                    if (isForced) continue; // Already targeting a forced target
                }
                
                // Find nearest forced target
                LivingEntity nearestForcedTarget = findNearestForcedTarget(mob, forcedTargets, level);
                if (nearestForcedTarget != null) {
                    mob.setTarget(nearestForcedTarget);
                }
            }
        }
    }

    private LivingEntity findNearestForcedTarget(Mob mob, Set<String> forcedTargets, ServerLevel level) {
        double nearestDist = Double.MAX_VALUE;
        LivingEntity nearest = null;
        
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mob) continue;
            if (!living.isAlive()) continue;
            
            ResourceLocation targetId = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
            String targetPlayerName = (living instanceof Player) ? living.getName().getString() : null;
            
            for (String forced : forcedTargets) {
                if (ModControlData.matchesSingleTarget(forced, targetId, targetPlayerName)) {
                    double dist = mob.distanceToSqr(living);
                    if (dist < nearestDist && dist < 256) { // Within 16 blocks
                        nearestDist = dist;
                        nearest = living;
                    }
                    break;
                }
            }
        }
        
        return nearest;
    }

    private boolean shouldBlockAttack(DamageSource source, LivingEntity target) {
        if (source == null || target == null) return false;
        
        Entity attacker = source.getEntity();
        Entity directSource = source.getDirectEntity();
        
        // Handle projectiles and potions
        if (directSource instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner != null) attacker = owner;
        }
        
        if (directSource instanceof ThrownPotion potion) {
            Entity owner = potion.getOwner();
            if (owner != null) attacker = owner;
        }
        
        if (attacker == null) return false;
        
        ResourceLocation attackerId = ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType());
        if (attackerId == null) return false;
        
        ResourceLocation targetId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String targetPlayerName = (target instanceof Player) ? target.getName().getString() : null;
        
        if (ModControlData.isAttackDisabled(attackerId, targetId, targetPlayerName)) {
            AnosModControl.LOGGER.debug("Blocked attack from {} to {}", attackerId, target.getName().getString());
            return true;
        }
        
        return false;
    }
}