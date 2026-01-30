package net.ano.modcontrol;

import net.ano.modcontrol.command.*;
import net.ano.modcontrol.command.argument.ArgumentTypeRegistry;
import net.ano.modcontrol.config.ModConfig;
import net.ano.modcontrol.data.ModControlData;
import net.ano.modcontrol.data.PlayerCamData;
import net.ano.modcontrol.data.SupremeAuthorityData;
import net.ano.modcontrol.handler.*;
import net.ano.modcontrol.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AnosModControl.MODID)
public class AnosModControl {
    public static final String MODID = "anos_modcontrol";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AnosModControl() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register config
        ModConfig.register();
        
        // Register argument types
        ArgumentTypeRegistry.register(modEventBus);
        
        // Register common setup for networking
        modEventBus.addListener(this::commonSetup);
        
        // Register server-side event handlers
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new SpawnEventHandler());
        MinecraftForge.EVENT_BUS.register(new BlockBreakEventHandler());
        MinecraftForge.EVENT_BUS.register(new AttackEventHandler());
        MinecraftForge.EVENT_BUS.register(new AngerEventHandler());
        MinecraftForge.EVENT_BUS.register(new CommandProtectionHandler());
        MinecraftForge.EVENT_BUS.register(new CommandInterceptHandler());
        MinecraftForge.EVENT_BUS.register(new StcProtectionTickHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerCamEventHandler());
        
        // Register client-side event handlers
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
            MinecraftForge.EVENT_BUS.register(new PlayerCamClientHandler());
        });
        
        LOGGER.info("Ano's ModControl initialized!");
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Check config for each command
        if (ModConfig.ENABLE_SPAWN_CONTROL.get()) {
            SpawnControlCommand.register(event.getDispatcher());
        }
        if (ModConfig.ENABLE_BLOCK_BREAK.get()) {
            BlockBreakCommand.register(event.getDispatcher());
        }
        if (ModConfig.ENABLE_DO_ATTACK.get()) {
            DoAttackCommand.register(event.getDispatcher());
        }
        if (ModConfig.ENABLE_DO_ANGER.get()) {
            DoAngerCommand.register(event.getDispatcher());
        }
        if (ModConfig.ENABLE_STC.get()) {
            StcCommand.register(event.getDispatcher());
            StcNoCommand.register(event.getDispatcher());
            StcListCommand.register(event.getDispatcher());
        }
        if (ModConfig.ENABLE_PLAYER_CAM.get()) {
            PlayerCamCommand.register(event.getDispatcher());
        }
        
        LOGGER.info("Ano's ModControl commands registered!");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ModControlData.load(event.getServer());
        SupremeAuthorityData.load(event.getServer());
        PlayerCamData.load(event.getServer());
        LOGGER.info("Ano's ModControl data loaded!");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ModControlData.save(event.getServer());
        SupremeAuthorityData.save(event.getServer());
        PlayerCamData.save(event.getServer());
        LOGGER.info("Ano's ModControl data saved!");
    }
}