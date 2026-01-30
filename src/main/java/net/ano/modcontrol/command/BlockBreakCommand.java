package net.ano.modcontrol.command;

import net.ano.modcontrol.data.ModControlData;
import net.ano.modcontrol.util.ModEntityUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockBreakCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("BlockBreak")
            .requires(source -> source.hasPermission(2))
            
            // /BlockBreak entity <entityId> [entityId2...] to on/off
            .then(Commands.literal("entity")
                .then(createEntityArgument("entity1")
                    .then(Commands.literal("to")
                        .then(Commands.literal("on").executes(ctx -> executeEntityCommand(ctx, true, "entity1")))
                        .then(Commands.literal("off").executes(ctx -> executeEntityCommand(ctx, false, "entity1")))
                    )
                    .then(createEntityArgument("entity2")
                        .then(Commands.literal("to")
                            .then(Commands.literal("on").executes(ctx -> executeEntityCommand(ctx, true, "entity1", "entity2")))
                            .then(Commands.literal("off").executes(ctx -> executeEntityCommand(ctx, false, "entity1", "entity2")))
                        )
                        .then(createEntityArgument("entity3")
                            .then(Commands.literal("to")
                                .then(Commands.literal("on").executes(ctx -> executeEntityCommand(ctx, true, "entity1", "entity2", "entity3")))
                                .then(Commands.literal("off").executes(ctx -> executeEntityCommand(ctx, false, "entity1", "entity2", "entity3")))
                            )
                            .then(createEntityArgument("entity4")
                                .then(Commands.literal("to")
                                    .then(Commands.literal("on").executes(ctx -> executeEntityCommand(ctx, true, "entity1", "entity2", "entity3", "entity4")))
                                    .then(Commands.literal("off").executes(ctx -> executeEntityCommand(ctx, false, "entity1", "entity2", "entity3", "entity4")))
                                )
                                .then(createEntityArgument("entity5")
                                    .then(Commands.literal("to")
                                        .then(Commands.literal("on").executes(ctx -> executeEntityCommand(ctx, true, "entity1", "entity2", "entity3", "entity4", "entity5")))
                                        .then(Commands.literal("off").executes(ctx -> executeEntityCommand(ctx, false, "entity1", "entity2", "entity3", "entity4", "entity5")))
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            // /BlockBreak mod <modId> [modId2...] to on/off
            .then(Commands.literal("mod")
                .then(createModArgument("mod1")
                    .then(Commands.literal("to")
                        .then(Commands.literal("on").executes(ctx -> executeModCommand(ctx, true, "mod1")))
                        .then(Commands.literal("off").executes(ctx -> executeModCommand(ctx, false, "mod1")))
                    )
                    .then(createModArgument("mod2")
                        .then(Commands.literal("to")
                            .then(Commands.literal("on").executes(ctx -> executeModCommand(ctx, true, "mod1", "mod2")))
                            .then(Commands.literal("off").executes(ctx -> executeModCommand(ctx, false, "mod1", "mod2")))
                        )
                        .then(createModArgument("mod3")
                            .then(Commands.literal("to")
                                .then(Commands.literal("on").executes(ctx -> executeModCommand(ctx, true, "mod1", "mod2", "mod3")))
                                .then(Commands.literal("off").executes(ctx -> executeModCommand(ctx, false, "mod1", "mod2", "mod3")))
                            )
                            .then(createModArgument("mod4")
                                .then(Commands.literal("to")
                                    .then(Commands.literal("on").executes(ctx -> executeModCommand(ctx, true, "mod1", "mod2", "mod3", "mod4")))
                                    .then(Commands.literal("off").executes(ctx -> executeModCommand(ctx, false, "mod1", "mod2", "mod3", "mod4")))
                                )
                                .then(createModArgument("mod5")
                                    .then(Commands.literal("to")
                                        .then(Commands.literal("on").executes(ctx -> executeModCommand(ctx, true, "mod1", "mod2", "mod3", "mod4", "mod5")))
                                        .then(Commands.literal("off").executes(ctx -> executeModCommand(ctx, false, "mod1", "mod2", "mod3", "mod4", "mod5")))
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            // /BlockBreak list
            .then(Commands.literal("list").executes(BlockBreakCommand::executeListCommand))
            
            // /BlockBreak detect
            .then(Commands.literal("detect").executes(BlockBreakCommand::executeDetectCommand))
            
            // /BlockBreak clear
            .then(Commands.literal("clear").executes(BlockBreakCommand::executeClearCommand));
        
        dispatcher.register(command);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> createEntityArgument(String name) {
        return Commands.argument(name, ResourceLocationArgument.id())
            .suggests((context, builder) -> {
                Set<ResourceLocation> entities = ModEntityUtils.getBlockBreakingEntities();
                return SharedSuggestionProvider.suggestResource(entities, builder);
            });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> createModArgument(String name) {
        return Commands.argument(name, StringArgumentType.word())
            .suggests((context, builder) -> {
                Set<String> mods = ModEntityUtils.getModsWithBlockBreakers();
                return SharedSuggestionProvider.suggest(mods, builder);
            });
    }

    private static int executeEntityCommand(CommandContext<CommandSourceStack> context, boolean enabled, String... argNames) {
        List<String> processedEntities = new ArrayList<>();
        Set<ResourceLocation> blockBreakers = ModEntityUtils.getBlockBreakingEntities();
        int successCount = 0;
        
        for (String argName : argNames) {
            try {
                ResourceLocation entityId = ResourceLocationArgument.getId(context, argName);
                
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
                    context.getSource().sendFailure(Component.literal("Unknown entity: " + entityId));
                    continue;
                }
                
                if (!blockBreakers.contains(entityId)) {
                    context.getSource().sendFailure(Component.literal("Entity '" + entityId + "' is not a known block breaker."));
                    continue;
                }
                
                ModControlData.setEntityBlockBreakEnabled(entityId, enabled);
                processedEntities.add(entityId.toString());
                successCount++;
            } catch (Exception e) {
                context.getSource().sendFailure(Component.literal("Invalid entity ID in argument: " + argName));
            }
        }
        
        if (successCount > 0) {
            String action = enabled ? "enabled" : "disabled";
            context.getSource().sendSuccess(() -> 
                Component.literal("Block breaking " + action + " for: " + String.join(", ", processedEntities)), true);
        }
        
        return successCount;
    }

    private static int executeModCommand(CommandContext<CommandSourceStack> context, boolean enabled, String... argNames) {
        List<String> processedMods = new ArrayList<>();
        Set<String> validMods = ModEntityUtils.getModsWithBlockBreakers();
        int successCount = 0;
        
        for (String argName : argNames) {
            try {
                String modId = StringArgumentType.getString(context, argName).toLowerCase();
                
                if (!validMods.contains(modId)) {
                    context.getSource().sendFailure(Component.literal("Mod '" + modId + "' not found or has no block-breaking entities."));
                    continue;
                }
                
                ModControlData.setModBlockBreakEnabled(modId, enabled);
                processedMods.add(modId);
                successCount++;
            } catch (Exception e) {
                context.getSource().sendFailure(Component.literal("Invalid mod ID in argument: " + argName));
            }
        }
        
        if (successCount > 0) {
            String action = enabled ? "enabled" : "disabled";
            context.getSource().sendSuccess(() -> 
                Component.literal("Block breaking " + action + " for mods: " + String.join(", ", processedMods)), true);
        }
        
        return successCount;
    }

    private static int executeListCommand(CommandContext<CommandSourceStack> context) {
        Set<ResourceLocation> disabledEntities = ModControlData.getBlockBreakDisabledEntities();
        Set<String> disabledMods = ModControlData.getBlockBreakDisabledMods();
        
        context.getSource().sendSuccess(() -> Component.literal("=== Block Break Control Status ==="), false);
        
        if (disabledEntities.isEmpty() && disabledMods.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No block break restrictions active."), false);
        } else {
            if (!disabledMods.isEmpty()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("Disabled Mods: " + String.join(", ", disabledMods)), false);
            }
            if (!disabledEntities.isEmpty()) {
                String entities = disabledEntities.stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.joining(", "));
                context.getSource().sendSuccess(() -> 
                    Component.literal("Disabled Entities: " + entities), false);
            }
        }
        
        return 1;
    }

    private static int executeDetectCommand(CommandContext<CommandSourceStack> context) {
        Set<ResourceLocation> blockBreakers = ModEntityUtils.getBlockBreakingEntities();
        context.getSource().sendSuccess(() -> Component.literal("=== Detected Block-Breaking Entities ==="), false);
        
        blockBreakers.stream()
            .collect(Collectors.groupingBy(ResourceLocation::getNamespace))
            .forEach((modId, entities) -> {
                String entityList = entities.stream()
                    .map(ResourceLocation::getPath)
                    .collect(Collectors.joining(", "));
                context.getSource().sendSuccess(() -> 
                    Component.literal("[" + modId + "] " + entityList), false);
            });
        
        return 1;
    }

    private static int executeClearCommand(CommandContext<CommandSourceStack> context) {
        ModControlData.getBlockBreakDisabledEntities().forEach(e -> ModControlData.setEntityBlockBreakEnabled(e, true));
        ModControlData.getBlockBreakDisabledMods().forEach(m -> ModControlData.setModBlockBreakEnabled(m, true));
        context.getSource().sendSuccess(() -> Component.literal("All block break restrictions cleared."), true);
        return 1;
    }
}