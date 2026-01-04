package com.github.nalamodikk.common.utils.nbt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.*;

public class NBTJsonConverter {



    public static JsonElement toJson(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            JsonObject obj = new JsonObject();
            for (String key : compound.getAllKeys()) {
                obj.add(key, toJson(compound.get(key)));
            }
            return obj;
        } else if (tag instanceof ListTag list) {
            JsonArray array = new JsonArray();
            for (Tag element : list) {
                array.add(toJson(element));
            }
            return array;
        } else if (tag instanceof IntTag intTag) {
            return new JsonPrimitive(intTag.getAsInt());
        } else if (tag instanceof StringTag strTag) {
            return new JsonPrimitive(strTag.getAsString());
        } else if (tag instanceof LongTag longTag) {
            return new JsonPrimitive(longTag.getAsLong());
        } else if (tag instanceof FloatTag floatTag) {
            return new JsonPrimitive(floatTag.getAsFloat());
        } else if (tag instanceof DoubleTag doubleTag) {
            return new JsonPrimitive(doubleTag.getAsDouble());
        } else if (tag instanceof ByteTag byteTag) {
            return new JsonPrimitive(byteTag.getAsByte());
        } else if (tag instanceof ShortTag shortTag) {
            return new JsonPrimitive(shortTag.getAsShort());
        }
        return new JsonPrimitive(tag.getAsString()); // fallback
    }



    public static CompoundTag fromJson(JsonElement json) {
        if (!json.isJsonObject()) throw new IllegalArgumentException("Root JSON element must be an object");
        return parseCompound(json.getAsJsonObject());
    }

    private static CompoundTag parseCompound(JsonObject obj) {
        CompoundTag tag = new CompoundTag();
        for (String key : obj.keySet()) {
            JsonElement el = obj.get(key);
            tag.put(key, parseElement(el));
        }
        return tag;
    }

    private static Tag parseElement(JsonElement el) {
        if (el.isJsonObject()) {
            return parseCompound(el.getAsJsonObject());
        } else if (el.isJsonArray()) {
            ListTag list = new ListTag();
            for (JsonElement item : el.getAsJsonArray()) {
                list.add(parseElement(item));
            }
            return list;
        } else if (el.isJsonPrimitive()) {
            JsonPrimitive prim = el.getAsJsonPrimitive();
            if (prim.isNumber()) {
                double num = prim.getAsDouble();
                if (num % 1 == 0) {
                    long l = (long) num;
                    if (l <= Byte.MAX_VALUE && l >= Byte.MIN_VALUE) return ByteTag.valueOf((byte) l);
                    if (l <= Short.MAX_VALUE && l >= Short.MIN_VALUE) return ShortTag.valueOf((short) l);
                    if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) return IntTag.valueOf((int) l);
                    return LongTag.valueOf(l);
                } else {
                    float f = (float) num;
                    if ((double) f == num) return FloatTag.valueOf(f);
                    return DoubleTag.valueOf(num);
                }
            } else if (prim.isBoolean()) {
                return ByteTag.valueOf((byte) (prim.getAsBoolean() ? 1 : 0));
            } else if (prim.isString()) {
                return StringTag.valueOf(prim.getAsString());
            }
        }
        return StringTag.valueOf(el.toString());
    }

    public static JsonObject convert(CompoundTag tag) {
        JsonElement result = toJson(tag);
        return result.getAsJsonObject();
    }
}
