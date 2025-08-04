package ua.valeriishymchuk.simpleitemgenerator.common.item;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;
import ua.valeriishymchuk.simpleitemgenerator.common.support.HeadDatabaseSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class HeadTexture {

    private static final UUID OWNER_UUID = UUID.fromString("687b38a7-5505-4253-9e15-733e387fc2f2");
    private static final String TEXTURE_JSON = "{\"textures\":{\"SKIN\":{\"url\":\"%url%\"}}}";
    private static final Pattern MOJANG_TEXTURES_URL_PATTERN = Pattern.compile("^https?://\\w+\\.minecraft\\.net/\\S+$");
    private static final Pattern PATTERN = Pattern.compile("^ *\\[(?<type>headdb|url|base64|nickname)] *(?<value>\\S*) *$");
    private static final boolean USE_MODERN_UUID = SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 16);
    private static final boolean USE_COMPONENTS = SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 20, 5);

    Type type;
    String value;
    // sttings options: headdb, url, base64, nickname. If nickname is used then allow %player% placeholder usage in it

    public static HeadTexture fromString(String rawValue) throws InvalidConfigurationException {
        Matcher matcher = PATTERN.matcher(rawValue);
        try {
            if (!matcher.matches()) throw new InvalidConfigurationException("Invalid head texture: " + rawValue + ". Expected: [headdb|url|base64|nickname] <value>.");
            Type type = Type.valueOf(matcher.group("type").toUpperCase());

            String value = matcher.group("value");
            if (type == Type.URL) {
                if (!MOJANG_TEXTURES_URL_PATTERN.matcher(value).matches())
                    throw InvalidConfigurationException.format(
                            "Invalid url %s. Only *.minecraft.net domains are supported. Examples:\n"
                                    + "http://textures.minecraft.net/texture/84da09279307027a4a57cb49784ba634b155d51531fba9ed334461e5de140766\n"
                                    + "http://textures.minecraft.net/texture/da91846245a7342cc5aea8f548525ae5260b028f85ed183245731e40a514e4c6"
                    , value);
            }
            return new HeadTexture(type, value);
        } catch (Throwable e) {
            throw InvalidConfigurationException.nestedPath(e, "item", "head-texture");
        }
    }

    public ItemStack apply(ItemStack inputItem, UnaryOperator<String> valuePreProcessor) {
        try {
            String value = valuePreProcessor.apply(this.value);
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
            switch (type) {
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
                    throw new RuntimeException("Not implemented for " + type + ". Report to the author immediately.\nGH issues: https://github.com/ValeraShimchuck/SimpleItemGenerator/issues");
            }
        } catch (Throwable e) {
            throw InvalidConfigurationException.nestedPath(e, "item", "head-texture");
        }
    }

    private ItemStack setInternal(ItemStack item, String base64) {
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
    private void setNBT(ItemStack item, String base64) {
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

    private ItemStack setComponent(ItemStack item, String base64) {
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

    private String toBase64Texture(String url) {
        return Base64.getEncoder().encodeToString(TEXTURE_JSON.replace("%url%", url).getBytes());
    }


    public enum Type {
        HEADDB, URL, BASE64, NICKNAME
    }



}
