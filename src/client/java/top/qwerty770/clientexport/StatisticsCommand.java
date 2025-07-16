package top.qwerty770.clientexport;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.level.storage.DataVersion;
import top.qwerty770.clientexport.mixin.StatsCounterAccessor;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.Objects;

import static top.qwerty770.clientexport.util.FileUtil.*;
import static top.qwerty770.clientexport.util.MappingResolverTool.*;

public class StatisticsCommand {
    public static void exportStatistics(CommandContext<FabricClientCommandSource> context) {
        assert Minecraft.getInstance().player != null;
        Object2IntMap<Stat<?>> stats = ((StatsCounterAccessor) Minecraft.getInstance().player.getStats()).getStats();
        try {
            JsonObject jsonObject = toJson(stats);
            FileWriter fileWriter = createFile("statistics");
            writeJson(fileWriter, jsonObject);
            fileWriter.close();
            ClientExportHelper.LOGGER.info("Client Export Helper exported {} lines of statistics", stats.size());
            context.getSource().sendFeedback(Component.translatable("commands.clientexport.statistics.success", stats.size()));
        }
        catch (IOException exception){
            sendFailure(exception, context, "commands.clientexport.statistics.fail");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static JsonObject toJson(Object2IntMap<Stat<?>> stats) {
        Map<StatType<?>, JsonObject> map = Maps.newHashMap();
        for (Object2IntMap.Entry<Stat<?>> statEntry : stats.object2IntEntrySet()) {
            Stat stat = statEntry.getKey();
            var registry = stat.getType().getRegistry();
            map.computeIfAbsent(stat.getType(), (statType) -> new JsonObject())
                    .addProperty(Objects.requireNonNull(registry.getKey(stat.getValue())).toString(), statEntry.getIntValue());
        }

        JsonObject jsonObject = new JsonObject();
        for(Map.Entry<StatType<?>, JsonObject> entry2 : map.entrySet()) {
            ResourceLocation location = BuiltInRegistries.STAT_TYPE.getKey(entry2.getKey());
            if (location == null) {
                ClientExportHelper.LOGGER.warn("StatType {} is not registered in BuiltInRegistries.STAT_TYPE", entry2.getKey());
                continue;
            }
            jsonObject.add(location.toString(), entry2.getValue());
        }

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.add("stats", jsonObject);
        jsonObject2.addProperty("DataVersion", getDataVersion());
        return jsonObject2;
    }

    public static int getDataVersion() {
        /// Returns the data version of Minecraft. Ensures the compatibility between Minecraft 1.21.5 and 1.21.6.
        try {
            // Implement this method via reflection
            WorldVersion worldVersion = SharedConstants.getCurrentVersion();
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            // WorldVersion.getDataVersion() was renamed to WorldVersion.dataVersion() in 1.21.6
            String name = mapIntermediaryMethodNames("net.minecraft.class_6489", "()Lnet/minecraft/class_6595;", "method_37912", "comp_4026");
            MethodHandle handle = lookup.findVirtual(WorldVersion.class, name, MethodType.methodType(DataVersion.class));
            DataVersion dataVersion = (DataVersion) handle.invokeExact(worldVersion);
            // DataVersion.getVersion() was renamed to DataVersion.version() in 1.21.6
            String name2 = mapIntermediaryMethodNames("net.minecraft.class_6595", "()I", "method_38494", "comp_4038");
            MethodHandle handle2 = lookup.findVirtual(DataVersion.class, name2, MethodType.methodType(int.class));
            return (int) handle2.invokeExact(dataVersion);
        }
        catch (Throwable e) {
            try {
                // Fallback to SharedConstants.WORLD_VERSION if reflection fails
                String name = mapIntermediaryFieldName("net.minecraft.class_155", "field_29732", "I");
                return SharedConstants.class.getField(name).getInt(null);
            } catch (Exception e2) {
                // This should never happen, but if it does, return a default value
                ClientExportHelper.LOGGER.error("Failed to get data version via reflection", e2);
                return 4438;
            }
        }
    }
}
