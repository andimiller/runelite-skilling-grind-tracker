package net.andimiller.runelite.skilling.grind.tracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ManualTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GrindTrackerPlugin.class);
		RuneLite.main(args);
	}
}