package ua.caunt.bungeeforge.mixin.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.caunt.bungeeforge.velocity.VelocityForwarding;

@Mixin(ServerboundCustomQueryAnswerPacket.class)
public abstract class ServerboundCustomQueryAnswerPacketMixin {

    @Shadow
    @Final
    private static int MAX_PAYLOAD_SIZE;

    @Inject(method = "readUnknownPayload", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bungee$readVelocityPayload(FriendlyByteBuf pBuffer, CallbackInfoReturnable<CustomQueryAnswerPayload> cir) {
        if (!VelocityForwarding.isEnabled()) return;

        FriendlyByteBuf buffer = pBuffer.readNullable(buf -> {
            int size = buf.readableBytes();
            if (size >= 0 && size <= MAX_PAYLOAD_SIZE) {
                return new FriendlyByteBuf(buf.readBytes(size));
            }
            throw new IllegalArgumentException("Payload may not be larger than " + MAX_PAYLOAD_SIZE + " bytes");
        });

        cir.setReturnValue(buffer == null ? null : new VelocityForwarding.VelocityPlayerDataPayload(buffer));
        cir.cancel();
    }
}
