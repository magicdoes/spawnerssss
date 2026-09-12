package com.magicsmp.magicspawners.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class SpawnerData {
    private final UUID id;
    private final String worldName;
    private final int x, y, z;
    private EntityType entityType;
    private int stackSize;
    private int storedExp;
    private final Map<Material, Integer> storage = new LinkedHashMap<>();

    public SpawnerData(UUID id, Location location, EntityType entityType, int stackSize) {
        this.id = id;
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX(); this.y = location.getBlockY(); this.z = location.getBlockZ();
        this.entityType = entityType;
        this.stackSize = Math.max(1, stackSize);
    }
    public UUID id() { return id; }
    public String worldName() { return worldName; }
    public int x() { return x; } public int y() { return y; } public int z() { return z; }
    public Location location() { World w = Bukkit.getWorld(worldName); return w == null ? null : new Location(w, x, y, z); }
    public EntityType entityType() { return entityType; }
    public void entityType(EntityType type) { this.entityType = type; }
    public int stackSize() { return stackSize; }
    public void stackSize(int size) { this.stackSize = Math.max(1, size); }
    public int storedExp() { return storedExp; }
    public void storedExp(int value) { this.storedExp = Math.max(0, value); }
    public Map<Material,Integer> storage() { return storage; }
    public int totalItems() { return storage.values().stream().mapToInt(Integer::intValue).sum(); }
    public String locationKey() { return worldName + ":" + x + ":" + y + ":" + z; }
}
