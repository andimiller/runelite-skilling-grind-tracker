package net.andimiller.runelite.skilling.grind.tracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Skilling Grind Tracker Plugin"
)
public class GrindTrackerPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private GrindTrackerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Gson gson;

    final static String CONFIG_GROUP = "skilling-grind-tracker";
    final static String COUNTERS_KEY = "counters";

    private GoalsOverlay overlay;

    private final Set<Integer> myInventories = Set.of(InventoryID.INV, InventoryID.BANK);

    // Plugin state for keeping track of the items we've seen
    // The outer Integer is the ItemId we're tracking
    // The inner Integer is the container id, this might be the user's inventory, or their bank
    HashMap<Integer, HashMap<Integer, Integer>> counters;

    // Plugin state for our goals
    List<Goal> goals;

    // Cache of names for all the things we're tracking
    HashMap<Integer, String> names;

    // boolean to flag if we're currently using a deposit box
    boolean depositing = false;
    // and some state to store with it for detecting changes
    Optional<HashMap<Integer, HashMap<Integer, Integer>>> preDepositCounters = Optional.empty();


    void loadFromConfig() {
        if (counters == null) {
            try {
                counters = gson.fromJson(configManager.getConfiguration(CONFIG_GROUP, COUNTERS_KEY), new TypeToken<HashMap<Integer, HashMap<Integer, Integer>>>() {
                }.getType());
            } catch (Exception e) {
                log.warn("Unable to deserialize a cached counter", e);
            }
        }
        if (counters == null) {
            counters = new HashMap<>();
        }
        try {
            goals = GoalParser.parseGoals(config.goals());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (names == null) {
            names = new HashMap<>();
        }

        if (overlay != null) {
            overlay.updateGoals(goals);
        }
    }

    @Override
    protected void startUp() throws Exception {
        loadFromConfig();
        // set up overlay
        overlay = new GoalsOverlay(counters, goals, names);
        overlay.setPreferredPosition(OverlayPosition.TOP_LEFT);
        overlayManager.add(overlay);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        loadFromConfig();
    }

    protected void saveCounters() {
        configManager.setConfiguration(CONFIG_GROUP, COUNTERS_KEY, gson.toJson(counters));
    }

    @Override
    protected void shutDown() throws Exception {
        saveCounters();
        overlayManager.remove(overlay);
    }

    private void syncInventory(int inventoryId) {
        ItemContainer container = client.getItemContainer(inventoryId);
        Set<Integer> ids = goals.stream().flatMap(g -> g.getTypes().stream()).collect(Collectors.toSet());

        for (Integer itemId : ids) {
            // update the name if we haven't tracked it
            names.putIfAbsent(itemId, names.computeIfAbsent(itemId, id -> itemManager.getItemComposition(id).getName()));

            // update the count
            int count = container.count(itemId);
            counters.putIfAbsent(itemId, new HashMap<>());
            counters.get(itemId).put(container.getId(), count);
        }
        saveCounters();
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged itemContainerChanged) {
        if (myInventories.contains(itemContainerChanged.getContainerId())) {
            syncInventory(itemContainerChanged.getContainerId());
            if (depositing && preDepositCounters.isPresent()) {
                DepositBoxDetector.handleDeposit(preDepositCounters.get(), counters);
                depositing = false;
                preDepositCounters = Optional.empty();
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked) {
        Widget depositBox = client.getWidget(WidgetInfo.DEPOSIT_BOX_INVENTORY_ITEMS_CONTAINER);
        if (depositBox == null || depositBox.isHidden()) {
            return;
        }

        if (menuOptionClicked.getMenuOption().startsWith("Deposit") && !depositing) {
            depositing = true;
            preDepositCounters = Optional.of(
                    gson.fromJson(gson.toJson(counters), new TypeToken<HashMap<Integer, HashMap<Integer, Integer>>>() {
                    }.getType())
            );
        }
    }

    @Provides
    GrindTrackerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GrindTrackerConfig.class);
    }
}
