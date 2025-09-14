package net.andimiller.runelite.skilling.grind.tracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Skilling Grind Tracker Plugin"
)
public class ItemGrindTrackerPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ItemGrindTrackerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ChatboxPanelManager chatboxPanelManager;

    @Inject
    private Gson gson;

    @Inject
    private State state;

    private GoalsOverlay overlay;

    private final Set<Integer> myInventories = Set.of(InventoryID.INV, InventoryID.BANK);

    // Plugin state for keeping track of the items we've seen
    // The outer Integer is the ItemId we're tracking
    // The inner Integer is the container id, this might be the user's inventory, or their bank
    HashMap<Integer, HashMap<Integer, Integer>> counters;

    // Plugin state for our goals
    ArrayList<Goal> goals;

    // Cache of names for all the things we're tracking
    HashMap<Integer, String> names;

    // boolean to flag if we're currently using a deposit box
    boolean depositing = false;
    // and some state to store with it for detecting changes
    Optional<HashMap<Integer, HashMap<Integer, Integer>>> preDepositCounters = Optional.empty();


    // some state for if our names are stale and need updating, will be processed the next game tick
    boolean namesStale = false;

    public void loadFromConfig() {
        if (config.showTracker()) {
            overlayManager.add(overlay);
        } else {
            overlayManager.remove(overlay);
        }
        overlay.setPreferredPosition(config.trackerLocation());
    }

    @Override
    protected void startUp() throws Exception {
        counters = state.loadCounters();
        goals = state.loadGoals();
        names = new HashMap<>();
        // set up overlay
        overlay = new GoalsOverlay(counters, goals, names);
        loadFromConfig();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        loadFromConfig();
    }

    @Override
    protected void shutDown() throws Exception {
        state.saveCounters(counters);
        state.saveGoals(goals);
        overlayManager.remove(overlay);
    }

    private void cacheNames() {
        Set<Integer> ids = goals.stream().flatMap(g -> g.getTypes().stream()).collect(Collectors.toSet());
        for (Integer itemId : ids) {
            // update the name if we haven't tracked it
            names.putIfAbsent(itemId, names.computeIfAbsent(itemId, id -> itemManager.getItemComposition(id).getName()));
        }
    }

    @Subscribe
    public void onClientTick(ClientTick event) {
        if (namesStale) {
            namesStale = false;
            syncInventory(InventoryID.INV);
            cacheNames();
        }
    }

    private void syncInventory(int inventoryId) {
        ItemContainer container = client.getItemContainer(inventoryId);
        Set<Integer> ids = goals.stream().flatMap(g -> g.getTypes().stream()).collect(Collectors.toSet());

        for (Integer itemId : ids) {

            // update the count
            int count = container.count(itemId);
            counters.putIfAbsent(itemId, new HashMap<>());
            counters.get(itemId).put(container.getId(), count);
        }
        state.saveCounters(counters);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged itemContainerChanged) {
        cacheNames();
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

    public void setGoal(MenuEntry event) {
        int itemId = Integer.parseInt(event.getTarget());
        chatboxPanelManager.openTextInput("How many would you like to get?")
                .onDone((value) -> {
                            if (value == null || value.isEmpty()) {
                                return;
                            }

                            try {
                                int goal = Integer.parseInt(value);

                                goals.add(new Goal(List.of(itemId), goal));
                                namesStale = true;
                                state.saveGoals(goals);
                            } catch (NumberFormatException e) {
                                return;
                            }
                        }
                ).build();
    }

    public void clearGoal(MenuEntry event) {
        Integer itemId = Integer.parseInt(event.getTarget());
        goals.removeIf(g -> g.getTypes().contains(itemId));
        state.saveGoals(goals);
    }

    public void clearGoals(MenuEntry event) {
        goals.clear();
        state.saveGoals(goals);
    }

    public void advancedGoal(MenuEntry event) {
        chatboxPanelManager.openTextInput("Enter your Goal Expression, see the Plugin details for how to write one")
                .onDone((value) -> {
                            if (value == null || value.isEmpty()) {
                                return;
                            }

                            try {
                                List<Goal> goal = GoalParser.parseGoals(value);

                                goals.addAll(goal);
                                namesStale = true;
                                state.saveGoals(goals);
                            } catch (NumberFormatException e) {
                                return;
                            }
                        }
                ).build();

    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        final boolean hotKeyPressed = client.isKeyPressed(KeyCode.KC_SHIFT);
        final boolean megaHotKeyPressed = client.isKeyPressed(KeyCode.KC_CONTROL);
        if (hotKeyPressed && event.getOption().equals("Use")) {
            Integer itemId = event.getItemId();
            if (megaHotKeyPressed) { // if they pressed both Shift and Ctrl we show the advanced option
                client.getMenu().createMenuEntry(-1)
                        .setOption("Set Advanced Goal")
                        .setType(MenuAction.RUNELITE)
                        .onClick(this::advancedGoal);
                client.getMenu().createMenuEntry(-1)
                        .setOption("Clear All Goals")
                        .setType(MenuAction.RUNELITE)
                        .onClick(this::clearGoals);
            }

            if ((Long) goals.stream().filter(g -> g.getTypes().contains(itemId)).count() == 0L) {
                client.getMenu().createMenuEntry(-1)
                        .setOption("Set Goal")
                        .setTarget(Integer.toString(event.getItemId()))
                        .setType(MenuAction.RUNELITE)
                        .onClick(this::setGoal);
            } else {
                client.getMenu().createMenuEntry(-1)
                        .setOption("Clear Goal")
                        .setTarget(Integer.toString(event.getItemId()))
                        .setType(MenuAction.RUNELITE)
                        .onClick(this::clearGoal);
            }
        }
    }

    @Provides
    ItemGrindTrackerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ItemGrindTrackerConfig.class);
    }
}
