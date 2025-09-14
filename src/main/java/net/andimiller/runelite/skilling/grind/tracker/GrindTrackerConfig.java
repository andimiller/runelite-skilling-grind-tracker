package net.andimiller.runelite.skilling.grind.tracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("skilling-grind-tracker")
public interface GrindTrackerConfig extends Config
{
	@ConfigItem(
		keyName = "goals",
		name = "Goals",
		description = "A comma separated list of item ids and quantities, eg. 453:5000 for grinding 5k coal"
	)
	default String goals()
	{
		return "";
	}
}
