package ua.caunt.bungeeforge.mixin.server.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.filters.NetworkFilters;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.caunt.bungeeforge.bridge.network.ConnectionBridge;

/**
 * Forces NeoForge to treat proxy connections as vanilla/OTHER so that:
 * 1. VanillaConnectionNetworkFilter is applied (strips NeoForge-specific entity attributes, commands, tags)
 * 2. GenericPacketSplitter is disabled (Velocity can't reassemble split packets)
 *
 * Without this, when a NeoForge client connects through Velocity, NeoForge detects the connection as NEOFORGE
 * (because Velocity transparently forwards the mod negotiation), and sends NeoForge-extended play packets
 * that Velocity cannot decode ("A packet did not decode successfully").
 */
@Mixin(net.minecraft.server.network.ServerConfigurationPacketListenerImpl.class)
public class ServerConfigurationPacketListenerImplMixin {

    @Shadow
    @Final
    Connection connection;

    @Inject(method = "handleConfigurationFinished", at = @At("HEAD"), remap = false)
    private void bungee$forceVanillaForProxy(ServerboundFinishConfigurationPacket packet, CallbackInfo ci) {
        ConnectionBridge bridge = (ConnectionBridge) connection;
        if (!bridge.bungee$getSpoofedAddress().isPresent())
            return;

        // Force connection type to OTHER so NeoForge applies vanilla-compatible packet filters
        ChannelAttributes.setConnectionType(connection, ConnectionType.OTHER);
        // Clear the negotiated payload setup so GenericPacketSplitter is not injected
        ChannelAttributes.setPayloadSetup(connection, NetworkPayloadSetup.empty());
        // Reinject network filters with the corrected connection type
        NetworkFilters.cleanIfNecessary(connection);
        NetworkFilters.injectIfNecessary(connection);
    }
}
