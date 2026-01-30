// src/main/java/net/ano/modcontrol/command/argument/EntityModIdArgument.java
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class EntityModIdArgument implements ArgumentType<String> {
    
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_MOD = new DynamicCommandExceptionType(
        (modId) -> Component.literal("Unknown mod or mod has no entities: " + modId)
    );

    private static final Collection<String> EXAMPLES = Arrays.asList("minecraft", "alexsmobs", "twilightforest");

    private EntityModIdArgument() {
    }

    public static EntityModIdArgument modId() {
        return new EntityModIdArgument();
    }

    public static String getModId(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        
        while (reader.canRead() && isAllowedCharacter(reader.peek())) {
            reader.skip();
        }
        
        String modId = reader.getString().substring(start, reader.getCursor()).toLowerCase();
        
        if (modId.isEmpty()) {
            throw ERROR_UNKNOWN_MOD.createWithContext(reader, modId);
        }
        
        Set<String> validMods = ModEntityUtils.getModsWithEntities();
        if (!validMods.contains(modId)) {
            throw ERROR_UNKNOWN_MOD.createWithContext(reader, modId);
        }
        
        return modId;
    }

    private static boolean isAllowedCharacter(char c) {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.';
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Set<String> mods = ModEntityUtils.getModsWithEntities();
        return SharedSuggestionProvider.suggest(mods, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}