package net.ano.modcontrol.handler;

import net.ano.modcontrol.AnosModControl;
import net.ano.modcontrol.data.SupremeAuthorityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;

public class CommandProtectionHandler {
    
    /**
     * Set the command executor before command runs
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onCommandStart(CommandEvent event) {
        Entity executor = event.getParseResults().getContext().getSource().getEntity();
        if (executor != null) {
            SupremeAuthorityData.setCurrentExecutor(executor.getUUID());
        } else {
            SupremeAuthorityData.setCurrentExecutor(null);
        }
    }
    
    /**
     * Server tick - restore health to protected entities and clear executor
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // Clear command executor at end of tick
        SupremeAuthorityData.clearCurrentExecutor();
        
        // Restore health to any protected entities that were damaged
        Map<UUID, Float> pending = SupremeAuthorityData.getPendingHealthRestores();
        if (pending.isEmpty()) return;
        
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;
                
                Float maxHealth = SupremeAuthorityData.getAndClearHealthRestore(living.getUUID());
                if (maxHealth != null) {
                    living.setHealth(maxHealth);
                    
                    if (living instanceof Player player) {
                        player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§6[STC] §eYou are protected!"), true);
                    }
                    
                    AnosModControl.LOGGER.debug("Restored health to protected entity: {}", living.getName().getString());
                }
            }
        }
    }
    
    /**
     * Block attacks on protected entities
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        
        if (SupremeAuthorityData.shouldProtectEntity(entity.getUUID())) {
            String msgId = event.getSource().getMsgId();
            
            // Block generic/command damage
            if (isCommandDamage(msgId)) {
                event.setCanceled(true);
                AnosModControl.LOGGER.debug("Blocked attack on protected entity: {} (source: {})", 
                    entity.getName().getString(), msgId);
            }
        }
    }
    
    /**
     * Block hurt on protected entities
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        
        if (SupremeAuthorityData.shouldProtectEntity(entity.getUUID())) {
            String msgId = event.getSource().getMsgId();
            
            if (isCommandDamage(msgId)) {
                event.setCanceled(true);
                AnosModControl.LOGGER.debug("Blocked hurt on protected entity: {} (source: {})", 
                    entity.getName().getString(), msgId);
            }
        }
    }
    
    /**
     * Block damage on protected entities
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        
        if (SupremeAuthorityData.shouldProtectEntity(entity.getUUID())) {
            String msgId = event.getSource().getMsgId();
            
            if (isCommandDamage(msgId)) {
                event.setCanceled(true);
                AnosModControl.LOGGER.debug("Blocked damage on protected entity: {} (source: {})", 
                    entity.getName().getString(), msgId);
            }
        }
    }
    
    /**
     * Block death on protected entities - this is the final fallback
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        if (SupremeAuthorityData.shouldProtectEntity(entity.getUUID())) {
            // Cancel death
            event.setCanceled(true);
            
            // Mark for health restoration on next tick
            SupremeAuthorityData.markForHealthRestore(entity.getUUID(), entity.getMaxHealth());
            
            // Immediately try to restore health
            entity.setHealth(entity.getMaxHealth());
            
            AnosModControl.LOGGER.debug("Prevented death of protected entity: {} (source: {})", 
                entity.getName().getString(), event.getSource().getMsgId());
        }
    }
    
    /**
     * Block negative potion effects on protected entities
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEffectApply(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        
        if (SupremeAuthorityData.shouldProtectEntity(entity.getUUID())) {
            if (event.getEffectInstance() != null && !event.getEffectInstance().getEffect().isBeneficial()) {
                event.setResult(Event.Result.DENY);
                AnosModControl.LOGGER.debug("Blocked negative effect on protected entity: {}", 
                    entity.getName().getString());
            }
        }
    }
    
    /**
     * Block teleportation of protected entities
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityTeleport(EntityTeleportEvent event) {
        Entity entity = event.getEntity();
        
        if (SupremeAuthorityData.shouldProtectEntity(entity.getUUID())) {
            event.setCanceled(true);
            
            if (entity instanceof Player player) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§6[STC] §eTeleportation blocked!"), true);
            }
            
            AnosModControl.LOGGER.debug("Blocked teleport of protected entity: {}", entity.getName().getString());
        }
    }
    
    /**
     * Block gamemode changes for protected players
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        Player player = event.getEntity();
        
        if (SupremeAuthorityData.shouldProtectEntity(player.getUUID())) {
            event.setCanceled(true);
            
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§6[STC] §eGamemode change blocked!"), true);
            
            AnosModControl.LOGGER.debug("Blocked gamemode change for protected player: {}", player.getName().getString());
        }
    }
    
    /**
     * Check if damage is from a command
     */
    private boolean isCommandDamage(String msgId) {
        return msgId.equals("generic") ||
               msgId.equals("kill") ||
               msgId.equals("genericKill") ||
               msgId.equals("outOfWorld") ||
               msgId.equals("even_more_magic") ||
               msgId.equals("magic") ||
               msgId.equals("indirectMagic") ||
               msgId.equals("thorns") ||
               msgId.equals("wither");
    }
}