package net.andimiller.runelite.skilling.grind.tracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.ui.overlay.OverlayPosition;

@ConfigGroup("item-grind-tracker")
public interface ItemGrindTrackerConfig extends Config
{

	@ConfigItem(
			keyName = "Show Tracker",
			name = "show-tracker",
		   description = "Show the Item Grind Tracker infobox"
	)
	default boolean showTracker() { return true; }

	@ConfigItem(
			keyName = "Tracker Location",
			name = "tracker-location",
			description = "Where to show the Tracker on screen"
	)
	default OverlayPosition trackerLocation() {return OverlayPosition.TOP_LEFT; }

}
