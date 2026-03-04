## BungeeForge
BungeeForge is a NeoForge mod that implements BungeeCord legacy forwarding.
It was originally built for Velocity legacy forwarding and also works with other Bungee-compatible proxies.

### Tested target
- Minecraft `1.21.1`
- NeoForge `21.1.219`

### Usage
- Put the mod jar into the server `mods/` directory
- In `velocity.toml`, set `player-info-forwarding-mode = "legacy"`
- In backend server `server.properties`, set `online-mode=false`
- Start the NeoForge server and connect through Velocity/Bungee proxy
- Skin and IP forwarding should now work

### Warning for Minecraft 1.13+ networks
Proxy switching on modern versions still requires compatible proxy/backend behavior. If your setup needs registry reset compatibility between switches, install [Ambassador](https://github.com/adde0109/Ambassador) alongside BungeeForge.

### Other versions
If you need support for other NeoForge versions, open an issue with your target Minecraft + NeoForge version.

### Void Proxy
You can also check [Void Proxy](https://github.com/caunt/Void) as an alternative to BungeeCord and Velocity.