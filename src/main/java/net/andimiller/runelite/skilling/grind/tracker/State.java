package net.andimiller.runelite.skilling.grind.tracker;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class State {
    @Inject
    private Gson gson;
    @Inject
    private ConfigManager configManager;

    // constants for where we save stuff
    final static String CONFIG_GROUP = "item-grind-tracker";
    final static String COUNTERS_KEY = "counters";
    final static String GOALS_KEY = "goals";


    // counters
    public void saveCounters(HashMap<Integer, HashMap<Integer, Integer>> counters) {
        configManager.setConfiguration(CONFIG_GROUP, COUNTERS_KEY, gson.toJson(counters));
    }

    public HashMap<Integer, HashMap<Integer, Integer>> loadCounters() {
                try {
                    return Optional.<HashMap<Integer, HashMap<Integer, Integer>>>ofNullable(gson.fromJson(
                            configManager.getConfiguration(CONFIG_GROUP, COUNTERS_KEY),
                            new TypeToken<HashMap<Integer, HashMap<Integer, Integer>>>() {
                            }.getType()
                    )).get();
                } catch (Exception e) {
                    return new HashMap<>();
                }
    }

    // goals
    public void saveGoals(ArrayList<Goal> goals) {
        configManager.setConfiguration(CONFIG_GROUP, GOALS_KEY, gson.toJson(goals));
    }

    public ArrayList<Goal> loadGoals() {
        try {
            return Optional.<ArrayList<Goal>>ofNullable(gson.fromJson(
                    configManager.getConfiguration(CONFIG_GROUP, GOALS_KEY),
                    new TypeToken<ArrayList<Goal>>() {}.getType()
            )).get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

}
