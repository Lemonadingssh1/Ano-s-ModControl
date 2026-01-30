package net.ano.modcontrol.handler;

import net.ano.modcontrol.AnosModControl;
import net.ano.modcontrol.data.SupremeAuthorityData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandInterceptHandler {
    
    // Commands that should be completely blocked when targeting protected entities
    private static final Set<String> BLOCK_COMMANDS = Set.of(
        "kick", "ban", "ban-ip", "deop", "pardon"
    );
    
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^/?([a-zA-Z_][a-zA-Z0-9_]*)");
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onCommand(CommandEvent event) {
        String command = event.getParseResults().getReader().getString();
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        
        // Set the command executor in data
        Entity executor = source.getEntity();
        UUID executorUuid = executor != null ? executor.getUUID() : null;
        SupremeAuthorityData.setCurrentExecutor(executorUuid);
        
        // Extract base command
        Matcher matcher = COMMAND_PATTERN.matcher(command.trim());
        if (!matcher.find()) return;
        String baseCommand = matcher.group(1).toLowerCase();
        
        // Only intercept specific commands that we can't protect via events
        if (!BLOCK_COMMANDS.contains(baseCommand)) return;
        
        // Check if command targets any protected entity
        if (source.getServer() == null) return;
        
        for (UUID protectedUuid : SupremeAuthorityData.getSupremeEntities()) {
            // Skip if executor is the protected entity
            if (protectedUuid.equals(executorUuid)) continue;
            
            ServerPlayer protectedPlayer = source.getServer().getPlayerList().getPlayer(protectedUuid);
            if (protectedPlayer != null) {
                String playerName = protectedPlayer.getName().getString();
                
                if (command.toLowerCase().contains(playerName.toLowerCase())) {
                    event.setCanceled(true);
                    source.sendFailure(Component.literal(
                        "§c[STC] Cannot execute /" + baseCommand + " on " + playerName + " - they have Supreme Authority."));
                    
                    protectedPlayer.sendSystemMessage(Component.literal(
                        "§6[STC] §eSomeone tried to use /" + baseCommand + " on you. Blocked!"));
                    
                    AnosModControl.LOGGER.info("Blocked /{} command targeting protected player {}", baseCommand, playerName);
                    return;
                }
            }
        }
    }
}