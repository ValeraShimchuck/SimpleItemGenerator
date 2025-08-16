package ua.valeriishymchuk.simpleitemgenerator.common.wrapper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public enum DyeColorWrapper {

    BLACK(0),
    BLUE(3949738),
    BROWN(8606770),
    CYAN(1481884),
    GRAY(4673362),
    GREEN(6192150),
    LIGHT_BLUE(3847130),
    LIGHT_GRAY(10329495),
    LIME(8439583),
    MAGENTA(13061821),
    ORANGE(16351261),
    PINK(15961002),
    PURPLE(8991416),
    RED(11546150),
    WHITE(16383998),
    YELLOW(16701501);
    int color;

}
