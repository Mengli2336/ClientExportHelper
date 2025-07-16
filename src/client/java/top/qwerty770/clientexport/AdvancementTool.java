package top.qwerty770.clientexport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import top.qwerty770.clientexport.mixin.ClientAdvancementsAccessor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static top.qwerty770.clientexport.util.MappingResolverTool.mapIntermediaryMethodNames;

public class AdvancementTool {
    public static AdvancementTree getAdvancementTree() {
        assert Minecraft.getInstance().player != null;
        ClientAdvancements clientAdvancements = Minecraft.getInstance().player.connection.getAdvancements();
        return clientAdvancements.getTree();
    }

    public static List<AdvancementHolder> getAdvancements() {
        return getAdvancementTree().nodes().stream().map(AdvancementNode::holder).toList();
    }

    public static JsonElement getJson(AdvancementHolder holder, AdvancementTree tree) {
        // To avoid exception "JsonSyntaxException: Missing trigger"
        HashMap<String, Integer> enchantments = null;
        Advancement.Builder builder;
        Advancement advancement = holder.value();
        if (advancement.sendsTelemetryEvent()) {
            builder = Advancement.Builder.advancement();
        } else {
            builder = Advancement.Builder.recipeAdvancement();
        }
        if (advancement.display().isPresent()) {
            ItemStack icon = advancement.display().get().getIcon();
            // Enchantments are only registered on server side
            if (icon.has(DataComponents.ENCHANTMENTS)) {
                ItemEnchantments enchantmentsComponent = ItemEnchantments.EMPTY;
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                String name = mapIntermediaryMethodNames("net.minecraft.class_9322", "(Lnet/minecraft/class_9331;)Ljava/lang/Object;",
                        "method_57824", "get");
                try {
                    var method = lookup.findVirtual(DataComponentHolder.class, name, MethodType.methodType(Object.class, DataComponentType.class));
                    enchantmentsComponent = (ItemEnchantments) method.invoke((DataComponentHolder) icon, DataComponents.ENCHANTMENTS);
                } catch (Throwable e) {
                    ClientExportHelper.LOGGER.error("Failed to get enchantments from ItemStack", e);
                    icon.remove(DataComponents.ENCHANTMENTS);
                }
                if (!enchantmentsComponent.isEmpty()) {
                    enchantments = new HashMap<>();
                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantmentsComponent.entrySet()) {
                        enchantments.put(entry.getKey().getRegisteredName(), entry.getIntValue());
                    }
                    icon.remove(DataComponents.ENCHANTMENTS);
                }
            }
            builder.display(advancement.display().get());
        }
        builder.rewards(advancement.rewards());
        builder.requirements(advancement.requirements());
        for (List<String> strings : advancement.requirements().requirements()) {
            for (String string : strings) {
                // Criteria are not synchronized to the client
                builder.addCriterion(string, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()));
            }
        }
        if (advancement.parent().isPresent()) {
            AdvancementNode node = tree.get(advancement.parent().orElseGet(() -> ResourceLocation.parse("")));
            if (node != null) builder.parent(node.holder());
        }
        try {
            JsonObject jsonObject = (JsonObject) Advancement.CODEC.encodeStart(JsonOps.INSTANCE, builder.build(holder.id()).value()).getOrThrow();
            if (enchantments != null) {
                JsonObject object = new JsonObject();
                enchantments.forEach(object::addProperty);
                jsonObject.add("clientexport:icon_enchantments", object);
            }
            return jsonObject;
        } catch (Exception e) {
            return error(e, "Failed to encode the advancement {} due to an unexpected error!", holder.id().toString());
        }
    }

    public static Map<AdvancementHolder, AdvancementProgress> getProgress() {
        assert Minecraft.getInstance().player != null;
        ClientAdvancements clientAdvancements = Minecraft.getInstance().player.connection.getAdvancements();
        return ((ClientAdvancementsAccessor) clientAdvancements).getProgress();
    }

    public static JsonElement getJson(AdvancementProgress progress) {
        try {
            JsonObject result = AdvancementProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow().getAsJsonObject();
            JsonArray remaining = new JsonArray();
            progress.getRemainingCriteria().forEach(remaining::add);
            result.add("remaining", remaining);
            result.addProperty("progress", progress.getPercent());
            return result;
        } catch (IllegalStateException | IllegalArgumentException e) {
            return error(e, "Failed to encode the progress of the advancement {}!", progress.toString());
        } catch (Exception e) {
            return error(e, "Failed to encode the progress of the advancement {} due to an unexpected error!", progress.toString());
        }
    }

    private static JsonElement error(Exception exception, String str1, String str2) {
        ClientExportHelper.LOGGER.error(str1, str2);
        ClientExportHelper.LOGGER.error(exception.getMessage());
        JsonObject fail = new JsonObject();
        fail.addProperty("error", exception.getMessage());
        return fail;
    }
}
