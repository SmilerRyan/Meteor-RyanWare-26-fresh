package com.example.addon;

import com.example.addon.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Example");

    @Override
    public void onInitialize() {    
        Modules.get().add(new AutoRespawn());
        Modules.get().add(new AutoWalkForwards());
        Modules.get().add(new ChatLogger());
        Modules.get().add(new ChatPlinger());
        Modules.get().add(new Flight());
        Modules.get().add(new ForceColoredChat());
        Modules.get().add(new ForceOpenTab());
        Modules.get().add(new HideItemFrameMaps());
        Modules.get().add(new PlayerList());
        Modules.get().add(new TabCompletePrivacy());
        Modules.get().add(new TabLogger());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}
