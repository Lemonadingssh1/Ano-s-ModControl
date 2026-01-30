// src/main/java/net/ano/modcontrol/command/argument/ArgumentTypeRegistry.java
package net.ano.modcontrol.command.argument;

import net.ano.modcontrol.AnosModControl;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ArgumentTypeRegistry {
    
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES = 
        DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, AnosModControl.MODID);

    public static final RegistryObject<ArgumentTypeInfo<?, ?>> ENTITY_MOD_ID = ARGUMENT_TYPES.register(
        "entity_mod_id",
        () -> ArgumentTypeInfos.registerByClass(
            EntityModIdArgument.class,
            SingletonArgumentInfo.contextFree(EntityModIdArgument::modId)
        )
    );

    public static final RegistryObject<ArgumentTypeInfo<?, ?>> BLOCK_BREAK_MOD_ID = ARGUMENT_TYPES.register(
        "block_break_mod_id",
        () -> ArgumentTypeInfos.registerByClass(
            BlockBreakModIdArgument.class,
            SingletonArgumentInfo.contextFree(BlockBreakModIdArgument::modId)
        )
    );

    public static final RegistryObject<ArgumentTypeInfo<?, ?>> ENTITY_ID = ARGUMENT_TYPES.register(
        "entity_id",
        () -> ArgumentTypeInfos.registerByClass(
            EntityIdArgument.class,
            SingletonArgumentInfo.contextFree(EntityIdArgument::entityId)
        )
    );

    public static final RegistryObject<ArgumentTypeInfo<?, ?>> BLOCK_BREAK_ENTITY_ID = ARGUMENT_TYPES.register(
        "block_break_entity_id",
        () -> ArgumentTypeInfos.registerByClass(
            BlockBreakEntityIdArgument.class,
            SingletonArgumentInfo.contextFree(BlockBreakEntityIdArgument::entityId)
        )
    );

    public static void register(IEventBus eventBus) {
        ARGUMENT_TYPES.register(eventBus);
    }
}