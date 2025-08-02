package ua.valeriishymchuk.simpleitemgenerator.dto;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.libs.net.kyori.adventure.text.Component;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class WithdrawItemDTO {

    Component senderMessage;
    @Getter(AccessLevel.NONE)
    @Nullable Component receiverMessage;
    boolean success;

    public Option<Component> getReceiverMessage() {
        return Option.of(receiverMessage);
    }
}
