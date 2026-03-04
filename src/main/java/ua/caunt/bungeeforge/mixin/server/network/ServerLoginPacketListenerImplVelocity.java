package ua.caunt.bungeeforge.mixin.server.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.caunt.bungeeforge.bridge.network.ConnectionBridge;
import ua.caunt.bungeeforge.velocity.VelocityForwarding;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(net.minecraft.server.network.ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplVelocity {

    @Unique
    private int bungee$velocityLoginMessageId = -1;

    @Final
    @Shadow
    Connection connection;

    @Shadow
    private GameProfile authenticatedProfile;

    @Shadow
    private void disconnect(Component reason) {}

    @Shadow
    private void startClientVerification(GameProfile profile) {}

    @Inject(
            method = "handleHello",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;startClientVerification(Lcom/mojang/authlib/GameProfile;)V", ordinal = 1),
            cancellable = true
    )
    private void bungee$handleHelloVelocity(ServerboundHelloPacket pPacket, CallbackInfo ci) {
        if (!VelocityForwarding.isEnabled())
            return;

        bungee$velocityLoginMessageId = ThreadLocalRandom.current().nextInt();
        connection.send(new ClientboundCustomQueryPacket(
                bungee$velocityLoginMessageId,
                new VelocityForwarding.VelocityMaxVersionPayload((byte) VelocityForwarding.MAX_SUPPORTED_FORWARDING_VERSION)
        ));
        ci.cancel();
    }

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    private void bungee$handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        if (!VelocityForwarding.isEnabled() || packet.transactionId() != bungee$velocityLoginMessageId)
            return;

        ci.cancel();

        if (packet.payload() == null) {
            disconnect(Component.literal("This server requires you to connect with Velocity."));
            return;
        }

        VelocityForwarding.VelocityPlayerDataPayload payload = (VelocityForwarding.VelocityPlayerDataPayload) packet.payload();
        var buf = payload.buffer();

        if (!VelocityForwarding.checkIntegrity(buf)) {
            disconnect(Component.literal("Unable to verify player details"));
            return;
        }

        int version = buf.readVarInt();
        if (version > VelocityForwarding.MAX_SUPPORTED_FORWARDING_VERSION) {
            disconnect(Component.literal("Unsupported Velocity forwarding version " + version + " (maximum supported: " + VelocityForwarding.MAX_SUPPORTED_FORWARDING_VERSION + ")"));
            return;
        }

        var playerAddress = VelocityForwarding.readAddress(buf);
        ((ConnectionBridge) connection).bungee$setSpoofedAddress(playerAddress.getHostAddress());

        var profile = VelocityForwarding.createProfile(buf);
        startClientVerification(profile);
    }
}
