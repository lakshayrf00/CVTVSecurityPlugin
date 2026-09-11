# CBTB Security Plugin

A highly advanced and optimized Security Camera and CCTV management plugin for Minecraft servers (Paper/Spigot 1.20+). Built professionally to allow players to monitor their bases with highly configurable 180° and 360° cameras, interactive computer terminals, and a secure group-based permission system.

## 🌟 Features

*   **Real-time CCTV Viewing:** Players can seamlessly view through physical cameras placed in the world.
*   **Dual Camera Types:** 
    *   **180° Cameras:** Restricted field of view. The camera head physically turns and the player's view is hard-clamped to prevent looking behind.
    *   **360° Cameras:** Full freedom of rotation to survey the entire surrounding area.
*   **Immersive Experience:** Includes zooming capabilities (spyglass), night vision, slowness for realistic camera turning speed, and a custom action bar showing `[● REC]`.
*   **Computer Terminals:** Link computers to camera groups via an interactive GUI. Watch multiple cameras from a single secure block.
*   **Secure Group System:**
    *   Normal players are restricted to **1 group** limit.
    *   Cameras and Computers can only be destroyed by their respective **Owners** or Server Admins.
    *   Complete privacy: Players can only view and manage their own groups.
*   **Admin Tools:** Comprehensive admin GUI to oversee all server groups, cameras, and force-delete broken cameras.
*   **Highly Optimized:** Built natively with Bukkit/Paper APIs utilizing custom block models (ArmorStands with Head poses) avoiding heavy NMS or lagging entity ticking. Uses smart caching and intelligent chunk loading logic.

## 📥 Commands & Permissions

*   `/cctv get cctv180` - Get a 180-degree camera.
*   `/cctv get cctv360` - Get a 360-degree camera.
*   `/cctv get computer` - Get a computer terminal.
*   `/cctv group create <name>` - Create a new camera group.
*   `/cctv group add <name> <cameraId>` - Add a camera to a group.
*   `/cctv group remove <name> <cameraId>` - Remove a camera from a group.
*   `/cctv admin` - Open the server-wide Admin management GUI.

**Permissions:**
*   `camera.admin` - Bypass all limits, destroy any camera/computer, access the admin GUI, and spawn items.

## ⚙️ Installation

1. Download the latest `CBTBSecurityPlugin.jar` release.
2. Place it into your server's `plugins/` directory.
3. Restart your server.
4. Provide `camera.admin` permission to administrators.

## 🛠️ Build from Source

This project uses Gradle. To build the plugin yourself:

```bash
git clone https://github.com/yourusername/CBTBSecurityPlugin.git
cd CBTBSecurityPlugin
gradle clean build
```
The compiled jar will be located in `build/libs/`.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
