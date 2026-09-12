package com.magicsmp.magicspawners.data;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.model.SpawnerData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Persistent storage with O(1) location lookup and snapshot-based async saves. */
public final class SpawnerStorage {
    private final MagicSpawners plugin;
    private final File file;
    private final Map<String, SpawnerData> byLocation = new HashMap<>();
    private final Map<UUID, SpawnerData> byId = new LinkedHashMap<>();
    private final AtomicBoolean saveInProgress = new AtomicBoolean(false);
    private volatile boolean dirty;

    public SpawnerStorage(MagicSpawners plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        byLocation.clear();
        byId.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = y.getConfigurationSection("spawners");
        if (root == null) return;

        for (String idText : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idText);
                String b = "spawners." + idText + ".";
                String world = y.getString(b + "world");
                var w = org.bukkit.Bukkit.getWorld(world);
                if (w == null) continue;
                EntityType type = EntityType.valueOf(y.getString(b + "type", "PIG"));
                SpawnerData d = new SpawnerData(id,
                        new org.bukkit.Location(w, y.getInt(b + "x"), y.getInt(b + "y"), y.getInt(b + "z")),
                        type, y.getInt(b + "stack", 1));
                d.storedExp(y.getInt(b + "exp", 0));
                ConfigurationSection sc = y.getConfigurationSection(b + "items");
                if (sc != null) {
                    for (String mk : sc.getKeys(false)) {
                        Material m = Material.matchMaterial(mk);
                        if (m != null) d.storage().put(m, sc.getInt(mk));
                    }
                }
                putInternal(d);
            } catch (Exception ex) {
                plugin.getLogger().warning("Skipping invalid saved spawner " + idText);
            }
        }
        dirty = false;
    }

    /** Saves only when something changed. Snapshot creation stays on the server thread. */
    public void saveIfDirty(boolean async) {
        if (!dirty) return;
        save(async);
    }

    public void save(boolean async) {
        if (!saveInProgress.compareAndSet(false, true)) return;
        YamlConfiguration snapshot = createSnapshot();
        dirty = false;

        Runnable writer = () -> {
            try {
                snapshot.save(file);
            } catch (IOException ex) {
                dirty = true;
                plugin.getLogger().severe("Could not save data.yml: " + ex.getMessage());
            } finally {
                saveInProgress.set(false);
            }
        };

        if (async && plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, writer);
        } else {
            writer.run();
        }
    }

    // Compatibility helper for existing callers.
    public void save() {
        save(false);
    }

    private YamlConfiguration createSnapshot() {
        YamlConfiguration y = new YamlConfiguration();
        for (SpawnerData d : new ArrayList<>(byId.values())) {
            String b = "spawners." + d.id() + ".";
            y.set(b + "world", d.worldName());
            y.set(b + "x", d.x());
            y.set(b + "y", d.y());
            y.set(b + "z", d.z());
            y.set(b + "type", d.entityType().name());
            y.set(b + "stack", d.stackSize());
            y.set(b + "exp", d.storedExp());
            for (var e : new LinkedHashMap<>(d.storage()).entrySet()) {
                y.set(b + "items." + e.getKey().name(), e.getValue());
            }
        }
        return y;
    }

    public void put(SpawnerData d) {
        putInternal(d);
        dirty = true;
    }

    private void putInternal(SpawnerData d) {
        byLocation.put(d.locationKey(), d);
        byId.put(d.id(), d);
    }

    public void remove(SpawnerData d) {
        byLocation.remove(d.locationKey());
        byId.remove(d.id());
        dirty = true;
    }

    public SpawnerData at(org.bukkit.Location l) {
        if (l == null || l.getWorld() == null) return null;
        return byLocation.get(l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ());
    }

    public Collection<SpawnerData> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }
}
