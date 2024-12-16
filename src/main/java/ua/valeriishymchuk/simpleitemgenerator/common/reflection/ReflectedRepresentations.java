package ua.valeriishymchuk.simpleitemgenerator.common.reflection;

import io.vavr.CheckedFunction1;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.MinecraftVersion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ua.valeriishymchuk.simpleitemgenerator.common.reflection.MinecraftReflection.getCraftBukkit;

public class ReflectedRepresentations {

    public static class ConsoleCommandSender {
        public static final Class<org.bukkit.command.ConsoleCommandSender> CLASS = org.bukkit.command.ConsoleCommandSender.class;

        public static void sendComponentMessage(
                org.bukkit.command.ConsoleCommandSender consoleCommandSender,
                Component component
        ) {
            boolean isSent = Option.ofOptional(Arrays.stream(CLASS.getMethods()).filter(m -> m.getName().equals("spigot"))
                    .findFirst())
                    .flatMap(CheckedFunction1.lift(method -> {
                        Object spigot = method.invoke(consoleCommandSender);
                        Class<?> spigotClass = spigot.getClass();
                        Method sendMessage = spigotClass.getMethod("sendMessage", BaseComponent[].class);
                        sendMessage.invoke(spigot, (Object) KyoriHelper.convert(component));
                        return true;
                    })).getOrElse(false);
            if (!isSent) consoleCommandSender.sendMessage(KyoriHelper.toLegacy(component));
        }

    }

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
                throw new IllegalArgumentException("Not a namespaced key " + reflectionObject.getClazz() + " " + reflectionObject.getObject());
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
        public static final Class<?> CRAFT_META = getCraftBukkit("inventory.CraftMetaItem");
        public static final Field DISPLAY_NAME_FIELD;
        public static final Field LORE_FIELD;

        static {
            try {
                DISPLAY_NAME_FIELD = CRAFT_META.getDeclaredField("displayName");
                LORE_FIELD = CRAFT_META.getDeclaredField("lore");
                DISPLAY_NAME_FIELD.setAccessible(true);
                LORE_FIELD.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        public static boolean isDisplayNameString() {
            return DISPLAY_NAME_FIELD.getType() == String.class;
        }

        @SneakyThrows
        public static void setDisplayName(org.bukkit.inventory.meta.ItemMeta meta, Component displayName) {
            // it will probably break on older versions
            if (!FeatureSupport.TEXT_COMPONENTS_IN_ITEMS_SUPPORT) {
                meta.setDisplayName(KyoriHelper.toLegacy(displayName));
                return;
            }
            Field displayNameField = CRAFT_META.getDeclaredField("displayName");
            displayNameField.setAccessible(true);
            if (isDisplayNameString()) DISPLAY_NAME_FIELD.set(meta, KyoriHelper.toJson(displayName));
            else DISPLAY_NAME_FIELD.set(meta, MinecraftReflection.toMinecraftComponent(displayName).getObject());
        }

        @SneakyThrows
        public static Option<Component> getDisplayName(org.bukkit.inventory.meta.ItemMeta meta) {
            Object displayName = DISPLAY_NAME_FIELD.get(meta);
            if (displayName == null)
                return Option.none();
            if (displayName instanceof String) {
                if (!FeatureSupport.TEXT_COMPONENTS_IN_ITEMS_SUPPORT)
                    return Option.some(KyoriHelper.fromLegacy((String) displayName));
                return Option.some(KyoriHelper.fromJson((String) displayName));
            }
            return Option.some(MinecraftReflection.fromMinecraftComponent(ReflectionObject.of(displayName)));
        }

        @SneakyThrows
        public static void setLore(org.bukkit.inventory.meta.ItemMeta meta, Collection<Component> lore) {
            LORE_FIELD.set(meta, lore.stream().map(component -> {
                if (!FeatureSupport.TEXT_COMPONENTS_IN_ITEMS_SUPPORT) return KyoriHelper.toLegacy(component);
                if (isDisplayNameString()) return KyoriHelper.toJson(component);
                else return MinecraftReflection.toMinecraftComponent(component).getObject();
            }).collect(Collectors.toList()));
        }

        @SneakyThrows
        public static List<Component> getLore(org.bukkit.inventory.meta.ItemMeta meta){
            List<Object> rawLore = (List<Object>) LORE_FIELD.get(meta);
            if (rawLore == null || rawLore.isEmpty()) return Collections.emptyList();
            return rawLore.stream().map(obj -> {
                if (!FeatureSupport.TEXT_COMPONENTS_IN_ITEMS_SUPPORT) return KyoriHelper.fromLegacy((String) obj);
                if (isDisplayNameString()) return KyoriHelper.fromJson((String) obj);
                else return MinecraftReflection.fromMinecraftComponent(ReflectionObject.of(obj));
            }).collect(Collectors.toList());
        }

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
            return new ReflectionObject(CLASS, enchantment).invokePublic("getKey")
                    .get();
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
