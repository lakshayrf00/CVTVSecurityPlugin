package com.royalfire.cvtv;

import com.royalfire.cvtv.commands.CameraCommand;
import com.royalfire.cvtv.listeners.CameraListener;
import com.royalfire.cvtv.managers.CameraManager;
import com.royalfire.cvtv.managers.SessionManager;
import com.royalfire.cvtv.managers.GuiManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CVTVSecurityPlugin extends JavaPlugin {

    private CameraManager cameraManager;
    private SessionManager sessionManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        cameraManager = new CameraManager(this);
        sessionManager = new SessionManager(this);
        guiManager = new GuiManager(this);

        getCommand("cvtv").setExecutor(new CameraCommand(this));
        getCommand("cvtv").setTabCompleter(new CameraCommand(this));
        getServer().getPluginManager().registerEvents(new CameraListener(this), this);

        getLogger().info("CVTVSecurity plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (sessionManager != null) {
            sessionManager.stopAll();
        }
        if (cameraManager != null) {
            cameraManager.saveData();
        }
        getLogger().info("CVTVSecurity plugin disabled!");
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}
