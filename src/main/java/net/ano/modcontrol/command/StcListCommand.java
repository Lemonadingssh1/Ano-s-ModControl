package net.ano.modcontrol.command;

import net.ano.modcontrol.data.SupremeAuthorityData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

public class StcListCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stclist")
            .requires(source -> source.hasPermission(4)) // OP level 4 only
            .executes(StcListCommand::executeList)
        );
        
        // Force revoke command for admins
        dispatcher.register(Commands.literal("stcrevoke")
            .requires(source -> source.hasPermission(4))
            .then(Commands.argument("targets", net.minecraft.commands.arguments.EntityArgument.entities())
                .executes(StcListCommand::executeForceRevoke)
            )
        );
    }
    
    private static int executeList(CommandContext<CommandSourceStack> context) {
        Set<UUID> supremeEntities = SupremeAuthorityData.getSupremeEntities();
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§6=== Supreme Authority Entities ==="), false);
        
        if (supremeEntities.isEmpty()) {
            context.getSource().sendSuccess(() -> 
                Component.literal("§7No entities have Supreme Authority."), false);
            return 0;
        }
        
        MinecraftServer server = context.getSource().getServer();
        
        for (UUID uuid : supremeEntities) {
            UUID grantedBy = SupremeAuthorityData.getGrantedBy(uuid);
            String grantedByName = grantedBy != null && grantedBy.equals(uuid) ? "self" : 
                (grantedBy != null ? grantedBy.toString().substring(0, 8) + "..." : "unknown");
            
            // Try to find entity name
            String entityName = uuid.toString().substring(0, 8) + "...";
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                entityName = player.getName().getString();
            }
            
            final String finalName = entityName;
            final String finalGrantedBy = grantedByName;
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§e- " + finalName + " §7(granted by: " + finalGrantedBy + ")"), false);
        }
        
        return supremeEntities.size();
    }
    
    private static int executeForceRevoke(CommandContext<CommandSourceStack> context) {
        try {
            var targets = net.minecraft.commands.arguments.EntityArgument.getEntities(context, "targets");
            
            int count = 0;
            for (Entity target : targets) {
                if (SupremeAuthorityData.hasSupremeAuthority(target.getUUID())) {
                    SupremeAuthorityData.revokeSupremeAuthority(target.getUUID());
                    count++;
                    
                    if (target instanceof ServerPlayer player) {
                        player.sendSystemMessage(Component.literal("§c§l[STC] §eYour Supreme Authority has been revoked by an administrator."));
                    }
                }
            }
            
            if (count > 0) {
                final int finalCount = count;
                context.getSource().sendSuccess(() -> 
                    Component.literal("§aRevoked Supreme Authority from " + finalCount + " entities."), true);
            } else {
                context.getSource().sendFailure(Component.literal("No targets had Supreme Authority."));
            }
            
            return count;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to revoke authority: " + e.getMessage()));
            return 0;
        }
    }
}