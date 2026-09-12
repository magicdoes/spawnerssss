package com.magicsmp.magicspawners.gui;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.model.SpawnerData;
import com.magicsmp.magicspawners.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Digital spawner storage GUI.
 *
 * Slots 0-44 are real withdrawable loot. The bottom row is protected and
 * contains navigation / Sell All / info / Collect Loot controls.
 */
public final class SpawnerGui {
    public record Session(SpawnerData data, int page) {}
    public record LootSlot(Material material, int amount) {}

    public static final Map<UUID, Session> OPEN = new HashMap<>();
    public static final int PAGE_SIZE = 45;

    private SpawnerGui() {}

    public static void open(Player p, SpawnerData d) {
        open(p, d, 0);
    }

    public static void open(Player p, SpawnerData d, int requestedPage) {
        int maxPage = maxPage(d);
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        Inventory inv = build(d, page);
        p.openInventory(inv);
        OPEN.put(p.getUniqueId(), new Session(d, page));
    }

    public static void refresh(Player p) {
        Session session = OPEN.get(p.getUniqueId());
        if (session != null) open(p, session.data(), session.page());
    }

    /** Refresh the current top inventory without reopening it, preserving cursor items. */
    public static void refreshInPlace(Player p) {
        Session session = OPEN.get(p.getUniqueId());
        if (session == null) return;
        int page = Math.max(0, Math.min(session.page(), maxPage(session.data())));
        Inventory top = p.getOpenInventory().getTopInventory();
        if (top.getSize() != 54) return;
        render(top, session.data(), page);
        OPEN.put(p.getUniqueId(), new Session(session.data(), page));
        p.updateInventory();
    }

    public static void previousPage(Player p) {
        Session session = OPEN.get(p.getUniqueId());
        if (session != null && session.page() > 0) {
            open(p, session.data(), session.page() - 1);
        }
    }

    public static void nextPage(Player p) {
        Session session = OPEN.get(p.getUniqueId());
        if (session == null) return;
        int maxPage = maxPage(session.data());
        if (session.page() < maxPage) {
            open(p, session.data(), session.page() + 1);
        }
    }

    private static Inventory build(SpawnerData d, int page) {
        MagicSpawners pl = MagicSpawners.get();
        String mob = pl.manager().pretty(d.entityType());
        int maxPage = maxPage(d);
        page = Math.max(0, Math.min(page, maxPage));

        String title = d.stackSize() + " " + mob + " ѕᴘᴀᴡɴᴇʀ &8(" + (page + 1) + "/" + (maxPage + 1) + ")";
        Inventory inv = Bukkit.createInventory(null, 54, ColorUtil.component(title));
        render(inv, d, page);
        return inv;
    }

    private static void render(Inventory inv, SpawnerData d, int page) {
        MagicSpawners pl = MagicSpawners.get();
        String mob = pl.manager().pretty(d.entityType());
        int maxPage = maxPage(d);
        page = Math.max(0, Math.min(page, maxPage));

        for (int slot = 0; slot < 54; slot++) inv.setItem(slot, null);

        List<ItemStack> stacks = lootStacks(d);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, stacks.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, stacks.get(i));
        }

        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < 54; slot++) inv.setItem(slot, filler);

        // Navigation only appears when there is actually another page.
        if (page > 0) {
            inv.setItem(45, button(Material.ARROW, "&#1FFD98← ᴘʀᴇᴠɪᴏᴜѕ ᴘᴀɢᴇ",
                    "&fClick to go to the previous page"));
        }
        inv.setItem(48, button(Material.GOLD_INGOT, "&#FF1D1Dѕᴇʟʟ ᴀʟʟ", "&fClick to sell all mob drops!"));

        int totalItems = d.totalItems();
        int capacity = storageCapacity();
        String fullness;
        if (capacity > 0) {
            double percent = Math.min(100.0, totalItems * 100.0 / capacity);
            fullness = "&7Storage: &f" + String.format(Locale.US, "%,d", totalItems)
                    + "&7/&f" + String.format(Locale.US, "%,d", capacity)
                    + " &8(&f" + String.format(Locale.US, "%.1f", percent) + "%&8)";
        } else {
            fullness = String.format(Locale.US, "&7Storage: &f%,d &7items", totalItems);
        }

        inv.setItem(49, button(Material.SPAWNER, "&#1FFD98" + mob + " ѕᴘᴀᴡɴᴇʀ",
                "&#1FFD98" + totalItems + " &fitems stored",
                "&7Stack: &fx" + d.stackSize(),
                "&7Page: &f" + (page + 1) + "/" + (maxPage + 1),
                fullness));
        inv.setItem(50, button(Material.DROPPER, "&#1FFD98ᴄᴏʟʟᴇᴄᴛ ʟᴏᴏᴛ", "&fClick to drop all loot in front of you"));
        if (page < maxPage) {
            inv.setItem(53, button(Material.ARROW, "&#1FFD98ɴᴇxᴛ ᴘᴀɢᴇ →",
                    "&fClick to go to the next page"));
        }
    }

    /** Returns the virtual loot represented by one visible loot slot. */
    public static LootSlot lootAt(SpawnerData d, int page, int slot) {
        if (slot < 0 || slot >= PAGE_SIZE) return null;
        List<ItemStack> stacks = lootStacks(d);
        int index = page * PAGE_SIZE + slot;
        if (index < 0 || index >= stacks.size()) return null;
        ItemStack stack = stacks.get(index);
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) return null;
        return new LootSlot(stack.getType(), stack.getAmount());
    }

    /** Removes up to amount of a material from virtual storage and returns the amount removed. */
    public static int withdraw(SpawnerData d, Material material, int amount) {
        if (material == null || amount <= 0) return 0;
        int stored = Math.max(0, d.storage().getOrDefault(material, 0));
        int removed = Math.min(stored, amount);
        int remaining = stored - removed;
        if (remaining <= 0) d.storage().remove(material);
        else d.storage().put(material, remaining);
        if (removed > 0) MagicSpawners.get().storage().markDirty();
        return removed;
    }

    public static int storageCapacity() {
        MagicSpawners pl = MagicSpawners.get();
        int configured = pl.getConfig().getInt("storage.max-items", -1);
        if (configured > 0) return configured;
        int legacy = pl.getConfig().getInt("generation.loot-limit", -1);
        return legacy > 0 ? legacy : -1;
    }

    public static int maxPage(SpawnerData d) {
        int stackCount = lootStackCount(d);
        return Math.max(0, (stackCount - 1) / PAGE_SIZE);
    }

    private static int lootStackCount(SpawnerData d) {
        int count = 0;
        for (var e : d.storage().entrySet()) {
            int amount = Math.max(0, e.getValue());
            int max = Math.max(1, e.getKey().getMaxStackSize());
            count += (amount + max - 1) / max;
        }
        return count;
    }

    private static List<ItemStack> lootStacks(SpawnerData d) {
        List<ItemStack> stacks = new ArrayList<>();
        for (var e : d.storage().entrySet()) {
            int left = Math.max(0, e.getValue());
            int max = Math.max(1, e.getKey().getMaxStackSize());
            while (left > 0) {
                int amount = Math.min(left, max);
                stacks.add(new ItemStack(e.getKey(), amount));
                left -= amount;
            }
        }
        return stacks;
    }

    public static ItemStack button(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        meta.displayName(ColorUtil.component(name));
        meta.lore(Arrays.stream(lore).map(ColorUtil::component).toList());
        i.setItemMeta(meta);
        return i;
    }
}
