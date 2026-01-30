package net.ano.modcontrol.network;

import net.ano.modcontrol.AnosModControl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(AnosModControl.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    public static void register() {
        CHANNEL.registerMessage(
            packetId++,
            PlayerCamSyncPacket.class,
            PlayerCamSyncPacket::encode,
            PlayerCamSyncPacket::decode,
            PlayerCamSyncPacket::handle
        );
        
        AnosModControl.LOGGER.info("Network packets registered!");
    }
    
    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    public static void sendToAllPlayers(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}