package ua.caunt.bungeeforge.mixin.server.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.filters.GenericPacketSplitter;
import net.neoforged.neoforge.network.filters.NetworkFilters;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import org.spongepowered.asm.mixin.Mixin;
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
public abstract class ServerConfigurationPacketListenerImplMixin extends ServerCommonPacketListenerImpl {

    protected ServerConfigurationPacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie cookie) {
        super(server, connection, cookie);
    }

    @Inject(method = "handleConfigurationFinished", at = @At("HEAD"), remap = false)
    private void bungee$forceVanillaForProxy(ServerboundFinishConfigurationPacket packet, CallbackInfo ci) {
        ConnectionBridge bridge = (ConnectionBridge) this.connection;
        if (!bridge.bungee$getSpoofedAddress().isPresent())
            return;

        // Force connection type to OTHER so NeoForge applies vanilla-compatible packet filters
        ChannelAttributes.setConnectionType(this.connection, ConnectionType.OTHER);
        // Keep the negotiated payload setup so that mod payloads (e.g. kubejs:sync_server_data)
        // pass NetworkRegistry.checkPacket() during the datapack sync event in placeNewPlayer
        // Reinject network filters with the corrected connection type
        NetworkFilters.cleanIfNecessary(this.connection);
        NetworkFilters.injectIfNecessary(this.connection);
        // Remove GenericPacketSplitter — proxies cannot reassemble split packets.
        // We do this after filter injection because GenericPacketSplitter.isNecessary() checks
        // if the SplitPacketPayload channel is in the payload setup (which we now preserve).
        var pipeline = this.connection.channel().pipeline();
        if (pipeline.get(GenericPacketSplitter.CHANNEL_HANDLER_NAME) != null) {
            pipeline.remove(GenericPacketSplitter.CHANNEL_HANDLER_NAME);
        }
    }
}
