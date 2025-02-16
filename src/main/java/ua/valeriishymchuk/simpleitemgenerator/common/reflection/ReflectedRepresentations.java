package ua.valeriishymchuk.simpleitemgenerator.common.reflection;

import io.vavr.CheckedFunction1;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ua.valeriishymchuk.simpleitemgenerator.common.boundingbox.BoundingBox;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.version.FeatureSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static ua.valeriishymchuk.simpleitemgenerator.common.reflection.MinecraftReflection.getCraftBukkit;

public class ReflectedRepresentations {

    public static class EntityEquipment {
        public static final Class<org.bukkit.inventory.EntityEquipment> CLASS = org.bukkit.inventory.EntityEquipment.class;

        @SneakyThrows
        public static void setItemInOffhand(org.bukkit.inventory.EntityEquipment equipment, ItemStack item) {
            try {
                Method m = CLASS.getMethod("setItemInOffHand", ItemStack.class);
                m.invoke(equipment, item);
            } catch (NoSuchMethodException ignored) {}
        }

        @SneakyThrows
        public static Option<ItemStack> getItemInOffhand(org.bukkit.inventory.EntityEquipment equipment) {
            try {
                Method m = CLASS.getMethod("getItemInOffHand");
                return Option.of((ItemStack) m.invoke(equipment));
            } catch (NoSuchMethodException e) {
                return Option.none();
            }
        }

    }

    public static class Material {
        public static final Class<org.bukkit.Material> CLASS = org.bukkit.Material.class;

        @SneakyThrows
        public static boolean isItem(org.bukkit.Material material) {
            Method m = Arrays.stream(CLASS.getMethods())
                    .filter(m2 -> m2.getName().equals("isItem")).findFirst().orElse(null);
            if (m == null) {
                boolean hasItemVersion = Try.of(() -> org.bukkit.Material.valueOf(material.name() + "_ITEM")).isSuccess();
                return !hasItemVersion;
                //return !material.isBlock();
            }
            return (boolean) m.invoke(material);
        }
    }

    public static class World {
        public static final Class<org.bukkit.World> CLASS = org.bukkit.World.class;

        @SneakyThrows
        public static int getMinHeight(org.bukkit.World world) {
            try {
                Method getMinHeight = CLASS.getMethod("getMinHeight");
                return (int) getMinHeight.invoke(world);
            } catch (NoSuchMethodException e) {
                return 0;
            }
        }
    }

    public static class PotionMeta {
        public static final Class<org.bukkit.inventory.meta.PotionMeta> CLASS = org.bukkit.inventory.meta.PotionMeta.class;

        public static boolean setColor(org.bukkit.inventory.meta.PotionMeta meta, org.bukkit.Color color) {
            AtomicBoolean result = new AtomicBoolean(false);
            Arrays.stream(CLASS.getMethods())
                    .filter(m -> m.getParameterTypes().length == 1 && m.getParameterTypes()[0].equals(org.bukkit.Color.class))
                    .findFirst().ifPresent(m -> {
                        try {
                            m.invoke(meta, color); // should work for 1.11+
                            result.set(true);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
            return result.get();
        }
    }

    public static class PotionEffect {
        public static final Class<org.bukkit.potion.PotionEffect> CLASS = org.bukkit.potion.PotionEffect.class;

        @SneakyThrows
        public static Option<org.bukkit.potion.PotionEffect> create(PotionEffectType type, int duration, int amplifier, boolean ambient, boolean particles, boolean icon) {
            Constructor<?> constructor = Arrays.stream(CLASS.getConstructors())
                    .filter(c -> c.getParameterCount() == 6
                            && c.getParameterTypes()[0].equals(PotionEffectType.class)
                            && c.getParameterTypes()[1].equals(int.class)
                            && c.getParameterTypes()[2].equals(int.class)
                            && c.getParameterTypes()[3].equals(boolean.class)
                            && c.getParameterTypes()[4].equals(boolean.class)
                            && c.getParameterTypes()[5].equals(boolean.class)
                    ).findFirst().orElse(null);
            if (constructor == null) return Option.none();
            return Option.some((org.bukkit.potion.PotionEffect) constructor.newInstance(type, duration, amplifier, ambient, particles, icon));
        }
    }

    public static class Entity {
        public static final Class<org.bukkit.entity.Entity> CLASS = org.bukkit.entity.Entity.class;
        public static final Class<?> CRAFT_ENTITY = getCraftBukkit("entity.CraftEntity");
        //private static final NameMapper nameMapper = new NameMapper(
        //        HashMap.of(
        //                new MinecraftVersion(1,8), "getBoundingBox",
        //        ).toJavaMap()
        //);

        private static Class<?> getEntityClass(@NonNull Class<?> entityDerivative) {
            if (entityDerivative.getName().endsWith("Entity")) return entityDerivative;
            return getEntityClass(entityDerivative.getSuperclass());
        }

        @SneakyThrows
        public static BoundingBox getEntitiesBoundingBox(org.bukkit.entity.Entity entity) {
            Object nmsEntity = CRAFT_ENTITY.getMethod("getHandle").invoke(entity);
            Class<?> nmsEntityClass = getEntityClass(nmsEntity.getClass());
            //Arrays.stream(nmsEntity.getClass().getMethods()).map(m -> m.getName() + " " + m.getReturnType())
            //        .forEach(System.out::println);
            boolean hasBoundingBoxMethod = Arrays.stream(nmsEntityClass.getMethods()).anyMatch(m -> m.getName().equals("getBoundingBox"));
            Object nmsBoundingBox;
            if (hasBoundingBoxMethod) {
                nmsBoundingBox = nmsEntity.getClass().getMethod("getBoundingBox").invoke(nmsEntity);
            }  else  {
                nmsBoundingBox = Arrays.stream(nmsEntityClass.getDeclaredFields())
                        .filter(field -> field.getType().getName().endsWith("BB"))
                        .filter(field -> (field.getModifiers() & Modifier.STATIC) == 0)
                        .peek(f -> f.setAccessible(true))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Can't find AABB field in " + nmsEntityClass.getName())).get(nmsEntity);
            }
            double[] inputs = Arrays.stream(nmsBoundingBox.getClass().getFields())
                    .map(CheckedFunction1.<Field, Double>of(field -> (double) field.get(nmsBoundingBox)).unchecked())
                    .mapToDouble(i -> i).toArray();
            return new BoundingBox(inputs[0], inputs[1], inputs[2], inputs[3], inputs[4], inputs[5]);
        }


    }

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
                SemanticVersion.CURRENT_MINECRAFT.assertAtLeast(1,13);
        }

        static {
            try {
                if (SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 13)) {
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

        @SneakyThrows
        public static void setUnbreakable(org.bukkit.inventory.meta.ItemMeta meta, boolean unbreakable) {
            Method method = Arrays.stream(CLASS.getMethods())
                    .filter(m -> m.getName().equals("setUnbreakable"))
                    .findFirst().orElse(null);
            if (method == null) {
                meta.spigot().setUnbreakable(unbreakable);
            } else method.invoke(meta, unbreakable);
        }

        @SneakyThrows
        public static boolean isUnbreakable(org.bukkit.inventory.meta.ItemMeta meta) {
            Method method = Arrays.stream(CLASS.getMethods())
                    .filter(m -> m.getName().equals("isUnbreakable")).findFirst()
                    .orElse(null);
            if (method == null) {
                return meta.spigot().isUnbreakable();
            } else return (boolean) method.invoke(meta);

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
                throw new IllegalStateException("Custom model data is supported from >=1.14. Current version " + SemanticVersion.CURRENT_MINECRAFT);
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

        public static Option<Key> tryGetKey(org.bukkit.enchantments.Enchantment enchantment) {
            if (!FeatureSupport.NAMESPACED_ENCHANTMENTS_SUPPORT) return Option.none();
            return Option.of(getKyoriKey(enchantment));
        }

        public static Key getKyoriKey(org.bukkit.enchantments.Enchantment enchantment) {
            ensureNamespacedEnchantmentsSupport();
            return ReflectedRepresentations.NamespacedKey.getKyoriKey(getNamespacedKey(enchantment));
        }


        private static void ensureNamespacedEnchantmentsSupport() {
            if (!FeatureSupport.NAMESPACED_ENCHANTMENTS_SUPPORT)
                throw new IllegalStateException("Namespaced enchantments is supported from >=1.13. Current version " + SemanticVersion.CURRENT_MINECRAFT);

        }

    }

}
