package com.magicsmp.magicspawners.task;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.model.DropDefinition;
import com.magicsmp.magicspawners.model.SpawnerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Processes a bounded number of virtual spawners per run instead of scanning
 * the whole server every interval. This keeps tick time stable as the server grows.
 */
public final class GenerationTask implements Runnable {
    private final MagicSpawners plugin;
    private int cursor;

    public GenerationTask(MagicSpawners plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        List<SpawnerData> spawners = new ArrayList<>(plugin.storage().all());
        if (spawners.isEmpty()) {
            cursor = 0;
            return;
        }

        final boolean requireNearby = plugin.getConfig().getBoolean("generation.require-player-nearby", true);
        final int range = Math.max(1, plugin.getConfig().getInt("generation.activation-range", 24));
        final long rangeSquared = (long) range * range;
        int configuredCapacity = plugin.getConfig().getInt("storage.max-items", -1);
        final int lootLimit = configuredCapacity > 0
                ? configuredCapacity
                : plugin.getConfig().getInt("generation.loot-limit", -1);
        final int budget = Math.max(1, plugin.getConfig().getInt("performance.max-spawners-per-generation-run", 100));
        final boolean skipUnloaded = plugin.getConfig().getBoolean("performance.skip-unloaded-chunks", true);

        int count = Math.min(budget, spawners.size());
        for (int n = 0; n < count; n++) {
            if (cursor >= spawners.size()) cursor = 0;
            SpawnerData data = spawners.get(cursor++);
            process(data, requireNearby, rangeSquared, lootLimit, skipUnloaded);
        }
    }

    private void process(SpawnerData data, boolean requireNearby, long rangeSquared, int lootLimit, boolean skipUnloaded) {
        World world = plugin.getServer().getWorld(data.worldName());
        if (world == null) return;

        int chunkX = data.x() >> 4;
        int chunkZ = data.z() >> 4;
        if (skipUnloaded && !world.isChunkLoaded(chunkX, chunkZ)) return;

        // Only construct a Location after the cheap world/chunk checks have passed.
        Location loc = new Location(world, data.x(), data.y(), data.z());
        if (loc.getBlock().getType() != Material.SPAWNER) return;

        if (requireNearby && !hasNearbyPlayer(world, data, rangeSquared)) return;

        List<DropDefinition> drops = plugin.entities().drops(data.entityType());
        if (drops.isEmpty()) return;

        double totalWeight = 0.0;
        for (DropDefinition drop : drops) totalWeight += Math.max(0.0, drop.weight());
        if (totalWeight <= 0.0) return;

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        DropDefinition chosen = drops.get(0);
        for (DropDefinition drop : drops) {
            roll -= Math.max(0.0, drop.weight());
            if (roll <= 0.0) {
                chosen = drop;
                break;
            }
        }

        int add = Math.max(1, data.stackSize());
        if (lootLimit >= 0) {
            add = Math.min(add, Math.max(0, lootLimit - data.totalItems()));
        }
        if (add <= 0) return;

        data.storage().merge(chosen.material(), add, Integer::sum);
        plugin.storage().markDirty();
    }

    private boolean hasNearbyPlayer(World world, SpawnerData data, long rangeSquared) {
        final double sx = data.x() + 0.5;
        final double sy = data.y() + 0.5;
        final double sz = data.z() + 0.5;
        for (Player player : world.getPlayers()) {
            if (!player.isOnline() || player.isDead()) continue;
            Location p = player.getLocation();
            double dx = p.getX() - sx;
            double dy = p.getY() - sy;
            double dz = p.getZ() - sz;
            if ((dx * dx + dy * dy + dz * dz) <= rangeSquared) return true;
        }
        return false;
    }
}
