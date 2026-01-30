package net.ano.modcontrol.data;

import net.ano.modcontrol.AnosModControl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SupremeAuthorityData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Map of entity UUID -> UUID of who granted them authority
    private static final Map<UUID, UUID> supremeEntities = new HashMap<>();
    
    // Track entities that need health restoration (for /kill protection)
    private static final Map<UUID, Float> pendingHealthRestore = new HashMap<>();
    
    // Track the current command executor UUID (set before command execution)
    private static UUID currentCommandExecutor = null;
    
    /**
     * Set the current command executor (called before command runs)
     */
    public static void setCurrentExecutor(UUID uuid) {
        currentCommandExecutor = uuid;
    }
    
    /**
     * Get the current command executor
     */
    public static UUID getCurrentExecutor() {
        return currentCommandExecutor;
    }
    
    /**
     * Clear the current command executor
     */
    public static void clearCurrentExecutor() {
        currentCommandExecutor = null;
    }
    
    /**
     * Check if damage/kill should be blocked for this entity
     */
    public static boolean shouldProtectEntity(UUID entityUuid) {
        if (!hasSupremeAuthority(entityUuid)) return false;
        
        // Always protect if executor is null (command block/console)
        if (currentCommandExecutor == null) return true;
        
        // Protect if executor is different from the entity (not self-command)
        return !currentCommandExecutor.equals(entityUuid);
    }
    
    /**
     * Mark entity for health restoration
     */
    public static void markForHealthRestore(UUID entityUuid, float maxHealth) {
        pendingHealthRestore.put(entityUuid, maxHealth);
    }
    
    /**
     * Get and clear pending health restore for entity
     */
    public static Float getAndClearHealthRestore(UUID entityUuid) {
        return pendingHealthRestore.remove(entityUuid);
    }
    
    /**
     * Get all pending health restores
     */
    public static Map<UUID, Float> getPendingHealthRestores() {
        return new HashMap<>(pendingHealthRestore);
    }
    
    /**
     * Clear a pending health restore
     */
    public static void clearHealthRestore(UUID entityUuid) {
        pendingHealthRestore.remove(entityUuid);
    }
    
    /**
     * Check if an entity has supreme authority
     */
    public static boolean hasSupremeAuthority(UUID entityUuid) {
        return supremeEntities.containsKey(entityUuid);
    }
    
    /**
     * Check if an entity has supreme authority
     */
    public static boolean hasSupremeAuthority(Entity entity) {
        if (entity == null) return false;
        return hasSupremeAuthority(entity.getUUID());
    }
    
    /**
     * Grant supreme authority to an entity
     */
    public static void grantSupremeAuthority(UUID entityUuid, UUID grantedBy) {
        supremeEntities.put(entityUuid, grantedBy);
    }
    
    /**
     * Check if an entity can revoke their own authority
     */
    public static boolean canRevokeOwnAuthority(UUID entityUuid) {
        UUID grantedBy = supremeEntities.get(entityUuid);
        return grantedBy != null && grantedBy.equals(entityUuid);
    }
    
    /**
     * Revoke supreme authority from an entity
     */
    public static void revokeSupremeAuthority(UUID entityUuid) {
        supremeEntities.remove(entityUuid);
    }
    
    /**
     * Get all entities with supreme authority
     */
    public static Set<UUID> getSupremeEntities() {
        return new HashSet<>(supremeEntities.keySet());
    }
    
    /**
     * Get who granted authority to an entity
     */
    public static UUID getGrantedBy(UUID entityUuid) {
        return supremeEntities.get(entityUuid);
    }
    
    // ============ Save/Load ============
    
    public static void load(MinecraftServer server) {
        Path configPath = server.getWorldPath(LevelResource.ROOT).resolve("anos_modcontrol");
        Path stcFile = configPath.resolve("supreme_authority.json");
        
        try {
            Files.createDirectories(configPath);
            if (Files.exists(stcFile)) {
                try (Reader reader = Files.newBufferedReader(stcFile)) {
                    SaveData data = GSON.fromJson(reader, SaveData.class);
                    if (data != null && data.supremeEntities != null) {
                        supremeEntities.clear();
                        data.supremeEntities.forEach((k, v) -> {
                            try {
                                supremeEntities.put(UUID.fromString(k), UUID.fromString(v));
                            } catch (Exception ignored) {}
                        });
                    }
                }
            }
        } catch (IOException e) {
            AnosModControl.LOGGER.error("Failed to load supreme authority data", e);
        }
    }
    
    public static void save(MinecraftServer server) {
        Path configPath = server.getWorldPath(LevelResource.ROOT).resolve("anos_modcontrol");
        Path stcFile = configPath.resolve("supreme_authority.json");
        
        try {
            Files.createDirectories(configPath);
            SaveData data = new SaveData();
            data.supremeEntities = new HashMap<>();
            supremeEntities.forEach((k, v) -> data.supremeEntities.put(k.toString(), v.toString()));
            
            try (Writer writer = Files.newBufferedWriter(stcFile)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            AnosModControl.LOGGER.error("Failed to save supreme authority data", e);
        }
    }
    
    private static class SaveData {
        Map<String, String> supremeEntities;
    }
}