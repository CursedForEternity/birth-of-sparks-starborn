package cursed.bos_starborn.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import cursed.bos_starborn.BOS;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = BOS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BosCommands {

    // Suggests full "namespace:path" IDs (e.g., bos_starborn:bumblebee, addon:bumblebee)
    private static final SuggestionProvider<CommandSourceStack> SET_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    ModularSetManager.getAllSets().keySet().stream().map(ResourceLocation::toString),
                    builder
            );

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bos")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("modular_sets")
                                .then(Commands.argument("torso", StringArgumentType.string())
                                        .suggests(SET_SUGGESTIONS)
                                        .executes(ctx -> applySet(ctx,
                                                StringArgumentType.getString(ctx, "torso"),
                                                null, null, null))

                                        .then(Commands.argument("head", StringArgumentType.string())
                                                .suggests(SET_SUGGESTIONS)
                                                .executes(ctx -> applySet(ctx,
                                                        StringArgumentType.getString(ctx, "torso"),
                                                        StringArgumentType.getString(ctx, "head"),
                                                        null, null))

                                                .then(Commands.argument("arms", StringArgumentType.string())
                                                        .suggests(SET_SUGGESTIONS)
                                                        .executes(ctx -> applySet(ctx,
                                                                StringArgumentType.getString(ctx, "torso"),
                                                                StringArgumentType.getString(ctx, "head"),
                                                                StringArgumentType.getString(ctx, "arms"),
                                                                null))

                                                        .then(Commands.argument("legs", StringArgumentType.string())
                                                                .suggests(SET_SUGGESTIONS)
                                                                .executes(ctx -> applySet(ctx,
                                                                        StringArgumentType.getString(ctx, "torso"),
                                                                        StringArgumentType.getString(ctx, "head"),
                                                                        StringArgumentType.getString(ctx, "arms"),
                                                                        StringArgumentType.getString(ctx, "legs")))
                                                        )
                                                )
                                        )
                                )
                        )
        );
    }

    private static ResourceLocation parseSetId(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        return raw.contains(":")
                ? ResourceLocation.tryParse(raw)
                : new ResourceLocation(BOS.MODID, raw);
    }

    private static int applySet(CommandContext<CommandSourceStack> ctx,
                                String torsoRaw, String headRaw, String armsRaw, String legsRaw) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }

        ResourceLocation torsoId = parseSetId(torsoRaw);
        if (torsoId == null || ModularSetManager.getSet(torsoId) == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown modular set: " + torsoRaw));
            return 0;
        }

        ResourceLocation headId = (headRaw != null) ? parseSetId(headRaw) : torsoId;
        ResourceLocation armsId = (armsRaw != null) ? parseSetId(armsRaw) : torsoId;
        ResourceLocation legsId = (legsRaw != null) ? parseSetId(legsRaw) : torsoId;

        if (headId == null || ModularSetManager.getSet(headId) == null ||
                armsId == null || ModularSetManager.getSet(armsId) == null ||
                legsId == null || ModularSetManager.getSet(legsId) == null) {
            ctx.getSource().sendFailure(Component.literal("One or more specified limb sets do not exist."));
            return 0;
        }

        var tag = player.getPersistentData();
        tag.putString("bos_torso", torsoId.toString());
        tag.putString("bos_head", headId.toString());
        tag.putString("bos_arms", armsId.toString());
        tag.putString("bos_legs", legsId.toString());

        SyncModularLoadoutPacket packet = new SyncModularLoadoutPacket(
                player.getUUID(), torsoId, headId, armsId, legsId
        );
        BOSNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "Equipped Modular Set -> Torso: " + torsoId +
                        " | Head: " + headId +
                        " | Arms: " + armsId +
                        " | Legs: " + legsId
        ), true);

        return 1;
    }
}
