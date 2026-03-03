package ua.caunt.bungeeforge.mixin.network.protocol.handshake;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.handshake.ClientIntent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ua.caunt.bungeeforge.bridge.network.protocol.handshake.ClientIntentionPacketBridge;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Mixin(net.minecraft.network.protocol.handshake.ClientIntentionPacket.class)
public class ClientIntentionPacket implements ClientIntentionPacketBridge {
    @Unique
    private static String bungee$spoofedAddress;
    @Unique
    private static UUID bungee$spoofedId;
    @Unique
    private static Property[] bungee$spoofedProperties;

    @Unique
    private static final Gson bungee$gson = new Gson();

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
        var chunks = hostName.split("\0");

        if (chunks.length <= 2)
            return hostName;

        var properties = bungee$gson.fromJson(chunks[3], Property[].class);

        bungee$spoofedAddress = chunks[1];
        bungee$spoofedId = UUID.fromString(ensureDashesInUuid(chunks[2]));
        bungee$spoofedProperties = Arrays.stream(properties)
                .filter(property -> !isFmlMarker(property))
                .toArray(Property[]::new);

        return chunks[1];
    }

    private static boolean isFmlMarker(Property property) {
        return Objects.equals(property.name(), "extraData") && property.value().startsWith("\u0001FORGE");
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
}