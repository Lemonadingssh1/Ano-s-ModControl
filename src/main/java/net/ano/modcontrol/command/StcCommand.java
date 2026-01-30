package net.ano.modcontrol.command;

import net.ano.modcontrol.data.SupremeAuthorityData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.UUID;

public class StcCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stc")
            .requires(source -> source.hasPermission(2))
            
            // /stc @a, /stc @e, /stc <player>, etc.
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(StcCommand::executeGrantAuthority)
            )
            
            // /stc (self)
            .executes(StcCommand::executeGrantSelf)
        );
    }
    
    private static int executeGrantAuthority(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
            
            Entity executor = context.getSource().getEntity();
            UUID executorUuid = executor != null ? executor.getUUID() : null;
            
            int count = 0;
            for (Entity target : targets) {
                if (!SupremeAuthorityData.hasSupremeAuthority(target.getUUID())) {
                    // Determine granter UUID
                    UUID granterUuid;
                    if (executor != null && executor.getUUID().equals(target.getUUID())) {
                        granterUuid = target.getUUID(); // Self-grant
                    } else if (executorUuid != null) {
                        granterUuid = executorUuid; // Granted by another player
                    } else {
                        granterUuid = target.getUUID(); // Command block - treat as self-grant
                    }
                    
                    SupremeAuthorityData.grantSupremeAuthority(target.getUUID(), granterUuid);
                    count++;
                    
                    if (target instanceof ServerPlayer player) {
                        player.sendSystemMessage(Component.literal("§6§l[STC] §eYou have been granted Supreme Authority!"));
                        player.sendSystemMessage(Component.literal("§7Commands from others will no longer affect you."));
                        if (granterUuid.equals(target.getUUID())) {
                            player.sendSystemMessage(Component.literal("§7Use §e/stcno§7 to revoke this privilege."));
                        } else {
                            player.sendSystemMessage(Component.literal("§7Note: Only an admin can revoke this."));
                        }
                    }
                }
            }
            
            if (count > 0) {
                final int finalCount = count;
                context.getSource().sendSuccess(() -> 
                    Component.literal("§aGranted Supreme Authority to " + finalCount + " entities."), true);
            } else {
                context.getSource().sendFailure(Component.literal("All targets already have Supreme Authority."));
            }
            
            return count;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to grant authority: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeGrantSelf(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getEntity();
        
        if (executor == null) {
            context.getSource().sendFailure(Component.literal("This command must be executed by an entity."));
            return 0;
        }
        
        if (SupremeAuthorityData.hasSupremeAuthority(executor.getUUID())) {
            context.getSource().sendFailure(Component.literal("You already have Supreme Authority."));
            return 0;
        }
        
        SupremeAuthorityData.grantSupremeAuthority(executor.getUUID(), executor.getUUID());
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§6§l[STC] §eYou have granted yourself Supreme Authority!"), true);
        context.getSource().sendSuccess(() -> 
            Component.literal("§7Commands from others will no longer affect you."), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§7Protected from: /kill, /damage, /effect, /tp, /gamemode, /kick, /ban"), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§7Use §e/stcno§7 to revoke this privilege."), false);
        
        return 1;
    }
}