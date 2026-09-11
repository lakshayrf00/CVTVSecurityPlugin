package com.royalfire.camera.managers;

import com.royalfire.camera.SecurityCameraPlugin;
import com.royalfire.camera.models.CameraData;
import com.royalfire.camera.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {
    private final SecurityCameraPlugin plugin;
    
    private final Map<UUID, CameraSession> activeSessions = new HashMap<>();
    private BukkitTask updateTask;

    public SessionManager(SecurityCameraPlugin plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, CameraSession> entry : activeSessions.entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null && p.isOnline()) {
                        entry.getValue().updateRotation(p);
                    } else {
                        endSession(entry.getKey());
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void startSession(Player player, CameraData camera) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            endSession(player.getUniqueId());
        }

        CameraSession session = new CameraSession(player, camera, plugin);
        activeSessions.put(player.getUniqueId(), session);
        session.start();
    }

    public void endSession(UUID playerId) {
        CameraSession session = activeSessions.remove(playerId);
        if (session != null) {
            session.end();
        }
    }

    public boolean isViewing(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
    
    public void stopAll() {
        if (updateTask != null) updateTask.cancel();
        for (CameraSession session : activeSessions.values()) {
            session.end();
        }
        activeSessions.clear();
    }

    private class CameraSession {
        private final UUID playerId;
        private final Location originalLoc;
        private final GameMode originalGameMode;
        private final ItemStack[] originalInventory;
        private final CameraData camera;
        private final SecurityCameraPlugin plugin;
        
        private ArmorStand visualStand;
        private ArmorStand seatStand;

        public CameraSession(Player player, CameraData camera, SecurityCameraPlugin plugin) {
            this.playerId = player.getUniqueId();
            this.originalLoc = player.getLocation().clone();
            this.originalGameMode = player.getGameMode();
            this.originalInventory = player.getInventory().getContents().clone();
            this.camera = camera;
            this.plugin = plugin;
        }

        public void start() {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) return;

            Location camLoc = camera.getLocation().clone();
            
            // Find existing visualStand
            for (org.bukkit.entity.Entity entity : camLoc.getWorld().getNearbyEntities(camLoc, 1, 2, 1)) {
                if (entity instanceof ArmorStand stand && stand.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "is_cctv"), org.bukkit.persistence.PersistentDataType.BYTE)) {
                    visualStand = stand;
                    break;
                }
            }
            
            // Check if missing
            if (visualStand == null || !visualStand.isValid()) {
                player.sendMessage(ChatColor.RED + "ERROR: Camera connection lost. The physical camera is missing or destroyed!");
                plugin.getCameraManager().removeCamera(camera.getId());
                plugin.getSessionManager().endSession(playerId);
                return;
            }
            
            // Spawn seat stand
            seatStand = (ArmorStand) camLoc.getWorld().spawnEntity(camLoc.clone().add(0, -1.7, 0), EntityType.ARMOR_STAND);
            seatStand.setVisible(false);
            seatStand.setGravity(false);
            
            player.teleport(seatStand.getLocation());
            seatStand.addPassenger(player);
            
            // Hide the head from the viewer so it doesn't block first-person view
            player.hideEntity(plugin, visualStand);
            
            // Hide the player from everyone else so they don't see floating items
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) {
                    p.hidePlayer(plugin, player);
                }
            }

            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            player.getInventory().setItem(4, new ItemStack(Material.SPYGLASS)); // Zoom tool
            player.getInventory().setItem(8, ItemUtils.getExitBarrier(plugin));
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 5, false, false, false)); // Slow rotation
            
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }

        public void updateRotation(Player player) {
            if (visualStand != null && visualStand.isValid()) {
                float pitch = player.getLocation().getPitch();
                float yaw = player.getLocation().getYaw();
                
                if (!camera.is360()) {
                    float baseYaw = camera.getBaseYaw();
                    float diff = yaw - baseYaw;
                    while (diff < -180) diff += 360;
                    while (diff > 180) diff -= 360;
                    
                    boolean clamped = false;
                    if (diff > 90) {
                        yaw = baseYaw + 90;
                        clamped = true;
                    } else if (diff < -90) {
                        yaw = baseYaw - 90;
                        clamped = true;
                    }
                    
                    if (clamped) {
                        player.setRotation(yaw, pitch);
                    }
                }
                
                visualStand.setRotation(yaw, 0);
                visualStand.setHeadPose(new EulerAngle(Math.toRadians(pitch), 0, 0));
                
                // Active Camera Particle
                if (Math.random() < 0.2) {
                    camera.getLocation().getWorld().spawnParticle(org.bukkit.Particle.DUST, camera.getLocation().clone().add(0.5, 0.5, 0.5), 1, 0, 0, 0, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.0F));
                }
                
                // Show action bar
                String symbol = (System.currentTimeMillis() / 500) % 2 == 0 ? "§c●" : "§7●";
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§8[ " + symbol + " §4REC §8] §7| §bCamera #" + camera.getId() + " §7| §eRight-Click Barrier to Exit"));
            }
        }

        public void end() {
            Player player = Bukkit.getPlayer(playerId);
            
            // WE NO LONGER REMOVE VISUAL STAND, it is persistent!
            if (seatStand != null) seatStand.remove();
            
            if (player != null && player.isOnline()) {
                if (visualStand != null && visualStand.isValid()) {
                    player.showEntity(plugin, visualStand); // Show it back to the player!
                }
                
                // Unhide the player
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(player)) {
                        p.showPlayer(plugin, player);
                    }
                }
                
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.setGameMode(originalGameMode);
                player.getInventory().setContents(originalInventory);
                player.teleport(originalLoc);
                player.playSound(originalLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        }
    }
}
