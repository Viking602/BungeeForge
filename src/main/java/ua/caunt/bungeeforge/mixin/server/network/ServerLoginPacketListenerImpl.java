package ua.caunt.bungeeforge.mixin.server.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ua.caunt.bungeeforge.bridge.network.ConnectionBridge;
import ua.caunt.bungeeforge.bridge.server.network.ServerLoginPacketListenerImplBridge;

import java.util.Arrays;
import java.util.regex.Pattern;

@Mixin(net.minecraft.server.network.ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImpl implements ServerLoginPacketListenerImplBridge {
    @Final
    @Shadow
    Connection connection;

    @Unique
    private static final Pattern PROP_PATTERN = Pattern.compile("\\w{0,16}");

    /**
     * Replaces the GameProfile argument at the HEAD of startClientVerification so that
     * both the authenticatedProfile field assignment AND any NeoForge events fired
     * within the method (e.g. PlayerNegotiationEvent) see the real proxied UUID.
     *
     * The previous @Redirect-on-PUTFIELD approach only changed this.authenticatedProfile
     * but left the method-local `profile` variable pointing to the original offline
     * profile. LuckPerms (and similar mods) subscribe to PlayerNegotiationEvent whose
     * GameProfile is built from the local variable, so they loaded user data for the
     * offline UUID while the player entity was later placed with the real UUID —
     * causing "Capability has not been initialised" at placeNewPlayer time.
     */
    @ModifyVariable(method = "startClientVerification(Lcom/mojang/authlib/GameProfile;)V", at = @At("HEAD"), argsOnly = true, remap = false)
    private GameProfile bungee$interceptProfile(GameProfile profile) {
        var connectionBridge = (ConnectionBridge) bungee$getConnection();

        if (!connectionBridge.bungee$hasSpoofedProfile()) {
            return profile;
        }

        var gameProfile = new GameProfile(connectionBridge.bungee$getSpoofedId().get(), profile.getName());
        var properties = gameProfile.getProperties();

        Arrays.stream(connectionBridge.bungee$getSpoofedProperties().get())
                .filter(property -> PROP_PATTERN.matcher(property.name()).matches())
                .forEach(property -> properties.put(property.name(), property));

        return gameProfile;
    }

    @Override
    public Connection bungee$getConnection() {
        return connection;
    }
}
