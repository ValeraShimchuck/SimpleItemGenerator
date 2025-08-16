package ua.valeriishymchuk.simpleitemgenerator.common.item;


import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.libs.net.kyori.adventure.key.Key;
import ua.valeriishymchuk.simpleitemgenerator.common.wrapper.PersistentDataTypeWrapper;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.ItemRepository;

import java.util.function.Consumer;

public class NBTCustomItem {

    private static final Key CUSTOM_ITEM_ID_KEY = key("custom_item_id");
    private static final Key CUSTOM_ITEM_COOLDOWN_KEY = key("custom_item_cooldown");
    private static final Key CUSTOM_ITEM_COOLDOWN_FREEZETIME_KEY = key("custom_item_freezetime");
    private static final Key CUSTOM_ITEM_SIGNATURE = key("custom_item_signature");
    private static final Key CUSTOM_ITEM_LAST_HOLDER = key("last_holder");

    private static Key key(String value) {
        return Key.key("simpleitemgenerator:" + value);
    }

    private static Key cooldown(int cooldownId) {
        return Key.key(CUSTOM_ITEM_COOLDOWN_KEY.asString() + cooldownId);
    }

    private static Key freezeTime(int cooldownId) {
        return Key.key(CUSTOM_ITEM_COOLDOWN_FREEZETIME_KEY.asString() + cooldownId);
    }

    public static Option<String> getCustomItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return Option.none();
        return Option.of(
                item.getItemMeta().getPersistentDataContainer()
                        .get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING)
        );
    }

    public static boolean hasCustomItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
    }

    public static void setCustomItemId(ItemStack item, String customItemId) {
        updateNBT(item, persistentDataContainer ->  {
            persistentDataContainer.set(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, customItemId);
        });
    }

    private static void updateNBT(ItemStack item, Consumer<PersistentDataContainer> consumer) {
        ItemMeta itemMeta = item.getItemMeta();
        PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
        consumer.accept(persistentDataContainer);
        item.setItemMeta(itemMeta);
    }

    public static void setSignature(ItemStack item, int signature) {
        updateNBT(item, persistentDataContainer -> {
            persistentDataContainer.set(CUSTOM_ITEM_SIGNATURE, PersistentDataType.INTEGER, signature);
        });
    }

    public static Option<Integer> getSignature(ItemStack item) {
        if (item == null || item.getType().isAir()) return Option.none();
        return Option.of(
                item.getItemMeta().getPersistentDataContainer().get(CUSTOM_ITEM_SIGNATURE, PersistentDataType.INTEGER)
        );
    }

    public static Option<String> getLastHolder(ItemStack item) {
        if (item == null || item.getType().isAir()) return Option.none();
        return Option.of(
                item.getItemMeta().getPersistentDataContainer().get(CUSTOM_ITEM_LAST_HOLDER, PersistentDataType.STRING)
        );
    }

    public static ItemRepository.ItemPatch setLastHolder(@Nullable String userName) {
        return new ItemRepository.ItemPatch.PersistentDataContainer(
                CUSTOM_ITEM_LAST_HOLDER,
                new PersistentDataTypeWrapper.String(),
                userName
        );
    }


    public static Cooldown getCooldown(ItemStack itemStack, int cooldownId) {
        PersistentDataContainer persistentDataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        Long cooldown = persistentDataContainer.get(cooldown(cooldownId), PersistentDataType.LONG);
        Long freezetime = persistentDataContainer.get(freezeTime(cooldownId), PersistentDataType.LONG);
        if (cooldown == null || cooldown < System.currentTimeMillis()) return CooldownType.NONE.toCooldown(0);
        if (freezetime == null || freezetime < System.currentTimeMillis()) return CooldownType.DEFAULT.toCooldown(cooldown);
        return CooldownType.FROZEN.toCooldown(freezetime);
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
        updateNBT(itemStack, persistentDataContainer -> {
            persistentDataContainer.set(cooldown(cooldownId), PersistentDataType.LONG, cooldown);
        });
    }

    public static void updateFreezetime(ItemStack itemStack, @Nullable Long freezetime, int cooldownId) {
        if (freezetime == null) return;
        if (freezetime <= 0) return;
        updateNBT(itemStack, persistentDataContainer -> {
            persistentDataContainer.set(freezeTime(cooldownId), PersistentDataType.LONG, freezetime);
        });
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
