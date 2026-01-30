package net.ano.modcontrol.command;

import net.ano.modcontrol.config.ModConfig;
import net.ano.modcontrol.data.PlayerCamData;
import net.ano.modcontrol.network.NetworkHandler;
import net.ano.modcontrol.network.PlayerCamSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class PlayerCamCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("PlayerCam")
            .requires(source -> source.hasPermission(2) && ModConfig.ENABLE_PLAYER_CAM.get());
        
        for (String perspective : PlayerCamData.ALL_PERSPECTIVES) {
            command.then(createPerspectiveBranch(perspective));
        }
        
        command.then(Commands.literal("list")
            .executes(PlayerCamCommand::executeList)
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> executeListForPlayers(ctx, EntityArgument.getPlayers(ctx, "targets")))
            )
        );
        
        command.then(Commands.literal("reset")
            .executes(ctx -> executeReset(ctx, Collections.emptyList()))
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> executeReset(ctx, EntityArgument.getPlayers(ctx, "targets")))
            )
        );
        
        dispatcher.register(command);
    }
    
    private static LiteralArgumentBuilder<CommandSourceStack> createPerspectiveBranch(String perspective) {
        return Commands.literal(perspective)
            .then(Commands.literal("force")
                .executes(ctx -> executeForce(ctx, perspective, Collections.emptyList()))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> executeForce(ctx, perspective, EntityArgument.getPlayers(ctx, "targets")))
                )
            )
            .then(Commands.literal("cancel")
                .executes(ctx -> executeCancel(ctx, perspective, Collections.emptyList()))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> executeCancel(ctx, perspective, EntityArgument.getPlayers(ctx, "targets")))
                )
            )
            .then(Commands.literal("remove")
                .executes(ctx -> executeRemove(ctx, perspective, Collections.emptyList()))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> executeRemove(ctx, perspective, EntityArgument.getPlayers(ctx, "targets")))
                )
            )
            .then(Commands.literal("able")
                .executes(ctx -> executeAble(ctx, perspective, Collections.emptyList()))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> executeAble(ctx, perspective, EntityArgument.getPlayers(ctx, "targets")))
                )
            );
    }
    
    private static Collection<ServerPlayer> getTargets(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> specified) {
        if (!specified.isEmpty()) {
            return specified;
        }
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            return Collections.singletonList(player);
        }
        return Collections.emptyList();
    }
    
    private static int executeForce(CommandContext<CommandSourceStack> ctx, String perspective, Collection<ServerPlayer> specified) {
        Collection<ServerPlayer> targets = getTargets(ctx, specified);
        
        if (targets.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("anos_modcontrol.cam.no_targets"));
            return 0;
        }
        
        int count = 0;
        for (ServerPlayer player : targets) {
            if (PlayerCamData.isPerspectiveRemoved(player.getUUID(), perspective)) {
                ctx.getSource().sendFailure(Component.translatable(
                    "anos_modcontrol.cam.cannot_force_removed", 
                    getPerspectiveName(perspective), 
                    player.getName().getString()));
                continue;
            }
            
            PlayerCamData.setForcedPerspective(player.getUUID(), perspective);
            NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(perspective));
            count++;
        }
        
        if (count > 0) {
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> 
                Component.translatable("anos_modcontrol.cam.force", getPerspectiveName(perspective), finalCount), true);
        }
        
        return count;
    }
    
    private static int executeCancel(CommandContext<CommandSourceStack> ctx, String perspective, Collection<ServerPlayer> specified) {
        Collection<ServerPlayer> targets = getTargets(ctx, specified);
        
        if (targets.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("anos_modcontrol.cam.no_targets"));
            return 0;
        }
        
        int count = 0;
        for (ServerPlayer player : targets) {
            PlayerCamData.clearForcedPerspective(player.getUUID());
            
            String targetPerspective = ModConfig.getCancelTarget(perspective);
            
            if (PlayerCamData.isPerspectiveRemoved(player.getUUID(), targetPerspective)) {
                targetPerspective = PlayerCamData.getNextAvailablePerspective(player.getUUID(), perspective);
            }
            
            NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(perspective, targetPerspective));
            count++;
        }
        
        if (count > 0) {
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> 
                Component.translatable("anos_modcontrol.cam.cancel", finalCount), true);
        }
        
        return count;
    }
    
    private static int executeRemove(CommandContext<CommandSourceStack> ctx, String perspective, Collection<ServerPlayer> specified) {
        Collection<ServerPlayer> targets = getTargets(ctx, specified);
        
        if (targets.isEmpty()) {
            // Global remove
            PlayerCamData.removeGlobalPerspective(perspective);
            
            if (ctx.getSource().getServer() != null) {
                for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                    // If forced to this perspective, cancel it first with proper switch
                    String forced = PlayerCamData.getForcedPerspective(player.getUUID());
                    if (perspective.equals(forced)) {
                        PlayerCamData.clearForcedPerspective(player.getUUID());
                    }
                    
                    // Get the cancel target based on config
                    String targetPerspective = ModConfig.getCancelTarget(perspective);
                    if (PlayerCamData.isPerspectiveRemoved(player.getUUID(), targetPerspective) || 
                        targetPerspective.equals(perspective)) {
                        targetPerspective = PlayerCamData.getNextAvailablePerspective(player.getUUID(), perspective);
                    }
                    
                    // Send cancel packet first to switch perspective
                    NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(perspective, targetPerspective));
                    // Then send remove packet
                    NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(PlayerCamSyncPacket.Action.REMOVE, perspective));
                }
            }
            
            ctx.getSource().sendSuccess(() -> 
                Component.translatable("anos_modcontrol.cam.remove_global", getPerspectiveName(perspective)), true);
            return 1;
        }
        
        int count = 0;
        for (ServerPlayer player : targets) {
            // If forced to this perspective, cancel it first
            String forced = PlayerCamData.getForcedPerspective(player.getUUID());
            if (perspective.equals(forced)) {
                PlayerCamData.clearForcedPerspective(player.getUUID());
            }
            
            // Get the cancel target based on config
            String targetPerspective = ModConfig.getCancelTarget(perspective);
            if (PlayerCamData.isPerspectiveRemoved(player.getUUID(), targetPerspective) || 
                targetPerspective.equals(perspective)) {
                targetPerspective = PlayerCamData.getNextAvailablePerspective(player.getUUID(), perspective);
            }
            
            PlayerCamData.removePerspective(player.getUUID(), perspective);
            
            // Send cancel packet first to switch perspective
            NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(perspective, targetPerspective));
            // Then send remove packet
            NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(PlayerCamSyncPacket.Action.REMOVE, perspective));
            count++;
        }
        
        if (count > 0) {
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> 
                Component.translatable("anos_modcontrol.cam.remove_players", getPerspectiveName(perspective), finalCount), true);
        }
        
        return count;
    }
    
    private static int executeAble(CommandContext<CommandSourceStack> ctx, String perspective, Collection<ServerPlayer> specified) {
        Collection<ServerPlayer> targets = getTargets(ctx, specified);
        
        if (targets.isEmpty()) {
            PlayerCamData.restoreGlobalPerspective(perspective);
            
            if (ctx.getSource().getServer() != null) {
                for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                    NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(PlayerCamSyncPacket.Action.RESTORE, perspective));
                }
            }
            
            ctx.getSource().sendSuccess(() -> 
                Component.translatable("anos_modcontrol.cam.restore_global", getPerspectiveName(perspective)), true);
            return 1;
        }
        
        int count = 0;
        for (ServerPlayer player : targets) {
            PlayerCamData.restorePerspective(player.getUUID(), perspective);
            NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(PlayerCamSyncPacket.Action.RESTORE, perspective));
            count++;
        }
        
        if (count > 0) {
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> 
                Component.translatable("anos_modcontrol.cam.restore_players", getPerspectiveName(perspective), finalCount), true);
        }
        
        return count;
    }
    
    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.translatable("anos_modcontrol.cam.list.title"), false);
        
        Set<String> globalRemoved = PlayerCamData.getGlobalRemovedPerspectives();
        if (!globalRemoved.isEmpty()) {
            ctx.getSource().sendSuccess(() -> 
                Component.translatable("anos_modcontrol.cam.list.global_removed", String.join(", ", globalRemoved)), false);
        }
        
        if (ctx.getSource().getServer() != null) {
            for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                String forced = PlayerCamData.getForcedPerspective(player.getUUID());
                Set<String> removed = PlayerCamData.getRemovedPerspectives(player.getUUID());
                removed.removeAll(globalRemoved);
                
                if (forced != null || !removed.isEmpty()) {
                    StringBuilder sb = new StringBuilder("§e" + player.getName().getString() + "§7:");
                    if (forced != null) {
                        sb.append(" §bForced: ").append(forced);
                    }
                    if (!removed.isEmpty()) {
                        sb.append(" §cRemoved: ").append(String.join(", ", removed));
                    }
                    final String msg = sb.toString();
                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                }
            }
        }
        
        return 1;
    }
    
    private static int executeListForPlayers(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            String forced = PlayerCamData.getForcedPerspective(player.getUUID());
            Set<String> removed = PlayerCamData.getRemovedPerspectives(player.getUUID());
            Set<String> available = PlayerCamData.getAvailablePerspectives(player.getUUID());
            
            ctx.getSource().sendSuccess(() -> Component.literal("§6=== " + player.getName().getString() + " ==="), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Forced: " + (forced != null ? "§b" + forced : "§aNone")), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Removed: " + (removed.isEmpty() ? "§aNone" : "§c" + String.join(", ", removed))), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Available: §a" + String.join(", ", available)), false);
        }
        return players.size();
    }
    
    private static int executeReset(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> specified) {
        Collection<ServerPlayer> targets = getTargets(ctx, specified);
        
        if (targets.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("anos_modcontrol.cam.no_targets"));
            return 0;
        }
        
        int count = 0;
        for (ServerPlayer player : targets) {
            PlayerCamData.clearForcedPerspective(player.getUUID());
            for (String perspective : PlayerCamData.ALL_PERSPECTIVES) {
                PlayerCamData.restorePerspective(player.getUUID(), perspective);
            }
            
            NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(
                PlayerCamData.getRemovedPerspectives(player.getUUID()),
                null
            ));
            count++;
        }
        
        if (count > 0) {
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> 
                Component.translatable("anos_modcontrol.cam.reset", finalCount), true);
        }
        
        return count;
    }
    
    private static Component getPerspectiveName(String perspective) {
        return switch (perspective) {
            case PlayerCamData.FIRST_PERSON -> Component.translatable("anos_modcontrol.cam.perspective.first");
            case PlayerCamData.SECOND_PERSON -> Component.translatable("anos_modcontrol.cam.perspective.second");
            case PlayerCamData.THIRD_PERSON -> Component.translatable("anos_modcontrol.cam.perspective.third");
            default -> Component.literal(perspective);
        };
    }
}