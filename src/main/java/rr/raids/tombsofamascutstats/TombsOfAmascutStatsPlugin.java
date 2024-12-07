/*
 * Copyright (c) 2023, Red Rookie
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
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
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
import org.apache.commons.lang3.StringUtils;
import rr.raids.tombsofamascutstats.stats.*;
import rr.raids.tombsofamascutstats.stats.phases.AkkhaPhase;
import rr.raids.tombsofamascutstats.stats.phases.BabaPhase;
import rr.raids.tombsofamascutstats.stats.phases.KephriPhase;
import rr.raids.tombsofamascutstats.stats.phases.ZebakPhase;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Tombs of Amascut Stats",
	description = "Tombs of Amascut phase times and damage tracker",
	tags = {"combat", "raid", "pve", "pvm", "bosses", "toa"}
)
public class TombsOfAmascutStatsPlugin extends Plugin
{
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

	public static final DecimalFormat DMG_FORMAT = new DecimalFormat("#,##0");
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.0");

	public static final String BABA = "Ba-Ba";
	public static final String KEPHRI = "Kephri";
	public static final String SCARABS = "Scarabs";
	public static final String AKKHA = "Akkha";
	public static final String AKKHAS_SHADOW = "Akkha's Shadow";
	public static final String ZEBAK = "Zebak";
	public static final String OBELISK = "Obelisk";
	public static final String TUMEKENS_WARDEN = "Tumeken's Warden";
	public static final String ELIDINIS_WARDEN = "Elidinis' Warden";
	public static final String CORE = "Core";
	public static final String ENERGY_SIPHON = "Energy Siphon";

	private static final int KEPHRI_SHIELDED_HEALING_HITSPLAT_ID = HitsplatID.CYAN_UP;
	private static final int WARDENS_P2_DAMAGE_ME_HITSPLAT_ID = HitsplatID.DAMAGE_ME_POISE;
	private static final int WARDENS_P2_DAMAGE_OTHER_HITSPLAT_ID = HitsplatID.DAMAGE_OTHER_POISE;
	private static final int WARDENS_P2_DAMAGE_MAX_ME_HITSPLAT_ID = HitsplatID.DAMAGE_MAX_ME_POISE;

	private static final int PRECISE_TIMER = 11866;
	private static final int TICK_LENGTH = 600;
	private static final int BABA_PET_ID = 27383;
	private static final int KEPHRI_PET_ID = 27384;
	private static final int AKKHA_PET_ID = 27382;
	private static final int ZEBAK_PET_ID = 27385;
	private static final int OBELISK_ICON_ID = 21788;
	private static final int ELIDNIS_WARDEN_PET_ID = 27354;
	private static final int TUMEKENS_WARDEN_PET_ID = 27352;
	private static final int TOA_NEXUS_REGION_ID = 14160;
	private static final int BABA_PUZZLE_ROOM_REGION_ID = 15186;
	private static final int BABA_ROOM_REGION_ID = 15188;
	private static final int KEPHRI_PUZZLE_ROOM_REGION_ID = 14162;
	private static final int KEPHRI_ROOM_REGION_ID = 14164;
	private static final int AKKHA_PUZZLE_ROOM_REGION_ID = 14674;
	private static final int AKKHA_ROOM_REGION_ID = 14676;
	private static final int ZEBAK_PUZZLE_ROOM_REGION_ID = 15698;
	private static final int ZEBAK_ROOM_REGION_ID = 15700;
	private static final int WARDENS_OBELISK_ROOM_REGION_ID = 15184;
	private static final int WARDENS_P3_ROOM_REGION_ID = 15696;
	private static final int TOA_LOOT_ROOM_REGION_ID = 14672;
	private static final int TOA_LOBBY_REGION_ID = 13454;

	private static final Set<Integer> TOA_ROOM_IDS = ImmutableSet.of(
			TOA_NEXUS_REGION_ID,
			BABA_PUZZLE_ROOM_REGION_ID,
			BABA_ROOM_REGION_ID,
			KEPHRI_PUZZLE_ROOM_REGION_ID,
			KEPHRI_ROOM_REGION_ID,
			AKKHA_PUZZLE_ROOM_REGION_ID,
			AKKHA_ROOM_REGION_ID,
			ZEBAK_PUZZLE_ROOM_REGION_ID,
			ZEBAK_ROOM_REGION_ID,
			WARDENS_OBELISK_ROOM_REGION_ID,
			WARDENS_P3_ROOM_REGION_ID,
			TOA_LOOT_ROOM_REGION_ID,
			TOA_LOBBY_REGION_ID
	);

	private static final Pattern BABA_STARTED = Pattern.compile("Challenge started: Ba-Ba\\.");
	private static final Pattern KEPHRI_STARTED = Pattern.compile("Challenge started: Kephri\\.");
	private static final Pattern AKKHA_STARTED = Pattern.compile("Challenge started: Akkha\\.");
	private static final Pattern ZEBAK_STARTED = Pattern.compile("Challenge started: Zebak\\.");
	private static final Pattern WARDENS_STARTED = Pattern.compile("Challenge started: The Wardens\\.");
	private static final Pattern BABA_COMPLETE = Pattern.compile("Challenge complete: Ba-Ba\\.");
	private static final Pattern KEPHRI_COMPLETE = Pattern.compile("Challenge complete: Kephri\\.");
	private static final Pattern AKKHA_COMPLETE = Pattern.compile("Challenge complete: Akkha\\.");
	private static final Pattern ZEBAK_COMPLETE = Pattern.compile("Challenge complete: Zebak\\.");
	private static final Pattern OBELISK_COMPLETE_TUMEKEN_SPAWNS = Pattern.compile("As Elidinis' Warden falls, Tumeken's Warden powers up!");
	private static final Pattern OBELISK_COMPLETE_ELIDINIS_SPAWNS = Pattern.compile("As Tumeken's Warden falls, Elidinis' Warden powers up!");
	private static final Pattern WARDENS_P2_COMPLETE_TUMEKEN_SPAWNS = Pattern.compile("Elidinis' Warden uses the last of its power to restore Tumeken's Warden!");
	private static final Pattern WARDENS_P2_COMPLETE_ELIDINIS_SPAWNS = Pattern.compile("Tumeken's Warden uses the last of its power to restore Elidinis' Warden!");
	private static final Pattern WARDENS_COMPLETE = Pattern.compile("Challenge complete: The Wardens\\.");

	private TombsOfAmascutStatsStatsInfoBox babaInfoBox;
	private TombsOfAmascutStatsStatsInfoBox kephriInfoBox;
	private TombsOfAmascutStatsStatsInfoBox akkhaInfoBox;
	private TombsOfAmascutStatsStatsInfoBox zebakInfoBox;
	private TombsOfAmascutStatsStatsInfoBox obeliskInfoBox;
	private TombsOfAmascutStatsStatsInfoBox wardensP2InfoBox;
	private TombsOfAmascutStatsStatsInfoBox wardensP3InfoBox;

	private boolean currentlyInsideToA;
	private boolean instanced;
	private boolean preciseTimers;

	private final BabaStats babaStats = new BabaStats();
	private final KephriStats kephriStats = new KephriStats();
	private final AkkhaStats akkhaStats = new AkkhaStats();
	private final ZebakStats zebakStats = new ZebakStats();
	private final ObeliskStats obeliskStats = new ObeliskStats();
	private final Set<BossStats> bossStats = ImmutableSet.of(
			babaStats, kephriStats, akkhaStats, zebakStats, obeliskStats
	);

	private int obeliskStartTick = -1;
	private int wardensP2StartTick = -1;
	private int wardensP3StartTick = -1;
	private boolean foughtElidinisWardenInP3;
	private boolean wardensP4EnrageHeal = false;
	private int wardensP3CompletionTime;
	private boolean energySiphonsWhereKilled = false;
	private int energySiphonBossDamage;
	private double wardensP3PersonalDamage;
	private double wardensP3TotalDamage;
	private LocalPoint lastEnergySiphonPosition;

	private final Map<String, Integer> personalDamage = new HashMap<>(); // key = enemy name, value = damage dealt to the enemy by the player
	private final Map<String, Integer> totalDamage = new HashMap<>(); // key = enemy name, value = total damage dealt to the enemy
	private final Map<String, Integer> totalHealing = new HashMap<>(); // key = enemy name, value = healing received by the enemy

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

		int preciseTimerVar = client.getVarbitValue(PRECISE_TIMER);
		preciseTimers = preciseTimerVar == 1 ;

		int currentRegionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
		currentlyInsideToA = TOA_ROOM_IDS.contains(currentRegionId);

		if (!currentlyInsideToA)
		{
			resetAll();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!currentlyInsideToA || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String strippedMessage = Text.removeTags(event.getMessage());
		List<String> messages = new ArrayList<>(Collections.emptyList());

		if (BABA_STARTED.matcher(strippedMessage).find())
		{
			babaStats.resetStats();
			babaStats.setStartTick(client.getTickCount());
		}
		else if (KEPHRI_STARTED.matcher(strippedMessage).find())
		{
			kephriStats.resetStats();
			kephriStats.setStartTick(client.getTickCount());
		}
		else if (AKKHA_STARTED.matcher(strippedMessage).find())
		{
			akkhaStats.resetStats();
			akkhaStats.setStartTick(client.getTickCount());
		}
		else if (ZEBAK_STARTED.matcher(strippedMessage).find())
		{
			zebakStats.resetStats();
			zebakStats.setStartTick(client.getTickCount());
		}
		else if (WARDENS_STARTED.matcher(strippedMessage).find())
		{
			resetObelisk();
			resetWardensP2();
			resetWardensP3();
			resetWardensInfoBoxes();
			obeliskStartTick = client.getTickCount();
		}
		else if (BABA_COMPLETE.matcher(strippedMessage).find())
		{
			if (babaStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			babaStats.getPhaseCompletionTimes().put(BabaPhase.PHASE_3, formatTime(currentTick - babaStats.getPreviousPhaseEndTick()));
			babaStats.getPhaseCompletionTimes().put(BabaPhase.TOTAL, formatTime(currentTick - babaStats.getStartTick()));

			if (config.chatboxSplits())
			{
				for (Map.Entry<BabaPhase, String> entry: babaStats.getPhaseCompletionTimes().entrySet())
				{
					String phaseName = entry.getKey().phaseName;
					String phaseTime = entry.getValue();
					messages.add(getStatsChatMessage(phaseName, phaseTime));
				}
			}

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("Damage dealt to Ba-Ba - ", DMG_FORMAT.format(babaStats.getPersonalDamage().get(BABA)) + " (" + DECIMAL_FORMAT.format(babaStats.getPercentageOfDamageDealt(BABA)) + "%)"));
			}

			String damage = babaStats.getInfoBoxBossDamageString();
			String splits = babaStats.getInfoBoxSplitTimesString();

			babaInfoBox = createInfoBox(BABA_PET_ID, BABA, babaStats.getPhaseCompletionTimes().get(BabaPhase.TOTAL), DECIMAL_FORMAT.format(babaStats.getPercentageOfDamageDealt(BABA)), damage, splits, "");
			infoBoxManager.addInfoBox(babaInfoBox);
			babaStats.resetStats();
		}
		else if (KEPHRI_COMPLETE.matcher(strippedMessage).find())
		{
			if (kephriStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			kephriStats.getPhaseCompletionTimes().put(KephriPhase.GREEN_HP, formatTime(currentTick - kephriStats.getPreviousPhaseEndTick()));
			kephriStats.getPhaseCompletionTimes().put(KephriPhase.TOTAL, formatTime(currentTick - kephriStats.getStartTick()));

			if (config.chatboxSplits())
			{
				for (Map.Entry<KephriPhase, String> entry: kephriStats.getPhaseCompletionTimes().entrySet())
				{
					String phaseName = entry.getKey().phaseName;
					String phaseTime = entry.getValue();
					messages.add(getStatsChatMessage(phaseName, phaseTime));
				}
			}

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("First down shield healed - ", DMG_FORMAT.format(kephriStats.getFirstShieldDownHealing())));
				messages.add(getStatsChatMessage("Second down shield healed - ", DMG_FORMAT.format(kephriStats.getSecondShieldDownHealing())));
				messages.add(getStatsChatMessage("Total shield healed - ", DMG_FORMAT.format(kephriStats.getShieldTotalHealing())));
				messages.add(getStatsChatMessage("Damage dealt to Kephri - ", DMG_FORMAT.format(kephriStats.getPersonalDamage().get(KEPHRI)) + " (" + DECIMAL_FORMAT.format(kephriStats.getPercentageOfDamageDealt(KEPHRI)) + "%)"));
				messages.add(getStatsChatMessage("Damage dealt to Scarabs - ", DMG_FORMAT.format(kephriStats.getPersonalDamage().get(SCARABS)) + " (" + DECIMAL_FORMAT.format(kephriStats.getPercentageOfDamageDealt(SCARABS)) + "%)"));
				messages.add(getStatsChatMessage("Total damage dealt - ", DMG_FORMAT.format(kephriStats.getTotalPersonalDamageDealt()) + " (" + DECIMAL_FORMAT.format(kephriStats.getTotalPercentageOfDamageDealt()) + "%)"));
			}

			String damage = kephriStats.getInfoBoxBossDamageString();
			String splits = kephriStats.getInfoBoxSplitTimesString();
			String healing = kephriStats.getInfoBoxHealingStatsString();

			kephriInfoBox = createInfoBox(KEPHRI_PET_ID, KEPHRI, kephriStats.getPhaseCompletionTimes().get(KephriPhase.TOTAL), DECIMAL_FORMAT.format(kephriStats.getTotalPercentageOfDamageDealt()), damage, splits, healing);
			infoBoxManager.addInfoBox(kephriInfoBox);
			kephriStats.resetStats();
		}
		else if (AKKHA_COMPLETE.matcher(strippedMessage).find())
		{
			if (akkhaStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.ENRAGE, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.TOTAL, formatTime(currentTick - akkhaStats.getStartTick()));

			if (config.chatboxSplits())
			{
				for (Map.Entry<AkkhaPhase, String> entry: akkhaStats.getPhaseCompletionTimes().entrySet())
				{
					String phaseName = entry.getKey().phaseName;
					String phaseTime = entry.getValue();
					messages.add(getStatsChatMessage(phaseName, phaseTime));
				}
			}

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("Damage dealt to Akkha - ", DMG_FORMAT.format(akkhaStats.getPersonalDamage().get(AKKHA)) + " (" + DECIMAL_FORMAT.format(akkhaStats.getPercentageOfDamageDealt(AKKHA)) + "%)"));
				messages.add(getStatsChatMessage("Damage dealt to Akkha's Shadows - ", DMG_FORMAT.format(akkhaStats.getPersonalDamage().get(AKKHAS_SHADOW)) + " (" + DECIMAL_FORMAT.format(akkhaStats.getPercentageOfDamageDealt(AKKHAS_SHADOW)) + "%)"));
				messages.add(getStatsChatMessage("Total damage dealt - ", DMG_FORMAT.format(akkhaStats.getTotalPersonalDamageDealt()) + " (" + DECIMAL_FORMAT.format(akkhaStats.getTotalPercentageOfDamageDealt()) + "%)"));
			}

			String damage = akkhaStats.getInfoBoxBossDamageString();
			String splits = akkhaStats.getInfoBoxSplitTimesString();

			akkhaInfoBox = createInfoBox(AKKHA_PET_ID, AKKHA, akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.TOTAL), DECIMAL_FORMAT.format(akkhaStats.getTotalPercentageOfDamageDealt()), damage, splits, "");
			infoBoxManager.addInfoBox(akkhaInfoBox);
			akkhaStats.resetStats();
		}
		else if (ZEBAK_COMPLETE.matcher(strippedMessage).find())
		{
			if (zebakStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			zebakStats.setTotalCompletionTime(formatTime(currentTick - zebakStats.getStartTick()));

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("Damage dealt to Zebak - ", DMG_FORMAT.format(zebakStats.getPersonalDamage().get(ZEBAK)) + " (" + DECIMAL_FORMAT.format(zebakStats.getPercentageOfDamageDealt(ZEBAK)) + "%)"));
			}

			String damage = zebakStats.getInfoBoxBossDamageString();
			String splits = zebakStats.getInfoBoxSplitTimesString();

			zebakInfoBox = createInfoBox(ZEBAK_PET_ID, ZEBAK, zebakStats.getTotalCompletionTime(), DECIMAL_FORMAT.format(zebakStats.getTotalPercentageOfDamageDealt()), damage, splits, "");
			infoBoxManager.addInfoBox(zebakInfoBox);
			zebakStats.resetStats();
		}
		else if (OBELISK_COMPLETE_TUMEKEN_SPAWNS.matcher(strippedMessage).find() || OBELISK_COMPLETE_ELIDINIS_SPAWNS.matcher(strippedMessage).find())
		{
			double personal = personalDamage.getOrDefault("Obelisk", 0);
			double total = totalDamage.getOrDefault("Obelisk", 0);
			double percent = (personal / total) * 100;
			int roomTicks;
			String roomCompletionTime = "";
			String damage = "</br>Damage Dealt:</br>";
			messages.clear();

			if (obeliskStartTick > 0)
			{
				roomTicks = client.getTickCount() - obeliskStartTick;
				roomCompletionTime = formatTime(roomTicks);
			}

			if (personal > 0)
			{
				damage += "Obelisk - " + DMG_FORMAT.format(personal);
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Obelisk - ")
									.append(Color.RED, DMG_FORMAT.format(personal) + " (" + DECIMAL_FORMAT.format(percent) + "%)")
									.build()
					);
				}
			}
			obeliskInfoBox = createInfoBox(OBELISK_ICON_ID, "Obelisk", roomCompletionTime, DECIMAL_FORMAT.format(percent), damage, "Kill Time - " + roomCompletionTime, "");
			infoBoxManager.addInfoBox(obeliskInfoBox);
			resetObelisk();
			wardensP2StartTick = client.getTickCount();
		}
		else if (WARDENS_P2_COMPLETE_ELIDINIS_SPAWNS.matcher(strippedMessage).find())
		{
			double personalCoreDamage = personalDamage.getOrDefault("Core", 0);
			double totalCoreDamage = totalDamage.getOrDefault("Core", 0);
			double percentCoreDamage = (personalCoreDamage / totalCoreDamage) * 100;

			double personalShieldDamage = personalDamage.getOrDefault("Tumeken's Warden Shielded", 0);
			double totalShieldDamage = totalDamage.getOrDefault("Tumeken's Warden Shielded", 0);
			double percentShieldDamage = (personalShieldDamage / totalShieldDamage) * 100;

			double personalTotalDamage = personalCoreDamage + personalShieldDamage;
			double totalDamage = totalCoreDamage + totalShieldDamage;
			double percentTotalDamage = (personalTotalDamage / totalDamage) * 100;

			int roomTicks;
			String roomCompletionTime = "";
			String damage = "</br>Damage Dealt:</br>";
			messages.clear();

			if (wardensP2StartTick > 0)
			{
				roomTicks = client.getTickCount() - wardensP2StartTick;
				roomCompletionTime = formatTime(roomTicks);
			}

			if (personalShieldDamage > 0)
			{
				damage += "Tumeken's Warden - " + DMG_FORMAT.format(personalShieldDamage) + " (" + DECIMAL_FORMAT.format(percentShieldDamage) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Tumeken's Warden - ")
									.append(Color.RED, DMG_FORMAT.format(personalShieldDamage) + " (" + DECIMAL_FORMAT.format(percentShieldDamage) + "%)")
									.build()
					);
				}
			}

			if (personalCoreDamage > 0)
			{
				damage += "Core - " + DMG_FORMAT.format(personalCoreDamage) + " (" + DECIMAL_FORMAT.format(percentCoreDamage) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Core - ")
									.append(Color.RED, DMG_FORMAT.format(personalCoreDamage) + " (" + DECIMAL_FORMAT.format(percentCoreDamage) + "%)")
									.build()
					);
				}
			}

			if (personalTotalDamage > 0)
			{
				damage += "Total Damage - " + DMG_FORMAT.format(personalTotalDamage);
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Total damage dealt - ")
									.append(Color.RED, DMG_FORMAT.format(personalTotalDamage) + " (" + DECIMAL_FORMAT.format(percentTotalDamage) + "%)")
									.build()
					);
				}
			}

			wardensP2InfoBox = createInfoBox(TUMEKENS_WARDEN_PET_ID, "P2 Tumeken's Warden", roomCompletionTime, DECIMAL_FORMAT.format(percentTotalDamage), damage, "Kill Time - " + roomCompletionTime, "");
			infoBoxManager.addInfoBox(wardensP2InfoBox);
			resetWardensP2();
			wardensP3StartTick = client.getTickCount();
			foughtElidinisWardenInP3 = true;
		}
		else if (WARDENS_P2_COMPLETE_TUMEKEN_SPAWNS.matcher(strippedMessage).find())
		{
			double personalCoreDamage = personalDamage.getOrDefault("Core", 0);
			double totalCoreDamage = totalDamage.getOrDefault("Core", 0);
			double percentCoreDamage = (personalCoreDamage / totalCoreDamage) * 100;

			double personalShieldDamage = personalDamage.getOrDefault("Elidinis' Warden Shielded", 0);
			double totalShieldDamage = totalDamage.getOrDefault("Elidinis' Warden Shielded", 0);
			double percentShieldDamage = (personalShieldDamage / totalShieldDamage) * 100;

			double personalTotalDamage = personalCoreDamage + personalShieldDamage;
			double totalDamage = totalCoreDamage + totalShieldDamage;
			double percentTotalDamage = (personalTotalDamage / totalDamage) * 100;

			int roomTicks;
			String roomCompletionTime = "";
			String damage = "</br>Damage Dealt:</br>";
			messages.clear();

			if (wardensP2StartTick > 0)
			{
				roomTicks = client.getTickCount() - wardensP2StartTick;
				roomCompletionTime = formatTime(roomTicks);
			}

			if (personalShieldDamage > 0)
			{
				damage += "Elidinis' Warden - " + DMG_FORMAT.format(personalShieldDamage) + " (" + DECIMAL_FORMAT.format(percentShieldDamage) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Elidinis' Warden - ")
									.append(Color.RED, DMG_FORMAT.format(personalShieldDamage) + " (" + DECIMAL_FORMAT.format(percentShieldDamage) + "%)")
									.build()
					);
				}
			}

			if (personalCoreDamage > 0)
			{
				damage += "Core - " + DMG_FORMAT.format(personalCoreDamage) + " (" + DECIMAL_FORMAT.format(percentCoreDamage) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Core - ")
									.append(Color.RED, DMG_FORMAT.format(personalCoreDamage) + " (" + DECIMAL_FORMAT.format(percentCoreDamage) + "%)")
									.build()
					);
				}
			}

			if (personalTotalDamage > 0)
			{
				damage += "Total Damage - " + DMG_FORMAT.format(personalTotalDamage);
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Total damage dealt - ")
									.append(Color.RED, DMG_FORMAT.format(personalTotalDamage) + " (" + DECIMAL_FORMAT.format(percentTotalDamage) + "%)")
									.build()
					);
				}
			}
			wardensP2InfoBox = createInfoBox(ELIDNIS_WARDEN_PET_ID, "P2 Elidinis' Warden", roomCompletionTime, DECIMAL_FORMAT.format(percentTotalDamage), damage, "Kill Time - " + roomCompletionTime, "");
			infoBoxManager.addInfoBox(wardensP2InfoBox);
			resetWardensP2();
			wardensP3StartTick = client.getTickCount();
			foughtElidinisWardenInP3 = false;
		}
		else if (WARDENS_COMPLETE.matcher(strippedMessage).find())
		{
			double personalTotalDamage;
			double totalDamage;
			int infoBoxIconId;
			String bossName;

			if (foughtElidinisWardenInP3)
			{
				personalTotalDamage = personalDamage.getOrDefault("Elidinis' Warden", 0);
				totalDamage = this.totalDamage.getOrDefault("Elidinis' Warden", 0);
				infoBoxIconId = ELIDNIS_WARDEN_PET_ID;
				bossName = "P3 Elidinis' Warden";
			}
			else
			{
				personalTotalDamage = personalDamage.getOrDefault("Tumeken's Warden", 0);
				totalDamage = this.totalDamage.getOrDefault("Tumeken's Warden", 0);
				infoBoxIconId = TUMEKENS_WARDEN_PET_ID;
				bossName = "P3 Tumeken's Warden";
			}

			double wardensP4PersonalDamage = personalTotalDamage - wardensP3PersonalDamage;
			double wardensP4TotalDamage = totalDamage - wardensP3TotalDamage;
			double wardensP3Percent = (wardensP3PersonalDamage / wardensP3TotalDamage) * 100;
			double wardensP4Percent = (wardensP4PersonalDamage / wardensP4TotalDamage) * 100;
			double totalPercent = (personalTotalDamage / totalDamage) * 100;
			int roomTicks;
			int wardensEnrageCompletionTime;
			String roomCompletionTime = "";
			String splits = "Times:</br>";
			String damage = "</br>Damage Dealt:</br>";
			messages.clear();

			if (wardensP3StartTick > 0)
			{
				roomTicks = client.getTickCount() - wardensP3StartTick;
				roomCompletionTime = formatTime(roomTicks);
				wardensEnrageCompletionTime = roomTicks - wardensP3CompletionTime;
				splits += "P3 to Enrage - " + formatTime(wardensP3CompletionTime) +
						"</br>" +
						"Enrage to kill - " + formatTime(wardensEnrageCompletionTime) +
						"</br>" +
						"Total - " + roomCompletionTime;

				if (config.chatboxSplits())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("P3 to Enrage - ")
									.append(Color.RED, formatTime(wardensP3CompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Enrage to kill - ")
									.append(Color.RED, formatTime(wardensEnrageCompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Total time - ")
									.append(Color.RED, roomCompletionTime)
									.build()
					);
				}
			}

			if (energySiphonBossDamage > 0) {
				double percentEnergySiphonDamage = (energySiphonBossDamage / totalDamage) * 100;
				damage += "Energy Siphon damage - " + DMG_FORMAT.format(energySiphonBossDamage) + " (" + DECIMAL_FORMAT.format(percentEnergySiphonDamage) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Energy Siphon damage - ")
									.append(Color.RED, DMG_FORMAT.format(energySiphonBossDamage) + " (" + DECIMAL_FORMAT.format(percentEnergySiphonDamage) + "%)")
									.build()
					);
				}
			}

			if (wardensP3PersonalDamage > 0)
			{
				damage += "P3 to Enrage - " + DMG_FORMAT.format(wardensP3PersonalDamage) + " (" + DECIMAL_FORMAT.format(wardensP3Percent) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("P3 to Enrage damage dealt - ")
									.append(Color.RED, DMG_FORMAT.format(wardensP3PersonalDamage) + " (" + DECIMAL_FORMAT.format(wardensP3Percent) + "%)")
									.build()
					);
				}
			}

			if (wardensP4PersonalDamage > 0)
			{
				damage += "Enrage to kill - " + DMG_FORMAT.format(wardensP4PersonalDamage) + " (" + DECIMAL_FORMAT.format(wardensP4Percent) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Enrage damage dealt - ")
									.append(Color.RED, DMG_FORMAT.format(wardensP4PersonalDamage) + " (" + DECIMAL_FORMAT.format(wardensP4Percent) + "%)")
									.build()
					);
				}
			}

			if (personalTotalDamage > 0)
			{
				damage += "Total Damage - " + DMG_FORMAT.format(personalTotalDamage);
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Total damage dealt - ")
									.append(Color.RED, DMG_FORMAT.format(personalTotalDamage) + " (" + DECIMAL_FORMAT.format(totalPercent) + "%)")
									.build()
					);
				}
			}
			wardensP3InfoBox = createInfoBox(infoBoxIconId, bossName, roomCompletionTime, DECIMAL_FORMAT.format(totalPercent), damage, splits, "");
			infoBoxManager.addInfoBox(wardensP3InfoBox);
			resetWardensP3();
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
		if (!currentlyInsideToA)
		{
			return;
		}

		NPC npc = event.getNpc();
		int npcId = npc.getId();
		int currentTick = client.getTickCount();

		switch (npcId)
		{
			case NpcID.BABA_11780: //Ba-Ba leaps to the top of the room and starts throwing boulders
				if (StringUtils.isEmpty(babaStats.getPhaseCompletionTimes().get(BabaPhase.PHASE_1))) //if a time for phase 1 has not yet been recorded
				{
					babaStats.getPhaseCompletionTimes().put(BabaPhase.PHASE_1, formatTime(currentTick - babaStats.getStartTick()));
				}
				else
				{
					babaStats.getPhaseCompletionTimes().put(BabaPhase.PHASE_2, formatTime(currentTick - babaStats.getPreviousPhaseEndTick()));
				}
				babaStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.BABA: //end of first boulder phase
				babaStats.getPhaseCompletionTimes().put(BabaPhase.BOULDERS_1, formatTime(currentTick - babaStats.getPreviousPhaseEndTick()));
				babaStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.BABA_11779: //end of second boulder phase
				babaStats.getPhaseCompletionTimes().put(BabaPhase.BOULDERS_2, formatTime(currentTick - babaStats.getPreviousPhaseEndTick()));
				babaStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.KEPHRI_11720: //Kephri's shield is depleted and Scarab Swarm phase starts
				if (kephriStats.isFirstShieldDown())
				{
					kephriStats.getPhaseCompletionTimes().put(KephriPhase.SHIELD_1, formatTime(currentTick - kephriStats.getStartTick()));
				}
				else
				{
					kephriStats.getPhaseCompletionTimes().put(KephriPhase.SHIELD_2, formatTime(currentTick - kephriStats.getPreviousPhaseEndTick()));
				}
				kephriStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.KEPHRI: //Kephri starts attacking again after regaining her shield
				if (kephriStats.isFirstShieldDown())
				{
					kephriStats.setFirstShieldDownHealing(kephriStats.getShieldTotalHealing());
					kephriStats.setFirstShieldDown(false);
				}
				kephriStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.KEPHRI_11721: //Kephri's green health bar becomes exposed
				kephriStats.getPhaseCompletionTimes().put(KephriPhase.SHIELD_3, formatTime(currentTick - kephriStats.getPreviousPhaseEndTick()));
				kephriStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.AKKHA_11795: //Enrage phase Akkha
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.TWENTY_TO_ENRAGE, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
				akkhaStats.setPreviousPhaseEndTick(currentTick);
				break;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!currentlyInsideToA)
		{
			return;
		}

		NPC npc = event.getNpc();

		if (npc.getId() == NpcID.AKKHAS_SHADOW)
		{
			int currentTick = client.getTickCount();
			if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.ONE_HUNDRED_TO_EIGHTY)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.ONE_HUNDRED_TO_EIGHTY, formatTime(currentTick - akkhaStats.getStartTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.EIGHTY_TO_SIXTY)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.EIGHTY_TO_SIXTY, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.SIXTY_TO_FORTY)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.SIXTY_TO_FORTY, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.FORTY_TO_TWENTY)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.FORTY_TO_TWENTY, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			akkhaStats.setPreviousPhaseEndTick(currentTick);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (!currentlyInsideToA)
		{
			return;
		}

		NPC npc = event.getNpc();

		if (npc.getId() == NpcID.AKKHAS_SHADOW)
		{
			int currentTick = client.getTickCount();
			if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.SHADOW_1)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.SHADOW_1, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.SHADOW_2)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.SHADOW_2, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.SHADOW_3)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.SHADOW_3, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.SHADOW_4)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.SHADOW_4, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			akkhaStats.setPreviousPhaseEndTick(currentTick);
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
		instanced = client.getLocalPlayer().getWorldView().isInstance();

		if (prevInstance && !instanced) //going from raid into lobby
		{
			resetAll();
		}
		else if (!prevInstance && !instanced) //going from lobby into raid
		{
			resetAll();
			resetAllInfoBoxes();
		}
		else if (!prevInstance && instanced) //going from lobby into anywhere else
		{
			resetAll();
			resetAllInfoBoxes();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!currentlyInsideToA)
		{
			return;
		}

		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) actor;
		String npcName = Text.removeTags(npc.getName());

		if (npcName == null)
		{
			return;
		}

		if (npcName.contains("Scarab"))
		{
			npcName = SCARABS; //group all damage to Scarab Overlords under one name
		}

		Hitsplat hitsplat = event.getHitsplat();

		if (isWardensP2Hitsplat(hitsplat))
		{
			npcName = npcName + " Shielded"; //save shield damage as separate enemy name due to damage Wardens receive from attacking the Core being saved already under Wardens' names
		}

		if (hitsplat.isMine())
		{
			int myDmg = personalDamage.getOrDefault(npcName, 0);
			int totalDmg = totalDamage.getOrDefault(npcName, 0);
			myDmg += hitsplat.getAmount();
			totalDmg += hitsplat.getAmount();
			personalDamage.put(npcName, myDmg);
			totalDamage.put(npcName, totalDmg);
			//TODO remove above lines

			for (BossStats bossStats: bossStats)
			{
				if (bossStats.getEnemyNames().contains(npcName))
				{
					bossStats.addToPersonalDamage(npcName, hitsplat.getAmount());
				}
			}
		}
		else if (hitsplat.isOthers())
		{
			int totalDmg = totalDamage.getOrDefault(npcName, 0);
			totalDmg += hitsplat.getAmount();
			totalDamage.put(npcName, totalDmg);
			//TODO remove above lines

			for (BossStats bossStats: bossStats)
			{
				if (bossStats.getEnemyNames().contains(npcName))
				{
					bossStats.addToTotalDamage(npcName, hitsplat.getAmount());
				}
			}

			if (isAWarden(npcName) && (wardensP3StartTick > -1) && energySiphonsWhereKilled)
			{
				//if a Warden receives damage in P3 and Energy Siphon projectiles were detected, attribute damage to Energy Siphons
				energySiphonBossDamage += hitsplat.getAmount();
				energySiphonsWhereKilled = false;
			}
		}
		else if (hitsplat.getHitsplatType() == HitsplatID.HEAL)
		{
			int healed = totalHealing.getOrDefault(npcName, 0);
			healed += hitsplat.getAmount();
			totalHealing.put(npcName, healed);

			if (isAWarden(npcName))
			{
				if (wardensP4EnrageHeal) //the Wardens heal twice, once at the very start of Phase 3 and once when they enter enrage phase/phase 4
				{
					//on the second heal, record time and damage up to that point
					wardensP3CompletionTime = client.getTickCount() - wardensP3StartTick;
					wardensP3PersonalDamage = personalDamage.getOrDefault(npcName, 0);
					wardensP3TotalDamage = totalDamage.getOrDefault(npcName, 0);
					wardensP4EnrageHeal = false;
				}
				wardensP4EnrageHeal = true;
			}
		}
		else if (hitsplat.getHitsplatType() == KEPHRI_SHIELDED_HEALING_HITSPLAT_ID && npcName.equals(KEPHRI)) //Hitsplat ID is shared with Palm of Resourcefulness
		{
			kephriStats.addToKephriShieldTotalHealing(hitsplat.getAmount());
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event) {
		if (event.getProjectile().getId() == 2226) //ID 2226 is that of the energy siphons (red skulls) as they fly to or from the Warden in P3
		{
			if (areLocalPointsEqual(event.getPosition(), lastEnergySiphonPosition))
			{
				if (!energySiphonsWhereKilled)
				{
					energySiphonsWhereKilled = true;
				}
			}
			lastEnergySiphonPosition = event.getPosition();
		}
	}

	private boolean areLocalPointsEqual(LocalPoint localPointOne, LocalPoint localPointTwo)
	{
		if (localPointOne == null || localPointTwo == null)
		{
			return false;
		}
		else {
			return localPointOne.getX() == localPointTwo.getX() &&
					localPointOne.getY() == localPointTwo.getY() &&
					localPointOne.getWorldView() == localPointTwo.getWorldView();
		}
	}

	private boolean isAWarden(String npcName) {
		if (npcName == null)
		{
			return false;
		}
		return npcName.equalsIgnoreCase("Elidinis' Warden") || npcName.equalsIgnoreCase("Tumeken's Warden");
	}

	private boolean isWardensP2Hitsplat(Hitsplat hitsplat) {
		int hitSplatId = hitsplat.getHitsplatType();
		return hitSplatId == WARDENS_P2_DAMAGE_ME_HITSPLAT_ID
				|| hitSplatId == WARDENS_P2_DAMAGE_MAX_ME_HITSPLAT_ID
				|| hitSplatId == WARDENS_P2_DAMAGE_OTHER_HITSPLAT_ID;
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

	private String getStatsChatMessage(String key, String value)
	{
		return new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append(key)
				.append(Color.RED, value)
				.build();
	}

	private void resetObelisk()
	{
		obeliskStartTick = -1;
		personalDamage.remove("Obelisk");
		totalDamage.remove("Obelisk");
	}

	private void resetWardensP2()
	{
		wardensP2StartTick = -1;
		personalDamage.remove("Elidinis' Warden");
		totalDamage.remove("Elidinis' Warden");
		personalDamage.remove("Tumeken's Warden");
		totalDamage.remove("Tumeken's Warden");
		personalDamage.remove("Elidinis' Warden Shielded");
		totalDamage.remove("Elidinis' Warden Shielded");
		personalDamage.remove("Tumeken's Warden Shielded");
		totalDamage.remove("Tumeken's Warden Shielded");
		personalDamage.remove("Core");
		totalDamage.remove("Core");
	}

	private void resetWardensP3()
	{
		wardensP3StartTick = -1;
		wardensP3CompletionTime = 0;
		wardensP3PersonalDamage = 0;
		wardensP3TotalDamage = 0;
		wardensP4EnrageHeal = false;
		energySiphonBossDamage = 0;
		energySiphonsWhereKilled = false;
		lastEnergySiphonPosition = null;
		personalDamage.clear();
		totalDamage.clear();
		totalHealing.clear();
	}

	private void resetAll()
	{
		babaStats.resetStats();
		kephriStats.resetStats();
		akkhaStats.resetStats();
		zebakStats.resetStats();
		resetObelisk();
		resetWardensP2();
		resetWardensP3();
	}

	private void resetWardensInfoBoxes()
	{
		infoBoxManager.removeInfoBox(obeliskInfoBox);
		infoBoxManager.removeInfoBox(wardensP2InfoBox);
		infoBoxManager.removeInfoBox(wardensP3InfoBox);
	}

	private void resetAllInfoBoxes()
	{
		infoBoxManager.removeInfoBox(babaInfoBox);
		infoBoxManager.removeInfoBox(kephriInfoBox);
		infoBoxManager.removeInfoBox(akkhaInfoBox);
		infoBoxManager.removeInfoBox(zebakInfoBox);
		infoBoxManager.removeInfoBox(obeliskInfoBox);
		infoBoxManager.removeInfoBox(wardensP2InfoBox);
		infoBoxManager.removeInfoBox(wardensP3InfoBox);
	}
}
