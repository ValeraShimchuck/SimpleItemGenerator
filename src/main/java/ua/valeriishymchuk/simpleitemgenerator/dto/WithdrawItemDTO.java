package ua.valeriishymchuk.simpleitemgenerator.dto;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.component.WrappedComponent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class WithdrawItemDTO {

    WrappedComponent senderMessage;
    @Getter(AccessLevel.NONE)
    @Nullable WrappedComponent receiverMessage;
    boolean success;

    public Option<WrappedComponent> getReceiverMessage() {
        return Option.of(receiverMessage);
    }
}
