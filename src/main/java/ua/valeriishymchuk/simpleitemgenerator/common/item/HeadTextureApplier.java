package ua.valeriishymchuk.simpleitemgenerator.common.item;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.support.HeadDatabaseSupport;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static ua.valeriishymchuk.simpleitemgenerator.common.item.HeadTexture.*;

@UsesMinecraft
@Deprecated(forRemoval = true) // Move it back to HeadTexture and mark every method with @UsesMinecraft
public class HeadTextureApplier {

    public static ItemStack apply(HeadTexture headTexture, ItemStack inputItem, UnaryOperator<String> valuePreProcessor) {
        try {
            String value = valuePreProcessor.apply(headTexture.getValue());
            if (!(inputItem.getItemMeta() instanceof SkullMeta)) {
                Material playerHeadMaterial = Arrays.stream(Material.values())
                        .filter(Material::isItem)
                        .filter(m -> !m.name().endsWith("AIR"))
                        .map(ItemStack::new)
                        .filter(ItemStack::hasItemMeta)
                        .filter(i -> i.getItemMeta() instanceof SkullMeta)
                        .map(ItemStack::getType).findFirst().get();
                throw new InvalidConfigurationException("Item's material should be " + playerHeadMaterial);
            }
            inputItem.setDurability((short) 3);
            switch (headTexture.getType()) {
                case HEADDB:
                    try {
                        String id = HeadDatabaseSupport.getBase64(value);
                        return setInternal(inputItem, id);
                    } catch (Exception e) {
                        if (!HeadDatabaseSupport.isPluginEnabled()) throw new InvalidConfigurationException("HeadDatabase plugin is not enabled");
                        throw new InvalidConfigurationException("Can't find head: " + value);
                    }
                case NICKNAME:
                    ItemMeta meta = inputItem.getItemMeta();
                    SkullMeta skullMeta = (SkullMeta) meta;
                    skullMeta.setOwner(value);
                    inputItem.setItemMeta(skullMeta);
                    return inputItem;
                case BASE64:
                    return setInternal(inputItem, value);
                case URL:
                    return setInternal(inputItem, toBase64Texture(value));
                default:
                    throw new RuntimeException("Not implemented for " + headTexture.getType() + ". Report to the author immediately.\nGH issues: https://github.com/ValeraShimchuck/SimpleItemGenerator/issues");
            }
        } catch (Throwable e) {
            throw InvalidConfigurationException.nestedPath(e, "item", "head-texture");
        }
    }

    private static ItemStack setInternal(ItemStack item, String base64) {
        if (USE_COMPONENTS) return setComponent(item, base64);
        setNBT(item, base64);
        return item;
    }

    // skull nbt data
    // SkullOwner[compound]:
    // uuid of player at 1.8 is required, for 1.16.5 is required for 1.20.4 it is optional
    //   Id[string]: uuid of player Before 1.16.
    //   Id[int_array]: ... 4 ints of player's uuid. Since 1.16
    //   For 1.20.5+ use ItemStacks from PacketEvents
    //   Properties[compound]:
    //     textures[list:compound]:
    //     - Value[string]: base64 encoded texture data
    //  uuid type changes at 1.16
    private static void setNBT(ItemStack item, String base64) {
        NBT.modify(item, nbt -> {
            ReadWriteNBT skullNbt = nbt.getOrCreateCompound("SkullOwner");
            if (!USE_MODERN_UUID)
                skullNbt.setString("Id", OWNER_UUID.toString());
            else skullNbt.setIntArray("Id", uuidToIntArray(OWNER_UUID));
            skullNbt.getOrCreateCompound("Properties")
                    .getCompoundList("textures")
                    .addCompound().setString("Value", base64);
        });
    }

    private static ItemStack setComponent(ItemStack item, String base64) {
        com.github.retrooper.packetevents.protocol.item.ItemStack packetItem = SpigotConversionUtil.fromBukkitItemStack(item);
        packetItem.setComponent(ComponentTypes.PROFILE, new ItemProfile(
                null,
                null,
                Arrays.asList(
                        new ItemProfile.Property("textures", base64, null)
                )
        ));
        return SpigotConversionUtil.toBukkitItemStack(packetItem);
    }

    private static int[] uuidToIntArray(UUID uuid) {
        long l = uuid.getMostSignificantBits();
        long m = uuid.getLeastSignificantBits();
        return leastMostToIntArray(l, m);
    }

    private static int[] leastMostToIntArray(long uuidMost, long uuidLeast) {
        return new int[]{(int) (uuidMost >> 32), (int) uuidMost, (int) (uuidLeast >> 32), (int) uuidLeast};
    }

    private static String toBase64Texture(String url) {
        return Base64.getEncoder().encodeToString(TEXTURE_JSON.replace("%url%", url).getBytes());
    }

}
