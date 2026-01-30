package net.ano.modcontrol.util;

import net.ano.modcontrol.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

public class MessageHelper {
    
    /**
     * Send a debug message to a player based on config settings
     */
    public static void sendDebug(Player player, String translationKey, Object... args) {
        if (!ModConfig.SHOW_DEBUG_MESSAGES.get()) return;
        
        Component message = Component.translatable(translationKey, args);
        
        if (ModConfig.DEBUG_TO_CHAT.get()) {
            player.sendSystemMessage(message);
        }
        if (ModConfig.DEBUG_TO_ACTION_BAR.get()) {
            player.displayClientMessage(message, true);
        }
    }
    
    /**
     * Send a debug message to a player based on config settings
     */
    public static void sendDebug(Player player, Component message) {
        if (!ModConfig.SHOW_DEBUG_MESSAGES.get()) return;
        
        if (ModConfig.DEBUG_TO_CHAT.get()) {
            player.sendSystemMessage(message);
        }
        if (ModConfig.DEBUG_TO_ACTION_BAR.get()) {
            player.displayClientMessage(message, true);
        }
    }
    
    /**
     * Send a command output message to a player based on config settings
     */
    public static void sendOutput(Player player, String translationKey, Object... args) {
        if (!ModConfig.SHOW_COMMAND_OUTPUT.get()) return;
        
        Component message = Component.translatable(translationKey, args);
        
        if (ModConfig.OUTPUT_TO_CHAT.get()) {
            player.sendSystemMessage(message);
        }
        if (ModConfig.OUTPUT_TO_ACTION_BAR.get()) {
            player.displayClientMessage(message, true);
        }
    }
    
    /**
     * Send a command output message to a player based on config settings
     */
    public static void sendOutput(Player player, Component message) {
        if (!ModConfig.SHOW_COMMAND_OUTPUT.get()) return;
        
        if (ModConfig.OUTPUT_TO_CHAT.get()) {
            player.sendSystemMessage(message);
        }
        if (ModConfig.OUTPUT_TO_ACTION_BAR.get()) {
            player.displayClientMessage(message, true);
        }
    }
    
    /**
     * Send an STC protection notification
     */
    public static void sendStcProtected(Player player, String translationKey, Object... args) {
        if (!ModConfig.STC_NOTIFY_PROTECTED.get()) return;
        
        Component message = Component.translatable(translationKey, args);
        player.displayClientMessage(message, true);
    }
    
    /**
     * Send an STC blocked notification to attacker/executor
     */
    public static void sendStcBlocked(Player player, String translationKey, Object... args) {
        if (!ModConfig.STC_NOTIFY_ATTACKER.get()) return;
        
        Component message = Component.translatable(translationKey, args);
        player.sendSystemMessage(message);
    }
    
    /**
     * Create a translatable component
     */
    public static Component translate(String key, Object... args) {
        return Component.translatable(key, args);
    }
}