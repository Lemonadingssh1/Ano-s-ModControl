// src/main/java/net/ano/modcontrol/handler/SpawnEventHandler.java
package net.ano.modcontrol.handler;

import net.ano.modcontrol.AnosModControl;
import net.ano.modcontrol.data.ModControlData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class SpawnEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onCheckSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob entity = event.getEntity();
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        
        if (entityId == null) return;
        
        if (ModControlData.isEntitySpawnDisabled(entityId)) {
            event.setSpawnCancelled(true);
            AnosModControl.LOGGER.debug("Blocked natural spawn of: {}", entityId);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        
        if (entityId == null) return;
        
        if (ModControlData.isEntitySpawnDisabled(entityId)) {
            event.setCanceled(true);
            AnosModControl.LOGGER.debug("Blocked entity from joining world: {}", entityId);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMobSpawnCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntityType());
        
        if (entityId == null) return;
        
        if (ModControlData.isEntitySpawnDisabled(entityId)) {
            event.setResult(Event.Result.DENY);
            AnosModControl.LOGGER.debug("Blocked spawn placement check for: {}", entityId);
        }
    }
}