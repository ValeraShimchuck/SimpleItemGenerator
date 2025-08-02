package ua.valeriishymchuk.simpleitemgenerator.common.component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.Component;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.minimessage.MiniMessage;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigSerializable
public class RawComponent {

    @Getter
    @Setting(nodeFromParent = true)
    List<String> raw;

    public RawComponent(List<String> raw) {
        this.raw = raw;
    }

    public RawComponent(String... raw) {
        this(Arrays.asList(raw));
    }

    private RawComponent() {
        this(Collections.emptyList());
    }


    public net.kyori.adventure.text.Component bake() {
        return KyoriHelper.convert(bakeInternal());
    }

    public Component bakeInternal() {
        Component message = Component.empty();
        for (int i = 0; i < raw.size(); i++) {
            String line = raw.get(i);
            message = message.append(MiniMessage.miniMessage().deserialize(line));
            if (i + 1 < raw.size()) {
                message = message.append(Component.newline());
            }
        }
        return message;
    }

    public RawComponent replaceText(String placeholder, String text) {
        return new RawComponent(
                raw.stream().map(s -> s.replace(placeholder, text)).collect(Collectors.toList())
        );
    }

    public RawComponent replaceText(String placeholder, Component text) {
        return replaceText(placeholder, MiniMessage.miniMessage().serialize(text));
    }

    public RawComponent replaceText(String placeholder, RawComponent text) {
        return replaceText(placeholder, text.bakeInternal());
    }

    public RawComponent replaceText(String placeholder, Object obj) {
        return replaceText(placeholder, obj.toString());
    }

}
