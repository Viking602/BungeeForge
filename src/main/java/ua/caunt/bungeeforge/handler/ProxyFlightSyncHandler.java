package ua.caunt.bungeeforge.handler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ua.caunt.bungeeforge.bridge.network.ConnectionBridge;
import ua.caunt.bungeeforge.mixin.server.network.ServerCommonPacketListenerAccessor;

/**
 * Syncs the player's vanilla flight ability for proxy connections where NeoForge
 * strips modded attributes like neoforge:creative_flight from the play stream.
 */
public final class ProxyFlightSyncHandler {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.connection == null || player.tickCount % 20 != 0) {
            return;
        }

        ConnectionBridge bridge = (ConnectionBridge) ((ServerCommonPacketListenerAccessor) player.connection).bungee$getConnection();
        if (!bridge.bungee$getSpoofedAddress().isPresent()) {
            return;
        }

        AttributeInstance flightAttr = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        boolean attrMayFly = flightAttr != null && flightAttr.getValue() > 0;

        GameType gameType = player.gameMode.getGameModeForPlayer();
        boolean gameModeMayFly = gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR;
        boolean shouldMayFly = gameModeMayFly || attrMayFly;

        if (player.getAbilities().mayfly == shouldMayFly) {
            return;
        }

        player.getAbilities().mayfly = shouldMayFly;
        if (!shouldMayFly) {
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
    }
}
