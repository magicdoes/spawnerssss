package com.magicsmp.magicspawners.gui;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.model.SpawnerData;
import com.magicsmp.magicspawners.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Admin spawner panel with dimension filters, pagination, teleport and removal.
 */
public final class SpawnerPanelGui {
    private SpawnerPanelGui() {}

    public enum View { MAIN, DIMENSION, CONFIRM_REMOVE }

    public record Session(View view, World.Environment environment, int page, Map<Integer, UUID> slotToSpawner, UUID pendingRemoval) {}

    public static final Map<UUID, Session> OPEN = new HashMap<>();
    private static final int PAGE_SIZE = 45;

    public static void open(Player p) {
        openMain(p);
    }

    public static void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, ColorUtil.component("ꜱᴘᴀᴡɴᴇʀ ᴘᴀɴᴇʟ"));
        fill(inv, 27);

        int overworld = count(World.Environment.NORMAL);
        int nether = count(World.Environment.NETHER);
        int end = count(World.Environment.THE_END);

        inv.setItem(11, SpawnerGui.button(Material.GRASS_BLOCK, "&#3AFF00ᴏᴠᴇʀᴡᴏʀʟᴅ",
                "&7Total Spawners: &f" + overworld, "", "&aClick to view"));
        inv.setItem(13, SpawnerGui.button(Material.NETHERRACK, "&#FE3333ɴᴇᴛʜᴇʀ",
                "&7Total Spawners: &f" + nether, "", "&aClick to view"));
        inv.setItem(15, SpawnerGui.button(Material.END_STONE, "&#C11FFEᴇɴᴅ",
                "&7Total Spawners: &f" + end, "", "&aClick to view"));

        p.openInventory(inv);
        OPEN.put(p.getUniqueId(), new Session(View.MAIN, null, 0, Map.of(), null));
    }

    public static void openDimension(Player p, World.Environment environment, int requestedPage) {
        List<SpawnerData> spawners = list(environment);
        int maxPage = Math.max(0, (spawners.size() - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, maxPage));

        String title = switch (environment) {
            case NETHER -> "ɴᴇᴛʜᴇʀ ꜱᴘᴀᴡɴᴇʀꜱ";
            case THE_END -> "ᴇɴᴅ ꜱᴘᴀᴡɴᴇʀꜱ";
            default -> "ᴏᴠᴇʀᴡᴏʀʟᴅ ꜱᴘᴀᴡɴᴇʀꜱ";
        };
        title += " &8(" + (page + 1) + "/" + (maxPage + 1) + ")";

        Inventory inv = Bukkit.createInventory(null, 54, ColorUtil.component(title));
        Map<Integer, UUID> slotMap = new HashMap<>();

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, spawners.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            SpawnerData d = spawners.get(i);
            ItemStack item = SpawnerGui.button(Material.SPAWNER,
                    "&e" + MagicSpawners.get().manager().pretty(d.entityType()) + " Spawner",
                    "&7Amount: &f" + d.stackSize(),
                    "&7World: &f" + d.worldName(),
                    "&7Location: &f" + d.x() + ", " + d.y() + ", " + d.z(),
                    "",
                    "&aRight-Click to teleport",
                    "&cLeft-Click to remove");
            inv.setItem(slot, item);
            slotMap.put(slot, d.id());
            slot++;
        }

        // Bottom navigation row. Only show Previous/Next when that page exists.
        for (int i = 45; i < 54; i++) inv.setItem(i, SpawnerGui.button(Material.GRAY_STAINED_GLASS_PANE, " "));

        if (page > 0) {
            inv.setItem(45, SpawnerGui.button(Material.ARROW, "&f← ᴘʀᴇᴠɪᴏᴜꜱ ᴘᴀɢᴇ", "&aClick for previous page"));
        }
        inv.setItem(49, SpawnerGui.button(Material.BARRIER, "&cBack to Spawner Panel"));
        if (page < maxPage) {
            inv.setItem(53, SpawnerGui.button(Material.ARROW, "&fɴᴇxᴛ ᴘᴀɢᴇ →", "&aClick for next page"));
        }

        p.openInventory(inv);
        OPEN.put(p.getUniqueId(), new Session(View.DIMENSION, environment, page, Map.copyOf(slotMap), null));
    }

    public static void openRemovalConfirm(Player p, Session from, SpawnerData d) {
        if (d == null) {
            openDimension(p, from.environment(), from.page());
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, ColorUtil.component("ᴄᴏɴꜰɪʀᴍ ꜱᴘᴀᴡɴᴇʀ ʀᴇᴍᴏᴠᴀʟ"));
        fill(inv, 27);
        inv.setItem(11, SpawnerGui.button(Material.RED_STAINED_GLASS_PANE, "&#FF1D1Dᴄᴀɴᴄᴇʟ", "&fClick to cancel"));
        inv.setItem(13, SpawnerGui.button(Material.SPAWNER,
                "&e" + MagicSpawners.get().manager().pretty(d.entityType()) + " Spawner",
                "&7Amount: &f" + d.stackSize(),
                "&7World: &f" + d.worldName(),
                "&7Location: &f" + d.x() + ", " + d.y() + ", " + d.z(),
                "", "&cThis spawner will be removed!"));
        inv.setItem(15, SpawnerGui.button(Material.LIME_STAINED_GLASS_PANE, "&#3AFF00ᴄᴏɴꜰɪʀᴍ", "&fClick to remove this spawner"));

        p.openInventory(inv);
        OPEN.put(p.getUniqueId(), new Session(View.CONFIRM_REMOVE, from.environment(), from.page(), Map.of(), d.id()));
    }

    public static SpawnerData find(UUID id) {
        if (id == null) return null;
        for (SpawnerData d : MagicSpawners.get().storage().all()) {
            if (d.id().equals(id)) return d;
        }
        return null;
    }

    private static List<SpawnerData> list(World.Environment environment) {
        List<SpawnerData> out = new ArrayList<>();
        for (SpawnerData d : MagicSpawners.get().storage().all()) {
            var loc = d.location();
            if (loc != null && loc.getWorld().getEnvironment() == environment) out.add(d);
        }
        out.sort(Comparator.comparing(SpawnerData::worldName)
                .thenComparingInt(SpawnerData::x)
                .thenComparingInt(SpawnerData::y)
                .thenComparingInt(SpawnerData::z));
        return out;
    }

    private static int count(World.Environment env) {
        int c = 0;
        for (SpawnerData d : MagicSpawners.get().storage().all()) {
            var loc = d.location();
            if (loc != null && loc.getWorld().getEnvironment() == env) c++;
        }
        return c;
    }

    private static void fill(Inventory inv, int size) {
        ItemStack filler = SpawnerGui.button(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) inv.setItem(i, filler);
    }
}
