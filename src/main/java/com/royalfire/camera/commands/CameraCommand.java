package com.royalfire.camera.commands;

import com.royalfire.camera.SecurityCameraPlugin;
import com.royalfire.camera.managers.CameraManager;
import com.royalfire.camera.models.CameraData;
import com.royalfire.camera.models.ComputerData;
import com.royalfire.camera.models.GroupData;
import com.royalfire.camera.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CameraCommand implements CommandExecutor, TabCompleter {

    private final SecurityCameraPlugin plugin;
    private final CameraManager cameraManager;

    public CameraCommand(SecurityCameraPlugin plugin) {
        this.plugin = plugin;
        this.cameraManager = plugin.getCameraManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "get":
                if (!player.hasPermission("camera.admin")) {
                    player.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /cvtv get <cvtv180|cvtv360|computer>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("cvtv180")) {
                    player.getInventory().addItem(ItemUtils.getCctvItem(plugin, false));
                    player.sendMessage(ChatColor.GREEN + "You received a 180° CVTV Camera!");
                } else if (args[1].equalsIgnoreCase("cvtv360") || args[1].equalsIgnoreCase("cvtv")) {
                    player.getInventory().addItem(ItemUtils.getCctvItem(plugin, true));
                    player.sendMessage(ChatColor.GREEN + "You received a 360° CVTV Camera!");
                } else if (args[1].equalsIgnoreCase("computer")) {
                    player.getInventory().addItem(ItemUtils.getComputerItem(plugin, null));
                    player.sendMessage(ChatColor.GREEN + "You received a Computer!");
                }
                break;

            case "group":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /cvtv group <create|add|remove> <name> [cameraId]");
                    return true;
                }
                String sub = args[1].toLowerCase();
                String groupName = args[2];

                if (sub.equals("create")) {
                    if (cameraManager.getGroup(groupName) != null) {
                        player.sendMessage(ChatColor.RED + "Group already exists.");
                        return true;
                    }
                    
                    if (!player.hasPermission("camera.admin")) {
                        int count = 0;
                        for (GroupData g : cameraManager.getGroups()) {
                            if (g.getOwnerId() != null && g.getOwnerId().equals(player.getUniqueId())) count++;
                        }
                        if (count >= 1) {
                            player.sendMessage(ChatColor.RED + "You can only create 1 group!");
                            return true;
                        }
                    }
                    
                    GroupData group = new GroupData(groupName, player.getUniqueId());
                    cameraManager.addGroup(group);
                    player.sendMessage(ChatColor.GREEN + "Group '" + groupName + "' created.");
                } else if (sub.equals("add")) {
                    if (args.length < 4) {
                        player.sendMessage(ChatColor.RED + "Usage: /cvtv group add <name> <cameraId>");
                        return true;
                    }
                    String camId = args[3];
                    GroupData group = cameraManager.getGroup(groupName);
                    if (group == null) {
                        player.sendMessage(ChatColor.RED + "Group not found.");
                        return true;
                    }
                    
                    if (group.getOwnerId() != null && !group.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("camera.admin")) {
                        player.sendMessage(ChatColor.RED + "You don't own this group!");
                        return true;
                    }
                    
                    CameraData cam = cameraManager.getCamera(camId);
                    if (cam == null) {
                        player.sendMessage(ChatColor.RED + "Camera not found.");
                        return true;
                    }
                    
                    if (cam.getGroupId() != null) {
                        GroupData old = cameraManager.getGroup(cam.getGroupId());
                        if (old != null) old.removeCamera(camId);
                    }
                    
                    group.addCamera(camId);
                    cam.setGroupId(group.getName());
                    cameraManager.saveData();
                    player.sendMessage(ChatColor.GREEN + "Camera " + camId + " added to group " + group.getName() + ".");
                } else if (sub.equals("remove")) {
                    if (args.length < 4) {
                        player.sendMessage(ChatColor.RED + "Usage: /cvtv group remove <name> <cameraId>");
                        return true;
                    }
                    String camId = args[3];
                    GroupData group = cameraManager.getGroup(groupName);
                    if (group == null) {
                        player.sendMessage(ChatColor.RED + "Group not found.");
                        return true;
                    }
                    
                    if (group.getOwnerId() != null && !group.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("camera.admin")) {
                        player.sendMessage(ChatColor.RED + "You don't own this group!");
                        return true;
                    }
                    
                    group.removeCamera(camId);
                    CameraData cam = cameraManager.getCamera(camId);
                    if (cam != null && groupName.equalsIgnoreCase(cam.getGroupId())) {
                        cam.setGroupId(null);
                    }
                    cameraManager.saveData();
                    player.sendMessage(ChatColor.GREEN + "Camera " + camId + " removed from group " + groupName + ".");
                } else if (sub.equals("delete")) {
                    GroupData group = cameraManager.getGroup(groupName);
                    if (group == null) {
                        player.sendMessage(ChatColor.RED + "Group not found.");
                        return true;
                    }
                    
                    if (group.getOwnerId() != null && !group.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("camera.admin")) {
                        player.sendMessage(ChatColor.RED + "You don't own this group!");
                        return true;
                    }
                    
                    cameraManager.removeGroup(groupName);
                    player.sendMessage(ChatColor.GREEN + "Group " + groupName + " deleted.");
                }
                break;
                
            case "linkcomputer":
                if (args.length < 3) return true;
                String compId = args[1];
                String linkGroupName = args[2];
                ComputerData targetComp = cameraManager.getComputer(compId);
                if (targetComp != null) {
                    targetComp.setGroupId(linkGroupName);
                    cameraManager.saveData();
                    player.sendMessage(ChatColor.GREEN + "Computer linked to group " + linkGroupName + ".");
                }
                return true;

            case "view":
                if (args.length < 2) return true;
                String viewCamId = args[1];
                CameraData targetCam = cameraManager.getCamera(viewCamId);
                if (targetCam == null) {
                    player.sendMessage(ChatColor.RED + "Camera not found.");
                    return true;
                }
                plugin.getSessionManager().startSession(player, targetCam);
                player.sendMessage(ChatColor.GREEN + "Viewing camera " + viewCamId + ".");
                break;
                
            case "gui":
                if (!player.hasPermission("camera.admin")) return true;
                plugin.getGuiManager().openAdminGui(player);
                break;

            case "list":
                if (!player.hasPermission("camera.admin")) return true;
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
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.AQUA + "=== Security Camera ===");
        player.sendMessage(ChatColor.YELLOW + "/cvtv get cvtv180|cvtv360" + ChatColor.GRAY + " - Get a CVTV camera");
        player.sendMessage(ChatColor.YELLOW + "/cvtv get computer" + ChatColor.GRAY + " - Get a computer");
        player.sendMessage(ChatColor.YELLOW + "/cvtv group create <name>");
        player.sendMessage(ChatColor.YELLOW + "/cvtv group delete <name>");
        player.sendMessage(ChatColor.YELLOW + "/cvtv group add <name> <cameraId>");
        player.sendMessage(ChatColor.YELLOW + "/cvtv group remove <name> <cameraId>");
        if (player.hasPermission("camera.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/cvtv gui" + ChatColor.GRAY + " - Open Admin GUI");
            player.sendMessage(ChatColor.YELLOW + "/cvtv list" + ChatColor.GRAY + " - List all cameras");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("get");
            completions.add("group");
            if (sender.hasPermission("camera.admin")) {
                completions.add("gui");
                completions.add("list");
                completions.add("view");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("get")) {
                completions.add("cvtv180");
                completions.add("cvtv360");
                completions.add("computer");
            } else if (args[0].equalsIgnoreCase("group")) {
                completions.add("create");
                completions.add("delete");
                completions.add("add");
                completions.add("remove");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("group")) {
                if (sender instanceof Player p) {
                    for (GroupData g : cameraManager.getGroups()) {
                        if (g.getOwnerId() != null && g.getOwnerId().equals(p.getUniqueId()) || p.hasPermission("camera.admin")) {
                            completions.add(g.getName());
                        }
                    }
                }
            }
        }
        
        // Filter by what they typed
        List<String> result = new ArrayList<>();
        for (String c : completions) {
            if (c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                result.add(c);
            }
        }
        return result;
    }
}
