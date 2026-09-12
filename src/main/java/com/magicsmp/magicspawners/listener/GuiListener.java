package com.magicsmp.magicspawners.listener;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.gui.SpawnerGui;
import com.magicsmp.magicspawners.gui.SpawnerPanelGui;
import com.magicsmp.magicspawners.model.SpawnerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GuiListener implements Listener {
    private final MagicSpawners plugin;

    public GuiListener(MagicSpawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        SpawnerGui.Session spawnerSession = SpawnerGui.OPEN.get(p.getUniqueId());
        if (spawnerSession != null) {
            boolean topClick = e.getClickedInventory() == e.getView().getTopInventory();
            int slot = e.getRawSlot();

            // Loot slots are the ONLY items players may take from this menu.
            // They cannot put items back into the virtual storage GUI.
            if (topClick && slot >= 0 && slot < SpawnerGui.PAGE_SIZE) {
                e.setCancelled(true);
                handleLootTake(p, e, spawnerSession, slot);
                return;
            }

            // Every control/filler slot is protected, including shift/double/hotbar tricks.
            if (affectsProtectedTop(e)) e.setCancelled(true);

            if (topClick) {
                e.setCancelled(true);
                SpawnerData d = spawnerSession.data();
                if (slot == 45) {
                    SpawnerGui.previousPage(p);
                } else if (slot == 53) {
                    SpawnerGui.nextPage(p);
                } else if (slot == 50) {
                    collect(p, d);
                    SpawnerGui.refresh(p);
                } else if (slot == 48) {
                    sell(p, d);
                    SpawnerGui.refresh(p);
                }
            }
            return;
        }

        SpawnerPanelGui.Session session = SpawnerPanelGui.OPEN.get(p.getUniqueId());
        if (session == null) return;

        if (affectsProtectedTop(e)) e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        e.setCancelled(true);
        int slot = e.getRawSlot();

        switch (session.view()) {
            case MAIN -> handleMainPanel(p, slot);
            case DIMENSION -> handleDimensionPanel(p, e, session, slot);
            case CONFIRM_REMOVE -> handleRemovalConfirm(p, session, slot);
        }
    }

    /**
     * Returns true for actions originating in the player inventory that can still
     * reach into the plugin's top inventory (shift-click, double-click collect, etc.).
     */
    private boolean affectsProtectedTop(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return false;
        if (e.getClickedInventory() == e.getView().getTopInventory()) return true;

        InventoryAction action = e.getAction();
        return e.isShiftClick()
                || e.getClick().isKeyboardClick()
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.HOTBAR_MOVE_AND_READD
                || action == InventoryAction.HOTBAR_SWAP;
    }

    private void handleLootTake(Player p, InventoryClickEvent e, SpawnerGui.Session session, int slot) {
        SpawnerGui.LootSlot loot = SpawnerGui.lootAt(session.data(), session.page(), slot);
        if (loot == null) return;

        Material material = loot.material();
        int visible = loot.amount();
        if (visible <= 0) return;

        // Shift-click sends as much of the visible stack as possible to the player inventory.
        if (e.isShiftClick()) {
            ItemStack give = new ItemStack(material, visible);
            Map<Integer, ItemStack> leftovers = p.getInventory().addItem(give);
            int left = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
            int moved = visible - left;
            if (moved > 0) SpawnerGui.withdraw(session.data(), material, moved);
            SpawnerGui.refreshInPlace(p);
            return;
        }

        // Number-key / double-click / drop actions stay blocked to prevent dupes.
        if (e.getClick().isKeyboardClick()
                || e.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || e.getAction() == InventoryAction.DROP_ALL_SLOT
                || e.getAction() == InventoryAction.DROP_ONE_SLOT) {
            return;
        }

        ItemStack cursor = e.getCursor();
        boolean emptyCursor = cursor == null || cursor.getType().isAir() || cursor.getAmount() <= 0;

        int wanted;
        if (e.isRightClick()) {
            wanted = emptyCursor ? Math.max(1, (visible + 1) / 2) : 1;
        } else if (e.isLeftClick()) {
            wanted = visible;
        } else {
            return;
        }

        if (emptyCursor) {
            int moved = SpawnerGui.withdraw(session.data(), material, wanted);
            if (moved > 0) p.setItemOnCursor(new ItemStack(material, moved));
        } else if (cursor.getType() == material && cursor.getAmount() < cursor.getMaxStackSize()) {
            int room = cursor.getMaxStackSize() - cursor.getAmount();
            int moved = SpawnerGui.withdraw(session.data(), material, Math.min(wanted, room));
            if (moved > 0) {
                cursor.setAmount(cursor.getAmount() + moved);
                p.setItemOnCursor(cursor);
            }
        }

        SpawnerGui.refreshInPlace(p);
    }

    private void handleMainPanel(Player p, int slot) {
        if (slot == 11) SpawnerPanelGui.openDimension(p, org.bukkit.World.Environment.NORMAL, 0);
        else if (slot == 13) SpawnerPanelGui.openDimension(p, org.bukkit.World.Environment.NETHER, 0);
        else if (slot == 15) SpawnerPanelGui.openDimension(p, org.bukkit.World.Environment.THE_END, 0);
    }

    private void handleDimensionPanel(Player p, InventoryClickEvent e, SpawnerPanelGui.Session session, int slot) {
        if (slot == 45) {
            if (session.page() > 0) SpawnerPanelGui.openDimension(p, session.environment(), session.page() - 1);
            return;
        }
        if (slot == 49) {
            SpawnerPanelGui.openMain(p);
            return;
        }
        if (slot == 53) {
            SpawnerPanelGui.openDimension(p, session.environment(), session.page() + 1);
            return;
        }

        UUID id = session.slotToSpawner().get(slot);
        if (id == null) return;
        SpawnerData d = SpawnerPanelGui.find(id);
        if (d == null) {
            SpawnerPanelGui.openDimension(p, session.environment(), session.page());
            return;
        }

        if (e.isRightClick()) {
            var loc = d.location();
            if (loc == null) {
                p.sendMessage("§cThat spawner's world is not loaded.");
                return;
            }
            p.closeInventory();
            loc.getChunk().load();
            p.teleport(loc.clone().add(0.5, 1.0, 0.5));
            p.sendMessage("§aTeleported to the §f" + plugin.manager().pretty(d.entityType()) + " Spawner§a.");
        } else if (e.isLeftClick()) {
            SpawnerPanelGui.openRemovalConfirm(p, session, d);
        }
    }

    private void handleRemovalConfirm(Player p, SpawnerPanelGui.Session session, int slot) {
        if (slot == 11) {
            SpawnerPanelGui.openDimension(p, session.environment(), session.page());
            return;
        }
        if (slot != 15) return;

        SpawnerData d = SpawnerPanelGui.find(session.pendingRemoval());
        if (d != null) {
            var loc = d.location();
            if (loc != null) {
                loc.getChunk().load();
                if (loc.getBlock().getType() == Material.SPAWNER) loc.getBlock().setType(Material.AIR, false);
            }
            plugin.storage().remove(d);
            plugin.storage().markDirty();
            p.sendMessage("§aSpawner removed from the panel and world.");
        } else {
            p.sendMessage("§cThat spawner no longer exists.");
        }
        SpawnerPanelGui.openDimension(p, session.environment(), session.page());
    }

    @EventHandler
    public void drag(InventoryDragEvent e) {
        UUID id = e.getWhoClicked().getUniqueId();
        if ((SpawnerGui.OPEN.containsKey(id) || SpawnerPanelGui.OPEN.containsKey(id))
                && e.getRawSlots().stream().anyMatch(s -> s < e.getView().getTopInventory().getSize())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            SpawnerGui.OPEN.remove(p.getUniqueId());
            SpawnerPanelGui.OPEN.remove(p.getUniqueId());
        }
    }

    private void collect(Player p, SpawnerData d) {
        if (d.storage().isEmpty()) {
            p.sendMessage(plugin.messages().get("no-items"));
            return;
        }

        // Drop the virtual loot where the player's camera is facing instead of
        // forcing it into their inventory. A short ray trace keeps the drops
        // from being spawned on the far side of a wall.
        Location eye = p.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location dropLocation = eye.clone().add(direction.clone().multiply(2.0));
        RayTraceResult hit = p.rayTraceBlocks(2.25);
        if (hit != null && hit.getHitPosition() != null) {
            dropLocation = hit.getHitPosition().toLocation(p.getWorld())
                    .subtract(direction.clone().multiply(0.30));
        }

        int total = 0;
        Map<Material, Integer> copy = new HashMap<>(d.storage());
        d.storage().clear();
        for (var en : copy.entrySet()) {
            int left = en.getValue();
            total += left;
            int max = Math.max(1, en.getKey().getMaxStackSize());
            while (left > 0) {
                int n = Math.min(left, max);
                ItemStack stack = new ItemStack(en.getKey(), n);
                var item = p.getWorld().dropItem(dropLocation, stack);
                item.setVelocity(direction.clone().multiply(0.18).add(new Vector(0, 0.04, 0)));
                item.setPickupDelay(10);
                left -= n;
            }
        }
        plugin.storage().markDirty();
        p.sendMessage(plugin.messages().get("collected-items", Map.of("count", String.valueOf(total))));
    }

    private void sell(Player p, SpawnerData d) {
        if (d.storage().isEmpty()) {
            p.sendMessage(plugin.messages().get("no-items"));
            return;
        }
        if (!plugin.economy().available()) {
            p.sendMessage("§cVault economy is not available.");
            return;
        }
        double total = 0;
        for (var en : d.storage().entrySet()) {
            total += plugin.entities().price(d.entityType(), en.getKey()) * en.getValue();
        }
        if (total <= 0) {
            p.sendMessage("§cThese drops do not have sell prices configured.");
            return;
        }
        d.storage().clear();
        plugin.storage().markDirty();
        plugin.economy().deposit(p, total);
        p.sendMessage(plugin.messages().get("sold-items", Map.of("price", String.format("%.2f", total))));
    }
}
