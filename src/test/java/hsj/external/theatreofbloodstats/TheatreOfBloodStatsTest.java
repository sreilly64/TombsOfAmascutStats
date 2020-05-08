package hsj.external.theatreofbloodstats;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TheatreOfBloodStatsTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TheatreOfBloodStatsPlugin.class);
		RuneLite.main(args);
	}
}