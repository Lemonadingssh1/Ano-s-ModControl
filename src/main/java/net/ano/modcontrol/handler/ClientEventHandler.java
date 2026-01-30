package net.ano.modcontrol.handler;

import net.ano.modcontrol.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ClientEventHandler {
    
    private static Screen lastScreen = null;
    private static long lastInventoryCloseTime = 0;
    private static final long COOLDOWN_MS = 200;
    private static boolean blockNextPauseScreen = false;
    
    /**
     * Track current screen each tick
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!ModConfig.NO_MORE_DOUBLE_ESC.get()) return;
        
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        
        // Detect when we transition from inventory to no screen
        if (lastScreen != null && currentScreen == null) {
            if (isInventoryScreen(lastScreen)) {
                lastInventoryCloseTime = System.currentTimeMillis();
                blockNextPauseScreen = true;
            }
        }
        
        // Reset block flag after cooldown
        if (blockNextPauseScreen) {
            long elapsed = System.currentTimeMillis() - lastInventoryCloseTime;
            if (elapsed > COOLDOWN_MS) {
                blockNextPauseScreen = false;
            }
        }
        
        lastScreen = currentScreen;
    }
    
    /**
     * Intercept pause screen opening
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenOpen(ScreenEvent.Opening event) {
        if (!ModConfig.NO_MORE_DOUBLE_ESC.get()) return;
        
        Screen newScreen = event.getNewScreen();
        
        if (newScreen instanceof PauseScreen) {
            long elapsed = System.currentTimeMillis() - lastInventoryCloseTime;
            
            if (blockNextPauseScreen && elapsed < COOLDOWN_MS) {
                event.setCanceled(true);
                blockNextPauseScreen = false;
            }
        }
    }
    
    /**
     * Also track when screens are about to close
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenClosing(ScreenEvent.Closing event) {
        if (!ModConfig.NO_MORE_DOUBLE_ESC.get()) return;
        
        Screen closingScreen = event.getScreen();
        
        if (isInventoryScreen(closingScreen)) {
            lastInventoryCloseTime = System.currentTimeMillis();
            blockNextPauseScreen = true;
        }
    }
    
    private boolean isInventoryScreen(Screen screen) {
        if (screen == null) return false;
        
        // Direct type checks for common inventory screens
        if (screen instanceof AbstractContainerScreen<?>) return true;
        if (screen instanceof InventoryScreen) return true;
        if (screen instanceof CreativeModeInventoryScreen) return true;
        
        // Fallback: check class name for mod compatibility
        String className = screen.getClass().getName().toLowerCase();
        String simpleClassName = screen.getClass().getSimpleName().toLowerCase();
        
        return simpleClassName.contains("inventory") ||
               simpleClassName.contains("container") ||
               simpleClassName.contains("creative") ||
               className.contains("inventory") || 
               className.contains("container") || 
               className.contains("chest") ||
               className.contains("crafting") ||
               className.contains("furnace") ||
               className.contains("enchant") ||
               className.contains("anvil") ||
               className.contains("brewing") ||
               className.contains("beacon") ||
               className.contains("hopper") ||
               className.contains("dispenser") ||
               className.contains("dropper") ||
               className.contains("shulker") ||
               className.contains("barrel") ||
               className.contains("smoker") ||
               className.contains("blast") ||
               className.contains("cartography") ||
               className.contains("grindstone") ||
               className.contains("loom") ||
               className.contains("stonecutter") ||
               className.contains("smithing") ||
               className.contains("merchant") ||
               className.contains("villager") ||
               className.contains("horse") ||
               className.contains("creative");
    }
}