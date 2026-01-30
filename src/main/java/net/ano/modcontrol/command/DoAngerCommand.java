package net.ano.modcontrol.command;

import net.ano.modcontrol.data.ModControlData;
import net.ano.modcontrol.util.ModEntityUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DoAngerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("DoAnger")
            .requires(source -> source.hasPermission(2))
            
            .then(Commands.literal("entity")
                .then(createEntityChain(1))
            )
            
            .then(Commands.literal("mod")
                .then(createModChain(1))
            )
            
            .then(Commands.literal("list").executes(DoAngerCommand::executeListCommand))
            .then(Commands.literal("clear").executes(DoAngerCommand::executeClearCommand))
        );
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> createEntityChain(int depth) {
        RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> arg = Commands.argument("entity" + depth, ResourceLocationArgument.id())
            .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(ModEntityUtils.getAllLivingEntities(), builder));
        
        arg.then(Commands.literal("to")
            .then(Commands.literal("on")
                .executes(ctx -> executeEntityCommand(ctx, true, null, depth))
                .then(Commands.argument("targets", StringArgumentType.greedyString())
                    .suggests(DoAngerCommand::suggestTargets)
                    .executes(ctx -> executeEntityCommand(ctx, true, "targets", depth))
                )
            )
            .then(Commands.literal("off")
                .executes(ctx -> executeEntityCommand(ctx, false, null, depth))
                .then(Commands.argument("targets", StringArgumentType.greedyString())
                    .suggests(DoAngerCommand::suggestTargets)
                    .executes(ctx -> executeEntityCommand(ctx, false, "targets", depth))
                )
            )
        );
        
        if (depth < 5) {
            arg.then(createEntityChain(depth + 1));
        }
        
        return arg;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> createModChain(int depth) {
        RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> arg = Commands.argument("mod" + depth, ResourceLocationArgument.id())
            .suggests((ctx, builder) -> {
                Set<String> mods = ModEntityUtils.getModsWithEntities();
                String remaining = builder.getRemaining().toLowerCase();
                for (String mod : mods) {
                    if (mod.toLowerCase().startsWith(remaining)) {
                        builder.suggest(mod);
                    }
                }
                return builder.buildFuture();
            });
        
        arg.then(Commands.literal("to")
            .then(Commands.literal("on")
                .executes(ctx -> executeModCommand(ctx, true, null, depth))
                .then(Commands.argument("targets", StringArgumentType.greedyString())
                    .suggests(DoAngerCommand::suggestTargets)
                    .executes(ctx -> executeModCommand(ctx, true, "targets", depth))
                )
            )
            .then(Commands.literal("off")
                .executes(ctx -> executeModCommand(ctx, false, null, depth))
                .then(Commands.argument("targets", StringArgumentType.greedyString())
                    .suggests(DoAngerCommand::suggestTargets)
                    .executes(ctx -> executeModCommand(ctx, false, "targets", depth))
                )
            )
        );
        
        if (depth < 5) {
            arg.then(createModChain(depth + 1));
        }
        
        return arg;
    }

    private static CompletableFuture<Suggestions> suggestTargets(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> suggestions = new ArrayList<>(Arrays.asList("@a", "@e", "@p", "@r", "@s"));
        
        if (context.getSource().getServer() != null) {
            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                suggestions.add(player.getName().getString());
            }
        }
        
        ModEntityUtils.getAllLivingEntities().forEach(rl -> suggestions.add(rl.toString()));
        
        String remaining = builder.getRemaining().toLowerCase();
        String[] parts = remaining.split(" ");
        String lastPart = parts.length > 0 ? parts[parts.length - 1] : "";
        
        return SharedSuggestionProvider.suggest(
            suggestions.stream().filter(s -> s.toLowerCase().startsWith(lastPart)).collect(Collectors.toList()),
            builder
        );
    }

    private static Set<String> parseTargets(CommandContext<CommandSourceStack> context, String targetArgName) {
        Set<String> targets = new HashSet<>();
        if (targetArgName == null) {
            targets.add("*");
            return targets;
        }
        try {
            String input = StringArgumentType.getString(context, targetArgName);
            for (String part : input.split("\\s+")) {
                if (!part.isEmpty()) targets.add(part.trim());
            }
        } catch (Exception ignored) {}
        if (targets.isEmpty()) targets.add("*");
        return targets;
    }

    private static String extractModId(CommandContext<CommandSourceStack> context, String argName) {
        try {
            ResourceLocation rl = ResourceLocationArgument.getId(context, argName);
            return rl.getNamespace();
        } catch (Exception e) {
            try {
                return context.getArgument(argName, String.class).toLowerCase();
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static int executeEntityCommand(CommandContext<CommandSourceStack> context, boolean enabled, String targetArgName, int entityCount) {
        List<String> processedEntities = new ArrayList<>();
        Set<String> targets = parseTargets(context, targetArgName);
        int successCount = 0;
        
        for (int i = 1; i <= entityCount; i++) {
            try {
                ResourceLocation entityId = ResourceLocationArgument.getId(context, "entity" + i);
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
                    context.getSource().sendFailure(Component.literal("Unknown entity: " + entityId));
                    continue;
                }
                
                ModControlData.setEntityAngerEnabled(entityId, enabled, targets);
                processedEntities.add(entityId.toString());
                successCount++;
            } catch (Exception ignored) {}
        }
        
        if (successCount > 0) {
            String action = enabled ? (targets.contains("*") ? "reset to default" : "forced on") : "disabled";
            String targetDesc = targets.contains("*") ? "" : " toward " + String.join(", ", targets);
            context.getSource().sendSuccess(() -> 
                Component.literal("Anger " + action + " for: " + String.join(", ", processedEntities) + targetDesc), true);
        }
        
        return successCount;
    }

    private static int executeModCommand(CommandContext<CommandSourceStack> context, boolean enabled, String targetArgName, int modCount) {
        List<String> processedMods = new ArrayList<>();
        Set<String> targets = parseTargets(context, targetArgName);
        Set<String> validMods = ModEntityUtils.getModsWithEntities();
        int successCount = 0;
        
        for (int i = 1; i <= modCount; i++) {
            String modId = extractModId(context, "mod" + i);
            if (modId == null) continue;
            
            if (!validMods.contains(modId)) {
                context.getSource().sendFailure(Component.literal("Mod '" + modId + "' not found or has no entities."));
                continue;
            }
            
            ModControlData.setModAngerEnabled(modId, enabled, targets);
            processedMods.add(modId);
            successCount++;
        }
        
        if (successCount > 0) {
            String action = enabled ? (targets.contains("*") ? "reset to default" : "forced on") : "disabled";
            String targetDesc = targets.contains("*") ? "" : " toward " + String.join(", ", targets);
            context.getSource().sendSuccess(() -> 
                Component.literal("Anger " + action + " for mods: " + String.join(", ", processedMods) + targetDesc), true);
        }
        
        return successCount;
    }

    private static int executeListCommand(CommandContext<CommandSourceStack> context) {
        Map<ResourceLocation, Set<String>> disabledEntities = ModControlData.getAngerDisabledEntities();
        Map<String, Set<String>> disabledMods = ModControlData.getAngerDisabledMods();
        Map<ResourceLocation, Set<String>> forcedEntities = ModControlData.getAngerForcedEntities();
        Map<String, Set<String>> forcedMods = ModControlData.getAngerForcedMods();
        
        context.getSource().sendSuccess(() -> Component.literal("=== Anger Control Status ==="), false);
        
        boolean hasAny = false;
        
        if (!disabledMods.isEmpty() || !disabledEntities.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§c[DISABLED]"), false);
            hasAny = true;
            disabledMods.forEach((modId, targets) -> {
                String targetDesc = targets.contains("*") ? "all" : String.join(", ", targets);
                context.getSource().sendSuccess(() -> Component.literal("  Mod [" + modId + "] -> " + targetDesc), false);
            });
            disabledEntities.forEach((entityId, targets) -> {
                String targetDesc = targets.contains("*") ? "all" : String.join(", ", targets);
                context.getSource().sendSuccess(() -> Component.literal("  Entity [" + entityId + "] -> " + targetDesc), false);
            });
        }
        
        if (!forcedMods.isEmpty() || !forcedEntities.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§a[FORCED ON]"), false);
            hasAny = true;
            forcedMods.forEach((modId, targets) -> {
                context.getSource().sendSuccess(() -> Component.literal("  Mod [" + modId + "] -> " + String.join(", ", targets)), false);
            });
            forcedEntities.forEach((entityId, targets) -> {
                context.getSource().sendSuccess(() -> Component.literal("  Entity [" + entityId + "] -> " + String.join(", ", targets)), false);
            });
        }
        
        if (!hasAny) {
            context.getSource().sendSuccess(() -> Component.literal("No anger rules active."), false);
        }
        
        return 1;
    }

    private static int executeClearCommand(CommandContext<CommandSourceStack> context) {
        ModControlData.getAngerDisabledEntities().forEach((e, t) -> ModControlData.setEntityAngerEnabled(e, true, Set.of("*")));
        ModControlData.getAngerDisabledMods().forEach((m, t) -> ModControlData.setModAngerEnabled(m, true, Set.of("*")));
        ModControlData.getAngerForcedEntities().forEach((e, t) -> ModControlData.setEntityAngerEnabled(e, true, Set.of("*")));
        ModControlData.getAngerForcedMods().forEach((m, t) -> ModControlData.setModAngerEnabled(m, true, Set.of("*")));
        context.getSource().sendSuccess(() -> Component.literal("All anger rules cleared."), true);
        return 1;
    }
}