package top.qwerty770.clientexport;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import top.qwerty770.clientexport.mixin.StatsCounterAccessor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static top.qwerty770.clientexport.util.FileUtil.*;

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
            Identifier location = BuiltInRegistries.STAT_TYPE.getKey(entry2.getKey());
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
        // Minecraft 26.1.2 ships unobfuscated, so the version accessors can be called directly.
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }
}
