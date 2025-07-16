package top.qwerty770.clientexport;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class ClientExportHelper implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("clientexport");
    public static final String version = "3.1.0";
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> register(dispatcher)));
        LOGGER.info("Client Export Helper initialized!");
    }

    @Environment(EnvType.CLIENT)
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        var command = dispatcher.register(ClientCommandManager.literal("clientexport")
                .executes(context -> {
                    context.getSource().sendFeedback(Component.literal("Client Export Helper " + version + " running!"));
                    return 0;
                })
                .then(ClientCommandManager.literal("advancements").executes(context -> {
                    AdvancementCommand.exportAllAdvancements(context);
                    return 1;
                }).then(ClientCommandManager.argument("namespace", StringArgumentType.string()).executes(context -> {
                    AdvancementCommand.exportAdvancements(context.getArgument("namespace", String.class), context);
                    return 1;
                })))
                .then(ClientCommandManager.literal("progress").executes(context -> {
                    AdvancementCommand.exportAllProgress(context);
                    return 1;
                }).then(ClientCommandManager.argument("namespace", StringArgumentType.string()).executes(context -> {
                    AdvancementCommand.exportProgress(context.getArgument("namespace", String.class), context);
                    return 1;
                })))
                .then(ClientCommandManager.literal("statistics").executes(context -> {
                    StatisticsCommand.exportStatistics(context);
                    return 1;
                }))
        );
        dispatcher.register(ClientCommandManager.literal("clex").redirect(command));
    }
}