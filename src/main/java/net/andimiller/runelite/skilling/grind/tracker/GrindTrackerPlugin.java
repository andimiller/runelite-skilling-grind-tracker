package net.andimiller.runelite.skilling.grind.tracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemClient;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Skilling Grind Tracker Plugin"
)
public class GrindTrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private GrindTrackerConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ItemManager itemManager;

	private GoalsOverlay overlay;

	// Plugin state for keeping track of the items we've seen
	// The outer Integer is the ItemId we're tracking
	// The inner Integer is the container id, this might be the user's inventory, or their bank
	HashMap<Integer, HashMap<Integer, Integer>> counters;

	// Plugin state for our goals
	List<Goal> goals;

	// Cache of names for all the things we're tracking
	HashMap<Integer, String> names;


	void loadFromConfig() {
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
	protected void startUp() throws Exception
	{
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

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		log.info("Goals plugin stopped!");
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged itemContainerChanged)
	{
		ItemContainer container = itemContainerChanged.getItemContainer();
		Set<Integer> ids = goals.stream().flatMap(g -> g.getTypes().stream()).collect(Collectors.toSet());

		for (Integer itemId : ids) {
			// update the name if we haven't tracked it
			names.putIfAbsent(itemId, names.computeIfAbsent(itemId, id -> itemManager.getItemComposition(id).getName()));

			// update the count
			int count = container.count(itemId);
			counters.putIfAbsent(itemId, new HashMap<>());
			counters.get(itemId).put(container.getId(), count);
		}
	}

	@Provides
	GrindTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GrindTrackerConfig.class);
	}
}
