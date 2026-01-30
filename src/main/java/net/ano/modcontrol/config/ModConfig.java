package net.ano.modcontrol.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

public class ModConfig {
    
    // ============ Client Config ============
    public static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CLIENT_SPEC;
    
    // NoMoreDoubleEsc
    public static final ForgeConfigSpec.BooleanValue NO_MORE_DOUBLE_ESC;
    
    static {
        CLIENT_BUILDER.push("Client Settings");
        
        NO_MORE_DOUBLE_ESC = CLIENT_BUILDER
            .comment("Prevents ESC from being registered twice when closing inventory screens.")
            .define("noMoreDoubleEsc", true);
        
        CLIENT_BUILDER.pop();
        
        CLIENT_SPEC = CLIENT_BUILDER.build();
    }
    
    // ============ Server Config ============
    public static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SERVER_SPEC;
    
    // Command Toggles
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPAWN_CONTROL;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BLOCK_BREAK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DO_ATTACK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DO_ANGER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_STC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PLAYER_CAM;
    
    // Debug Settings
    public static final ForgeConfigSpec.BooleanValue SHOW_DEBUG_MESSAGES;
    public static final ForgeConfigSpec.BooleanValue DEBUG_TO_CHAT;
    public static final ForgeConfigSpec.BooleanValue DEBUG_TO_ACTION_BAR;
    
    // Command Output Settings
    public static final ForgeConfigSpec.BooleanValue SHOW_COMMAND_OUTPUT;
    public static final ForgeConfigSpec.BooleanValue OUTPUT_TO_CHAT;
    public static final ForgeConfigSpec.BooleanValue OUTPUT_TO_ACTION_BAR;
    
    // STC Settings
    public static final ForgeConfigSpec.BooleanValue STC_NOTIFY_PROTECTED;
    public static final ForgeConfigSpec.BooleanValue STC_NOTIFY_ATTACKER;
    
    // PlayerCam Settings
    public static final ForgeConfigSpec.ConfigValue<String> CAM_CANCEL_FROM_FIRST;
    public static final ForgeConfigSpec.ConfigValue<String> CAM_CANCEL_FROM_SECOND;
    public static final ForgeConfigSpec.ConfigValue<String> CAM_CANCEL_FROM_THIRD;
    public static final ForgeConfigSpec.ConfigValue<String> CAM_REMOVE_FALLBACK;
    
    static {
        SERVER_BUILDER.push("Command Toggles");
        
        ENABLE_SPAWN_CONTROL = SERVER_BUILDER
            .comment("Enable /SpawnControl command")
            .define("enableSpawnControl", true);
        
        ENABLE_BLOCK_BREAK = SERVER_BUILDER
            .comment("Enable /BlockBreak command")
            .define("enableBlockBreak", true);
        
        ENABLE_DO_ATTACK = SERVER_BUILDER
            .comment("Enable /DoAttack command")
            .define("enableDoAttack", true);
        
        ENABLE_DO_ANGER = SERVER_BUILDER
            .comment("Enable /DoAnger command")
            .define("enableDoAnger", true);
        
        ENABLE_STC = SERVER_BUILDER
            .comment("Enable /stc, /stcno, /stclist, /stcrevoke commands")
            .define("enableStc", true);
        
        ENABLE_PLAYER_CAM = SERVER_BUILDER
            .comment("Enable /PlayerCam command")
            .define("enablePlayerCam", true);
        
        SERVER_BUILDER.pop();
        
        SERVER_BUILDER.push("Debug Settings");
        
        SHOW_DEBUG_MESSAGES = SERVER_BUILDER
            .comment("Show debug messages when events are blocked/modified")
            .define("showDebugMessages", false);
        
        DEBUG_TO_CHAT = SERVER_BUILDER
            .comment("Send debug messages to chat (if showDebugMessages is true)")
            .define("debugToChat", false);
        
        DEBUG_TO_ACTION_BAR = SERVER_BUILDER
            .comment("Send debug messages to action bar (if showDebugMessages is true)")
            .define("debugToActionBar", true);
        
        SERVER_BUILDER.pop();
        
        SERVER_BUILDER.push("Command Output Settings");
        
        SHOW_COMMAND_OUTPUT = SERVER_BUILDER
            .comment("Show command feedback messages to affected players")
            .define("showCommandOutput", true);
        
        OUTPUT_TO_CHAT = SERVER_BUILDER
            .comment("Send command output to chat")
            .define("outputToChat", true);
        
        OUTPUT_TO_ACTION_BAR = SERVER_BUILDER
            .comment("Send command output to action bar")
            .define("outputToActionBar", false);
        
        SERVER_BUILDER.pop();
        
        SERVER_BUILDER.push("STC Settings");
        
        STC_NOTIFY_PROTECTED = SERVER_BUILDER
            .comment("Notify players when they are protected by STC")
            .define("stcNotifyProtected", true);
        
        STC_NOTIFY_ATTACKER = SERVER_BUILDER
            .comment("Notify command executor when their command is blocked by STC")
            .define("stcNotifyAttacker", true);
        
        SERVER_BUILDER.pop();
        
        SERVER_BUILDER.push("PlayerCam Settings");
        
        CAM_CANCEL_FROM_FIRST = SERVER_BUILDER
            .comment("Perspective to switch to when cancelling from first person",
                     "Valid values: firstperson, secondperson, thirdperson")
            .define("cancelFromFirstPerson", "thirdperson");
        
        CAM_CANCEL_FROM_SECOND = SERVER_BUILDER
            .comment("Perspective to switch to when cancelling from second person")
            .define("cancelFromSecondPerson", "firstperson");
        
        CAM_CANCEL_FROM_THIRD = SERVER_BUILDER
            .comment("Perspective to switch to when cancelling from third person")
            .define("cancelFromThirdPerson", "firstperson");
        
        CAM_REMOVE_FALLBACK = SERVER_BUILDER
            .comment("Perspective to switch to when current perspective is removed")
            .define("removeFallback", "firstperson");
        
        SERVER_BUILDER.pop();
        
        SERVER_SPEC = SERVER_BUILDER.build();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(Type.CLIENT, CLIENT_SPEC, "anos_modcontrol-client.toml");
        ModLoadingContext.get().registerConfig(Type.SERVER, SERVER_SPEC, "anos_modcontrol-server.toml");
    }
    
    // Utility methods
    public static String getCancelTarget(String currentPerspective) {
        if (currentPerspective == null) return "firstperson";
        
        return switch (currentPerspective) {
            case "firstperson" -> CAM_CANCEL_FROM_FIRST.get();
            case "secondperson" -> CAM_CANCEL_FROM_SECOND.get();
            case "thirdperson" -> CAM_CANCEL_FROM_THIRD.get();
            default -> "firstperson";
        };
    }
    
    public static String getRemoveFallback() {
        return CAM_REMOVE_FALLBACK.get();
    }
}