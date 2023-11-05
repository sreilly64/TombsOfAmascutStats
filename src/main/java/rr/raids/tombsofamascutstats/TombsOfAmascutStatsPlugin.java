/*
 * Copyright (c) 2020, HSJ
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package rr.raids.tombsofamascutstats;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(
	name = "Tombs of Amascut Stats",
	description = "Tombs of Amascut room splits and damage",
	tags = {"combat", "raid", "pve", "pvm", "bosses", "toa"},
	enabledByDefault = false
)
public class TombsOfAmascutStatsPlugin extends Plugin
{
	private static final DecimalFormat DMG_FORMAT = new DecimalFormat("#,##0");
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.0");
	private static final int PRECISE_TIMER = 11866;
	private static final int THEATRE_OF_BLOOD_ROOM_STATUS = 6447;
	private static final int TOB_LOBBY = 14642;
	private static final int NYLOCAS_REGION = 13122;
	private static final int SOTETSEG_MAZE_REGION = 13379;
	private static final int NYLOCAS_WAVES_TOTAL = 31;
	private static final int TICK_LENGTH = 600;
	private static final int MAIDEN_ID = 25748;
	private static final int BLOAT_ID = 25749;
	private static final int NYLOCAS_ID = 25750;
	private static final int SOTETSEG_ID = 25751;
	private static final int XARPUS_ID = 25752;
	private static final int VERZIK_ID = 22473;
	private static final Pattern MAIDEN_WAVE = Pattern.compile("Wave 'The Maiden of Sugadinti' \\(.*\\) complete!");
	private static final Pattern BLOAT_WAVE = Pattern.compile("Wave 'The Pestilent Bloat' \\(.*\\) complete!Duration: (\\d+):(\\d+)\\.?(\\d+)");
	private static final Pattern NYLOCAS_WAVE = Pattern.compile("Wave 'The Nylocas' \\(.*\\) complete!");
	private static final Pattern SOTETSEG_WAVE = Pattern.compile("Wave 'Sotetseg' \\(.*\\) complete!");
	private static final Pattern XARPUS_WAVE = Pattern.compile("Wave 'Xarpus' \\(.*\\) complete!");
	private static final Pattern COMPLETION = Pattern.compile("Theatre of Blood total completion time:");
	private static final Set<Integer> NYLOCAS_IDS = ImmutableSet.of(
		NpcID.NYLOCAS_HAGIOS, NpcID.NYLOCAS_HAGIOS_8347, NpcID.NYLOCAS_HAGIOS_8350, NpcID.NYLOCAS_HAGIOS_8353,
		NpcID.NYLOCAS_HAGIOS_10776, NpcID.NYLOCAS_HAGIOS_10779, NpcID.NYLOCAS_HAGIOS_10782, NpcID.NYLOCAS_HAGIOS_10785,
		NpcID.NYLOCAS_HAGIOS_10793, NpcID.NYLOCAS_HAGIOS_10796, NpcID.NYLOCAS_HAGIOS_10799, NpcID.NYLOCAS_HAGIOS_10802,
		NpcID.NYLOCAS_TOXOBOLOS_8343, NpcID.NYLOCAS_TOXOBOLOS_8346, NpcID.NYLOCAS_TOXOBOLOS_8349, NpcID.NYLOCAS_TOXOBOLOS_8352,
		NpcID.NYLOCAS_TOXOBOLOS_10775, NpcID.NYLOCAS_TOXOBOLOS_10778, NpcID.NYLOCAS_TOXOBOLOS_10781, NpcID.NYLOCAS_TOXOBOLOS_10784,
		NpcID.NYLOCAS_TOXOBOLOS_10792, NpcID.NYLOCAS_TOXOBOLOS_10795, NpcID.NYLOCAS_TOXOBOLOS_10798, NpcID.NYLOCAS_TOXOBOLOS_10801,
		NpcID.NYLOCAS_ISCHYROS_8342, NpcID.NYLOCAS_ISCHYROS_8345, NpcID.NYLOCAS_ISCHYROS_8348, NpcID.NYLOCAS_ISCHYROS_8351,
		NpcID.NYLOCAS_ISCHYROS_10774, NpcID.NYLOCAS_ISCHYROS_10777, NpcID.NYLOCAS_ISCHYROS_10780, NpcID.NYLOCAS_ISCHYROS_10783,
		NpcID.NYLOCAS_ISCHYROS_10791, NpcID.NYLOCAS_ISCHYROS_10794, NpcID.NYLOCAS_ISCHYROS_10797, NpcID.NYLOCAS_ISCHYROS_10800
	);
	private static final Set<Point> NYLOCAS_VALID_SPAWNS = ImmutableSet.of(
		new Point(17, 24), new Point(17, 25), new Point(18, 24), new Point(18, 25),
		new Point(31, 9), new Point(31, 10), new Point(32, 9), new Point(32, 10),
		new Point(46, 24), new Point(46, 25), new Point(47, 24), new Point(47, 25)
	);
	private static final Set<String> BOSS_NAMES = ImmutableSet.of(
		"The Maiden of Sugadinti", "Pestilent Bloat", "Nylocas Vasilias", "Sotetseg", "Xarpus", "Verzik Vitur"
	);

	@Inject
	private Client client;

	@Inject
	private TombsOfAmascutStatsStatsConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	private TombsOfAmascutStatsStatsInfoBox maidenInfoBox;
	private TombsOfAmascutStatsStatsInfoBox bloatInfoBox;
	private TombsOfAmascutStatsStatsInfoBox nyloInfoBox;
	private TombsOfAmascutStatsStatsInfoBox soteInfoBox;
	private TombsOfAmascutStatsStatsInfoBox xarpusInfoBox;
	private TombsOfAmascutStatsStatsInfoBox verzikInfoBox;

	private int prevRegion;
	private boolean tobInside;
	private boolean instanced;
	private boolean preciseTimers;

	private int maidenStartTick = -1;
	private int maiden70time;
	private int maiden50time;
	private int maiden30time;

	private int nyloStartTick = -1;
	private int currentNylos;
	private boolean nyloWavesFinished;
	private boolean nyloCleanupFinished;
	private boolean waveThisTick = false;
	private int waveTime;
	private int cleanupTime;
	private int bossSpawnTime;
	private int nyloWave = 0;

	private int soteStartTick = -1;
	private boolean sote66;
	private int sote66time;
	private boolean sote33;
	private int sote33time;

	private int xarpusStartTick = -1;
	private int xarpusAcidTime;
	private int xarpusRecoveryTime;
	private int xarpusPreScreech;
	private int xarpusPreScreechTotal;

	private int verzikStartTick = -1;
	private int verzikP1time;
	private int verzikP2time;
	private double verzikP1personal;
	private double verzikP1total;
	private double verzikP2personal;
	private double verzikP2total;
	private double verzikP2healed;

	private final Map<String, Integer> personalDamage = new HashMap<>();
	private final Map<String, Integer> totalDamage = new HashMap<>();
	private final Map<String, Integer> totalHealing = new HashMap<>();

	@Provides
	TombsOfAmascutStatsStatsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TombsOfAmascutStatsStatsConfig.class);
	}

	@Override
	protected void shutDown() throws Exception
	{
		resetAll();
		resetAllInfoBoxes();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		int tobVar = client.getVarbitValue(Varbits.THEATRE_OF_BLOOD);
		tobInside = tobVar == 2 || tobVar == 3;

		int preciseTimerVar = client.getVarbitValue(PRECISE_TIMER);
		preciseTimers = preciseTimerVar == 1 ;

		if (!tobInside)
		{
			resetAll();
		}

		int region = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
		int status = client.getVarbitValue(THEATRE_OF_BLOOD_ROOM_STATUS);

		if (status == 1 && region != prevRegion && region != SOTETSEG_MAZE_REGION)
		{
			prevRegion = region;
		}

		if (region == TOB_LOBBY)
		{
			resetMaiden();
			resetBloat();
			resetNylo();
			resetSote();
			resetXarpus();
			resetVerzik();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!tobInside || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String strippedMessage = Text.removeTags(event.getMessage());
		List<String> messages = new ArrayList<>(Collections.emptyList());

		if (MAIDEN_WAVE.matcher(strippedMessage).find())
		{
			double personal = personalDamage.getOrDefault("The Maiden of Sugadinti", 0);
			double total = totalDamage.getOrDefault("The Maiden of Sugadinti", 0);
			int healed = totalHealing.getOrDefault("The Maiden of Sugadinti", 0);
			String healing = "Total Healing - " + DMG_FORMAT.format(healed);
			double percent = (personal / total) * 100;
			int roomTicks;
			String roomTime = "";
			String splits = "";
			String damage = "";
			messages.clear();

			if (maidenStartTick > 0)
			{
				roomTicks = client.getTickCount() - maidenStartTick;
				roomTime = formatTime(roomTicks);
				splits = "70% - " + formatTime(maiden70time) +
					"</br>" +
					"50% - " + formatTime(maiden50time) + " (" + formatTime(maiden50time - maiden70time) + ")" +
					"</br>" +
					"30% - " + formatTime(maiden30time) + " (" + formatTime(maiden30time - maiden50time) + ")" +
					"</br>" +
					"Room Complete - " + roomTime + " (" + formatTime(roomTicks - maiden30time) + ")";

				if (config.chatboxSplits())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("70% - ")
							.append(Color.RED, formatTime(maiden70time))
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("50% - ")
							.append(Color.RED, formatTime(maiden50time) + " (" + formatTime(maiden50time - maiden70time) + ")")
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("30% - ")
							.append(Color.RED, formatTime(maiden30time) + " (" + formatTime(maiden30time - maiden50time) + ")")
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Room Complete - ")
							.append(Color.RED, roomTime + " (" + formatTime(roomTicks - maiden30time) + ")")
							.build()
					);
				}
			}

			if (personal > 0)
			{
				damage = "Personal Boss Damage - " + DMG_FORMAT.format(personal);
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Personal Boss Damage - ")
							.append(Color.RED, DMG_FORMAT.format(personal) + " (" + DECIMAL_FORMAT.format(percent) + "%)")
							.build()
					);
				}
			}

			if (config.chatboxHealed())
			{
				messages.add(
					new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("Total Healing - ")
						.append(Color.RED, DMG_FORMAT.format(healed))
						.build()
				);
			}

			maidenInfoBox = createInfoBox(MAIDEN_ID, "Maiden", roomTime, DECIMAL_FORMAT.format(percent), damage, splits, healing);
			infoBoxManager.addInfoBox(maidenInfoBox);
			resetMaiden();
		}
		else if (BLOAT_WAVE.matcher(strippedMessage).find())
		{
			double personal = personalDamage.getOrDefault("Pestilent Bloat", 0);
			double total = totalDamage.getOrDefault("Pestilent Bloat", 0);
			double percent = (personal / total) * 100;
			Matcher m = BLOAT_WAVE.matcher(strippedMessage);
			String roomTime = "";
			if (m.find())
			{
				if (preciseTimers)
				{
					roomTime = m.group(1) + ":" + m.group(2) + "." + m.group(3).charAt(0);
				}
				else
				{
					roomTime = m.group(1) + ":" + m.group(2);
				}
			}
			String damage = "";
			messages.clear();

			if (personal > 0)
			{
				damage = "Personal Boss Damage - " + DMG_FORMAT.format(personal);
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Personal Boss Damage - ")
							.append(Color.RED, DMG_FORMAT.format(personal) + " (" + DECIMAL_FORMAT.format(percent) + "%)")
							.build()
					);
				}
			}

			bloatInfoBox = createInfoBox(BLOAT_ID, "Bloat", roomTime, DECIMAL_FORMAT.format(percent), damage, "Room Complete - " + roomTime, "");
			infoBoxManager.addInfoBox(bloatInfoBox);
			resetBloat();
		}
		else if (NYLOCAS_WAVE.matcher(strippedMessage).find())
		{
			double personal = personalDamage.getOrDefault("Nylocas Vasilias", 0);
			double total = totalDamage.getOrDefault("Nylocas Vasilias", 0);
			int healed = totalHealing.getOrDefault("Nylocas Vasilias", 0);
			String healing = "Total Healing - " + DMG_FORMAT.format(healed);
			double percent = (personal / total) * 100;
			int roomTicks;
			String roomTime = "";
			String splits = "";
			String damage = "";
			messages.clear();

			if (nyloStartTick > 0)
			{
				roomTicks = client.getTickCount() - nyloStartTick;
				roomTime = formatTime(roomTicks);
				splits = "Waves - " + formatTime(waveTime) +
					"</br>" +
					"Cleanup - " + formatTime(cleanupTime) + " (" + formatTime(cleanupTime - waveTime) + ")" +
					"</br>" +
					"Boss Spawn - " + formatTime(bossSpawnTime) + " (" + formatTime(bossSpawnTime - cleanupTime) + ")" +
					"</br>" +
					"Room Complete - " + roomTime + " (" + formatTime(roomTicks - bossSpawnTime) + ")";

				if (config.chatboxSplits())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Waves - ")
							.append(Color.RED, formatTime(waveTime))
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Cleanup - ")
							.append(Color.RED, formatTime(cleanupTime) + " (" + formatTime(cleanupTime - waveTime) + ")")
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Boss Spawn - ")
							.append(Color.RED, formatTime(bossSpawnTime) + " (" + formatTime(bossSpawnTime - cleanupTime) + ")")
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Room Complete - ")
							.append(Color.RED, roomTime + " (" + formatTime(roomTicks - bossSpawnTime) + ")")
							.build()
					);
				}
			}

			if (personal > 0)
			{
				damage = "Personal Boss Damage - " + DMG_FORMAT.format(personal);
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Personal Boss Damage - ")
							.append(Color.RED, DMG_FORMAT.format(personal) + " (" + DECIMAL_FORMAT.format(percent) + "%)")
							.build()
					);
				}
			}

			if (config.chatboxHealed())
			{
				messages.add(
					new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("Total Healing - ")
						.append(Color.RED, DMG_FORMAT.format(healed))
						.build()
				);
			}

			nyloInfoBox = createInfoBox(NYLOCAS_ID, "Nylocas", roomTime, DECIMAL_FORMAT.format(percent), damage, splits, healing);
			infoBoxManager.addInfoBox(nyloInfoBox);
			resetNylo();
		}
		else if (SOTETSEG_WAVE.matcher(strippedMessage).find())
		{
			double personal = personalDamage.getOrDefault("Sotetseg", 0);
			double total = totalDamage.getOrDefault("Sotetseg", 0);
			double percent = (personal / total) * 100;
			int roomTicks;
			String roomTime = "";
			String splits = "";
			String damage = "";
			messages.clear();

			if (soteStartTick > 0)
			{
				roomTicks = client.getTickCount() - soteStartTick;
				roomTime = formatTime(roomTicks);
				splits = "66% - " + formatTime(sote66time) +
					"</br>" +
					"33% - " + formatTime(sote33time) + " (" + formatTime(sote33time - sote66time) + ")" +
					"</br>" +
					"Room Complete - " + roomTime + " (" + formatTime(roomTicks - sote33time) + ")";

				if (config.chatboxSplits())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("66% - ")
							.append(Color.RED, formatTime(sote66time))
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("33% - ")
							.append(Color.RED, formatTime(sote33time) + " (" + formatTime(sote33time - sote66time) + ")")
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Room Complete - ")
							.append(Color.RED, roomTime + " (" + formatTime(roomTicks - sote33time) + ")")
							.build()
					);
				}
			}

			if (personal > 0)
			{
				damage = "Personal Boss Damage - " + DMG_FORMAT.format(personal);
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Personal Boss Damage - ")
							.append(Color.RED, DMG_FORMAT.format(personal) + " (" + DECIMAL_FORMAT.format(percent) + "%)")
							.build()
					);
				}
			}

			soteInfoBox = createInfoBox(SOTETSEG_ID, "Sotetseg", roomTime, DECIMAL_FORMAT.format(percent), damage, splits, "");
			infoBoxManager.addInfoBox(soteInfoBox);
			resetSote();
		}
		else if (XARPUS_WAVE.matcher(strippedMessage).find())
		{
			double personal = personalDamage.getOrDefault("Xarpus", 0);
			double total = totalDamage.getOrDefault("Xarpus", 0);
			int healed = totalHealing.getOrDefault("Xarpus", 0);
			String healing = "Total Healing - " + DMG_FORMAT.format(healed);
			double xarpusPostScreech = personal - xarpusPreScreech;
			double personalPercent = (personal / total) * 100;
			double preScreechPercent = ((double) xarpusPreScreech / (double) xarpusPreScreechTotal) * 100;
			double postScreechPercent = (xarpusPostScreech / (total - xarpusPreScreechTotal)) * 100;
			int roomTicks;
			String roomTime = "";
			String splits = "";
			String damage = "";
			messages.clear();

			if (xarpusStartTick > 0)
			{
				roomTicks = client.getTickCount() - xarpusStartTick;
				roomTime = formatTime(roomTicks);
				splits = "Recovery Phase - " + formatTime(xarpusRecoveryTime) +
					"</br>" +
					"Screech Time - " + formatTime(xarpusAcidTime) + " (" + formatTime(xarpusAcidTime - xarpusRecoveryTime) + ")" +
					"</br>" +
					"Room Complete - " + roomTime + " (" + formatTime(roomTicks - xarpusAcidTime) + ")";

				if (config.chatboxSplits())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Recovery Phase - ")
							.append(Color.RED, formatTime(xarpusRecoveryTime))
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Screech Time - ")
							.append(Color.RED, formatTime(xarpusAcidTime) + " (" + formatTime(xarpusAcidTime - xarpusRecoveryTime) + ")")
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Room Complete - ")
							.append(Color.RED, roomTime + " (" + formatTime(roomTicks - xarpusAcidTime) + ")")
							.build()
					);
				}
			}

			if (xarpusPreScreech > 0)
			{
				damage += "Pre Screech Damage - " + DMG_FORMAT.format(xarpusPreScreech) + " (" + DECIMAL_FORMAT.format(preScreechPercent) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Pre Screech Damage - ")
							.append(Color.RED, DMG_FORMAT.format(xarpusPreScreech) + " (" + DECIMAL_FORMAT.format(preScreechPercent) + "%)")
							.build()
					);
				}
			}

			if (xarpusPostScreech > 0)
			{
				damage += "Post Screech Damage - " + DMG_FORMAT.format(xarpusPostScreech) + " (" + DECIMAL_FORMAT.format(postScreechPercent) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Post Screech Damage - ")
							.append(Color.RED, DMG_FORMAT.format(xarpusPostScreech) + " (" + DECIMAL_FORMAT.format(postScreechPercent) + "%)")
							.build()
					);
				}
			}

			if (personal > 0)
			{
				damage += "Personal Boss Damage - " + DMG_FORMAT.format(personal);
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Personal Boss Damage - ")
							.append(Color.RED, DMG_FORMAT.format(personal) + " (" + DECIMAL_FORMAT.format(personalPercent) + "%)")
							.build()
					);
				}
			}

			if (config.chatboxHealed())
			{
				messages.add(
					new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("Total Healed - ")
						.append(Color.RED, DMG_FORMAT.format(healed))
						.build()
				);
			}

			xarpusInfoBox = createInfoBox(XARPUS_ID, "Xarpus", roomTime, DECIMAL_FORMAT.format(personalPercent), damage, splits, healing);
			infoBoxManager.addInfoBox(xarpusInfoBox);
			resetXarpus();
		}
		else if (COMPLETION.matcher(strippedMessage).find())
		{
			double personal = personalDamage.getOrDefault("Verzik Vitur", 0) ;
			double total = totalDamage.getOrDefault("Verzik Vitur", 0);
			double p3personal = personalDamage.getOrDefault("Verzik Vitur", 0) - (verzikP1personal + verzikP2personal);
			double p3total = totalDamage.getOrDefault("Verzik Vitur", 0) - (verzikP1total + verzikP2total);
			double p3healed = totalHealing.getOrDefault("Verzik Vitur", 0) - verzikP2healed;
			double healed = totalHealing.getOrDefault("Verzik Vitur", 0);
			double p3percent = (p3personal / p3total) * 100;
			double p1percent = (verzikP1personal / verzikP1total) * 100;
			double p2percent = (verzikP2personal / verzikP2total) * 100;
			double percent = (personal / total) * 100;
			int roomTicks;
			String roomTime = "";
			String splits = "";
			String damage = "";
			String healing = "P2 Healed - " + DMG_FORMAT.format(verzikP2healed) +
				"</br>" +
				"P3 Healed - " + DMG_FORMAT.format(p3healed) +
				"</br>" +
				"Total Healed - " + DMG_FORMAT.format(healed);
			messages.clear();

			if (verzikStartTick > 0)
			{
				roomTicks = client.getTickCount() - verzikStartTick;
				roomTime = formatTime(roomTicks);
				splits = "P1 - " + formatTime(verzikP1time) +
					"</br>" +
					"P2 - " + formatTime(verzikP2time) + " (" + formatTime(verzikP2time - verzikP1time) + ")" +
					"</br>" +
					"P3 - " + roomTime + " (" + formatTime(roomTicks - verzikP2time) + ")";

				if (config.chatboxSplits())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("P1 - ")
							.append(Color.RED, formatTime(verzikP1time))
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("P2 - ")
							.append(Color.RED, formatTime(verzikP2time) + " (" + formatTime(verzikP2time - verzikP1time) + ")")
							.build()
					);

					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("P3 - ")
							.append(Color.RED, roomTime + " (" + formatTime(roomTicks - verzikP2time) + ")")
							.build()
					);
				}
			}

			if (verzikP1personal > 0)
			{
				damage += "P1 Personal Damage - " + DMG_FORMAT.format(verzikP1personal) + " (" + DECIMAL_FORMAT.format(p1percent) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("P1 Personal Damage - ")
							.append(Color.RED, DMG_FORMAT.format(verzikP1personal) + " (" + DECIMAL_FORMAT.format(p1percent) + "%)")
							.build()
					);
				}
			}

			if (verzikP2personal > 0)
			{
				damage += "P2 Personal Damage - " + DMG_FORMAT.format(verzikP2personal) + " (" + DECIMAL_FORMAT.format(p2percent) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("P2 Personal Damage - ")
							.append(Color.RED, DMG_FORMAT.format(verzikP2personal) + " (" + DECIMAL_FORMAT.format(p2percent) + "%)")
							.build()
					);
				}
			}

			if (p3personal > 0)
			{
				damage += "P3 Personal Damage - " + DMG_FORMAT.format(p3personal) + " (" + DECIMAL_FORMAT.format(p3percent) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("P3 Personal Damage - ")
							.append(Color.RED, DMG_FORMAT.format(p3personal) + " (" + DECIMAL_FORMAT.format(p3percent) + "%)")
							.build()
					);
				}
			}

			if (personal > 0)
			{
				damage += "Total Personal Damage - " + DMG_FORMAT.format(personal);
				if (config.chatboxDmg())
				{
					messages.add(
						new ChatMessageBuilder()
							.append(ChatColorType.NORMAL)
							.append("Total Personal Damage - ")
							.append(Color.RED, DMG_FORMAT.format(personal) + " (" + DECIMAL_FORMAT.format(percent) + "%)")
							.build()
					);
				}
			}

			if (config.chatboxHealed())
			{
				messages.add(
					new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("P2 Healed - ")
						.append(Color.RED, DMG_FORMAT.format(verzikP2healed))
						.build()
				);

				messages.add(
					new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("P3 Healed - ")
						.append(Color.RED, DMG_FORMAT.format(p3healed))
						.build()
				);

				messages.add(
					new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("Total Healed - ")
						.append(Color.RED, DMG_FORMAT.format(healed))
						.build()
				);
			}

			verzikInfoBox = createInfoBox(VERZIK_ID, "Verzik", roomTime, DECIMAL_FORMAT.format(percent), damage, splits, healing);
			infoBoxManager.addInfoBox(verzikInfoBox);
			resetVerzik();
		}

		if (!messages.isEmpty())
		{
			for (String m : messages)
			{
				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.GAMEMESSAGE)
					.runeLiteFormattedMessage(m)
					.build());
			}
			messages.clear();
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		if (!tobInside)
		{
			return;
		}

		NPC npc = event.getNpc();
		int npcId = npc.getId();

		switch (npcId)
		{
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8361:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_10815:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_10823:
				if (maidenStartTick != -1)
				{
					maiden70time = client.getTickCount() - maidenStartTick;
				}
				break;
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8362:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_10816:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_10824:
				if (maidenStartTick != -1)
				{
					maiden50time = client.getTickCount() - maidenStartTick;
				}
				break;
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8363:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_10817:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_10825:
				if (maidenStartTick != -1)
				{
					maiden30time = client.getTickCount() - maidenStartTick;
				}
				break;
			case NpcID.SOTETSEG_8388:
			case NpcID.SOTETSEG_10865:
			case NpcID.SOTETSEG_10868:
				if (soteStartTick == -1)
				{
					soteStartTick = client.getTickCount();
				}
				break;
			case NpcID.SOTETSEG:
			case NpcID.SOTETSEG_10864:
			case NpcID.SOTETSEG_10867:
				if (soteStartTick != -1)
				{
					if (!sote66)
					{
						sote66time = client.getTickCount() - soteStartTick;
						sote66 = true;
					}
					else if (!sote33)
					{
						sote33time = client.getTickCount() - soteStartTick;
						sote33 = true;
					}
				}
				break;
			case NpcID.XARPUS_8339:
			case NpcID.XARPUS_10767:
			case NpcID.XARPUS_10771:
				xarpusStartTick = client.getTickCount();
				break;
			case NpcID.XARPUS_8340:
			case NpcID.XARPUS_10768:
			case NpcID.XARPUS_10772:
				xarpusRecoveryTime = client.getTickCount() - xarpusStartTick;
				break;
			case NpcID.VERZIK_VITUR_8370:
			case NpcID.VERZIK_VITUR_10831:
			case NpcID.VERZIK_VITUR_10848:
				verzikStartTick = client.getTickCount();
				break;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!tobInside)
		{
			return;
		}

		NPC npc = event.getNpc();
		int npcId = npc.getId();

		switch (npcId)
		{
			case NpcID.THE_MAIDEN_OF_SUGADINTI:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_10814:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_10822:
				maidenStartTick = client.getTickCount();
				break;
			case NullNpcID.NULL_8358:
			case NullNpcID.NULL_10790:
			case NullNpcID.NULL_10811:
				nyloStartTick = client.getTickCount();
				break;
			case NpcID.NYLOCAS_VASILIAS:
			case NpcID.NYLOCAS_VASILIAS_10786:
			case NpcID.NYLOCAS_VASILIAS_10807:
				bossSpawnTime = client.getTickCount() - nyloStartTick;
				break;
			case NpcID.VERZIK_VITUR_8371:
			case NpcID.VERZIK_VITUR_10832:
			case NpcID.VERZIK_VITUR_10849:
				verzikP1time = client.getTickCount() - verzikStartTick;
				verzikP1personal = personalDamage.getOrDefault("Verzik Vitur", 0);
				verzikP1total = totalDamage.getOrDefault("Verzik Vitur", 0);
				break;
			case NpcID.VERZIK_VITUR_8373:
			case NpcID.VERZIK_VITUR_10834:
			case NpcID.VERZIK_VITUR_10851:
				verzikP2time = client.getTickCount() - verzikStartTick;
				verzikP2personal = personalDamage.getOrDefault("Verzik Vitur", 0) - verzikP1personal;
				verzikP2total = totalDamage.getOrDefault("Verzik Vitur", 0) - verzikP1total;
				verzikP2healed = totalHealing.getOrDefault("Verzik Vitur", 0);
				break;
		}

		if (!NYLOCAS_IDS.contains(npcId) || prevRegion != NYLOCAS_REGION)
		{
			return;
		}

		currentNylos++;
		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, npc.getLocalLocation());
		Point spawnLoc = new Point(worldPoint.getRegionX(), worldPoint.getRegionY());

		if (!NYLOCAS_VALID_SPAWNS.contains(spawnLoc))
		{
			return;
		}

		if (!waveThisTick)
		{
			nyloWave++;
			waveThisTick = true;
		}

		if (nyloWave == NYLOCAS_WAVES_TOTAL && !nyloWavesFinished)
		{
			waveTime = client.getTickCount() - nyloStartTick;
			nyloWavesFinished = true;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (!tobInside)
		{
			return;
		}

		NPC npc = event.getNpc();
		int npcId = npc.getId();

		if (!NYLOCAS_IDS.contains(npcId) || prevRegion != NYLOCAS_REGION)
		{
			return;
		}

		currentNylos--;

		if (nyloWavesFinished && !nyloCleanupFinished && currentNylos == 0)
		{
			cleanupTime = client.getTickCount() - nyloStartTick;
			nyloCleanupFinished = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!tobInside)
		{
			return;
		}

		if (waveThisTick)
		{
			waveThisTick = false;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOADING)
		{
			return;
		}

		boolean prevInstance = instanced;
		instanced = client.isInInstancedRegion();
		if (prevInstance && !instanced)
		{
			resetAll();
			resetAllInfoBoxes();
		}
		else if (!prevInstance && instanced)
		{
			resetAll();
			resetAllInfoBoxes();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!tobInside)
		{
			return;
		}

		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) actor;
		String npcName = npc.getName();
		if (npcName == null || !(BOSS_NAMES.contains(npcName)))
		{
			return;
		}

		npcName = Text.removeTags(npcName);
		Hitsplat hitsplat = event.getHitsplat();

		if (hitsplat.isMine())
		{
			int myDmg = personalDamage.getOrDefault(npcName, 0);
			int totalDmg = totalDamage.getOrDefault(npcName, 0);
			myDmg += hitsplat.getAmount();
			totalDmg += hitsplat.getAmount();
			personalDamage.put(npcName, myDmg);
			totalDamage.put(npcName, totalDmg);
		}
		else if (hitsplat.isOthers())
		{
			int totalDmg = totalDamage.getOrDefault(npcName, 0);
			totalDmg += hitsplat.getAmount();
			totalDamage.put(npcName, totalDmg);
		}
		else if (hitsplat.getHitsplatType() == HitsplatID.HEAL)
		{
			int healed = totalHealing.getOrDefault(npcName, 0);
			healed += hitsplat.getAmount();
			totalHealing.put(npcName, healed);
		}
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		Actor npc = event.getActor();
		if (!(npc instanceof NPC) || !tobInside)
		{
			return;
		}

		String overheadText = event.getOverheadText();
		String npcName = npc.getName();
		if (npcName != null && npcName.equals("Xarpus") && overheadText.equals("Screeeeech!"))
		{
			xarpusAcidTime = client.getTickCount() - xarpusStartTick;
			xarpusPreScreech = personalDamage.getOrDefault(npcName, 0);
			xarpusPreScreechTotal = totalDamage.getOrDefault(npcName, 0);
		}
	}

	private TombsOfAmascutStatsStatsInfoBox createInfoBox(int itemId, String room, String time, String percent, String damage, String splits, String healed)
	{
		BufferedImage image = itemManager.getImage(itemId);
		return new TombsOfAmascutStatsStatsInfoBox(image, config, this, room, time, percent, damage, splits, healed);
	}

	private String formatTime(int ticks)
	{
		int millis = ticks * TICK_LENGTH;
		String hundredths = String.valueOf(millis % 1000).substring(0, 1);

		if (preciseTimers)
		{
			return String.format("%d:%02d.%s",
				TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
				TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1),
				hundredths);
		}
		else
		{
			return String.format("%d:%02d",
				TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
				TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
		}
	}

	private void resetMaiden()
	{
		maidenStartTick = -1;
		maiden70time = 0;
		maiden50time = 0;
		maiden30time = 0;
		personalDamage.remove("The Maiden of Sugadinti");
		totalDamage.remove("The Maiden of Sugadinti");
		totalHealing.remove("The Maiden of Sugadinti");
	}

	private void resetBloat()
	{
		personalDamage.remove("Pestilent Bloat");
		totalDamage.remove("Pestilent Bloat");
	}

	private void resetNylo()
	{
		nyloStartTick = -1;
		currentNylos = 0;
		nyloWavesFinished = false;
		nyloCleanupFinished = false;
		waveTime = 0;
		cleanupTime = 0;
		bossSpawnTime = 0;
		waveThisTick = false;
		nyloWave = 0;
		personalDamage.remove("Nylocas Vasilias");
		totalDamage.remove("Nylocas Vasilias");
	}

	private void resetSote()
	{
		soteStartTick = -1;
		sote66 = false;
		sote66time = 0;
		sote33 = false;
		sote33time = 0;
		personalDamage.remove("Sotetseg");
		totalDamage.remove("Sotetseg");
	}

	private void resetXarpus()
	{
		xarpusStartTick = -1;
		xarpusRecoveryTime = 0;
		xarpusAcidTime = 0;
		xarpusPreScreech = 0;
		xarpusPreScreechTotal = 0;
		personalDamage.remove("Xarpus");
		totalDamage.remove("Xarpus");
		totalHealing.remove("Xarpus");
	}

	private void resetVerzik()
	{
		verzikStartTick = -1;
		verzikP1time = 0;
		verzikP2time = 0;
		verzikP1personal = 0;
		verzikP1total = 0;
		verzikP2personal = 0;
		verzikP2total = 0;
		verzikP2healed = 0;
		personalDamage.clear();
		totalDamage.clear();
		totalHealing.clear();
	}

	private void resetAll()
	{
		resetMaiden();
		resetBloat();
		resetNylo();
		resetSote();
		resetXarpus();
		resetVerzik();
	}

	private void resetAllInfoBoxes()
	{
		infoBoxManager.removeInfoBox(maidenInfoBox);
		infoBoxManager.removeInfoBox(bloatInfoBox);
		infoBoxManager.removeInfoBox(nyloInfoBox);
		infoBoxManager.removeInfoBox(soteInfoBox);
		infoBoxManager.removeInfoBox(xarpusInfoBox);
		infoBoxManager.removeInfoBox(verzikInfoBox);
	}
}
