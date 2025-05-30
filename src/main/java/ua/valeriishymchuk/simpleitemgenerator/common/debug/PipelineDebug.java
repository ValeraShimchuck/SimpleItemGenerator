package ua.valeriishymchuk.simpleitemgenerator.common.debug;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PipelineDebug {

    String name;
    List<PipelineDebug> children = new ArrayList<>();
    Set<Tag> tags = new HashSet<>();

    public static PipelineDebug prepend(PipelineDebug other, String name, Tag... tags) {
        PipelineDebug debug = new PipelineDebug(name);
        debug.tags.addAll(Arrays.asList(tags));
        debug.children.add(other);
        return debug;
    }

    public static PipelineDebug root(String name, Tag... tags) {
        PipelineDebug debug = new PipelineDebug(name);
        debug.tags.addAll(Arrays.asList(tags));
        return debug;
    }

    public PipelineDebug append(String name, Tag... tags) {
        PipelineDebug newChild = new PipelineDebug(name);
        newChild.tags.addAll(Arrays.asList(tags));
        children.add(newChild);
        return newChild;
    }

    public PipelineDebug appendAndReturnSelf(String name, Tag... tags) {
        append(name, tags);
        return this;
    }


    public PipelineDebug appendAllAndReturnSelf(Collection<PipelineDebug> debugs) {
        children.addAll(debugs);
        return this;
    }

    public void print(Tag... excludeTags) {
        System.out.println(get(excludeTags));
    }

    // should be something like that
    // [name]
    // [child1]
    // [child2]
    // |--[grandchild1]
    // |--[grandchild2]
    // |--[grandchild3]
    public String get(Tag... excludeTags) {
        StringBuilder sb = new StringBuilder(name).append('\n');
        children.stream()
                .filter(debug -> Arrays.stream(excludeTags).noneMatch(tags::contains))
                .forEach(child -> {
                    sb.append(child.name).append('\n');
                    child.children.stream()
                            .filter(debug -> Arrays.stream(excludeTags).noneMatch(tags::contains))
                            .forEach(grandChild -> {
                                Arrays.stream(grandChild.getDescenders(excludeTags).split("\n"))
                                        .map(s -> "|--" + s + "\n")
                                        .forEach(sb::append);
                            });
                });
        return sb.toString();
    }


    // should give something like that
    // >[name]
    // --[child1]
    // --[child2]
    // --[child3]
    private String getDescenders(Tag... excludeTags) {
        StringBuilder sb = new StringBuilder(">").append(name).append('\n');
        children.stream()
                .filter(debug -> Arrays.stream(excludeTags).noneMatch(tags::contains))
                .forEach(debug -> {
                    Arrays.stream(debug.getDescenders(excludeTags).split("\n"))
                            .map(s -> "--" + s + "\n")
                            .forEach(sb::append);
                });
        return sb.toString();
    }

    public enum Tag {
        TICK,
        INVENTORY,
        OTHER
    }

}
