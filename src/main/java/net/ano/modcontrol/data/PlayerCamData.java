package net.ano.modcontrol.data;

import net.ano.modcontrol.AnosModControl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PlayerCamData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Perspective types
    public static final String FIRST_PERSON = "firstperson";
    public static final String SECOND_PERSON = "secondperson"; // Third person front in vanilla
    public static final String THIRD_PERSON = "thirdperson";   // Third person back in vanilla
    
    public static final Set<String> ALL_PERSPECTIVES = Set.of(FIRST_PERSON, SECOND_PERSON, THIRD_PERSON);
    
    // Map of player UUID -> forced perspective (null if not forced)
    private static final Map<UUID, String> forcedPerspectives = new HashMap<>();
    
    // Map of player UUID -> Set of removed perspectives
    private static final Map<UUID, Set<String>> removedPerspectives = new HashMap<>();
    
    // Global removed perspectives (affects all players)
    private static final Set<String> globalRemovedPerspectives = new HashSet<>();
    
    // ============ Forced Perspective Methods ============
    
    public static boolean hasForcedPerspective(UUID playerUuid) {
        return forcedPerspectives.containsKey(playerUuid);
    }
    
    public static String getForcedPerspective(UUID playerUuid) {
        return forcedPerspectives.get(playerUuid);
    }
    
    public static void setForcedPerspective(UUID playerUuid, String perspective) {
        if (perspective != null && ALL_PERSPECTIVES.contains(perspective)) {
            forcedPerspectives.put(playerUuid, perspective);
        }
    }
    
    public static void clearForcedPerspective(UUID playerUuid) {
        forcedPerspectives.remove(playerUuid);
    }
    
    public static Map<UUID, String> getAllForcedPerspectives() {
        return new HashMap<>(forcedPerspectives);
    }
    
    // ============ Removed Perspective Methods ============
    
    public static boolean isPerspectiveRemoved(UUID playerUuid, String perspective) {
        // Check global first
        if (globalRemovedPerspectives.contains(perspective)) {
            return true;
        }
        // Check player-specific
        Set<String> playerRemoved = removedPerspectives.get(playerUuid);
        return playerRemoved != null && playerRemoved.contains(perspective);
    }
    
    public static void removePerspective(UUID playerUuid, String perspective) {
        if (perspective != null && ALL_PERSPECTIVES.contains(perspective)) {
            removedPerspectives.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(perspective);
        }
    }
    
    public static void restorePerspective(UUID playerUuid, String perspective) {
        Set<String> playerRemoved = removedPerspectives.get(playerUuid);
        if (playerRemoved != null) {
            playerRemoved.remove(perspective);
            if (playerRemoved.isEmpty()) {
                removedPerspectives.remove(playerUuid);
            }
        }
    }
    
    public static Set<String> getRemovedPerspectives(UUID playerUuid) {
        Set<String> result = new HashSet<>(globalRemovedPerspectives);
        Set<String> playerRemoved = removedPerspectives.get(playerUuid);
        if (playerRemoved != null) {
            result.addAll(playerRemoved);
        }
        return result;
    }
    
    public static Set<String> getAvailablePerspectives(UUID playerUuid) {
        Set<String> available = new HashSet<>(ALL_PERSPECTIVES);
        available.removeAll(getRemovedPerspectives(playerUuid));
        return available;
    }
    
    // ============ Global Removed Methods ============
    
    public static void removeGlobalPerspective(String perspective) {
        if (perspective != null && ALL_PERSPECTIVES.contains(perspective)) {
            globalRemovedPerspectives.add(perspective);
        }
    }
    
    public static void restoreGlobalPerspective(String perspective) {
        globalRemovedPerspectives.remove(perspective);
    }
    
    public static Set<String> getGlobalRemovedPerspectives() {
        return new HashSet<>(globalRemovedPerspectives);
    }
    
    // ============ Utility Methods ============
    
    public static String getNextAvailablePerspective(UUID playerUuid, String currentPerspective) {
        Set<String> available = getAvailablePerspectives(playerUuid);
        if (available.isEmpty()) return FIRST_PERSON;
        
        // Order: first -> third -> second -> first
        List<String> order = Arrays.asList(FIRST_PERSON, THIRD_PERSON, SECOND_PERSON);
        int currentIndex = order.indexOf(currentPerspective);
        
        for (int i = 1; i <= order.size(); i++) {
            int nextIndex = (currentIndex + i) % order.size();
            String next = order.get(nextIndex);
            if (available.contains(next)) {
                return next;
            }
        }
        
        return available.iterator().next();
    }
    
    public static boolean isValidPerspective(String perspective) {
        return ALL_PERSPECTIVES.contains(perspective);
    }
    
    // ============ Save/Load ============
    
    public static void load(MinecraftServer server) {
        Path configPath = server.getWorldPath(LevelResource.ROOT).resolve("anos_modcontrol");
        Path camFile = configPath.resolve("player_cam.json");
        
        try {
            Files.createDirectories(configPath);
            if (Files.exists(camFile)) {
                try (Reader reader = Files.newBufferedReader(camFile)) {
                    SaveData data = GSON.fromJson(reader, SaveData.class);
                    if (data != null) {
                        forcedPerspectives.clear();
                        removedPerspectives.clear();
                        globalRemovedPerspectives.clear();
                        
                        if (data.forcedPerspectives != null) {
                            data.forcedPerspectives.forEach((k, v) -> {
                                try { forcedPerspectives.put(UUID.fromString(k), v); } catch (Exception ignored) {}
                            });
                        }
                        if (data.removedPerspectives != null) {
                            data.removedPerspectives.forEach((k, v) -> {
                                try { removedPerspectives.put(UUID.fromString(k), new HashSet<>(v)); } catch (Exception ignored) {}
                            });
                        }
                        if (data.globalRemovedPerspectives != null) {
                            globalRemovedPerspectives.addAll(data.globalRemovedPerspectives);
                        }
                    }
                }
            }
        } catch (IOException e) {
            AnosModControl.LOGGER.error("Failed to load player cam data", e);
        }
    }
    
    public static void save(MinecraftServer server) {
        Path configPath = server.getWorldPath(LevelResource.ROOT).resolve("anos_modcontrol");
        Path camFile = configPath.resolve("player_cam.json");
        
        try {
            Files.createDirectories(configPath);
            SaveData data = new SaveData();
            data.forcedPerspectives = new HashMap<>();
            forcedPerspectives.forEach((k, v) -> data.forcedPerspectives.put(k.toString(), v));
            data.removedPerspectives = new HashMap<>();
            removedPerspectives.forEach((k, v) -> data.removedPerspectives.put(k.toString(), new HashSet<>(v)));
            data.globalRemovedPerspectives = new HashSet<>(globalRemovedPerspectives);
            
            try (Writer writer = Files.newBufferedWriter(camFile)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            AnosModControl.LOGGER.error("Failed to save player cam data", e);
        }
    }
    
    private static class SaveData {
        Map<String, String> forcedPerspectives;
        Map<String, Set<String>> removedPerspectives;
        Set<String> globalRemovedPerspectives;
    }
}