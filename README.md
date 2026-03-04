## BungeeForge
BungeeForge is a NeoForge mod that implements BungeeCord legacy forwarding and Velocity modern forwarding.

### Tested target
- Minecraft `1.21.1`
- NeoForge `21.1.219`

### Usage — Legacy BungeeCord / Velocity legacy forwarding
- Put the mod jar into the server `mods/` directory
- In `velocity.toml`, set `player-info-forwarding-mode = "legacy"`
- In backend server `server.properties`, set `online-mode=false`
- Start the NeoForge server and connect through Velocity/Bungee proxy
- Skin and IP forwarding should now work

### Usage — Velocity modern forwarding (recommended)
- Put the mod jar into the server `mods/` directory
- In `velocity.toml`, set `player-info-forwarding-mode = "modern"`
- Copy the `forwarding.secret` file from your Velocity installation to the backend server root, renaming it to `velocity-forwarding.secret`
- In backend server `server.properties`, set `online-mode=false`
- Start the NeoForge server and connect through Velocity
- Player UUID, skin, and IP forwarding are verified with HMAC-SHA256 for security

### Warning for Minecraft 1.13+ networks
Proxy switching on modern versions still requires compatible proxy/backend behavior. If your setup needs registry reset compatibility between switches, install [Ambassador](https://github.com/adde0109/Ambassador) alongside BungeeForge.

### Other versions
If you need support for other NeoForge versions, open an issue with your target Minecraft + NeoForge version.

### Void Proxy
You can also check [Void Proxy](https://github.com/caunt/Void) as an alternative to BungeeCord and Velocity.