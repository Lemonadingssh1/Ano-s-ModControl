package net.ano.modcontrol.command;

import net.ano.modcontrol.data.ModControlData;
import net.ano.modcontrol.util.ModEntityUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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

public class DoAttackCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("DoAttack")
            .requires(source -> source.hasPermission(2))
            
            // Entity commands
            .then(Commands.literal("entity")
                .then(createEntityChain(1))
            )
            
            // Mod commands - aligned with entity structure
            .then(Commands.literal("mod")
                .then(createModChain(1))
            )
            
            .then(Commands.literal("list").executes(DoAttackCommand::executeListCommand))
            .then(Commands.literal("clear").executes(DoAttackCommand::executeClearCommand))
        );
    }

    // ============ Entity Chain Builder ============
    
    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> createEntityChain(int depth) {
        RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> arg = Commands.argument("entity" + depth, ResourceLocationArgument.id())
            .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(ModEntityUtils.getAllLivingEntities(), builder));
        
        // Add "to on/off" branch
        arg.then(Commands.literal("to")
            .then(Commands.literal("on")
                .executes(ctx -> executeEntityCommand(ctx, true, null, depth))
                .then(Commands.argument("targets", StringArgumentType.greedyString())
                    .suggests(DoAttackCommand::suggestTargets)
                    .executes(ctx -> executeEntityCommand(ctx, true, "targets", depth))
                )
            )
            .then(Commands.literal("off")
                .executes(ctx -> executeEntityCommand(ctx, false, null, depth))
                .then(Commands.argument("targets", StringArgumentType.greedyString())
                    .suggests(DoAttackCommand::suggestTargets)
                    .executes(ctx -> executeEntityCommand(ctx, false, "targets", depth))
                )
            )
        );
        
        // Add next entity in chain (up to 5)
        if (depth < 5) {
            arg.then(createEntityChain(depth + 1));
        }
        
        return arg;
    }

    // ============ Mod Chain Builder ============
    
    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> createModChain(int depth) {
        RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> arg = Commands.argument("mod" + depth, ResourceLocationArgument.id())
            .suggests((ctx, builder) -> {
                Set<String> mods = ModEntityUtils.getModsWithEntities();
                List<ResourceLocation> modRLs = mods.stream()
                    .map(m -> new ResourceLocation(m, "mod"))
                    .collect(Collectors.toList());
                
                String remaining = builder.getRemaining().toLowerCase();
                for (String mod : mods) {
                    if (mod.toLowerCase().startsWith(remaining)) {
                        builder.suggest(mod);
                    }
                }
                return builder.buildFuture();
            });
        
        // Add "to on/off" branch
        arg.then(Commands.literal("to")
            .then(Commands.literal("on")
                .executes(ctx -> executeModCommand(ctx, true, null, depth))
                .then(Commands.argument("targets", StringArgumentType.greedyString())
                    .suggests(DoAttackCommand::suggestTargets)
                    .executes(ctx -> executeModCommand(ctx, true, "targets", depth))
                )
            )
            .then(Commands.literal("off")
                .executes(ctx -> executeModCommand(ctx, false, null, depth))
                .then(Commands.argument("targets", StringArgumentType.greedyString())
                    .suggests(DoAttackCommand::suggestTargets)
                    .executes(ctx -> executeModCommand(ctx, false, "targets", depth))
                )
            )
        );
        
        // Add next mod in chain (up to 5)
        if (depth < 5) {
            arg.then(createModChain(depth + 1));
        }
        
        return arg;
    }

    // ============ Suggestions ============
    
    private static CompletableFuture<Suggestions> suggestTargets(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> suggestions = new ArrayList<>();
        suggestions.addAll(Arrays.asList("@a", "@e", "@p", "@r", "@s"));
        
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

    // ============ Parse Helpers ============
    
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
                // Fallback: try to get as string
                return context.getArgument(argName, String.class).toLowerCase();
            } catch (Exception e2) {
                return null;
            }
        }
    }

    // ============ Execute Commands ============
    
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
                
                ModControlData.setEntityAttackEnabled(entityId, enabled, targets);
                
                // If enabling attack with specific targets, also enable anger
                if (enabled && !targets.contains("*")) {
                    ModControlData.setEntityAngerEnabled(entityId, true, targets);
                }
                
                processedEntities.add(entityId.toString());
                successCount++;
            } catch (Exception ignored) {}
        }
        
        if (successCount > 0) {
            String action = enabled ? (targets.contains("*") ? "reset to default" : "forced on") : "disabled";
            String targetDesc = targets.contains("*") ? "" : " toward " + String.join(", ", targets);
            context.getSource().sendSuccess(() -> 
                Component.literal("Attack " + action + " for: " + String.join(", ", processedEntities) + targetDesc), true);
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
            
            ModControlData.setModAttackEnabled(modId, enabled, targets);
            
            if (enabled && !targets.contains("*")) {
                ModControlData.setModAngerEnabled(modId, true, targets);
            }
            
            processedMods.add(modId);
            successCount++;
        }
        
        if (successCount > 0) {
            String action = enabled ? (targets.contains("*") ? "reset to default" : "forced on") : "disabled";
            String targetDesc = targets.contains("*") ? "" : " toward " + String.join(", ", targets);
            context.getSource().sendSuccess(() -> 
                Component.literal("Attack " + action + " for mods: " + String.join(", ", processedMods) + targetDesc), true);
        }
        
        return successCount;
    }

    private static int executeListCommand(CommandContext<CommandSourceStack> context) {
        Map<ResourceLocation, Set<String>> disabledEntities = ModControlData.getAttackDisabledEntities();
        Map<String, Set<String>> disabledMods = ModControlData.getAttackDisabledMods();
        Map<ResourceLocation, Set<String>> forcedEntities = ModControlData.getAttackForcedEntities();
        Map<String, Set<String>> forcedMods = ModControlData.getAttackForcedMods();
        
        context.getSource().sendSuccess(() -> Component.literal("=== Attack Control Status ==="), false);
        
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
            context.getSource().sendSuccess(() -> Component.literal("No attack rules active."), false);
        }
        
        return 1;
    }

    private static int executeClearCommand(CommandContext<CommandSourceStack> context) {
        ModControlData.getAttackDisabledEntities().forEach((e, t) -> ModControlData.setEntityAttackEnabled(e, true, Set.of("*")));
        ModControlData.getAttackDisabledMods().forEach((m, t) -> ModControlData.setModAttackEnabled(m, true, Set.of("*")));
        ModControlData.getAttackForcedEntities().forEach((e, t) -> ModControlData.setEntityAttackEnabled(e, true, Set.of("*")));
        ModControlData.getAttackForcedMods().forEach((m, t) -> ModControlData.setModAttackEnabled(m, true, Set.of("*")));
        context.getSource().sendSuccess(() -> Component.literal("All attack rules cleared."), true);
        return 1;
    }
}