package com.royalfire.camera.listeners;

import com.royalfire.camera.SecurityCameraPlugin;
import com.royalfire.camera.managers.CameraManager;
import com.royalfire.camera.managers.SessionManager;
import com.royalfire.camera.models.CameraData;
import com.royalfire.camera.models.ComputerData;
import com.royalfire.camera.models.GroupData;
import com.royalfire.camera.utils.ItemUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class CameraListener implements Listener {

    private final SecurityCameraPlugin plugin;
    private final CameraManager cameraManager;
    private final SessionManager sessionManager;

    public CameraListener(SecurityCameraPlugin plugin) {
        this.plugin = plugin;
        this.cameraManager = plugin.getCameraManager();
        this.sessionManager = plugin.getSessionManager();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getItemMeta() == null) return;
        ItemMeta meta = item.getItemMeta();

        NamespacedKey computerKey = new NamespacedKey(plugin, "is_computer");

        if (meta.getPersistentDataContainer().has(computerKey, PersistentDataType.BYTE)) {
            NamespacedKey groupKey = new NamespacedKey(plugin, "computer_group");
            String group = meta.getPersistentDataContainer().get(groupKey, PersistentDataType.STRING);
            
            ComputerData comp = cameraManager.addComputer(event.getBlock().getLocation(), group, event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage(ChatColor.GREEN + "Computer placed! ID: " + ChatColor.YELLOW + comp.getId());
            event.getPlayer().playSound(event.getBlock().getLocation(), org.bukkit.Sound.BLOCK_WOOD_PLACE, 1f, 1f);
            if (group != null) {
                event.getPlayer().sendMessage(ChatColor.GRAY + "Auto-linked to group: " + ChatColor.YELLOW + group);
            }
        }
        
        NamespacedKey cctvKey = new NamespacedKey(plugin, "is_cctv");
        if (meta.getPersistentDataContainer().has(cctvKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractPlaceCamera(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            ItemStack item = event.getItem();
            if (item != null && item.hasItemMeta()) {
                NamespacedKey cctvKey = new NamespacedKey(plugin, "is_cctv");
                if (item.getItemMeta().getPersistentDataContainer().has(cctvKey, PersistentDataType.BYTE)) {
                    event.setCancelled(true);
                    
                    Block clicked = event.getClickedBlock();
                    org.bukkit.block.BlockFace face = event.getBlockFace();
                    org.bukkit.Location loc = clicked.getRelative(face).getLocation().add(0.5, 0, 0.5);
                    
                    // Check if a camera already exists here
                    if (cameraManager.getCameraAt(loc) != null) {
                        return; // Prevent duplicate placements
                    }
                    
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        event.getPlayer().getInventory().setItemInHand(null);
                    }
                    
                    org.bukkit.Location spawnLoc = loc.clone().add(0, -1.4, 0);
                    
                    float yaw = 0;
                    switch (face) {
                        case NORTH: yaw = 180; break;
                        case SOUTH: yaw = 0; break;
                        case WEST: yaw = 90; break;
                        case EAST: yaw = -90; break;
                        default: yaw = event.getPlayer().getLocation().getYaw(); break;
                    }
                    spawnLoc.setYaw(yaw);
                    
                    boolean is360 = true;
                    if (item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "cctv_type"), PersistentDataType.STRING)) {
                        String typeStr = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "cctv_type"), PersistentDataType.STRING);
                        is360 = "360".equals(typeStr);
                    }
                    
                    org.bukkit.entity.ArmorStand camEntity = (org.bukkit.entity.ArmorStand) loc.getWorld().spawnEntity(spawnLoc, org.bukkit.entity.EntityType.ARMOR_STAND);
                    camEntity.setVisible(false);
                    camEntity.setGravity(false);
                    camEntity.getEquipment().setHelmet(com.royalfire.camera.utils.ItemUtils.getCctvItem(plugin, is360));
                    camEntity.getPersistentDataContainer().set(cctvKey, PersistentDataType.BYTE, (byte) 1);
                    
                    CameraData cam = cameraManager.addCamera(loc, event.getPlayer().getUniqueId(), is360, yaw);
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Camera placed! ID: " + ChatColor.YELLOW + cam.getId());
                    event.getPlayer().playSound(loc, org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 2f);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        
        ComputerData comp = cameraManager.getComputerAt(block.getLocation());
        if (comp != null) {
            if (comp.getOwnerId() != null && !comp.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("camera.admin")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You don't own this computer!");
                return;
            }
            cameraManager.removeComputer(comp.getId());
            event.getPlayer().sendMessage(ChatColor.RED + "Computer removed.");
            event.setDropItems(false);
            block.getWorld().dropItemNaturally(block.getLocation(), ItemUtils.getComputerItem(plugin, comp.getGroupId()));
            return;
        }
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.ArmorStand stand) {
            if (stand.getPersistentDataContainer().has(new NamespacedKey(plugin, "is_cctv"), PersistentDataType.BYTE)) {
                event.setCancelled(true); // Always cancel to prevent breaking by non-players or dropping armor stand item
                
                if (event.getDamager() instanceof Player player) {
                    org.bukkit.Location loc = stand.getLocation().clone().add(0, 1.4, 0); // Reconstruct original block location center
                    loc.setYaw(0); loc.setPitch(0); // CameraData locations don't have yaw/pitch
                    
                    CameraData cam = null;
                    // Find closest camera data since floats might not match perfectly
                    for (CameraData c : plugin.getCameraManager().getCameras()) {
                        if (c.getLocation().distance(loc) < 1.0) {
                            cam = c;
                            break;
                        }
                    }
                    
                    if (cam != null) {
                        if (cam.getOwnerId() != null && !cam.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("camera.admin")) {
                            player.sendMessage(ChatColor.RED + "You don't own this camera!");
                            return;
                        }
                        cameraManager.removeCamera(cam.getId());
                        player.sendMessage(ChatColor.RED + "Camera " + cam.getId() + " removed.");
                        stand.getWorld().dropItemNaturally(stand.getLocation().add(0, 1.4, 0), ItemUtils.getCctvItem(plugin, cam.is360()));
                    } else {
                        stand.getWorld().dropItemNaturally(stand.getLocation().add(0, 1.4, 0), ItemUtils.getCctvItem(plugin, true));
                    }
                    stand.remove();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (sessionManager.isViewing(player)) {
                event.setCancelled(true); // Prevent damage while in camera
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        if (sessionManager.isViewing(event.getPlayer())) {
            event.setCancelled(true); // Prevent dismount from sneak
        }
    }
    
    @EventHandler
    public void onEntityDismount(org.bukkit.event.entity.EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (sessionManager.isViewing(player)) {
                event.setCancelled(true); // Force them to stay on the armor stand
            }
        }
    }

    @EventHandler
    public void onArmorStandInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof org.bukkit.entity.ArmorStand stand) {
            if (stand.getPersistentDataContainer().has(new NamespacedKey(plugin, "is_cctv"), PersistentDataType.BYTE)) {
                event.setCancelled(true); // Prevent stealing the camera head
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (sessionManager.isViewing(player)) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.SPYGLASS) {
                return; // Let them use spyglass
            }
            
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (item != null && item.getType() == Material.BARRIER) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "is_exit_barrier"), PersistentDataType.BYTE)) {
                        sessionManager.endSession(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "Exited camera view.");
                    }
                }
            }
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            ComputerData comp = cameraManager.getComputerAt(block.getLocation());
            
            if (comp != null) {
                event.setCancelled(true);
                if (comp.getOwnerId() != null && !comp.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("camera.admin")) {
                    player.sendMessage(ChatColor.RED + "Only the owner can use this computer.");
                    return;
                }
                plugin.getGuiManager().openComputerGui(player, comp);
            }
        }
    }

    private void handleComputerClick(Player player, ComputerData comp) {
        plugin.getGuiManager().openComputerGui(player, comp);
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
        
        Player player = (Player) event.getWhoClicked();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");
        String action = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        
        if (action != null) {
            event.setCancelled(true);
            
            if (action.equals("link_group")) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                String groupName = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "group_name"), PersistentDataType.STRING);
                String compId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "comp_id"), PersistentDataType.STRING);
                
                ComputerData comp = cameraManager.getComputer(compId);
                if (comp != null && groupName != null) {
                    comp.setGroupId(groupName);
                    cameraManager.saveData();
                    player.sendMessage(ChatColor.GREEN + "Computer linked to group " + groupName + ".");
                    player.closeInventory();
                    plugin.getGuiManager().openComputerGui(player, comp); // Reopen to show cameras
                }
            } else if (action.equals("view_cam")) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                String camId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "cam_id"), PersistentDataType.STRING);
                CameraData targetCam = cameraManager.getCamera(camId);
                if (targetCam != null) {
                    player.closeInventory();
                    plugin.getSessionManager().startSession(player, targetCam);
                    player.sendMessage(ChatColor.GREEN + "Viewing camera " + camId + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "Camera not found.");
                }
            } else if (action.equals("unlink_comp")) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                String compId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "comp_id"), PersistentDataType.STRING);
                ComputerData comp = cameraManager.getComputer(compId);
                if (comp != null) {
                    comp.setGroupId(null);
                    cameraManager.saveData();
                    player.sendMessage(ChatColor.YELLOW + "Computer unlinked.");
                    player.closeInventory();
                    plugin.getGuiManager().openComputerGui(player, comp); // Reopen to show group selection
                }
            } else if (action.equals("admin_list")) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                player.sendMessage(ChatColor.YELLOW + "Camera List:");
                for (GroupData group : cameraManager.getGroups()) {
                    player.sendMessage(ChatColor.GRAY + "- Group: " + ChatColor.AQUA + group.getName());
                    for (String camId : group.getCameraIds()) {
                        CameraData cam = cameraManager.getCamera(camId);
                        if (cam != null) {
                            player.sendMessage(ChatColor.WHITE + "  -> Camera " + camId + " at " + cam.getLocation().getBlockX() + ", " + cam.getLocation().getBlockY() + ", " + cam.getLocation().getBlockZ());
                        }
                    }
                }
                player.closeInventory();
            } else if (action.equals("admin_view_groups")) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getGuiManager().openAdminGroupViewer(player);
            } else if (action.equals("admin_view_cams")) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                String groupName = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "group_name"), PersistentDataType.STRING);
                GroupData group = cameraManager.getGroup(groupName);
                if (group != null) {
                    plugin.getGuiManager().openAdminCameraViewer(player, group);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (sessionManager.isViewing(event.getPlayer())) {
            sessionManager.endSession(event.getPlayer().getUniqueId());
        }
    }
}
