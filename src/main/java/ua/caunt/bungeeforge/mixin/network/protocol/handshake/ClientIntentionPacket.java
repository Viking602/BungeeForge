package ua.caunt.bungeeforge.mixin.network.protocol.handshake;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.handshake.ClientIntent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.caunt.bungeeforge.bridge.network.protocol.handshake.ClientIntentionPacketBridge;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Mixin(net.minecraft.network.protocol.handshake.ClientIntentionPacket.class)
public class ClientIntentionPacket implements ClientIntentionPacketBridge {
    @Unique
    private String bungee$spoofedAddress;
    @Unique
    private UUID bungee$spoofedId;
    @Unique
    private Property[] bungee$spoofedProperties;

    @Unique
    private static final ThreadLocal<SpoofedProfile> bungee$pendingSpoofedProfile = new ThreadLocal<>();
    @Unique
    private static final Gson bungee$gson = new Gson();

    @Unique
    private record SpoofedProfile(String address, UUID id, Property[] properties) { }

    // In MC 1.21.1, ClientIntentionPacket is a record with canonical constructor:
    // ClientIntentionPacket(int protocolVersion, String hostName, int port, ClientIntent intention)
    // We intercept the hostName argument to parse BungeeCord data and return a clean hostname.
    @ModifyVariable(
            method = "<init>(ILjava/lang/String;ILnet/minecraft/network/protocol/handshake/ClientIntent;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private static String bungee$modifyHostName(String hostName) {
        bungee$pendingSpoofedProfile.remove();

        var chunks = hostName.split("\0");

        if (chunks.length <= 3)
            return hostName;
        if (chunks[1].isBlank())
            return hostName;

        try {
            var properties = bungee$gson.fromJson(chunks[3], Property[].class);
            if (properties == null)
                return hostName;

            var spoofedId = UUID.fromString(ensureDashesInUuid(chunks[2]));
            var spoofedProperties = Arrays.stream(properties)
                    .filter(Objects::nonNull)
                    .filter(property -> !isFmlMarker(property))
                    .toArray(Property[]::new);

            bungee$pendingSpoofedProfile.set(new SpoofedProfile(chunks[1], spoofedId, spoofedProperties));
            return chunks[1];
        } catch (RuntimeException ignored) {
            bungee$pendingSpoofedProfile.remove();
            return hostName;
        }
    }

    @Inject(method = "<init>(ILjava/lang/String;ILnet/minecraft/network/protocol/handshake/ClientIntent;)V", at = @At("RETURN"))
    private void bungee$captureSpoofedProfile(int protocolVersion, String hostName, int port, ClientIntent intention, CallbackInfo ci) {
        var spoofedProfile = bungee$pendingSpoofedProfile.get();
        bungee$pendingSpoofedProfile.remove();

        if (spoofedProfile == null)
            return;

        bungee$spoofedAddress = spoofedProfile.address();
        bungee$spoofedId = spoofedProfile.id();
        bungee$spoofedProperties = spoofedProfile.properties();
    }

    private static boolean isFmlMarker(Property property) {
        return Objects.equals(property.name(), "extraData")
                && property.value() != null
                && property.value().startsWith("\u0001FORGE");
    }

    private static String ensureDashesInUuid(String source) {
        if (source.length() > 32)
            return source;

        var builder = new StringBuilder(source);
        builder.insert(8, "-");
        builder.insert(13, "-");
        builder.insert(18, "-");
        builder.insert(23, "-");

        return builder.toString();
    }

    @Override
    public String bungee$getSpoofedAddress() {
        return bungee$spoofedAddress;
    }

    @Override
    public UUID bungee$getSpoofedId() {
        return bungee$spoofedId;
    }

    @Override
    public Property[] bungee$getSpoofedProperties() {
        return bungee$spoofedProperties;
    }

    @Override
    public boolean bungee$hasSpoofedProfile() {
        return bungee$spoofedAddress != null && bungee$spoofedId != null && bungee$spoofedProperties != null;
    }
}