package ua.valeriishymchuk.simpleitemgenerator.tester.stream;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WrappedPrintStream extends PrintStream {

    Consumer<Object> consumer;

    public WrappedPrintStream(@NotNull OutputStream out, Consumer<Object> consumer) {
        super(out);
        this.consumer = consumer;
    }


    @Override
    public void print(boolean b) {
        super.print(b);
        consumer.accept(b);
    }

    @Override
    public void print(char c) {
        super.print(c);
        consumer.accept(c);
    }

    @Override
    public void print(int i) {
        super.print(i);
        consumer.accept(i);
    }

    @Override
    public void print(long l) {
        super.print(l);
        consumer.accept(l);
    }

    @Override
    public void print(float f) {
        super.print(f);
        consumer.accept(f);
    }

    @Override
    public void print(double d) {
        super.print(d);
        consumer.accept(d);
    }

    @Override
    public void print(@NotNull char[] s) {
        super.print(s);
        consumer.accept(s);
    }

    @Override
    public void print(@Nullable String s) {
        super.print(s);
        consumer.accept(s);
    }

    @Override
    public void print(@Nullable Object obj) {
        super.print(obj);
        consumer.accept(obj);
    }
}
