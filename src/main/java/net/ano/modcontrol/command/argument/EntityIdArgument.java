// src/main/java/net/ano/modcontrol/command/argument/EntityIdArgument.java
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

public class EntityIdArgument implements ArgumentType<ResourceLocation> {
    
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ENTITY = new DynamicCommandExceptionType(
        (entityId) -> Component.literal("Unknown entity: " + entityId)
    );

    private static final Collection<String> EXAMPLES = Arrays.asList(
        "minecraft:zombie", 
        "minecraft:creeper", 
        "minecraft:skeleton"
    );

    private EntityIdArgument() {
    }

    public static EntityIdArgument entityId() {
        return new EntityIdArgument();
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
        
        Set<ResourceLocation> livingEntities = ModEntityUtils.getAllLivingEntities();
        if (!livingEntities.contains(resourceLocation)) {
            reader.setCursor(start);
            throw ERROR_UNKNOWN_ENTITY.createWithContext(reader, resourceLocation);
        }
        
        return resourceLocation;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Set<ResourceLocation> entities = ModEntityUtils.getAllLivingEntities();
        return SharedSuggestionProvider.suggestResource(entities, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}