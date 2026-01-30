package net.ano.modcontrol.handler;

import net.ano.modcontrol.data.PlayerCamData;
import net.ano.modcontrol.network.NetworkHandler;
import net.ano.modcontrol.network.PlayerCamSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerCamEventHandler {
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Sync camera state to player
            String forced = PlayerCamData.getForcedPerspective(player.getUUID());
            java.util.Set<String> removed = PlayerCamData.getRemovedPerspectives(player.getUUID());
            
            NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(removed, forced));
        }
    }
    
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Re-sync camera state after respawn
            String forced = PlayerCamData.getForcedPerspective(player.getUUID());
            java.util.Set<String> removed = PlayerCamData.getRemovedPerspectives(player.getUUID());
            
            NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(removed, forced));
        }
    }
    
    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Re-sync camera state after dimension change
            String forced = PlayerCamData.getForcedPerspective(player.getUUID());
            java.util.Set<String> removed = PlayerCamData.getRemovedPerspectives(player.getUUID());
            
            NetworkHandler.sendToPlayer(player, new PlayerCamSyncPacket(removed, forced));
        }
    }
}