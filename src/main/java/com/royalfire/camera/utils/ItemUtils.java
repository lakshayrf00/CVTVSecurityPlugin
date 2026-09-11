package com.royalfire.camera.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemUtils {

    public static final String CCTV_TEXTURE_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmE2ZTU5NzIzN2YyMjA5MjY4Y2RiOWRiZTM2ZTZmNjMzNjA2NjJjNTk1ZTZiYWUwYzJhNjRkZTBkYzI0NDMzOSJ9fX0=";
    public static final UUID CCTV_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    public static ItemStack getCctvItem(Plugin plugin, boolean is360) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            String typeStr = is360 ? "360°" : "180°";
            meta.setDisplayName(ChatColor.DARK_GRAY + "CCTV Camera (" + typeStr + ")");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Place this to set up a camera.");
            lore.add(ChatColor.YELLOW + "Type: " + ChatColor.AQUA + typeStr);
            meta.setLore(lore);

            // Paper API method with random UUID to defeat client-side caching of broken skins
            com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "CCTV");
            profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", CCTV_TEXTURE_BASE64));
            meta.setPlayerProfile(profile);

            // Mark as custom item
            NamespacedKey key = new NamespacedKey(plugin, "is_cctv");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            
            NamespacedKey typeKey = new NamespacedKey(plugin, "cctv_type");
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, is360 ? "360" : "180");

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getComputerItem(Plugin plugin, String linkedGroup) {
        ItemStack item = new ItemStack(Material.POLISHED_BLACKSTONE_STAIRS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.BLUE + "Computer");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Place this to set up a viewing computer.");
            if (linkedGroup != null && !linkedGroup.isEmpty()) {
                lore.add(ChatColor.GREEN + "Linked to: " + ChatColor.YELLOW + linkedGroup);
                NamespacedKey groupKey = new NamespacedKey(plugin, "computer_group");
                meta.getPersistentDataContainer().set(groupKey, PersistentDataType.STRING, linkedGroup);
            }
            meta.setLore(lore);

            // Mark as custom item
            NamespacedKey key = new NamespacedKey(plugin, "is_computer");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getExitBarrier(Plugin plugin) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Exit Camera View");
            NamespacedKey key = new NamespacedKey(plugin, "is_exit_barrier");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}
