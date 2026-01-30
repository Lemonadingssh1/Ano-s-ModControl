package net.ano.modcontrol.network;

import net.ano.modcontrol.handler.PlayerCamClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class PlayerCamSyncPacket {
    
    public enum Action {
        FORCE,      // Force a perspective
        CANCEL,     // Cancel forced perspective and switch
        REMOVE,     // Remove a perspective from available list
        RESTORE,    // Restore a perspective to available list
        SYNC        // Full sync of state
    }
    
    private final Action action;
    private final String perspective;
    private final String targetPerspective; // For CANCEL - what to switch to
    private final Set<String> removedPerspectives; // For SYNC
    private final String forcedPerspective; // For SYNC
    
    // Constructor for FORCE action
    public PlayerCamSyncPacket(String perspective) {
        this.action = Action.FORCE;
        this.perspective = perspective;
        this.targetPerspective = null;
        this.removedPerspectives = null;
        this.forcedPerspective = null;
    }
    
    // Constructor for CANCEL action
    public PlayerCamSyncPacket(String currentPerspective, String targetPerspective) {
        this.action = Action.CANCEL;
        this.perspective = currentPerspective;
        this.targetPerspective = targetPerspective;
        this.removedPerspectives = null;
        this.forcedPerspective = null;
    }
    
    // Constructor for REMOVE/RESTORE action
    public PlayerCamSyncPacket(Action action, String perspective) {
        this.action = action;
        this.perspective = perspective;
        this.targetPerspective = null;
        this.removedPerspectives = null;
        this.forcedPerspective = null;
    }
    
    // Constructor for SYNC action
    public PlayerCamSyncPacket(Set<String> removedPerspectives, String forcedPerspective) {
        this.action = Action.SYNC;
        this.perspective = null;
        this.targetPerspective = null;
        this.removedPerspectives = removedPerspectives;
        this.forcedPerspective = forcedPerspective;
    }
    
    // Full constructor for decoding
    private PlayerCamSyncPacket(Action action, String perspective, String targetPerspective, 
                                 Set<String> removedPerspectives, String forcedPerspective) {
        this.action = action;
        this.perspective = perspective;
        this.targetPerspective = targetPerspective;
        this.removedPerspectives = removedPerspectives;
        this.forcedPerspective = forcedPerspective;
    }
    
    public static void encode(PlayerCamSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.perspective != null ? packet.perspective : "");
        buf.writeUtf(packet.targetPerspective != null ? packet.targetPerspective : "");
        
        if (packet.removedPerspectives != null) {
            buf.writeInt(packet.removedPerspectives.size());
            for (String p : packet.removedPerspectives) {
                buf.writeUtf(p);
            }
        } else {
            buf.writeInt(0);
        }
        
        buf.writeUtf(packet.forcedPerspective != null ? packet.forcedPerspective : "");
    }
    
    public static PlayerCamSyncPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        String perspective = buf.readUtf();
        if (perspective.isEmpty()) perspective = null;
        String targetPerspective = buf.readUtf();
        if (targetPerspective.isEmpty()) targetPerspective = null;
        
        int removedCount = buf.readInt();
        Set<String> removedPerspectives = new HashSet<>();
        for (int i = 0; i < removedCount; i++) {
            removedPerspectives.add(buf.readUtf());
        }
        
        String forcedPerspective = buf.readUtf();
        if (forcedPerspective.isEmpty()) forcedPerspective = null;
        
        return new PlayerCamSyncPacket(action, perspective, targetPerspective, removedPerspectives, forcedPerspective);
    }
    
    public static void handle(PlayerCamSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                PlayerCamClientHandler.handlePacket(packet);
            });
        });
        ctx.get().setPacketHandled(true);
    }
    
    // Getters
    public Action getAction() { return action; }
    public String getPerspective() { return perspective; }
    public String getTargetPerspective() { return targetPerspective; }
    public Set<String> getRemovedPerspectives() { return removedPerspectives; }
    public String getForcedPerspective() { return forcedPerspective; }
}