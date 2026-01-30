package net.ano.modcontrol.handler;

import net.ano.modcontrol.AnosModControl;
import net.ano.modcontrol.data.PlayerCamData;
import net.ano.modcontrol.network.PlayerCamSyncPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class PlayerCamClientHandler {
    
    private static String forcedPerspective = null;
    private static final Set<String> removedPerspectives = new HashSet<>();
    private static String lastPerspective = null;
    
    public static void handlePacket(PlayerCamSyncPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        
        switch (packet.getAction()) {
            case FORCE -> {
                forcedPerspective = packet.getPerspective();
                setPerspective(packet.getPerspective());
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        Component.translatable("anos_modcontrol.cam.force_notify", 
                            getPerspectiveName(packet.getPerspective())), true);
                }
            }
            case CANCEL -> {
                forcedPerspective = null;
                String target = packet.getTargetPerspective();
                if (target != null && !removedPerspectives.contains(target)) {
                    setPerspective(target);
                }
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        Component.translatable("anos_modcontrol.cam.cancel_notify"), true);
                }
            }
            case REMOVE -> {
                removedPerspectives.add(packet.getPerspective());
                String current = getCurrentPerspective();
                if (current.equals(packet.getPerspective())) {
                    String next = getNextAvailablePerspective(current);
                    setPerspective(next);
                }
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        Component.translatable("anos_modcontrol.cam.remove_notify", 
                            getPerspectiveName(packet.getPerspective())), true);
                }
            }
            case RESTORE -> {
                removedPerspectives.remove(packet.getPerspective());
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        Component.translatable("anos_modcontrol.cam.restore_notify", 
                            getPerspectiveName(packet.getPerspective())), true);
                }
            }
            case SYNC -> {
                removedPerspectives.clear();
                if (packet.getRemovedPerspectives() != null) {
                    removedPerspectives.addAll(packet.getRemovedPerspectives());
                }
                forcedPerspective = packet.getForcedPerspective();
                
                if (forcedPerspective != null) {
                    setPerspective(forcedPerspective);
                }
                String current = getCurrentPerspective();
                if (removedPerspectives.contains(current)) {
                    setPerspective(getNextAvailablePerspective(current));
                }
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyInput(InputEvent.Key event) {
        if (event.getKey() != GLFW.GLFW_KEY_F5) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        
        if (forcedPerspective != null) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.translatable("anos_modcontrol.cam.locked"), true);
            }
        }
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        String currentPerspective = getCurrentPerspective();
        
        if (forcedPerspective != null) {
            if (!currentPerspective.equals(forcedPerspective)) {
                setPerspective(forcedPerspective);
            }
            lastPerspective = currentPerspective;
            return;
        }
        
        if (removedPerspectives.contains(currentPerspective)) {
            String next = getNextAvailablePerspective(currentPerspective);
            setPerspective(next);
        }
        
        lastPerspective = currentPerspective;
    }
    
    public static String getCurrentPerspective() {
        Minecraft mc = Minecraft.getInstance();
        CameraType type = mc.options.getCameraType();
        
        return switch (type) {
            case FIRST_PERSON -> PlayerCamData.FIRST_PERSON;
            case THIRD_PERSON_FRONT -> PlayerCamData.SECOND_PERSON;
            case THIRD_PERSON_BACK -> PlayerCamData.THIRD_PERSON;
        };
    }
    
    public static void setPerspective(String perspective) {
        Minecraft mc = Minecraft.getInstance();
        
        CameraType type = switch (perspective) {
            case PlayerCamData.FIRST_PERSON -> CameraType.FIRST_PERSON;
            case PlayerCamData.SECOND_PERSON -> CameraType.THIRD_PERSON_FRONT;
            case PlayerCamData.THIRD_PERSON -> CameraType.THIRD_PERSON_BACK;
            default -> CameraType.FIRST_PERSON;
        };
        
        mc.options.setCameraType(type);
    }
    
    public static String getNextAvailablePerspective(String current) {
        String[] order = {PlayerCamData.FIRST_PERSON, PlayerCamData.THIRD_PERSON, PlayerCamData.SECOND_PERSON};
        
        int currentIndex = -1;
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(current)) {
                currentIndex = i;
                break;
            }
        }
        
        for (int i = 1; i <= order.length; i++) {
            int nextIndex = (currentIndex + i) % order.length;
            String next = order[nextIndex];
            if (!removedPerspectives.contains(next)) {
                return next;
            }
        }
        
        return PlayerCamData.FIRST_PERSON;
    }
    
    public static Component getPerspectiveName(String perspective) {
        return switch (perspective) {
            case PlayerCamData.FIRST_PERSON -> Component.translatable("anos_modcontrol.cam.perspective.first");
            case PlayerCamData.SECOND_PERSON -> Component.translatable("anos_modcontrol.cam.perspective.second");
            case PlayerCamData.THIRD_PERSON -> Component.translatable("anos_modcontrol.cam.perspective.third");
            default -> Component.literal(perspective);
        };
    }
    
    public static String getForcedPerspective() {
        return forcedPerspective;
    }
    
    public static Set<String> getRemovedPerspectives() {
        return new HashSet<>(removedPerspectives);
    }
    
    public static void reset() {
        forcedPerspective = null;
        removedPerspectives.clear();
    }
}