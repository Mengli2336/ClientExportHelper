package top.qwerty770.clientexport.util;

import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import top.qwerty770.clientexport.ClientExportHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtil {
    public static FileWriter createFile(String name) throws IOException {
        File file = new File("export/" + name + "-" +
                new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date()) + ".json");
        if (!file.getParentFile().exists()) {
            if (file.getParentFile().mkdirs()) {
                ClientExportHelper.LOGGER.debug("Created path: {}", file.getParentFile().getPath());
            }
        }
        if (!file.createNewFile()) {
            ClientExportHelper.LOGGER.warn("File already exists: {}.", file.getName());
        }
        return new FileWriter(file, StandardCharsets.UTF_8);
    }

    public static FileWriter createFile(String namespace, String type) throws IOException {
        return createFile(namespace + "-" + type);
    }

    public static void writeJson(FileWriter fileWriter, JsonObject jsonObject) throws IOException {
        JsonWriter jsonWriter = new JsonWriter(fileWriter);
        jsonWriter.setIndent("  ");
        Streams.write(jsonObject, jsonWriter);
    }

    public static void sendFailure(IOException exception, CommandContext<FabricClientCommandSource> context, String translatable) {
        ClientExportHelper.LOGGER.error("Failed to export to a file!");
        ClientExportHelper.LOGGER.error(exception.getMessage());
        context.getSource().sendFeedback(Component.translatable(translatable));
    }
}
