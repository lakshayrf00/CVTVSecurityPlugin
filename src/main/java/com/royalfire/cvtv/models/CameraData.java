package com.royalfire.cvtv.models;

import org.bukkit.Location;
import java.util.UUID;

public class CameraData {
    private final String id;
    private final Location location;
    private String groupId;
    private UUID ownerId;
    private boolean is360;
    private float baseYaw;

    public CameraData(String id, Location location, String groupId, UUID ownerId, boolean is360, float baseYaw) {
        this.id = id;
        this.location = location;
        this.groupId = groupId;
        this.ownerId = ownerId;
        this.is360 = is360;
        this.baseYaw = baseYaw;
    }

    public String getId() { return id; }
    public Location getLocation() { return location; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public boolean is360() { return is360; }
    public void set360(boolean is360) { this.is360 = is360; }
    public float getBaseYaw() { return baseYaw; }
    public void setBaseYaw(float baseYaw) { this.baseYaw = baseYaw; }
}
