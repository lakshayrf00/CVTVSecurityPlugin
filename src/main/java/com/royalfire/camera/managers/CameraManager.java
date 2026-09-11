package com.royalfire.camera.managers;

import com.royalfire.camera.SecurityCameraPlugin;
import com.royalfire.camera.models.CameraData;
import com.royalfire.camera.models.ComputerData;
import com.royalfire.camera.models.GroupData;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CameraManager {
    private final SecurityCameraPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    private final Map<String, CameraData> cameras = new HashMap<>();
    private final Map<String, ComputerData> computers = new HashMap<>();
    private final Map<String, GroupData> groups = new HashMap<>();
    
    private int nextCameraId = 1;
    private int nextComputerId = 1;

    public CameraManager(SecurityCameraPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
    }

    public void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        cameras.clear();
        computers.clear();
        groups.clear();

        // Load groups
        if (dataConfig.contains("groups")) {
            for (String groupName : dataConfig.getConfigurationSection("groups").getKeys(false)) {
                String ownerStr = dataConfig.getString("groups." + groupName + ".ownerId");
                UUID ownerId = ownerStr != null ? UUID.fromString(ownerStr) : null;
                GroupData group = new GroupData(groupName, ownerId);
                if (dataConfig.contains("groups." + groupName + ".cameras")) {
                    for (String camId : dataConfig.getStringList("groups." + groupName + ".cameras")) {
                        group.addCamera(camId);
                    }
                }
                groups.put(groupName.toLowerCase(), group);
            }
        }

        // Load cameras
        if (dataConfig.contains("cameras")) {
            for (String camId : dataConfig.getConfigurationSection("cameras").getKeys(false)) {
                Location loc = dataConfig.getLocation("cameras." + camId + ".location");
                String groupId = dataConfig.getString("cameras." + camId + ".groupId");
                String ownerStr = dataConfig.getString("cameras." + camId + ".ownerId");
                boolean is360 = dataConfig.getBoolean("cameras." + camId + ".is360", true);
                float baseYaw = (float) dataConfig.getDouble("cameras." + camId + ".baseYaw", 0.0);
                UUID ownerId = ownerStr != null ? UUID.fromString(ownerStr) : null;
                if (loc != null) {
                    CameraData cam = new CameraData(camId, loc, groupId, ownerId, is360, baseYaw);
                    cameras.put(camId, cam);
                    int idNum = parseId(camId);
                    if (idNum >= nextCameraId) nextCameraId = idNum + 1;
                }
            }
        }

        // Load computers
        if (dataConfig.contains("computers")) {
            for (String compId : dataConfig.getConfigurationSection("computers").getKeys(false)) {
                Location loc = dataConfig.getLocation("computers." + compId + ".location");
                String groupId = dataConfig.getString("computers." + compId + ".groupId");
                String ownerStr = dataConfig.getString("computers." + compId + ".ownerId");
                UUID ownerId = ownerStr != null ? UUID.fromString(ownerStr) : null;
                if (loc != null) {
                    ComputerData comp = new ComputerData(compId, loc, groupId, ownerId);
                    computers.put(compId, comp);
                    int idNum = parseId(compId);
                    if (idNum >= nextComputerId) nextComputerId = idNum + 1;
                }
            }
        }
    }

    public void saveData() {
        // Save groups
        dataConfig.set("groups", null);
        for (GroupData group : groups.values()) {
            dataConfig.set("groups." + group.getName() + ".cameras", group.getCameraIds().stream().toList());
            if (group.getOwnerId() != null) dataConfig.set("groups." + group.getName() + ".ownerId", group.getOwnerId().toString());
        }

        // Save cameras
        dataConfig.set("cameras", null);
        for (CameraData cam : cameras.values()) {
            dataConfig.set("cameras." + cam.getId() + ".location", cam.getLocation());
            dataConfig.set("cameras." + cam.getId() + ".groupId", cam.getGroupId());
            dataConfig.set("cameras." + cam.getId() + ".is360", cam.is360());
            dataConfig.set("cameras." + cam.getId() + ".baseYaw", cam.getBaseYaw());
            if (cam.getOwnerId() != null) dataConfig.set("cameras." + cam.getId() + ".ownerId", cam.getOwnerId().toString());
        }

        // Save computers
        dataConfig.set("computers", null);
        for (ComputerData comp : computers.values()) {
            dataConfig.set("computers." + comp.getId() + ".location", comp.getLocation());
            dataConfig.set("computers." + comp.getId() + ".groupId", comp.getGroupId());
            if (comp.getOwnerId() != null) dataConfig.set("computers." + comp.getId() + ".ownerId", comp.getOwnerId().toString());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int parseId(String idStr) {
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public CameraData addCamera(Location loc, UUID ownerId, boolean is360, float baseYaw) {
        int idNum = 1;
        while (cameras.containsKey(String.valueOf(idNum))) {
            idNum++;
        }
        String id = String.valueOf(idNum);
        CameraData cam = new CameraData(id, loc, null, ownerId, is360, baseYaw);
        cameras.put(id, cam);
        if (idNum >= nextCameraId) nextCameraId = idNum + 1;
        saveData();
        return cam;
    }

    public CameraData getCameraAt(Location loc) {
        for (CameraData cam : cameras.values()) {
            if (cam.getLocation().equals(loc)) return cam;
        }
        return null;
    }

    public CameraData getCamera(String id) {
        return cameras.get(id);
    }
    
    public java.util.Collection<CameraData> getCameras() {
        return cameras.values();
    }

    public void removeCamera(String id) {
        CameraData cam = cameras.remove(id);
        if (cam != null && cam.getGroupId() != null) {
            GroupData group = groups.get(cam.getGroupId().toLowerCase());
            if (group != null) group.removeCamera(id);
        }
        saveData();
    }

    public ComputerData addComputer(Location loc, String groupId, UUID ownerId) {
        int idNum = 1;
        while (computers.containsKey(String.valueOf(idNum))) {
            idNum++;
        }
        String id = String.valueOf(idNum);
        ComputerData comp = new ComputerData(id, loc, groupId, ownerId);
        computers.put(id, comp);
        if (idNum >= nextComputerId) nextComputerId = idNum + 1;
        saveData();
        return comp;
    }

    public ComputerData getComputerAt(Location loc) {
        for (ComputerData comp : computers.values()) {
            if (comp.getLocation().equals(loc)) return comp;
        }
        return null;
    }

    public ComputerData getComputer(String id) {
        return computers.get(id);
    }

    public void removeComputer(String id) {
        computers.remove(id);
        saveData();
    }

    public void addGroup(GroupData group) {
        groups.put(group.getName().toLowerCase(), group);
        saveData();
    }

    public void removeGroup(String name) {
        groups.remove(name.toLowerCase());
        for (CameraData cam : cameras.values()) {
            if (name.equalsIgnoreCase(cam.getGroupId())) {
                cam.setGroupId(null);
            }
        }
        for (ComputerData comp : computers.values()) {
            if (name.equalsIgnoreCase(comp.getGroupId())) {
                comp.setGroupId(null);
            }
        }
        saveData();
    }

    public GroupData getGroup(String name) {
        return groups.get(name.toLowerCase());
    }

    public Collection<GroupData> getGroups() {
        return groups.values();
    }
}
