package me.matl114.logitech.listeners.Listeners;

import java.util.Locale;
import me.matl114.logitech.MyAddon;
import me.matl114.logitech.utils.BukkitUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

public class CraftingListener implements Listener {
    public static String AddonName = MyAddon.getInstance().getName().toLowerCase(Locale.ROOT);

    private NamespacedKey tryManualMatch(ItemStack[] matrix) {
        return BukkitUtils.matchRecipeManually(matrix);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        Recipe rawRecipe = e.getRecipe();

        if (rawRecipe instanceof ShapedRecipe sr) {
            NamespacedKey key = sr.getKey();
            if (AddonName.equals(key.getNamespace())) {
                ItemStack realOutput = BukkitUtils.getOriginalRecipeOutput(key);
                if (realOutput != null) {
                    e.getInventory().setResult(realOutput.clone());
                }
            }
            return;
        }

        if (rawRecipe == null) {
            ItemStack[] matrix = e.getInventory().getMatrix();
            if (matrix == null) return;

            boolean hasItems = false;
            for (ItemStack item : matrix) {
                if (item != null && !item.getType().isAir()) {
                    hasItems = true;
                    break;
                }
            }
            if (!hasItems) return;

            NamespacedKey matchedKey = tryManualMatch(matrix);
            if (matchedKey != null) {
                ItemStack realOutput = BukkitUtils.getOriginalRecipeOutput(matchedKey);
                if (realOutput != null) {
                    e.getInventory().setResult(realOutput.clone());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCraft(CraftItemEvent e) {
        if (e.getRecipe() instanceof ShapedRecipe sr) {
            NamespacedKey key = sr.getKey();
            if (AddonName.equals(key.getNamespace())) {
                ItemStack realOutput = BukkitUtils.getOriginalRecipeOutput(key);
                if (realOutput != null) {
                    e.getInventory().setResult(realOutput.clone());
                }
            }
            return;
        }

        if (e.getRecipe() == null) {
            ItemStack result = e.getInventory().getResult();
            if (result == null || result.getType().isAir()) return;

            ItemStack[] matrix = e.getInventory().getMatrix();
            if (matrix == null) return;

            NamespacedKey matchedKey = tryManualMatch(matrix);
            if (matchedKey != null) {
                ItemStack realOutput = BukkitUtils.getOriginalRecipeOutput(matchedKey);
                if (realOutput != null) {
                    e.setResult(Event.Result.ALLOW);
                    e.getInventory().setResult(realOutput.clone());

                    final CraftingInventory inv = e.getInventory();
                    Bukkit.getScheduler().runTask(MyAddon.getInstance(), () -> {
                        consumeOneMaterial(inv);
                    });
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory() instanceof CraftingInventory craftInv)) return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (e instanceof CraftItemEvent) return;

        ItemStack result = craftInv.getResult();
        if (result == null || result.getType().isAir()) return;

        ItemStack[] matrix = craftInv.getMatrix();
        if (matrix == null) return;

        NamespacedKey matchedKey = tryManualMatch(matrix);
        if (matchedKey == null) return;

        ItemStack realOutput = BukkitUtils.getOriginalRecipeOutput(matchedKey);
        if (realOutput == null) return;

        e.setResult(Event.Result.ALLOW);
        e.setCancelled(false);

        final CraftingInventory inv = craftInv;
        Bukkit.getScheduler().runTask(MyAddon.getInstance(), () -> {
            consumeOneMaterial(inv);
        });
    }

    private void consumeOneMaterial(CraftingInventory inv) {
        try {
            ItemStack[] matrix = inv.getMatrix();
            if (matrix == null) return;

            for (int i = 0; i < matrix.length; i++) {
                ItemStack item = matrix[i];
                if (item != null && !item.getType().isAir()) {
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        matrix[i] = null;
                    }
                }
            }
            inv.setMatrix(matrix);
        } catch (Throwable ignored) {
        }
    }
}
