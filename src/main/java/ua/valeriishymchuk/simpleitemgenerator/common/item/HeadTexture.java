package ua.valeriishymchuk.simpleitemgenerator.common.item;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.simpleitemgenerator.common.config.exception.InvalidConfigurationException;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class HeadTexture {

    protected static final UUID OWNER_UUID = UUID.fromString("687b38a7-5505-4253-9e15-733e387fc2f2");
    protected static final String TEXTURE_JSON = "{\"textures\":{\"SKIN\":{\"url\":\"%url%\"}}}";
    protected static final Pattern MOJANG_TEXTURES_URL_PATTERN = Pattern.compile("^https?://\\w+\\.minecraft\\.net/\\S+$");
    protected static final Pattern PATTERN = Pattern.compile("^ *\\[(?<type>headdb|url|base64|nickname)] *(?<value>\\S*) *$");
    protected static final boolean USE_MODERN_UUID = SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 16);
    protected static final boolean USE_COMPONENTS = SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 20, 5);

    Type type;
    String value;
    // settings options: headdb, url, base64, nickname. If nickname is used then allow %player% placeholder usage in it

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

    public enum Type {
        HEADDB, URL, BASE64, NICKNAME
    }



}
