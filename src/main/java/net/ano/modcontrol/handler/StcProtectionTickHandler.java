package net.ano.modcontrol.handler;

import net.ano.modcontrol.AnosModControl;
import net.ano.modcontrol.data.SupremeAuthorityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Backup protection that runs every tick to ensure protected entities stay alive
 */
public class StcProtectionTickHandler {
    
    // Track previous health of protected entities
    private static final Map<UUID, Float> lastKnownHealth = new HashMap<>();
    private static final Map<UUID, Float> lastKnownMaxHealth = new HashMap<>();
    
    private int tickCounter = 0;
    
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        
        // Run every tick for immediate protection
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;
                
                UUID uuid = living.getUUID();
                
                if (!SupremeAuthorityData.hasSupremeAuthority(uuid)) {
                    // Clean up tracking for non-protected entities
                    lastKnownHealth.remove(uuid);
                    lastKnownMaxHealth.remove(uuid);
                    continue;
                }
                
                float currentHealth = living.getHealth();
                float maxHealth = living.getMaxHealth();
                Float lastHealth = lastKnownHealth.get(uuid);
                
                // If entity is dead or has very low health, restore them
                if (currentHealth <= 0 || living.isDeadOrDying()) {
                    living.setHealth(maxHealth);
                    
                    if (living instanceof ServerPlayer player) {
                        player.displayClientMessage(Component.literal("§6[STC] §eYou have been protected from death!"), true);
                    }
                    
                    AnosModControl.LOGGER.debug("Tick protection restored health for: {}", living.getName().getString());
                }
                // If health dropped significantly in one tick (likely from command), restore it
                else if (lastHealth != null && currentHealth < lastHealth * 0.5f && 
                         SupremeAuthorityData.getCurrentExecutor() == null) {
                    // Only restore if no executor (command block/console)
                    living.setHealth(maxHealth);
                    
                    if (living instanceof ServerPlayer player) {
                        player.displayClientMessage(Component.literal("§6[STC] §eYou are protected!"), true);
                    }
                    
                    AnosModControl.LOGGER.debug("Tick protection restored health (sudden drop) for: {}", living.getName().getString());
                }
                
                // Update tracking
                lastKnownHealth.put(uuid, living.getHealth());
                lastKnownMaxHealth.put(uuid, maxHealth);
            }
        }
        
        // Periodic cleanup every 100 ticks
        if (tickCounter >= 100) {
            tickCounter = 0;
            cleanupTracking(event.getServer());
        }
    }
    
    private void cleanupTracking(net.minecraft.server.MinecraftServer server) {
        // Remove entries for entities that no longer exist or are no longer protected
        lastKnownHealth.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            if (!SupremeAuthorityData.hasSupremeAuthority(uuid)) return true;
            
            // Check if entity exists
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getEntity(uuid) != null) return false;
            }
            return true;
        });
        
        lastKnownMaxHealth.entrySet().removeIf(entry -> !lastKnownHealth.containsKey(entry.getKey()));
    }
}