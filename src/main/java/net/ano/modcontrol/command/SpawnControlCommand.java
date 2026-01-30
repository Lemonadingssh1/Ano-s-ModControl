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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SpawnControlCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("SpawnControl")
            .requires(source -> source.hasPermission(2))
            
            // /SpawnControl entity <entityId> [entityId2...] to on/off
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
            
            // /SpawnControl mod <modId> [modId2...] to on/off
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
            
            // /SpawnControl list
            .then(Commands.literal("list").executes(SpawnControlCommand::executeListCommand))
            
            // /SpawnControl clear
            .then(Commands.literal("clear").executes(SpawnControlCommand::executeClearCommand));
        
        dispatcher.register(command);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> createEntityArgument(String name) {
        return Commands.argument(name, ResourceLocationArgument.id())
            .suggests((context, builder) -> {
                Set<ResourceLocation> entities = ModEntityUtils.getAllLivingEntities();
                return SharedSuggestionProvider.suggestResource(entities, builder);
            });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> createModArgument(String name) {
        return Commands.argument(name, StringArgumentType.word())
            .suggests((context, builder) -> {
                Set<String> mods = ModEntityUtils.getModsWithEntities();
                return SharedSuggestionProvider.suggest(mods, builder);
            });
    }

    private static int executeEntityCommand(CommandContext<CommandSourceStack> context, boolean enabled, String... argNames) {
        List<String> processedEntities = new ArrayList<>();
        List<ResourceLocation> entityIds = new ArrayList<>();
        int successCount = 0;
        
        for (String argName : argNames) {
            try {
                ResourceLocation entityId = ResourceLocationArgument.getId(context, argName);
                
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
                    context.getSource().sendFailure(Component.literal("Unknown entity: " + entityId));
                    continue;
                }
                
                ModControlData.setEntitySpawnEnabled(entityId, enabled);
                processedEntities.add(entityId.toString());
                entityIds.add(entityId);
                successCount++;
            } catch (Exception e) {
                context.getSource().sendFailure(Component.literal("Invalid entity ID in argument: " + argName));
            }
        }
        
        if (successCount > 0) {
            String action = enabled ? "enabled" : "disabled";
            context.getSource().sendSuccess(() -> 
                Component.literal("Spawning " + action + " for: " + String.join(", ", processedEntities)), true);
            
            // If disabling, remove existing entities
            if (!enabled) {
                int removed = removeExistingEntities(context.getSource(), entityIds);
                if (removed > 0) {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("Removed " + removed + " existing entities from the world."), true);
                }
            }
        }
        
        return successCount;
    }

    private static int executeModCommand(CommandContext<CommandSourceStack> context, boolean enabled, String... argNames) {
        List<String> processedMods = new ArrayList<>();
        List<String> modIds = new ArrayList<>();
        Set<String> validMods = ModEntityUtils.getModsWithEntities();
        int successCount = 0;
        
        for (String argName : argNames) {
            try {
                String modId = StringArgumentType.getString(context, argName).toLowerCase();
                
                if (!validMods.contains(modId)) {
                    context.getSource().sendFailure(Component.literal("Mod '" + modId + "' not found or has no entities."));
                    continue;
                }
                
                ModControlData.setModSpawnEnabled(modId, enabled);
                processedMods.add(modId);
                modIds.add(modId);
                successCount++;
            } catch (Exception e) {
                context.getSource().sendFailure(Component.literal("Invalid mod ID in argument: " + argName));
            }
        }
        
        if (successCount > 0) {
            String action = enabled ? "enabled" : "disabled";
            context.getSource().sendSuccess(() -> 
                Component.literal("Spawning " + action + " for mods: " + String.join(", ", processedMods)), true);
            
            // If disabling, remove existing entities from those mods
            if (!enabled) {
                int removed = removeExistingEntitiesByMod(context.getSource(), modIds);
                if (removed > 0) {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("Removed " + removed + " existing entities from the world."), true);
                }
            }
        }
        
        return successCount;
    }

    private static int removeExistingEntities(CommandSourceStack source, List<ResourceLocation> entityIds) {
        int removedCount = 0;
        
        if (source.getServer() == null) return 0;
        
        for (ServerLevel level : source.getServer().getAllLevels()) {
            List<Entity> toRemove = new ArrayList<>();
            
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity)) continue;
                
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                if (entityId != null && entityIds.contains(entityId)) {
                    toRemove.add(entity);
                }
            }
            
            for (Entity entity : toRemove) {
                entity.discard();
                removedCount++;
            }
        }
        
        return removedCount;
    }

    private static int removeExistingEntitiesByMod(CommandSourceStack source, List<String> modIds) {
        int removedCount = 0;
        
        if (source.getServer() == null) return 0;
        
        for (ServerLevel level : source.getServer().getAllLevels()) {
            List<Entity> toRemove = new ArrayList<>();
            
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity)) continue;
                
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                if (entityId != null && modIds.contains(entityId.getNamespace())) {
                    toRemove.add(entity);
                }
            }
            
            for (Entity entity : toRemove) {
                entity.discard();
                removedCount++;
            }
        }
        
        return removedCount;
    }

    private static int executeListCommand(CommandContext<CommandSourceStack> context) {
        Set<ResourceLocation> disabledEntities = ModControlData.getDisabledEntities();
        Set<String> disabledMods = ModControlData.getDisabledMods();
        
        context.getSource().sendSuccess(() -> Component.literal("=== Spawn Control Status ==="), false);
        
        if (disabledEntities.isEmpty() && disabledMods.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No spawn restrictions active."), false);
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

    private static int executeClearCommand(CommandContext<CommandSourceStack> context) {
        ModControlData.getDisabledEntities().forEach(e -> ModControlData.setEntitySpawnEnabled(e, true));
        ModControlData.getDisabledMods().forEach(m -> ModControlData.setModSpawnEnabled(m, true));
        context.getSource().sendSuccess(() -> Component.literal("All spawn restrictions cleared."), true);
        return 1;
    }
}