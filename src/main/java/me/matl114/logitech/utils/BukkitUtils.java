package me.matl114.logitech.utils;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import me.matl114.logitech.core.AddSlimefunItems;
import me.matl114.logitech.manager.Schedules;
import me.matl114.logitech.utils.Algorithms.PairList;
import me.matl114.logitech.utils.UtilClass.RecipeClass.ShapedMachineRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;

public class BukkitUtils {
    public static final RecipeType VANILLA_CRAFTTABLE = new RecipeType(
            AddUtils.getNameKey("vanilla_crafttable"),
            new CustomItemStack(Material.CRAFTING_TABLE, null, "", "&6万物起源 工作台"));
    public static final RecipeType VANILLA_FURNACE = new RecipeType(
            AddUtils.getNameKey("vanilla_furnace"), new CustomItemStack(Material.FURNACE, null, "", "&6原版熔炉"));

    public static void sendRecipeToVanilla(NamespacedKey key, ShapedMachineRecipe recipe) {
        sendShapedRecipeToVanilla(key, recipe.getInput(), recipe.getOutput()[0]);
    }

    public static Optional<String> getOptionalVanillaMyPluginRecipe(NamespacedKey key) {
        try {
            if (AddUtils.isNamespace(key)) {
                String valueId = key.getKey();
                valueId = valueId.substring(0, valueId.length() - 3).toUpperCase(Locale.ROOT);
                return Optional.of(valueId);
            }
            return Optional.empty();
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public static Optional<SlimefunItem> getOptionalVanillaSlimefunRecipe(NamespacedKey key) {
        try {
            if (AddUtils.isNamespace(key)) {
                String valueId = key.getKey();
                valueId = valueId.substring(0, valueId.length() - 3).toUpperCase(Locale.ROOT);
                return Optional.ofNullable(SlimefunItem.getById(valueId));
            }
            return Optional.empty();
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public static void sendRecipeToVanilla(SlimefunItem sfitem, Class<?> craftingType) {
        sendRecipeToVanilla(sfitem.getId(), sfitem.getRecipe(), sfitem.getRecipeOutput(), craftingType);
    }

    public static NamespacedKey getIdKey(String uniqueId) {
        return AddUtils.getNameKey(uniqueId + "_vl");
    }

    public static void sendRecipeToVanilla(String uniqueId, ItemStack[] in, ItemStack out, Class<?> craftingType) {
        if (craftingType == ShapedRecipe.class) {
            sendShapedRecipeToVanilla(AddUtils.getNameKey(uniqueId + "_vl"), in, out);
        } else if (craftingType == SmithingTrimRecipe.class) {
            sendSmithRecipeToVanilla(AddUtils.getNameKey(uniqueId + "_vl"), in, out, true);
        } else if (craftingType == SmithingTransformRecipe.class) {
            sendSmithRecipeToVanilla(AddUtils.getNameKey(uniqueId + "_vl"), in, out, false);
        }
    }

    private static final PairList<SlimefunItem, Class<?>> moreVanillaRecipes = new PairList<>();

    public static void setup() {
        addMoreRecipes();
    }

    public static void addMoreRecipes() {
        sendRecipeToVanilla(AddSlimefunItems.CRAFT_MANUAL, ShapedRecipe.class);
        sendRecipeToVanilla(AddSlimefunItems.ENHANCED_CRAFT_MANUAL, ShapedRecipe.class);
        sendRecipeToVanilla(AddSlimefunItems.MAGIC_WORKBENCH_MANUAL, ShapedRecipe.class);
        moreVanillaRecipes.forEach(pair -> {
            sendRecipeToVanilla(pair.getFirstValue(), pair.getSecondValue());
        });
    }

    private static final HashMap<NamespacedKey, ItemStack[]> vanillaRecipeOriginalInputs = new HashMap<>();
    private static final HashMap<NamespacedKey, ItemStack> vanillaRecipeOriginalOutputs = new HashMap<>();
    private static final List<NamespacedKey> registeredRecipeKeys = new ArrayList<>();

    public static ItemStack[] getOriginalRecipeInput(NamespacedKey key) {
        return vanillaRecipeOriginalInputs.get(key);
    }

    public static ItemStack getOriginalRecipeOutput(NamespacedKey key) {
        return vanillaRecipeOriginalOutputs.get(key);
    }

    public static List<NamespacedKey> getRegisteredRecipeKeys() {
        return registeredRecipeKeys;
    }

    public static NamespacedKey matchRecipeManually(ItemStack[] matrix) {
        for (NamespacedKey key : registeredRecipeKeys) {
            ItemStack[] pattern = vanillaRecipeOriginalInputs.get(key);
            if (pattern == null) continue;
            if (matchesPattern(matrix, pattern)) {
                return key;
            }
        }
        return null;
    }

    private static boolean matchesPattern(ItemStack[] matrix, ItemStack[] pattern) {
        if (matrix == null) return false;
        int len = Math.min(9, Math.min(matrix.length, pattern.length));
        for (int i = 0; i < len; i++) {
            ItemStack expected = (i < pattern.length) ? pattern[i] : null;
            ItemStack actual = (i < matrix.length) ? matrix[i] : null;

            boolean expectedEmpty = (expected == null || expected.getType().isAir());
            boolean actualEmpty = (actual == null || actual.getType().isAir());

            if (expectedEmpty && actualEmpty) {
                continue;
            }
            if (expectedEmpty != actualEmpty) {
                return false;
            }
            if (expected.getType() != actual.getType()) {
                return false;
            }
            SlimefunItem expectedSf = SlimefunItem.getByItem(expected);
            if (expectedSf != null) {
                SlimefunItem actualSf = SlimefunItem.getByItem(actual);
                if (actualSf == null || !expectedSf.getId().equals(actualSf.getId())) {
                    return false;
                }
            }
        }
        for (int i = len; i < matrix.length && i < 9; i++) {
            ItemStack actual = matrix[i];
            if (actual != null && !actual.getType().isAir()) {
                return false;
            }
        }
        for (int i = len; i < pattern.length && i < 9; i++) {
            ItemStack expected = pattern[i];
            if (expected != null && !expected.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    public static void sendShapedRecipeToVanilla(NamespacedKey key, ItemStack[] input, ItemStack output) {
        if (input.length > 9) {
            return;
        }
        vanillaRecipeOriginalInputs.put(key, input.clone());
        vanillaRecipeOriginalOutputs.put(key, output.clone());
        if (!registeredRecipeKeys.contains(key)) {
            registeredRecipeKeys.add(key);
        }

        try {
            ItemStack cleanOutput = new ItemStack(output.getType(), output.getAmount());

                HashMap<Material, Character> materialCharMap = new HashMap<>();
                char nextChar = 'A';
                char[] slotChars = new char[9];

                for (int i = 0; i < 9; i++) {
                    if (i < input.length && input[i] != null && !input[i].getType().isAir()) {
                        Material mat = input[i].getType();
                        if (!materialCharMap.containsKey(mat)) {
                            materialCharMap.put(mat, nextChar);
                            nextChar++;
                        }
                        slotChars[i] = materialCharMap.get(mat);
                    } else {
                        slotChars[i] = ' ';
                    }
                }

                String[] pattern = new String[3];
                for (int i = 0; i < 3; i++) {
                    pattern[i] = new String(new char[]{slotChars[3 * i], slotChars[3 * i + 1], slotChars[3 * i + 2]});
                }

                Bukkit.removeRecipe(key);

                ShapedRecipe vanillaRecipe = new ShapedRecipe(key, cleanOutput);
                vanillaRecipe.shape(pattern);

                for (var entry : materialCharMap.entrySet()) {
                    vanillaRecipe.setIngredient(entry.getValue(), new RecipeChoice.MaterialChoice(entry.getKey()));
                }

                Bukkit.addRecipe(vanillaRecipe);
        } catch (Throwable ignored) {
        }
    }

    public static void sendSmithRecipeToVanilla(
            NamespacedKey key, ItemStack[] input, ItemStack output, boolean isTrimOrTransform) {
        SmithingRecipe recipe;
        RecipeChoice input1 = new RecipeChoice.ExactChoice(input[0]);
        RecipeChoice input2 = new RecipeChoice.ExactChoice(input[1]);
        RecipeChoice input3 = new RecipeChoice.ExactChoice(input[2]);
        if (isTrimOrTransform) {
            recipe = new SmithingTrimRecipe(key, input1, input2, input3);
        } else {
            recipe = new SmithingTransformRecipe(key, output, input1, input2, input3);
        }
        Bukkit.addRecipe(recipe);
    }

    public static void executeSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Schedules.launchSchedules(runnable, 0, true, 0);
        }
    }

    public static void executeAsync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            Schedules.launchSchedules(runnable, 0, false, 0);
        } else {
            runnable.run();
        }
    }
}
