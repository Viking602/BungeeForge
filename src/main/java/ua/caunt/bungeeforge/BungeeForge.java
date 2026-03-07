package ua.caunt.bungeeforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import ua.caunt.bungeeforge.handler.ProxyFlightSyncHandler;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("bungeeforge")
public class BungeeForge {
    public BungeeForge() {
        NeoForge.EVENT_BUS.register(new ProxyFlightSyncHandler());
    }
}
