package ua.valeriishymchuk.simpleitemgenerator.common.item;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class NBTCustomItem {

    private static final Key CUSTOM_ITEM_ID_KEY = Key.key("simpleitemgenerator:custom_item_id");
    private static final Key CUSTOM_ITEM_COOLDOWN_KEY = Key.key("simpleitemgenerator:custom_item_cooldown");
    private static final Key CUSTOM_ITEM_COOLDOWN_FREEZETIME_KEY = Key.key("simpleitemgenerator:custom_item_freezetime");
    private static final Key CUSTOM_ITEM_SIGNATURE = Key.key("simpleitemgenerator:custom_item_signature");
    private static final Key CUSTOM_ITEM_LAST_HOLDER = Key.key("simpleitemgenerator:last_holder");
    private static final String PUBLIC_BUKKIT_VALUES = "PublicBukkitValues";

    public static Option<String> getCustomItemId(ItemStack item) {
        if (item == null || item.getType().name().endsWith("AIR")) return Option.none();
        return NBT.get(item, nbt -> {
            return getBukkitValues(nbt)
                    .filter(bukkitValues -> bukkitValues.hasTag(CUSTOM_ITEM_ID_KEY.asString(), NBTType.NBTTagString))
                    .map(bukkitValues -> bukkitValues.getString(CUSTOM_ITEM_ID_KEY.asString()))
                    .flatMap(Option::of);
        });
    }

    public static boolean hasCustomItemId(ItemStack item) {
        return getCustomItemId(item).isDefined();
    }

    public static void setCustomItemId(ItemStack item, String customItemId) {
        NBT.modify(item, nbt -> {
            nbt.getOrCreateCompound(PUBLIC_BUKKIT_VALUES).setString(CUSTOM_ITEM_ID_KEY.asString(), customItemId);
        });
    }

    public static void setSignature(ItemStack item, int signature) {
        NBT.modify(item, nbt -> {
            nbt.getOrCreateCompound(PUBLIC_BUKKIT_VALUES).setInteger(CUSTOM_ITEM_SIGNATURE.asString(), signature);
        });
    }

    public static Option<Integer> getSignature(ItemStack item) {
        if (item == null || item.getType().name().endsWith("AIR")) return Option.none();
        return NBT.get(item, nbt -> {
            return getBukkitValues(nbt)
                    .map(bukkitValues -> bukkitValues.getInteger(CUSTOM_ITEM_SIGNATURE.asString()))
                    .flatMap(Option::of);
        });
    }

    public static Option<String> getLastHolder(ItemStack item) {
        if (item == null || item.getType().name().endsWith("AIR")) return Option.none();
        return NBT.get(item, nbt -> {
            return getBukkitValues(nbt)
                    .map(bukkitValues -> bukkitValues.getString(CUSTOM_ITEM_LAST_HOLDER.asString()))
                    .flatMap(Option::of);
        });
    }

    public static void setLastHolder(ItemStack item, @Nullable String userName) {
        if (item == null || item.getType().name().endsWith("AIR")) return;
        NBT.modify(item, nbt -> {
            if (userName != null)
                nbt.getOrCreateCompound(PUBLIC_BUKKIT_VALUES).setString(CUSTOM_ITEM_LAST_HOLDER.asString(), userName);
            else nbt.getOrCreateCompound(PUBLIC_BUKKIT_VALUES).removeKey(CUSTOM_ITEM_LAST_HOLDER.asString());
        });
    }


    public static Cooldown getCooldown(ItemStack itemStack, int cooldownId) {
        return NBT.get(itemStack, nbt -> {
            Long cooldown = getBukkitValues(nbt)
                    .map(bukkitValues -> bukkitValues.getLong(CUSTOM_ITEM_COOLDOWN_KEY.asString() + cooldownId)).getOrNull();
            Long freezetime = getBukkitValues(nbt)
                    .map(bukkitValues -> bukkitValues.getLong(CUSTOM_ITEM_COOLDOWN_FREEZETIME_KEY.asString() + cooldownId)).getOrNull();
            if (cooldown == null || cooldown < System.currentTimeMillis()) return CooldownType.NONE.toCooldown(0);
            if (freezetime == null || freezetime < System.currentTimeMillis()) return CooldownType.DEFAULT.toCooldown(cooldown);
            return CooldownType.FROZEN.toCooldown(freezetime);
        });
    }

    public static Cooldown queryCooldown(ItemStack itemStack, long cooldownMillis, long freezetimeMillis, int cooldownId) {
        Cooldown cooldown = getCooldown(itemStack, cooldownId);
        if (cooldown.isFrozen()) return cooldown;
        if (cooldown.isAbsent()) {
            updateCooldown(itemStack, cooldownMillis <= 0 ? null : System.currentTimeMillis() + cooldownMillis, cooldownId);
            updateFreezetime(itemStack,  freezetimeMillis <= 0 || cooldownMillis <= 0 ? null : System.currentTimeMillis() + freezetimeMillis, cooldownId);
            return CooldownType.NONE.toCooldown(0);
        }
        updateFreezetime(itemStack, System.currentTimeMillis() + freezetimeMillis, cooldownId);
        return cooldown;
    }

    public static void updateCooldown(ItemStack itemStack, @Nullable Long cooldown, int cooldownId) {
        if (cooldown == null) return;
        if (cooldown <= 0) return;
        NBT.modify(itemStack, nbt -> {
            ReadWriteNBT bukkitValues = nbt.getOrCreateCompound(PUBLIC_BUKKIT_VALUES);
            bukkitValues.setLong(CUSTOM_ITEM_COOLDOWN_KEY.asString() + cooldownId, cooldown);
        });
    }

    public static void updateFreezetime(ItemStack itemStack, @Nullable Long freezetime, int cooldownId) {
        if (freezetime == null) return;
        if (freezetime <= 0) return;
        NBT.modify(itemStack, nbt -> {
            ReadWriteNBT bukkitValues = nbt.getOrCreateCompound(PUBLIC_BUKKIT_VALUES);
            bukkitValues.setLong(CUSTOM_ITEM_COOLDOWN_FREEZETIME_KEY.asString() + cooldownId, freezetime);
        });
    }


    private static Option<ReadableNBT> getBukkitValues(ReadableNBT item) {
        return Option.of(item.getCompound(PUBLIC_BUKKIT_VALUES));
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    public static class Cooldown {
        CooldownType type;
        long remainingMillis;

        public boolean isFrozen() {
            return type == CooldownType.FROZEN;
        }

        public boolean isDefault() {
            return type == CooldownType.DEFAULT;
        }

        public boolean isAbsent() {
            return type == CooldownType.NONE;
        }

    }

    public enum CooldownType {
        NONE,
        DEFAULT,
        FROZEN;
        public Cooldown toCooldown(long epoch) {
            return new Cooldown(this, epoch - System.currentTimeMillis());
        }
    }



}
