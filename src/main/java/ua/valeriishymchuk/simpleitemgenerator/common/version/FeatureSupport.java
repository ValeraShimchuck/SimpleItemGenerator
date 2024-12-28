package ua.valeriishymchuk.simpleitemgenerator.common.version;

public class FeatureSupport {

    public static final boolean CMD_SUPPORT = MinecraftVersion.CURRENT.isAtLeast(1, 14);
    public static final boolean MODERN_CMD_SUPPORT = MinecraftVersion.CURRENT.isAtLeast(1,21,4);
    public static final boolean NAMESPACED_KEYS_SUPPORT = MinecraftVersion.CURRENT.isAtLeast(1, 13);
    public static final boolean TEXT_COMPONENTS_IN_ITEMS_SUPPORT = MinecraftVersion.CURRENT.isAtLeast(1, 13);
    public static final boolean NAMESPACED_ENCHANTMENTS_SUPPORT = MinecraftVersion.CURRENT.isAtLeast(1, 13);
    public static final boolean NAMESPACED_ATTRIBUTES_SUPPORT = MinecraftVersion.CURRENT.isAtLeast(1, 16);
    public static final boolean ITEM_COMPONENTS_SUPPORT = MinecraftVersion.CURRENT.isAtLeast(1, 20, 5);

}
