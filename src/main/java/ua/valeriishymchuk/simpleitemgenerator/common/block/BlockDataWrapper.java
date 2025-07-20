package ua.valeriishymchuk.simpleitemgenerator.common.block;

import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ClassHelper;

import java.lang.reflect.Method;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlockDataWrapper {

    private static final Class<Block> BLOCK_CLASS = Block.class;
    private static final Option<Class<?>> BLOCK_DATA_CLASS_OPTION
            = ClassHelper.tryGetClass("org.bukkit.block.data.BlockData");
    private static final Option<Method> GET_BLOCK_DATA_METHOD_OPTION
            = ClassHelper.tryGetMethod(BLOCK_CLASS, "getBlockData");

    Block block;
    @Nullable Object blockData;

    public BlockDataWrapper(Block block) {
        this.block = block;
        blockData = GET_BLOCK_DATA_METHOD_OPTION.flatMap(m -> Try.of(() -> m.invoke(block))
                .onFailure(Throwable::printStackTrace).toOption()).getOrNull();
    }

    public Option<Object> getBlockData() {
        return Option.of(blockData);
    }

    public Option<Boolean> isInstanceOf(String className) {
        return ClassHelper.tryGetClass(className)
                .flatMap(clazz -> getBlockData().map(clazz::isInstance));
    }

}
