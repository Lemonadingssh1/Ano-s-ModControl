package net.ano.modcontrol.handler;

import net.ano.modcontrol.AnosModControl;
import net.ano.modcontrol.data.ModControlData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

public class AngerEventHandler {

    private int tickCounter = 0;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity newTarget = event.getNewTarget();
        
        if (entity == null || newTarget == null) return;
        
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) return;
        
        ResourceLocation targetId = ForgeRegistries.ENTITY_TYPES.getKey(newTarget.getType());
        String targetPlayerName = (newTarget instanceof Player) ? newTarget.getName().getString() : null;
        
        // Check if anger is disabled toward this target
        if (ModControlData.isAngerDisabled(entityId, targetId, targetPlayerName)) {
            event.setCanceled(true);
            
            if (entity instanceof NeutralMob neutralMob) {
                neutralMob.stopBeingAngry();
            }
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
            }
            if (entity instanceof Creeper creeper) {
                creeper.setSwellDir(-1);
            }
            
            AnosModControl.LOGGER.debug("Blocked {} from targeting {}", entityId, newTarget.getName().getString());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        if (tickCounter < 5) return;
        tickCounter = 0;
        
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob)) continue;
                
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
                if (entityId == null) continue;
                
                // Handle creepers specially
                if (mob instanceof Creeper creeper) {
                    handleCreeperAnger(creeper, entityId);
                }
                
                // Handle forced anger
                Set<String> forcedTargets = ModControlData.getForcedAngerTargets(entityId);
                if (!forcedTargets.isEmpty()) {
                    LivingEntity currentTarget = mob.getTarget();
                    if (currentTarget == null) {
                        LivingEntity nearestForcedTarget = findNearestForcedTarget(mob, forcedTargets, level);
                        if (nearestForcedTarget != null) {
                            mob.setTarget(nearestForcedTarget);
                        }
                    }
                    continue;
                }
                
                // Handle disabled anger - clear invalid targets
                LivingEntity target = mob.getTarget();
                if (target == null) continue;
                
                ResourceLocation targetId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
                String targetPlayerName = (target instanceof Player) ? target.getName().getString() : null;
                
                if (ModControlData.isAngerDisabled(entityId, targetId, targetPlayerName)) {
                    mob.setTarget(null);
                    if (mob instanceof NeutralMob neutralMob) {
                        neutralMob.stopBeingAngry();
                    }
                }
            }
        }
    }

    private void handleCreeperAnger(Creeper creeper, ResourceLocation entityId) {
        if (creeper.getSwellDir() <= 0) return;
        
        Player nearestPlayer = creeper.level().getNearestPlayer(creeper, 7.0D);
        if (nearestPlayer == null) return;
        
        ResourceLocation targetId = ForgeRegistries.ENTITY_TYPES.getKey(nearestPlayer.getType());
        String targetPlayerName = nearestPlayer.getName().getString();
        
        if (ModControlData.isAngerDisabled(entityId, targetId, targetPlayerName)) {
            creeper.setSwellDir(-1);
            AnosModControl.LOGGER.debug("Stopped creeper from swelling toward {}", targetPlayerName);
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
                    if (dist < nearestDist && dist < 256) {
                        nearestDist = dist;
                        nearest = living;
                    }
                    break;
                }
            }
        }
        
        return nearest;
    }
}