package ua.valeriishymchuk.itemgenerator.common.reflection;

import io.vavr.control.Option;
import lombok.SneakyThrows;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;
import ua.valeriishymchuk.itemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.itemgenerator.common.version.MinecraftVersion;

import java.lang.reflect.Method;

public class ReflectedRepresentations {

    public static class NamespacedKey {

        public static final Class<?> CLASS;

        @SneakyThrows
        public static ReflectionObject of(String namespace, String key) {
            validate();
            return ReflectionObject.newInstance(CLASS, namespace, key);
        }

        public static ReflectionObject from(Key key) {
            return of(key.namespace(), key.value());
        }

        public static ReflectionObject from(String key) {
            return from(Key.key(key));
        }

        public static Key getKyoriKey(ReflectionObject reflectionObject) {
            validate();
            if (reflectionObject.getClazz() != CLASS)
                throw new IllegalArgumentException("Not a namespaced key " + reflectionObject.getClazz());
            return Key.key(reflectionObject.getObject().toString());
        }

        private static void validate() {
            if (CLASS == null)
                MinecraftVersion.CURRENT.assertAtLeast(1,13);
        }

        static {
            try {
                if (MinecraftVersion.CURRENT.isAtLeast(1, 13)) {
                    CLASS = Class.forName("org.bukkit.NamespacedKey");
                } else CLASS = null;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class ItemMeta {
        public static final Class<org.bukkit.inventory.meta.ItemMeta> CLASS = org.bukkit.inventory.meta.ItemMeta.class;

        @SneakyThrows
        public static Option<Integer> getCustomModelData(org.bukkit.inventory.meta.ItemMeta itemMeta) {
            ensureCmdSupport();
            Method getCustomModelData = CLASS.getMethod("getCustomModelData");
            return Option.of((Integer) getCustomModelData.invoke(itemMeta));
        }

        public static Option<Integer> tryGetCustomModelData(org.bukkit.inventory.meta.ItemMeta itemMeta) {
            if (!FeatureSupport.CMD_SUPPORT)
                return Option.none();
            return getCustomModelData(itemMeta);
        }

        @SneakyThrows
        public static void setCustomModelData(org.bukkit.inventory.meta.ItemMeta itemMeta, Integer cmd) {
            ensureCmdSupport();
            Method setCustomModelData = CLASS.getMethod("setCustomModelData", Integer.class);
            setCustomModelData.invoke(itemMeta, cmd);
        }

        private static void ensureCmdSupport() {
            if (!FeatureSupport.CMD_SUPPORT)
                throw new IllegalStateException("Custom model data is supported from >=1.14. Current version " + MinecraftVersion.CURRENT);
        }

    }

    public static class Enchantment {

        public static final Class<org.bukkit.enchantments.Enchantment> CLASS = org.bukkit.enchantments.Enchantment.class;

        public static org.bukkit.enchantments.Enchantment getByKey(Key key) {
            return getByKey(key.asString());
        }

        public static Option<org.bukkit.enchantments.Enchantment> tryGetByKey(Key key) {
            return tryGetByKey(key.asString());
        }

        public static Option<org.bukkit.enchantments.Enchantment> tryGetByKey(String key) {
            if (!FeatureSupport.NAMESPACED_ENCHANTMENTS_SUPPORT)
                return Option.none();
            return Option.of(getByKey(key));
        }

        @SneakyThrows
        public static org.bukkit.enchantments.Enchantment getByKey(String key) {
            ensureNamespacedEnchantmentsSupport();
            return ReflectionObject.ofStatic(org.bukkit.enchantments.Enchantment.class)
                    .invokePublic("getByKey", ReflectedRepresentations.NamespacedKey.from(key))
                    .<org.bukkit.enchantments.Enchantment>map(ReflectionObject::cast)
                    .getOrElseThrow(() -> new IllegalArgumentException("Unknown enchantment: " + key));
        }

        public static ReflectionObject getNamespacedKey(org.bukkit.enchantments.Enchantment enchantment) {
            ensureNamespacedEnchantmentsSupport();
            return new ReflectionObject(CLASS, enchantment).invokePublic("getKey").get();
        }

        public static Key getKyoriKey(org.bukkit.enchantments.Enchantment enchantment) {
            ensureNamespacedEnchantmentsSupport();
            return ReflectedRepresentations.NamespacedKey.getKyoriKey(getNamespacedKey(enchantment));
        }


        private static void ensureNamespacedEnchantmentsSupport() {
            if (!FeatureSupport.NAMESPACED_ENCHANTMENTS_SUPPORT)
                throw new IllegalStateException("Namespaced enchantments is supported from >=1.13. Current version " + MinecraftVersion.CURRENT);

        }

    }

}
