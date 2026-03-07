package ua.caunt.bungeeforge.mixin.server.level;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.caunt.bungeeforge.bridge.network.ConnectionBridge;
import ua.caunt.bungeeforge.mixin.server.network.ServerCommonPacketListenerAccessor;

/**
 * Syncs the player flight ability for proxy connections where NeoForge modded entity
 * attributes (like neoforge:creative_flight) are stripped from packets by the
 * VanillaConnectionNetworkFilter.
 *
 * When a NeoForge client connects through a BungeeCord/Velocity proxy,
 * BungeeForge forces ConnectionType.OTHER to prevent proxy packet decoding issues.
 * This causes the VanillaConnectionNetworkFilter to strip modded attributes from
 * ClientboundUpdateAttributesPacket. As a result, the client never learns about
 * attribute values like creative_flight from mods such as Apothic-Attributes.
 *
 * This mixin detects the creative_flight attribute value server-side and syncs
 * the corresponding vanilla mayfly ability flag via ClientboundPlayerAbilitiesPacket,
 * ensuring flight works correctly for proxy players.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Shadow
    public ServerGamePacketListenerImpl connection;

    @Inject(method = "tick", at = @At("TAIL"))
    private void bungee$syncCreativeFlightAbility(CallbackInfo ci) {
        if (this.connection == null) return;

        ServerPlayer self = (ServerPlayer) (Object) this;

        // Only check every 20 ticks (once per second) to reduce overhead,
        // since flight attribute changes are infrequent (potion apply/expire).
        if (self.tickCount % 20 != 0) return;

        // Only apply to proxy connections
        ConnectionBridge bridge = (ConnectionBridge) ((ServerCommonPacketListenerAccessor) this.connection).bungee$getConnection();
        if (!bridge.bungee$getSpoofedAddress().isPresent()) return;

        // Check if the NeoForge creative_flight attribute grants flight
        AttributeInstance flightAttr = self.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        boolean attrMayFly = flightAttr != null && flightAttr.getValue() > 0;

        // Check if game mode grants flight (creative or spectator)
        GameType gameType = self.gameMode.getGameModeForPlayer();
        boolean gameModeMayFly = gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR;

        boolean shouldMayFly = gameModeMayFly || attrMayFly;

        // Only update and sync if there's a discrepancy
        if (self.getAbilities().mayfly != shouldMayFly) {
            self.getAbilities().mayfly = shouldMayFly;
            if (!shouldMayFly) {
                self.getAbilities().flying = false;
            }
            self.onUpdateAbilities();
        }
    }
}
