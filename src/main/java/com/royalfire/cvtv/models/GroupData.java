package com.royalfire.cvtv.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupData {
    private final String name;
    private final List<String> cameraIds;
    private final UUID ownerId;

    public GroupData(String name, UUID ownerId) {
        this.name = name;
        this.cameraIds = new ArrayList<>();
        this.ownerId = ownerId;
    }

    public String getName() { return name; }
    public List<String> getCameraIds() { return cameraIds; }
    public UUID getOwnerId() { return ownerId; }
    public void addCamera(String id) { cameraIds.add(id); }
    public void removeCamera(String id) { cameraIds.remove(id); }
}
