package top.qwerty770.clientexport.util;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

public class MappingResolverTool {
    private static final MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();

    public static String mapIntermediaryFieldName(String owner, String name, String descriptor) {
        return resolver.mapFieldName("intermediary", owner, name, descriptor);
    }

    public static String mapIntermediaryMethodName(String owner, String name, String descriptor) {
        return resolver.mapMethodName("intermediary", owner, name, descriptor);
    }

    public static String mapIntermediaryMethodNames(String owner, String descriptor, String... names) {
        try {
            for (String name : names) {
                String mappedName = mapIntermediaryMethodName(owner, name, descriptor);
                if (mappedName != null && !mappedName.equals(name)) {
                    return mappedName;
                }
            }
        } catch (Exception ignored) {}
        // ClientExportHelper.LOGGER.error("Failed to map method names for owner: {}, descriptor: {}, names: {}", owner, descriptor, String.join(", ", names));
        return mapIntermediaryMethodName(owner, names[0], descriptor); // Fallback to the first name
    }
}
