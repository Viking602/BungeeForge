package ua.caunt.bungeeforge.mixin.network;


import com.mojang.authlib.properties.Property;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.caunt.bungeeforge.bridge.network.ConnectionBridge;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = net.minecraft.network.Connection.class)
public class Connection implements ConnectionBridge {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("BungeeForge/Connection");
    @Unique
    private static final String CUSTOM_PAYLOAD_DECODE_ERROR_PREFIX = "Failed decoding custom payload";

    @Unique
    private String bungee$spoofedAddress;
    @Unique
    private UUID bungee$spoofedId;
    @Unique
    private Property[] bungee$spoofedProperties;
    @Shadow
    private SocketAddress address;

    @Override
    public void bungee$setSpoofedAddress(String spoofedAddress) {
        this.bungee$spoofedAddress = spoofedAddress;
        if (spoofedAddress == null || spoofedAddress.isBlank())
            return;
        this.address = new InetSocketAddress(spoofedAddress, 0);
    }

    @Override
    public void bungee$setSpoofedId(UUID spoofedId) {
        this.bungee$spoofedId = spoofedId;
    }

    @Override
    public void bungee$setSpoofedProperties(Property[] spoofedProperties) {
        this.bungee$spoofedProperties = spoofedProperties;
    }

    @Override
    public Optional<String> bungee$getSpoofedAddress() {
        return Optional.ofNullable(bungee$spoofedAddress);
    }

    @Override
    public Optional<UUID> bungee$getSpoofedId() {
        return Optional.ofNullable(bungee$spoofedId);
    }

    @Override
    public Optional<Property[]> bungee$getSpoofedProperties() {
        return Optional.ofNullable(bungee$spoofedProperties);
    }

    @Override
    public boolean bungee$hasSpoofedProfile() {
        return bungee$getSpoofedAddress().isPresent() && bungee$getSpoofedId().isPresent() && bungee$getSpoofedProperties().isPresent();
    }

    /**
     * For proxy connections, silently drop custom payload packets that fail to decode
     * (e.g. NeoForge-extended ItemStack encoding sent by a mod client that the vanilla
     * codec cannot read) instead of disconnecting the player.
     *
     * The frame bytes are fully consumed and released by MessageToMessageDecoder before
     * the DecoderException reaches this handler, so subsequent packets are unaffected.
     */
    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true, remap = false)
    private void bungee$handleCustomPayloadDecodeError(ChannelHandlerContext context, Throwable throwable, CallbackInfo ci) {
        if (!bungee$getSpoofedAddress().isPresent()) {
            return;
        }

        Throwable cause = throwable instanceof DecoderException ? throwable.getCause() : null;
        if (cause instanceof RuntimeException && cause.getMessage() != null && cause.getMessage().startsWith(CUSTOM_PAYLOAD_DECODE_ERROR_PREFIX)) {
            LOGGER.warn("[BungeeForge] Dropping undecodable custom payload on proxy connection from {}: {}", address, cause.getMessage());
            ci.cancel();
        }
    }
}
