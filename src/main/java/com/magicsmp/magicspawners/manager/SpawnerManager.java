package com.magicsmp.magicspawners.manager;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.model.SpawnerData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SpawnerManager {
    private final MagicSpawners plugin;
    private final NamespacedKey typeKey, legacyAmountKey;

    public SpawnerManager(MagicSpawners plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "entity_type");
        // Kept only so spawner items made by older MagicSpawners versions still work.
        this.legacyAmountKey = new NamespacedKey(plugin, "stack_amount");
    }

    /** Creates one normal, stackable spawner item. */
    public ItemStack createItem(EntityType type) {
        ItemStack item = new ItemStack(Material.SPAWNER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(com.magicsmp.magicspawners.util.ColorUtil.component("&#1FFD98" + pretty(type) + " Spawner"));
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the requested amount as real Minecraft item stacks.
     * Example: 64 = one real stack of 64; 100 = 64 + 36.
     */
    public List<ItemStack> createItems(EntityType type, int amount) {
        List<ItemStack> result = new ArrayList<>();
        int left = Math.max(0, amount);
        while (left > 0) {
            int n = Math.min(64, left);
            ItemStack stack = createItem(type);
            stack.setAmount(n);
            result.add(stack);
            left -= n;
        }
        return result;
    }

    public EntityType itemType(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER || !item.hasItemMeta()) return null;
        String v = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (v == null) return null;
        try { return EntityType.valueOf(v); } catch (Exception e) { return null; }
    }

    /**
     * New items always represent one spawner per physical item. Older v2.x items
     * that stored a whole digital stack in PDC are still accepted for migration.
     */
    public int itemAmount(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1;
        Integer legacy = item.getItemMeta().getPersistentDataContainer().get(legacyAmountKey, PersistentDataType.INTEGER);
        return legacy == null ? 1 : Math.max(1, legacy);
    }

    public SpawnerData register(org.bukkit.Location loc, EntityType type, int amount) {
        SpawnerData d = new SpawnerData(UUID.randomUUID(), loc, type, amount);
        plugin.storage().put(d);
        if (loc.getBlock().getState() instanceof CreatureSpawner cs) {
            cs.setSpawnedType(type);
            cs.update(true, false);
        }
        return d;
    }

    public String pretty(EntityType type) {
        String s = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder b = new StringBuilder();
        for (String part : s.split(" ")) {
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return b.toString().trim();
    }
}
