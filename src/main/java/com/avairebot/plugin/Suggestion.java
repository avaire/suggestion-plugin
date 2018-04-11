package com.avairebot.plugin;

import com.avairebot.plugin.utility.SuggestCommand;

public class Suggestion extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        registerCommand(new SuggestCommand(this));
    }
}
