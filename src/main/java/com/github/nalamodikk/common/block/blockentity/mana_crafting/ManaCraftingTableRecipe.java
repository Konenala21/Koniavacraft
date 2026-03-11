package com.github.nalamodikk.common.block.blockentity.mana_crafting;

import com.github.nalamodikk.register.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class ManaCraftingTableRecipe implements Recipe<ManaCraftingTableRecipe.ManaCraftingInput> {
    private ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int manaCost;
    private final boolean isShaped;
    // 保證這兩個欄位都是可變結構
    private final List<String> pattern = new ArrayList<>();
    private final Map<String, Ingredient> key = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(ManaCraftingTableRecipe.class);
    private final Ingredient[][] patternMatrix;

    public ManaCraftingTableRecipe(ResourceLocation id,NonNullList<Ingredient> ingredients,ItemStack result,int manaCost,boolean isShaped,List<String> patternFromConstructor, Map<String, Ingredient> keyFromConstructor) {
        this.id = id;
        this.ingredients = ingredients;
        this.result = result;
        this.manaCost = manaCost;
        this.isShaped = isShaped;

        if (isShaped) {
            this.pattern.addAll(patternFromConstructor);
            this.key.putAll(keyFromConstructor);
            this.patternMatrix = buildMatrixFromPattern();
        } else {
            this.patternMatrix = new Ingredient[0][0]; // 不會被用到
        }
    }
    public ManaCraftingTableRecipe(
            ResourceLocation id,
            List<String> pattern,
            Map<String, Ingredient> key,
            ItemStack result,
            int manaCost
    ) {
        this.id = id;
        this.pattern.addAll(pattern);
        this.key.putAll(key);
        this.result = result;
        this.manaCost = manaCost;
        this.isShaped = true;

        this.patternMatrix = buildMatrixFromPattern();
        this.ingredients = flattenMatrix(patternMatrix);
    }

    public int getMaxCraftsPossible(ManaCraftingInput input) {
        int max = Integer.MAX_VALUE;
        NonNullList<Ingredient> ingredients = this.getIngredients();

        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            if (ing.isEmpty()) continue;

            ItemStack in = input.getItem(i);
            if (!ing.test(in)) return 0;

            max = Math.min(max, in.getCount());
        }
        return max;
    }


    public ManaCraftingTableRecipe( ResourceLocation id, NonNullList<Ingredient> ingredients, ItemStack result, int manaCost, boolean isShaped) {
        this.id = id;
        this.ingredients = ingredients;
        this.result = result;
        this.manaCost = manaCost;
        this.isShaped = isShaped;

        if (isShaped) {
            throw new IllegalStateException("❌ Shaped 請使用 pattern + key 建構子！");
        } else {
            this.patternMatrix = new Ingredient[0][0];
        }
    }


    public ManaCraftingTableRecipe withId(ResourceLocation id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean matches(ManaCraftingInput input, Level level) {

        if (input.getContainerSize() != 9) {
            return false;
        }

        if (isShaped) {
            // 有序合成
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient expected = ingredients.get(i);
                ItemStack actual = input.getItem(i);
                if (!expected.test(actual)) {
                    return false;
                }
            }
            return true;
        } else {
            // 無序合成
            List<Ingredient> remainingIngredients = new ArrayList<>(ingredients);
            int usedSlotCount = 0;

            for (int i = 0; i < 9; i++) {
                ItemStack stackInSlot = input.getItem(i);
                if (stackInSlot.isEmpty()) {
                    continue;
                }

                usedSlotCount++;

                boolean matched = false;
                Iterator<Ingredient> iterator = remainingIngredients.iterator();
                while (iterator.hasNext()) {
                    Ingredient ingredient = iterator.next();
                    if (ingredient.test(stackInSlot)) {
                        iterator.remove();
                        matched = true;
                        break;
                    }
                }

                if (!matched) {
                    return false;
                }
            }

            return remainingIngredients.isEmpty() && usedSlotCount == ingredients.size();
        }
    }

    private Ingredient[][] buildMatrixFromPattern() {
        int height = pattern.size();
        int width = pattern.get(0).length();
        Ingredient[][] matrix = new Ingredient[height][width];
        for (int y = 0; y < height; y++) {
            String line = pattern.get(y);
            for (int x = 0; x < width; x++) {
                String symbol = line.substring(x, x + 1);
                Ingredient ing = key.getOrDefault(symbol, Ingredient.EMPTY);
                matrix[y][x] = ing;
            }
        }
        return matrix;
    }

    private NonNullList<Ingredient> flattenMatrix(Ingredient[][] matrix) {
        NonNullList<Ingredient> list = NonNullList.withSize(9, Ingredient.EMPTY);
        for (int y = 0; y < matrix.length; y++) {
            for (int x = 0; x < matrix[y].length; x++) {
                list.set(y * 3 + x, matrix[y][x]);
            }
        }
        return list;
    }


    private boolean matchesPattern(ManaCraftingInput input, int xOffset, int yOffset, boolean mirrored) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int relX = x - xOffset;
                int relY = y - yOffset;
                Ingredient expected = Ingredient.EMPTY;

                if (relX >= 0 && relY >= 0 && relX < patternMatrix[0].length && relY < patternMatrix.length) {
                    int patternX = mirrored ? patternMatrix[0].length - relX - 1 : relX;
                    expected = patternMatrix[relY][patternX];
                }

                ItemStack actual = input.getItem(x + y * 3);
                if (!expected.test(actual)) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public ItemStack assemble(ManaCraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return isShaped ? (width == 3 && height == 3) : (width * height >= ingredients.size());
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MANA_CRAFTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MANA_CRAFTING_TYPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public int getManaCost() {
        return manaCost;
    }

    public boolean isShaped() {
        return isShaped;
    }
    @Override
    public boolean isSpecial() {
        return true;
    }


    public ResourceLocation getId() {
        if (id == null) {
            LOGGER.error("getId() was called before the recipe ID was assigned. result={}, ingredients={}", result, ingredients, new RuntimeException("trace"));
            throw new IllegalStateException("Recipe ID is not set");
        }
        return id;
    }


    public static ManaCraftingTableRecipe createShapeless(List<Ingredient> ingredients, ItemStack result, int manaCost) {
        NonNullList<Ingredient> list = NonNullList.copyOf(ingredients);

        return new ManaCraftingTableRecipe(null, list, result.copy(), manaCost, false
        );

    }

    public static ManaCraftingTableRecipe createShaped(List<String> pattern, Map<Character, Ingredient> key, ItemStack result, int manaCost) {
        Map<String, Ingredient> keyMap = new HashMap<>();
        for (var entry : key.entrySet()) {
            keyMap.put(String.valueOf(entry.getKey()), entry.getValue()); // 將 char 轉成 string
        }

        return new ManaCraftingTableRecipe(null, pattern, keyMap, result.copy(), manaCost);
    }



    public static ManaCraftingTableRecipe fromCodec(
            boolean shaped, int manaCost,
            @Nullable List<String> pattern,
            @Nullable Map<String, Ingredient> key,
            List<Ingredient> ingredients,
            ItemStack result
    ) {
        pattern = pattern != null ? pattern : List.of();
        key = key != null ? key : Map.of();

        NonNullList<Ingredient> resolvedIngredients;
        if (shaped) {
            resolvedIngredients = NonNullList.withSize(9, Ingredient.EMPTY);
            for (int row = 0; row < pattern.size(); row++) {
                String line = pattern.get(row);
                for (int col = 0; col < line.length(); col++) {
                    String c = String.valueOf(line.charAt(col));
                    Ingredient ing = key.getOrDefault(c, Ingredient.EMPTY);
                    resolvedIngredients.set(row * 3 + col, ing);
                }
            }

            // ✅ 修復：有序配方使用 pattern + key 構造函數
            ManaCraftingTableRecipe recipe = new ManaCraftingTableRecipe(null, pattern, key, result.copy(), manaCost);
            return recipe;
        } else {
            resolvedIngredients = NonNullList.of(Ingredient.EMPTY, ingredients.toArray(new Ingredient[0]));

            // ✅ 無序配方使用 ingredients 構造函數
            return new ManaCraftingTableRecipe(null, resolvedIngredients, result.copy(), manaCost, false);
        }
    }

    public ItemStack getResult() {
        return result;
    }
    public static ManaCraftingTableRecipe createWithoutId(NonNullList<Ingredient> ingredients, ItemStack result, int manaCost, boolean isShaped) {
        return new ManaCraftingTableRecipe(null, ingredients, result, manaCost, isShaped);
    }


    public static class Serializer implements RecipeSerializer<ManaCraftingTableRecipe> {


        public static final MapCodec<ManaCraftingTableRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                        ResourceLocation.CODEC.fieldOf("id").forGetter(r -> r.getId()), // ✅ 自訂 id 欄位，從 JSON 中解析
                        Codec.BOOL.fieldOf("shaped").forGetter(ManaCraftingTableRecipe::isShaped),
                        Codec.INT.fieldOf("mana_cost").forGetter(ManaCraftingTableRecipe::getManaCost),
                        Codec.list(Codec.STRING).optionalFieldOf("pattern", List.of()).forGetter(r -> r.pattern),
                        Codec.unboundedMap(Codec.STRING, Ingredient.CODEC).optionalFieldOf("key", Map.of()).forGetter(r -> r.key),
                        Ingredient.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(r -> r.ingredients),
                        ItemStack.CODEC.fieldOf("result").forGetter(ManaCraftingTableRecipe::getResult)
                ).apply(inst, (id, shaped, manaCost, pattern, key, ingredients, result) -> {
                    ManaCraftingTableRecipe recipe = ManaCraftingTableRecipe.fromCodec(shaped, manaCost, pattern, key, ingredients, result);
                    return recipe.withId(id); // ✅ 自動補上 id
                })
        );



        public static final StreamCodec<RegistryFriendlyByteBuf, List<Ingredient>> INGREDIENT_LIST_STREAM_CODEC =
                StreamCodec.of(
                        (buf, list) -> {
                            buf.writeVarInt(list.size());
                            for (Ingredient ingredient : list) {
                                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                            }
                        },
                        buf -> {
                            int size = buf.readVarInt();
                            List<Ingredient> list = new ArrayList<>(size);
                            for (int i = 0; i < size; i++) {
                                list.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                            }
                            return list;
                        }
                );

        public static final StreamCodec<RegistryFriendlyByteBuf, List<String>> STRING_LIST_CODEC =
                StreamCodec.of(
                        (buf, list) -> {
                            buf.writeVarInt(list.size());
                            for (String s : list) buf.writeUtf(s);
                        },
                        buf -> {
                            int size = buf.readVarInt();
                            List<String> result = new ArrayList<>(size);
                            for (int i = 0; i < size; i++) result.add(buf.readUtf());
                            return result;
                        }
                );

        public static final StreamCodec<RegistryFriendlyByteBuf, Map<String, Ingredient>> STRING_INGREDIENT_MAP_CODEC =
                StreamCodec.of(
                        (buf, map) -> {
                            buf.writeVarInt(map.size());
                            for (Map.Entry<String, Ingredient> e : map.entrySet()) {
                                buf.writeUtf(e.getKey());
                                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, e.getValue());
                            }
                        },
                        buf -> {
                            int size = buf.readVarInt();
                            Map<String, Ingredient> result = new HashMap<>();
                            for (int i = 0; i < size; i++) {
                                String key = buf.readUtf();
                                Ingredient ing = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                                result.put(key, ing);
                            }
                            return result;
                        }
                );


        public static final StreamCodec<RegistryFriendlyByteBuf, PatternKey> PATTERN_KEY_CODEC =
                StreamCodec.of(
                        // encode
                        (buf, patternKey) -> {
                            // Write pattern list
                            List<String> pattern = patternKey.pattern();
                            buf.writeVarInt(pattern.size());
                            for (String s : pattern) {
                                buf.writeUtf(s);
                            }

                            // Write key map
                            Map<String, Ingredient> key = patternKey.key();
                            buf.writeVarInt(key.size());
                            for (Map.Entry<String, Ingredient> entry : key.entrySet()) {
                                buf.writeUtf(entry.getKey());
                                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, entry.getValue());
                            }
                        },
                        // decode
                        buf -> {
                            // Read pattern list
                            int patternSize = buf.readVarInt();
                            List<String> pattern = new ArrayList<>(patternSize);
                            for (int i = 0; i < patternSize; i++) {
                                pattern.add(buf.readUtf());
                            }

                            // Read key map
                            int keySize = buf.readVarInt();
                            Map<String, Ingredient> key = new HashMap<>();
                            for (int i = 0; i < keySize; i++) {
                                String k = buf.readUtf();
                                Ingredient v = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                                key.put(k, v);
                            }

                            return new PatternKey(pattern, key);
                        }
                );
        private record PatternKey(List<String> pattern, Map<String, Ingredient> key) {}


        @SuppressWarnings("unchecked")
        public static final StreamCodec<RegistryFriendlyByteBuf, ManaCraftingTableRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC.mapStream(buf -> buf),
                        ManaCraftingTableRecipe::getId,

                        INGREDIENT_LIST_STREAM_CODEC,
                        ManaCraftingTableRecipe::getIngredients,

                        ItemStack.STREAM_CODEC,
                        ManaCraftingTableRecipe::getResult,

                        ByteBufCodecs.VAR_INT,
                        ManaCraftingTableRecipe::getManaCost,

                        ByteBufCodecs.BOOL,
                        ManaCraftingTableRecipe::isShaped,

                        PATTERN_KEY_CODEC,
                        recipe -> new PatternKey(recipe.pattern, recipe.key),

                        // ✅ 這裡直接 new，因為你 constructor 已經做了所有初始化
                        (id, ingredients, result, mana, shaped, patternKey) -> {
                            if (shaped) {
                                // ✅ 有序配方：使用 pattern + key 構造函數
                                return new ManaCraftingTableRecipe(id, patternKey.pattern(), patternKey.key(), result.copy(), mana);
                            } else {
                                // ✅ 無序配方：使用 ingredients 構造函數
                                var resolved = NonNullList.of(Ingredient.EMPTY, ingredients.toArray(new Ingredient[0]));
                                return new ManaCraftingTableRecipe(id, resolved, result.copy(), mana, false);
                            }
                        }
                );


        @Override
        public MapCodec<ManaCraftingTableRecipe> codec() {
            return CODEC.xmap(
                    Function.identity(),
                    recipe -> {
                        // 這裡一定不該再被叫到 getId，否則還是 null
                        return recipe;
                    }
            );
        }


        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ManaCraftingTableRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Type implements RecipeType<ManaCraftingTableRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "mana_crafting";

        @Override
        public String toString() {
            return ID;
        }
    }
    public static class ManaCraftingInput extends SimpleContainer implements RecipeInput {
        public ManaCraftingInput(int size) {
            super(size);
        }

        @Override
        public int size() {
            return super.getContainerSize();
        }

    }


}
