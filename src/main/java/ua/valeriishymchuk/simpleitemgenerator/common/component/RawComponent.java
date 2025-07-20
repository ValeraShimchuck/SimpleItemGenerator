package ua.valeriishymchuk.simpleitemgenerator.common.component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

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

    public WrappedComponent bake() {
        Component message = Component.empty();
        for (int i = 0; i < raw.size(); i++) {
            String line = raw.get(i);
            message = message.append(MiniMessage.miniMessage().deserialize(line));
            if (i + 1 < raw.size()) {
                message = message.append(Component.newline());
            }
        }
        return new WrappedComponent(message);
    }

    public List<WrappedComponent> bakeAsLore() {
        return raw.stream().map(MiniMessage.miniMessage()::deserialize)
                .map(WrappedComponent::new)
                .collect(Collectors.toList());
    }

    public RawComponent replaceText(String placeholder, String text) {
        return new RawComponent(
                raw.stream().map(s -> s.replace(placeholder, text)).collect(Collectors.toList())
        );
    }

    public RawComponent replaceText(String placeholder, Component text) {
        return replaceText(placeholder, MiniMessage.miniMessage().serialize(text));
    }

    public RawComponent replaceText(String placeholder, WrappedComponent text) {
        return replaceText(placeholder, MiniMessage.miniMessage().serialize(text.getComponent()));
    }

    public RawComponent replaceText(String placeholder, RawComponent text) {
        return replaceText(placeholder, text.bake());
    }

    public RawComponent replaceText(String placeholder, Object obj) {
        return replaceText(placeholder, obj.toString());
    }

}
