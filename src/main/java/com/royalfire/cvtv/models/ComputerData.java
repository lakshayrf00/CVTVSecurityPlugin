package com.royalfire.cvtv.models;

import org.bukkit.Location;
import java.util.UUID;

public class ComputerData {
    private final String id;
    private final Location location;
    private String groupId;
    private UUID ownerId;

    public ComputerData(String id, Location location, String groupId, UUID ownerId) {
        this.id = id;
        this.location = location;
        this.groupId = groupId;
        this.ownerId = ownerId;
    }

    public String getId() { return id; }
    public Location getLocation() { return location; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
}
