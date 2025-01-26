package ua.valeriishymchuk.simpleitemgenerator.common.nbt;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import lombok.SneakyThrows;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class NBTConverter {


    @SneakyThrows
    public static CompoundBinaryTag fromNBTApi(ReadableNBT nbt) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        nbt.writeCompound(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return BinaryTagIO.reader().read(bais, BinaryTagIO.Compression.GZIP);
    }

    @SneakyThrows
    public static ReadWriteNBT toNBTApi(CompoundBinaryTag nbt) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(nbt, baos, BinaryTagIO.Compression.GZIP);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return NBT.readNBT(bais);
    }

}
