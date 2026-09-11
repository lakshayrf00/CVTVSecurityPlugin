package com.royalfire.cvtv.managers;

import com.royalfire.cvtv.CVTVSecurityPlugin;
import com.royalfire.cvtv.models.CameraData;
import com.royalfire.cvtv.models.ComputerData;
import com.royalfire.cvtv.models.GroupData;
import com.royalfire.cvtv.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {

    private final CVTVSecurityPlugin plugin;

    public GuiManager(CVTVSecurityPlugin plugin) {
        this.plugin = plugin;
    }

    public void openComputerGui(Player player, ComputerData comp) {
        if (comp.getGroupId() == null || comp.getGroupId().isEmpty()) {
            openGroupSelectionGui(player, comp);
        } else {
            openCameraListGui(player, comp);
        }
    }

    public void openGroupSelectionGui(Player player, ComputerData comp) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Select Group to Link");
        
        int slot = 0;
        for (GroupData group : plugin.getCameraManager().getGroups()) {
            if (group.getOwnerId() != null && !group.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("camera.admin")) {
                continue;
            }
            if (slot >= 27) break;
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + group.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Cameras in group: " + ChatColor.GREEN + group.getCameraIds().size());
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to link this computer.");
            meta.setLore(lore);
            
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "link_group");
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "group_name"), PersistentDataType.STRING, group.getName());
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "comp_id"), PersistentDataType.STRING, comp.getId());
            
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        
        player.openInventory(inv);
    }

    public void openCameraListGui(Player player, ComputerData comp) {
        GroupData group = plugin.getCameraManager().getGroup(comp.getGroupId());
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Linked group not found.");
            comp.setGroupId(null);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Cameras: " + group.getName());
        
        int slot = 0;
        for (String camId : group.getCameraIds()) {
            if (slot >= 53) break;
            CameraData cam = plugin.getCameraManager().getCamera(camId);
            if (cam != null) {
                ItemStack item = ItemUtils.getCctvItem(plugin, cam.is360());
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "Camera #" + cam.getId());
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Location: " + cam.getLocation().getBlockX() + ", " + cam.getLocation().getBlockY() + ", " + cam.getLocation().getBlockZ());
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to view camera.");
                meta.setLore(lore);
                
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "view_cam");
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "cam_id"), PersistentDataType.STRING, cam.getId());
                
                item.setItemMeta(meta);
                inv.setItem(slot++, item);
            }
        }
        
        // Unlink button
        ItemStack unlink = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta unlinkMeta = unlink.getItemMeta();
        unlinkMeta.setDisplayName(ChatColor.RED + "Unlink Computer");
        unlinkMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "unlink_comp");
        unlinkMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "comp_id"), PersistentDataType.STRING, comp.getId());
        unlink.setItemMeta(unlinkMeta);
        
        inv.setItem(53, unlink);
        
        player.openInventory(inv);
    }

    public void openAdminGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Camera Admin Panel");
        
        ItemStack createGroup = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta cm = createGroup.getItemMeta();
        cm.setDisplayName(ChatColor.YELLOW + "View All Groups");
        List<String> cLore = new ArrayList<>();
        cLore.add(ChatColor.GRAY + "Click to see all groups");
        cm.setLore(cLore);
        cm.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "admin_view_groups");
        createGroup.setItemMeta(cm);
        
        inv.setItem(11, createGroup);
        
        ItemStack listCams = new ItemStack(Material.COMPASS);
        ItemMeta lm = listCams.getItemMeta();
        lm.setDisplayName(ChatColor.GREEN + "List Cameras in Chat");
        lm.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "admin_list");
        listCams.setItemMeta(lm);
        
        inv.setItem(15, listCams);
        
        player.openInventory(inv);
    }
    
    public void openAdminGroupViewer(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_RED + "All Server Groups");
        int slot = 0;
        for (GroupData group : plugin.getCameraManager().getGroups()) {
            if (slot >= 54) break;
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + group.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Cameras: " + ChatColor.GREEN + group.getCameraIds().size());
            lore.add(ChatColor.GRAY + "Owner ID: " + ChatColor.AQUA + (group.getOwnerId() != null ? group.getOwnerId().toString() : "None"));
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to view cameras in group.");
            meta.setLore(lore);
            
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "admin_view_cams");
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "group_name"), PersistentDataType.STRING, group.getName());
            
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        player.openInventory(inv);
    }
    
    public void openAdminCameraViewer(Player player, GroupData group) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_RED + "Cameras: " + group.getName());
        int slot = 0;
        for (String camId : group.getCameraIds()) {
            if (slot >= 54) break;
            CameraData cam = plugin.getCameraManager().getCamera(camId);
            if (cam == null) continue;
            
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "Camera #" + camId);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.YELLOW + (cam.is360() ? "360°" : "180°"));
            lore.add(ChatColor.GRAY + "Location: " + cam.getLocation().getBlockX() + ", " + cam.getLocation().getBlockY() + ", " + cam.getLocation().getBlockZ());
            lore.add("");
            lore.add(ChatColor.AQUA + "Click to view this camera.");
            meta.setLore(lore);
            
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "view_cam");
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "cam_id"), PersistentDataType.STRING, camId);
            
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        player.openInventory(inv);
    }
}
