package ua.valeriishymchuk.itemgenerator.common.version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class MinecraftVersion implements Comparable<MinecraftVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(\\.(\\d+))?.*");

    public static final MinecraftVersion CURRENT = parse(Bukkit.getBukkitVersion());

    int major;
    int minor;
    int patch;

    @Override
    public int compareTo(@NotNull MinecraftVersion o) {
       if (major > o.major) return 1;
       if (major < o.major) return -1;
       if (minor > o.minor) return 1;
       if (minor < o.minor) return -1;
       return Integer.compare(patch, o.patch);
    }

    @Override
    public String toString() {
        String lastPart = patch == 0 ? "" : "." + patch;
        return major + "." + minor + lastPart;
    }

    public boolean isAtLeast(MinecraftVersion other) {
        return this.compareTo(other) >= 0;
    }

    public boolean isAtLeast(int major, int minor, int patch) {
        return isAtLeast(new MinecraftVersion(major, minor, patch));
    }

    public boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    public void assertAtLeast(int major, int minor, int patch) {
        assertAtLeast(new MinecraftVersion(major, minor, patch));
    }

    public void assertAtLeast(int major, int minor) {
        assertAtLeast(major, minor, 0);
    }

    public void assertAtLeast(MinecraftVersion other) {
        if (!isAtLeast(other))
            throw new IllegalStateException("Feature is supported from " + other + ". Current version " + this);
    }


    public static MinecraftVersion parse(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        return new MinecraftVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                matcher.groupCount() >= 4 ? Integer.parseInt(matcher.group(4)) : 0
        );
    }
}
