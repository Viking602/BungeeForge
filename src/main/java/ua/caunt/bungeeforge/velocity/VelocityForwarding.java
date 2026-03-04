package ua.caunt.bungeeforge.velocity;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for Velocity modern IP forwarding support.
 * Ported from PaperMC's VelocityProxy implementation.
 * See: https://docs.papermc.io/velocity/player-information-forwarding
 */
public final class VelocityForwarding {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("BungeeForge/Velocity");

    public static final int MAX_SUPPORTED_FORWARDING_VERSION = 4;
    public static final ResourceLocation PLAYER_INFO_CHANNEL = ResourceLocation.fromNamespaceAndPath("velocity", "player_info");

    private static volatile String secret = null;
    private static volatile boolean loaded = false;
    private static final ThreadLocal<Boolean> velocityQueryPending = ThreadLocal.withInitial(() -> false);

    private VelocityForwarding() {}

    public static void loadSecret(Path serverDirectory) {
        var secretFile = serverDirectory.resolve("velocity-forwarding.secret");
        if (Files.exists(secretFile)) {
            try {
                secret = Files.readString(secretFile).trim();
            } catch (IOException e) {
                LOGGER.error("Failed to read velocity-forwarding.secret: {}", e.getMessage());
            }
        }
        loaded = true;
    }

    public static boolean isEnabled() {
        if (!loaded) {
            loadSecret(Path.of("."));
        }
        return secret != null && !secret.isEmpty();
    }

    public static void setVelocityQueryPending(boolean pending) {
        velocityQueryPending.set(pending);
    }

    public static boolean isVelocityQueryPending() {
        return velocityQueryPending.get();
    }

    public static boolean checkIntegrity(final FriendlyByteBuf buf) {
        final byte[] signature = new byte[32];
        buf.readBytes(signature);

        final byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);

        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            final byte[] mySignature = mac.doFinal(data);
            return MessageDigest.isEqual(signature, mySignature);
        } catch (final InvalidKeyException | NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public static InetAddress readAddress(final FriendlyByteBuf buf) {
        return InetAddresses.forString(buf.readUtf(Short.MAX_VALUE));
    }

    public static GameProfile createProfile(final FriendlyByteBuf buf) {
        final GameProfile profile = new GameProfile(buf.readUUID(), buf.readUtf(16));
        final int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            final String name = buf.readUtf(Short.MAX_VALUE);
            final String value = buf.readUtf(Short.MAX_VALUE);
            final String signature = buf.readBoolean() ? buf.readUtf(Short.MAX_VALUE) : null;
            profile.getProperties().put(name, new Property(name, value, signature));
        }
        return profile;
    }

    public record VelocityMaxVersionPayload(byte maxVersion) implements CustomQueryPayload {

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeByte(maxVersion);
        }

        @Override
        public ResourceLocation id() {
            return PLAYER_INFO_CHANNEL;
        }
    }

    public record VelocityPlayerDataPayload(FriendlyByteBuf buffer) implements CustomQueryAnswerPayload {

        @Override
        public void write(final FriendlyByteBuf buf) {
            // This payload is only ever read (received from Velocity); it is never sent by the server.
        }
    }
}
