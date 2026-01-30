package net.ano.modcontrol.command;

import net.ano.modcontrol.data.SupremeAuthorityData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class StcNoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stcno")
            // No permission check - only entities with self-granted authority can use this
            .executes(StcNoCommand::executeRevokeAuthority)
        );
    }
    
    private static int executeRevokeAuthority(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getEntity();
        
        if (executor == null) {
            context.getSource().sendFailure(Component.literal("This command must be executed by an entity."));
            return 0;
        }
        
        if (!SupremeAuthorityData.hasSupremeAuthority(executor.getUUID())) {
            context.getSource().sendFailure(Component.literal("You don't have Supreme Authority."));
            return 0;
        }
        
        if (!SupremeAuthorityData.canRevokeOwnAuthority(executor.getUUID())) {
            context.getSource().sendFailure(Component.literal("§cYou cannot revoke authority that was granted to you by someone else."));
            context.getSource().sendFailure(Component.literal("§7Only self-granted authority can be revoked with /stcno."));
            return 0;
        }
        
        SupremeAuthorityData.revokeSupremeAuthority(executor.getUUID());
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§6§l[STC] §eYou have revoked your Supreme Authority."), true);
        context.getSource().sendSuccess(() -> 
            Component.literal("§7You are now vulnerable to commands again."), false);
        
        return 1;
    }
}