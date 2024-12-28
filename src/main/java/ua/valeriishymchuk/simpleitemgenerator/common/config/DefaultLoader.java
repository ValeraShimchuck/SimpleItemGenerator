package ua.valeriishymchuk.simpleitemgenerator.common.config;

import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public class DefaultLoader {

    private static final YamlConfigurationLoader YAML_LOADER = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).build();

    public static YamlConfigurationLoader yaml() {
        return YAML_LOADER;
    }

}
