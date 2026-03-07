package ua.caunt.bungeeforge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ua.caunt.bungeeforge.bridge.network.ConnectionBridge;
import ua.caunt.bungeeforge.mixin.server.network.ServerCommonPacketListenerAccessor;

@EventBusSubscriber(modid = "bungeeforge")
public final class ProxyFlightSyncHandler {

    private ProxyFlightSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        ServerGamePacketListenerImpl packetListener = player.connection;
        if (packetListener == null) {
            return;
        }

        ConnectionBridge bridge = (ConnectionBridge) ((ServerCommonPacketListenerAccessor) packetListener).bungee$getConnection();
        if (!bridge.bungee$getSpoofedAddress().isPresent()) {
            return;
        }

        AttributeInstance flightAttr = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        boolean attrMayFly = flightAttr != null && flightAttr.getValue() > 0;

        GameType gameType = player.gameMode.getGameModeForPlayer();
        boolean gameModeMayFly = gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR;

        boolean shouldMayFly = gameModeMayFly || attrMayFly;

        if (player.getAbilities().mayfly != shouldMayFly) {
            player.getAbilities().mayfly = shouldMayFly;
            if (!shouldMayFly) {
                player.getAbilities().flying = false;
            }
            player.onUpdateAbilities();
        }
    }
}
