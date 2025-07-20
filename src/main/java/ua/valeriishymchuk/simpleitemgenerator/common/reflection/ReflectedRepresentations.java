package ua.valeriishymchuk.simpleitemgenerator.common.reflection;

import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemCustomModelData;
import io.vavr.CheckedFunction1;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ua.valeriishymchuk.simpleitemgenerator.common.boundingbox.BoundingBox;
import ua.valeriishymchuk.simpleitemgenerator.common.component.WrappedComponent;
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

    public static class ItemStack {
        public static Class<org.bukkit.inventory.ItemStack> ITEM_STACK_CLASS = org.bukkit.inventory.ItemStack.class;

        public static org.bukkit.inventory.ItemStack createItemStack(org.bukkit.Material material) {
            try {
                Method method = ITEM_STACK_CLASS.getMethod("of", org.bukkit.Material.class);
                return (org.bukkit.inventory.ItemStack) method.invoke(null, material);
            } catch (Throwable e) {
                return new org.bukkit.inventory.ItemStack(material);
            }
        }

    }

    public static class PlayerInteractEvent {

        @Deprecated(forRemoval = true)
        public static EquipmentSlot getClickedItemSlot(org.bukkit.event.player.PlayerInteractEvent event) {
            return event.getHand();
        }

    }

    public static class EntityEquipment {
        public static final Class<org.bukkit.inventory.EntityEquipment> CLASS = org.bukkit.inventory.EntityEquipment.class;

        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static void setItemInOffhand(org.bukkit.inventory.EntityEquipment equipment, org.bukkit.inventory.ItemStack item) {
            equipment.setItem(EquipmentSlot.OFF_HAND, item);
        }

        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static Option<org.bukkit.inventory.ItemStack> getItemInOffhand(org.bukkit.inventory.EntityEquipment equipment) {
            return Option.of(
                    equipment.getItem(EquipmentSlot.OFF_HAND)
            ).filter(itemStack -> itemStack.getType().isAir());
        }

    }

    public static class Material {

        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static boolean isItem(org.bukkit.Material material) {
            return material.isItem();
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

    public static class NamespacedKey {

        public static final Class<?> CLASS;

        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static ReflectionObject of(String namespace, String key) {
            validate();
            return ReflectionObject.newInstance(CLASS, namespace, key);
        }

        @Deprecated(forRemoval = true)
        public static ReflectionObject from(Key key) {
            return of(key.namespace(), key.value());
        }

        @Deprecated(forRemoval = true)
        public static ReflectionObject from(String key) {
            return from(Key.key(key));
        }

        @Deprecated(forRemoval = true)
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

        public static void clearToolComponent(
                org.bukkit.inventory.meta.ItemMeta meta
        ) {
            try {
                Method getTool = org.bukkit.inventory.meta.ItemMeta.class.getMethod("getTool");
                getTool.setAccessible(true);
                Class<?> toolClass = getTool.getReturnType();
                Method setTool = org.bukkit.inventory.meta.ItemMeta.class
                        .getMethod("setTool", toolClass);
                setTool.setAccessible(true);
                setTool.invoke(meta, (Object) null);
            } catch (Exception ignored) {

            }
        }

        public static void copyToolComponent(
                org.bukkit.inventory.meta.ItemMeta src,
                org.bukkit.inventory.meta.ItemMeta dst
        ) {
           try {
               Method getTool = org.bukkit.inventory.meta.ItemMeta.class.getMethod("getTool");
               getTool.setAccessible(true);
               Class<?> toolClass = getTool.getReturnType();
               Object tool = getTool.invoke(src);
               Method setTool = org.bukkit.inventory.meta.ItemMeta.class
                       .getMethod("setTool", toolClass);
               setTool.invoke(dst, tool);
           } catch (Exception ignored) {
               ignored.printStackTrace();
           }
        }

        @SneakyThrows
        public static Option<ItemCustomModelData> getModernCustomModelData(org.bukkit.inventory.meta.ItemMeta itemMeta) {
            if (!FeatureSupport.MODERN_CMD_SUPPORT) return Option.none();
            Class<?> cmdBukkitComponentClass = Class.forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");
            Object bukkitCmdComponent = CLASS.getMethod("getCustomModelDataComponent").invoke(itemMeta);
            Method getFloats = cmdBukkitComponentClass.getMethod("getFloats");
            Method getFlags = cmdBukkitComponentClass.getMethod("getFlags");
            Method getStrings = cmdBukkitComponentClass.getMethod("getStrings");
            Method getColors = cmdBukkitComponentClass.getMethod("getColors");
            return Option.some(new ItemCustomModelData(
                    (List<Float>) getFloats.invoke(bukkitCmdComponent),
                    (List<Boolean>) getFlags.invoke(bukkitCmdComponent),
                    (List<String>) getStrings.invoke(bukkitCmdComponent),
                    ((List<Color>) getColors.invoke(bukkitCmdComponent)).stream()
                            .map(bukkitColor -> new com.github.retrooper.packetevents.protocol.color.Color(
                                    bukkitColor.getRed(),
                                    bukkitColor.getGreen(),
                                    bukkitColor.getBlue()
                            )).toList()
            ));
        }

        @SneakyThrows
        public static void setModernCustomModelData(org.bukkit.inventory.meta.ItemMeta itemMeta, ItemCustomModelData customModelData) {
            if (!FeatureSupport.MODERN_CMD_SUPPORT) return;
            Class<?> cmdBukkitComponentClass = Class.forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");
            Object bukkitCmdComponent = CLASS.getMethod("getCustomModelDataComponent").invoke(itemMeta);
            Method setFloats = cmdBukkitComponentClass.getMethod("setFloats", List.class);
            Method setFlags = cmdBukkitComponentClass.getMethod("setFlags", List.class);
            Method setStrings = cmdBukkitComponentClass.getMethod("setStrings", List.class);
            Method setColors = cmdBukkitComponentClass.getMethod("setColors", List.class);
            setFloats.invoke(bukkitCmdComponent, customModelData.getFloats());
            setFlags.invoke(bukkitCmdComponent, customModelData.getFlags());
            setStrings.invoke(bukkitCmdComponent, customModelData.getStrings());
            setColors.invoke(
                    bukkitCmdComponent,
                    customModelData.getColors().stream()
                            .map(packetEventsColor -> Color.fromRGB(packetEventsColor.asRGB()))
                            .toList()
            );


        }


        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static void setUnbreakable(org.bukkit.inventory.meta.ItemMeta meta, boolean unbreakable) {
            meta.setUnbreakable(unbreakable);
        }

        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static boolean isUnbreakable(org.bukkit.inventory.meta.ItemMeta meta) {
            return meta.isUnbreakable();

        }

        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static Option<WrappedComponent> getDisplayName(org.bukkit.inventory.meta.ItemMeta meta) {
            return WrappedComponent.displayName(meta);
        }

        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static List<WrappedComponent> getLore(org.bukkit.inventory.meta.ItemMeta meta) {
            return WrappedComponent.lore(meta);
        }

        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static Option<Integer> getCustomModelData(org.bukkit.inventory.meta.ItemMeta itemMeta) {
            if (itemMeta.hasCustomModelData()) return Option.some(itemMeta.getCustomModelData());
            return Option.none();
        }

        @Deprecated(forRemoval = true)
        public static Option<Integer> tryGetCustomModelData(org.bukkit.inventory.meta.ItemMeta itemMeta) {
            return getCustomModelData(itemMeta);
        }

        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static void setCustomModelData(org.bukkit.inventory.meta.ItemMeta itemMeta, Integer cmd) {
            itemMeta.setCustomModelData(cmd);
        }

    }

    public static class Enchantment {

        public static final Class<org.bukkit.enchantments.Enchantment> CLASS = org.bukkit.enchantments.Enchantment.class;

        @Deprecated(forRemoval = true)
        public static org.bukkit.enchantments.Enchantment getByKey(Key key) {
            return getByKey(key.asString());
        }

        @Deprecated(forRemoval = true)
        public static Option<org.bukkit.enchantments.Enchantment> tryGetByKey(Key key) {
            return tryGetByKey(key.asString());
        }

        @Deprecated(forRemoval = true)
        public static Option<org.bukkit.enchantments.Enchantment> tryGetByKey(String key) {
            return Option.of(getByKey(key));
        }

        @SneakyThrows
        @Deprecated(forRemoval = true)
        public static org.bukkit.enchantments.Enchantment getByKey(String key) {
            return org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.fromString(key));
        }

        @Deprecated(forRemoval = true)
                public static ReflectionObject getNamespacedKey(org.bukkit.enchantments.Enchantment enchantment) {
            ensureNamespacedEnchantmentsSupport();
            return new ReflectionObject(CLASS, enchantment).invokePublic("getKey")
                    .get();
        }

        @Deprecated(forRemoval = true)
        public static Option<Key> tryGetKey(org.bukkit.enchantments.Enchantment enchantment) {
            return Option.of(getKyoriKey(enchantment));
        }

        @Deprecated(forRemoval = true)
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
