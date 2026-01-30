// src/main/java/net/ano/modcontrol/command/argument/BlockBreakEntityIdArgument.java
package net.ano.modcontrol.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.ano.modcontrol.util.ModEntityUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class BlockBreakEntityIdArgument implements ArgumentType<ResourceLocation> {
    
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ENTITY = new DynamicCommandExceptionType(
        (entityId) -> Component.literal("Unknown entity or not a block breaker: " + entityId)
    );

    private static final Collection<String> EXAMPLES = Arrays.asList(
        "minecraft:creeper", 
        "minecraft:enderman", 
        "minecraft:wither"
    );

    private BlockBreakEntityIdArgument() {
    }

    public static BlockBreakEntityIdArgument entityId() {
        return new BlockBreakEntityIdArgument();
    }

    public static ResourceLocation getEntityId(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, ResourceLocation.class);
    }

    @Override
    public ResourceLocation parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        ResourceLocation resourceLocation = ResourceLocation.read(reader);
        
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(resourceLocation)) {
            reader.setCursor(start);
            throw ERROR_UNKNOWN_ENTITY.createWithContext(reader, resourceLocation);
        }
        
        Set<ResourceLocation> blockBreakers = ModEntityUtils.getBlockBreakingEntities();
        if (!blockBreakers.contains(resourceLocation)) {
            reader.setCursor(start);
            throw ERROR_UNKNOWN_ENTITY.createWithContext(reader, resourceLocation);
        }
        
        return resourceLocation;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Set<ResourceLocation> entities = ModEntityUtils.getBlockBreakingEntities();
        return SharedSuggestionProvider.suggestResource(entities, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}