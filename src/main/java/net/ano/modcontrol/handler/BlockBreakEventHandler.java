package net.ano.modcontrol.handler;

import net.ano.modcontrol.AnosModControl;
import net.ano.modcontrol.data.ModControlData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockBreakEventHandler {

    /**
     * Handles the mob griefing game rule check.
     * This affects: endermen picking blocks, zombies breaking doors, etc.
     * This does NOT affect entity damage from explosions.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMobGriefing(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;
        
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) return;
        
        if (ModControlData.isEntityBlockBreakDisabled(entityId)) {
            event.setResult(Event.Result.DENY);
            AnosModControl.LOGGER.debug("Blocked mob griefing for: {}", entityId);
        }
    }

    /**
     * Handles entities directly destroying blocks.
     * Used by: ravagers, withers, ender dragons, etc.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDestroyBlock(LivingDestroyBlockEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;
        
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) return;
        
        if (ModControlData.isEntityBlockBreakDisabled(entityId)) {
            event.setCanceled(true);
            AnosModControl.LOGGER.debug("Blocked block destruction by: {}", entityId);
        }
    }

    /**
     * Handles explosion block damage ONLY.
     * This clears affected blocks but KEEPS entity damage intact.
     * Used for: creepers, ghasts, withers, TNT ignited by mobs, etc.
     * 
     * IMPORTANT: We only clear blocks here, NOT cancel the explosion.
     * This ensures entities still take damage from explosions.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Explosion explosion = event.getExplosion();
        
        // Get the entity that caused the explosion
        Entity directSource = explosion.getDirectSourceEntity();
        LivingEntity indirectSource = explosion.getIndirectSourceEntity();
        
        // Determine the causing entity (direct source takes priority)
        Entity causingEntity = directSource;
        if (causingEntity == null) {
            causingEntity = indirectSource;
        }
        
        // If no entity caused this explosion, don't interfere
        if (causingEntity == null) return;
        
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(causingEntity.getType());
        if (entityId == null) return;
        
        if (ModControlData.isEntityBlockBreakDisabled(entityId)) {
            // ONLY clear affected blocks - entities in getAffectedEntities() will still take damage
            event.getAffectedBlocks().clear();
            AnosModControl.LOGGER.debug("Blocked explosion block damage from: {} (entity damage still applied)", entityId);
        }
    }
    
    // NOTE: We intentionally DO NOT have an onExplosionStart handler here.
    // Canceling ExplosionEvent.Start would cancel the ENTIRE explosion including entity damage.
    // We only want to prevent block damage, not entity damage.
}