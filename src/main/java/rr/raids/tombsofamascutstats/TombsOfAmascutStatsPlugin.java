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
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Tombs of Amascut Stats",
	description = "Tombs of Amascut phase times and damage tracker",
	tags = {"combat", "raid", "pve", "pvm", "bosses", "toa"},
	enabledByDefault = false
)
public class TombsOfAmascutStatsPlugin extends Plugin
{
	private static final DecimalFormat DMG_FORMAT = new DecimalFormat("#,##0");
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.0");
	private static final int PRECISE_TIMER = 11866;
	private static final int TICK_LENGTH = 600;
	private static final int WARDENS_P2_DAMAGE_ME_HITSPLAT_ID = 53;
	private static final int WARDENS_P2_DAMAGE_OTHER_HITSPLAT_ID = 54;
	private static final int WARDENS_P2_DAMAGE_MAX_ME_HITSPLAT_ID = 55;
	private static final int BABA_PET_ID = 27383;
	private static final int KEPHRI_SHIELDED_HEALING_HITSPLAT_ID = 11;
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

	private static final Set<String> ENEMY_NAMES = ImmutableSet.of(
			"Ba-Ba",
			"Kephri", "Spitting Scarab", "Arcane Scarab", "Soldier Scarab",
			"Akkha", "Akkha's Shadow",
			"Zebak",
			"Obelisk", "Tumeken's Warden", "Elidinis' Warden", "Core", "Energy Siphon"
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

	private TombsOfAmascutStatsStatsInfoBox babaInfoBox;
	private TombsOfAmascutStatsStatsInfoBox kephriInfoBox;
	private TombsOfAmascutStatsStatsInfoBox akkhaInfoBox;
	private TombsOfAmascutStatsStatsInfoBox zebakInfoBox;
	private TombsOfAmascutStatsStatsInfoBox obeliskInfoBox;
	private TombsOfAmascutStatsStatsInfoBox wardensP2InfoBox;
	private TombsOfAmascutStatsStatsInfoBox wardensP3InfoBox;

	private boolean toaInside;
	private boolean instanced;
	private boolean preciseTimers;

	private int babaStartTick = -1;
	private int babaPhase1CompletionTime;
	private int babaBoulder1CompletionTime;
	private int babaPhase2CompletionTime;
	private int babaBoulder2CompletionTime;

	private int kephriStartTick = -1;
	private int kephriPhase1CompletionTime;
	private int kephriPhase2StartTick;
	private int kephriPhase2CompletionTime;
	private int kephriPhase3StartTick;
	private int kephriPhase3CompletionTime;
	private int kephriPhase4StartTick;
	private boolean kephriFirstShieldDown = true;
	private int kephriFirstDownHealing;
	private int kephriSecondDownHealing;
	private int kephriTotalHealing;

	private int akkhaStartTick = -1;
	private int akkhasShadowOneStartTick;
	private int akkha80PercentStartTick;
	private int akkhasShadowTwoStartTick;
	private int akkha60PercentStartTick;
	private int akkhasShadowThreeStartTick;
	private int akkha40PercentStartTick;
	private int akkhasShadowFourStartTick;
	private int akkha20PercentStartTick;
	private int akkhaEnrageStartTick;

	private int zebakStartTick = -1;

	private int obeliskStartTick = -1;
	private int wardensP2StartTick = -1;
	private int wardensP3StartTick = -1;
	private boolean foughtElidinisWardenInP3;
	private boolean wardensP4EnrageHeal = false;
	private int wardensP3CompletionTime;
	private int energySiphonBossDamage;
	private double wardensP3PersonalDamage;
	private double wardensP3TotalDamage;

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

		int region = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
		toaInside = TOA_ROOM_IDS.contains(region);

		int preciseTimerVar = client.getVarbitValue(PRECISE_TIMER);
		preciseTimers = preciseTimerVar == 1 ;

		if (!toaInside)
		{
			resetAll();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!toaInside || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String strippedMessage = Text.removeTags(event.getMessage());
		List<String> messages = new ArrayList<>(Collections.emptyList());

		if (BABA_STARTED.matcher(strippedMessage).find())
		{
			resetBaba();
			babaStartTick = client.getTickCount();
		}
		else if (KEPHRI_STARTED.matcher(strippedMessage).find())
		{
			resetKephri();
			kephriStartTick = client.getTickCount();
		}
		else if (AKKHA_STARTED.matcher(strippedMessage).find())
		{
			resetAkkha();
			akkhaStartTick = client.getTickCount();
		}
		else if (ZEBAK_STARTED.matcher(strippedMessage).find())
		{
			resetZebak();
			zebakStartTick = client.getTickCount();
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
			double personal = personalDamage.getOrDefault("Ba-Ba", 0);
			double total = totalDamage.getOrDefault("Ba-Ba", 0);
			double percent = (personal / total) * 100;
			int roomTicks;
			int babaPhase3CompletionTime;
			String roomCompletionTime = "";
			String splits = "Times:</br>";
			String damage = "</br>Damage Dealt:</br>";
			messages.clear();

			if (babaStartTick > 0)
			{
				roomTicks = client.getTickCount() - babaStartTick;
				roomCompletionTime = formatTime(roomTicks);
				babaPhase3CompletionTime = roomTicks - babaPhase1CompletionTime - babaBoulder1CompletionTime - babaPhase2CompletionTime - babaBoulder2CompletionTime;
				splits += "Phase 1 - " + formatTime(babaPhase1CompletionTime) +
						"</br>" +
						"Boulders 1 - " + formatTime(babaBoulder1CompletionTime) +
						"</br>" +
						"Phase 2 - " + formatTime(babaPhase2CompletionTime) +
						"</br>" +
						"Boulders 2 - " + formatTime(babaBoulder2CompletionTime) +
						"</br>" +
						"Phase 3 - " + formatTime(babaPhase3CompletionTime) +
						"</br>" +
						"Total - " + roomCompletionTime;
				if (config.chatboxSplits())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Phase 1 - ")
									.append(Color.RED, formatTime(babaPhase1CompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Boulders 1 - ")
									.append(Color.RED, formatTime(babaBoulder1CompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Phase 2 - ")
									.append(Color.RED, formatTime(babaPhase2CompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Boulders 2 - ")
									.append(Color.RED, formatTime(babaBoulder2CompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Phase 3 - ")
									.append(Color.RED, formatTime(babaPhase3CompletionTime))
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

			if (personal > 0)
			{
				damage += "Ba-Ba - " + DMG_FORMAT.format(personal);
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Ba-Ba - ")
									.append(Color.RED, DMG_FORMAT.format(personal) + " (" + DECIMAL_FORMAT.format(percent) + "%)")
									.build()
					);
				}
			}
			babaInfoBox = createInfoBox(BABA_PET_ID, "Ba-Ba", roomCompletionTime, DECIMAL_FORMAT.format(percent), damage, splits, "");
			infoBoxManager.addInfoBox(babaInfoBox);
			resetBaba();
		}
		else if (KEPHRI_COMPLETE.matcher(strippedMessage).find())
		{
			double personalKephri = personalDamage.getOrDefault("Kephri", 0);
			double totalKephri = totalDamage.getOrDefault("Kephri", 0);
			double percentKephri = (personalKephri / totalKephri) * 100;

			double personalScarab = personalDamage.getOrDefault("Scarabs", 0);
			double totalScarab = totalDamage.getOrDefault("Scarabs", 0);
			double percentScarab = (personalScarab / totalScarab) * 100;

			double personalTotalDamage = personalKephri + personalScarab;
			double totalDamage = totalKephri + totalScarab;
			double percentTotalDamage = (personalTotalDamage / totalDamage) * 100;

			int roomTicks;
			int kephriPhase4CompletionTime;
			String roomCompletionTime = "";
			String splits = "Times:</br>";
			String damage = "</br>Damage Dealt:</br>";
			String healing = "</br>Boss Healing:" +
					"</br>" +
					"First down healed - " + DMG_FORMAT.format(kephriFirstDownHealing) +
					"</br>" +
					"Second down healed - " + DMG_FORMAT.format(kephriSecondDownHealing) +
					"</br>" +
					"Total shield healed - " + DMG_FORMAT.format(kephriTotalHealing);
			messages.clear();

			if (kephriStartTick > 0)
			{
				roomTicks = client.getTickCount() - kephriStartTick;
				roomCompletionTime = formatTime(roomTicks);
				kephriPhase4CompletionTime = client.getTickCount() - kephriPhase4StartTick;

				splits += "Shield 1 - " + formatTime(kephriPhase1CompletionTime) +
						"</br>" +
						"Shield 2 - " + formatTime(kephriPhase2CompletionTime) +
						"</br>" +
						"Shield 3 - " + formatTime(kephriPhase3CompletionTime) +
						"</br>" +
						"Green Health - " + formatTime(kephriPhase4CompletionTime) +
						"</br>" +
						"Total - " + roomCompletionTime;
				if (config.chatboxSplits())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Shield 1 - ")
									.append(Color.RED, formatTime(kephriPhase1CompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Shield 2 - ")
									.append(Color.RED, formatTime(kephriPhase2CompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Shield 3 - ")
									.append(Color.RED, formatTime(kephriPhase3CompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Green Health - ")
									.append(Color.RED, formatTime(kephriPhase4CompletionTime))
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

			if (personalKephri > 0)
			{
				damage+= "Kephri - " + DMG_FORMAT.format(personalKephri) + " (" +DECIMAL_FORMAT.format(percentKephri) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("First down shield healed - ")
									.append(Color.RED, DMG_FORMAT.format(kephriFirstDownHealing))
									.build()
					);
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Second down shield healed - ")
									.append(Color.RED, DMG_FORMAT.format(kephriSecondDownHealing))
									.build()
					);
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Total shield healed - ")
									.append(Color.RED, DMG_FORMAT.format(kephriTotalHealing))
									.build()
					);
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Kephri - ")
									.append(Color.RED, DMG_FORMAT.format(personalKephri) + " (" + DECIMAL_FORMAT.format(percentKephri) + "%)")
									.build()
					);
				}
			}

			if (personalScarab > 0)
			{
				damage+= "Scarabs - " + DMG_FORMAT.format(personalScarab) + " (" +DECIMAL_FORMAT.format(percentScarab) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Scarabs - ")
									.append(Color.RED, DMG_FORMAT.format(personalScarab) + " (" + DECIMAL_FORMAT.format(percentScarab) + "%)")
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

			kephriInfoBox = createInfoBox(KEPHRI_PET_ID, "Kephri", roomCompletionTime, DECIMAL_FORMAT.format(percentTotalDamage), damage, splits, healing);
			infoBoxManager.addInfoBox(kephriInfoBox);
			resetKephri();
		}
		else if (AKKHA_COMPLETE.matcher(strippedMessage).find())
		{
			double personalAkkhaDamage = personalDamage.getOrDefault("Akkha", 0);
			double totalAkkhaDamage = totalDamage.getOrDefault("Akkha", 0);
			double percentAkkhaDamage = (personalAkkhaDamage / totalAkkhaDamage) * 100;

			double personalShadowDamage = personalDamage.getOrDefault("Akkha's Shadow", 0);
			double totalShadowDamage = totalDamage.getOrDefault("Akkha's Shadow", 0);
			double percentShadowDamage = (personalShadowDamage / totalShadowDamage) * 100;

			double personalTotalDamage = personalAkkhaDamage + personalShadowDamage;
			double totalDamage = totalAkkhaDamage + totalShadowDamage;
			double percentTotalDamage = (personalTotalDamage / totalDamage) * 100;

			int akkhaPhaseOneCompletionTime = akkhasShadowOneStartTick - akkhaStartTick;
			int akkhaShadowOneCompletionTime = akkha80PercentStartTick - akkhasShadowOneStartTick;
			int akkhaPhaseTwoCompletionTime = akkhasShadowTwoStartTick - akkha80PercentStartTick;
			int akkhaShadowTwoCompletionTime = akkha60PercentStartTick - akkhasShadowTwoStartTick;
			int akkhaPhaseThreeCompletionTime = akkhasShadowThreeStartTick - akkha60PercentStartTick;
			int akkhaShadowThreeCompletionTime = akkha40PercentStartTick - akkhasShadowThreeStartTick;
			int akkhaPhaseFourCompletionTime = akkhasShadowFourStartTick - akkha40PercentStartTick;
			int akkhaShadowFourCompletionTime = akkha20PercentStartTick - akkhasShadowFourStartTick;
			int akkhaPhaseFiveCompletionTime = akkhaEnrageStartTick - akkha20PercentStartTick;
			int akkhaEnrageCompletionTime = client.getTickCount() - akkhaEnrageStartTick;

			int roomTicks;
			String roomCompletionTime = "";
			String splits = "Times:</br>";
			String damage = "</br>Damage Dealt:</br>";
			messages.clear();

			if (akkhaStartTick > 0)
			{
				roomTicks = client.getTickCount() - akkhaStartTick;
				roomCompletionTime = formatTime(roomTicks);
				splits += "100%-80% - " + formatTime(akkhaPhaseOneCompletionTime) +
						"</br>" +
						"Shadow 1 - " + formatTime(akkhaShadowOneCompletionTime) +
						"</br>" +
						"80%-60% - " + formatTime(akkhaPhaseTwoCompletionTime) +
						"</br>" +
						"Shadow 2 - " + formatTime(akkhaShadowTwoCompletionTime) +
						"</br>" +
						"60%-40% - " + formatTime(akkhaPhaseThreeCompletionTime) +
						"</br>" +
						"Shadow 3 - " + formatTime(akkhaShadowThreeCompletionTime) +
						"</br>" +
						"40%-20% - " + formatTime(akkhaPhaseFourCompletionTime) +
						"</br>" +
						"Shadow 4 - " + formatTime(akkhaShadowFourCompletionTime) +
						"</br>" +
						"20%-0% - " + formatTime(akkhaPhaseFiveCompletionTime) +
						"</br>" +
						"Enrage - " + formatTime(akkhaEnrageCompletionTime) +
						"</br>" +
						"Total - " + roomCompletionTime;

				if (config.chatboxSplits())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("100%-80% - ")
									.append(Color.RED, formatTime(akkhaPhaseOneCompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Shadow 1 - ")
									.append(Color.RED, formatTime(akkhaShadowOneCompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("80%-60% - ")
									.append(Color.RED, formatTime(akkhaPhaseTwoCompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Shadow 2 - ")
									.append(Color.RED, formatTime(akkhaShadowTwoCompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("60%-40% - ")
									.append(Color.RED, formatTime(akkhaPhaseThreeCompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Shadow 3 - ")
									.append(Color.RED, formatTime(akkhaShadowThreeCompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("40%-20% - ")
									.append(Color.RED, formatTime(akkhaPhaseFourCompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Shadow 4 - ")
									.append(Color.RED, formatTime(akkhaShadowFourCompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("20%-0% - ")
									.append(Color.RED, formatTime(akkhaPhaseFiveCompletionTime))
									.build()
					);

					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Enrage - ")
									.append(Color.RED, formatTime(akkhaEnrageCompletionTime))
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

			if (personalAkkhaDamage > 0)
			{
				damage += "Akkha - " + DMG_FORMAT.format(personalAkkhaDamage) + " (" +DECIMAL_FORMAT.format(percentAkkhaDamage) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Akkha - ")
									.append(Color.RED, DMG_FORMAT.format(personalAkkhaDamage) + " (" + DECIMAL_FORMAT.format(percentAkkhaDamage) + "%)")
									.build()
					);
				}
			}

			if (personalShadowDamage > 0)
			{
				damage += "Akkha's Shadows - " + DMG_FORMAT.format(personalShadowDamage) + " (" +DECIMAL_FORMAT.format(percentShadowDamage) + "%)" + "</br>";
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Akkha's Shadows - ")
									.append(Color.RED, DMG_FORMAT.format(personalShadowDamage) + " (" + DECIMAL_FORMAT.format(percentShadowDamage) + "%)")
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

			akkhaInfoBox = createInfoBox(AKKHA_PET_ID, "Akkha", roomCompletionTime, DECIMAL_FORMAT.format(percentTotalDamage), damage, splits, "");
			infoBoxManager.addInfoBox(akkhaInfoBox);
			resetAkkha();
		}
		else if (ZEBAK_COMPLETE.matcher(strippedMessage).find())
		{
			double personal = personalDamage.getOrDefault("Zebak", 0);
			double total = totalDamage.getOrDefault("Zebak", 0);
			double percent = (personal / total) * 100;
			int roomTicks;
			String roomCompletionTime = "";
			String damage = "</br>Damage Dealt:</br>";
			messages.clear();

			if (zebakStartTick > 0)
			{
				roomTicks = client.getTickCount() - zebakStartTick;
				roomCompletionTime = formatTime(roomTicks);
			}

			if (personal > 0)
			{
				damage += "Zebak - " + DMG_FORMAT.format(personal);
				if (config.chatboxDmg())
				{
					messages.add(
							new ChatMessageBuilder()
									.append(ChatColorType.NORMAL)
									.append("Damage dealt to Zebak - ")
									.append(Color.RED, DMG_FORMAT.format(personal) + " (" + DECIMAL_FORMAT.format(percent) + "%)")
									.build()
					);
				}
			}
			zebakInfoBox = createInfoBox(ZEBAK_PET_ID, "Zebak", roomCompletionTime, DECIMAL_FORMAT.format(percent), damage, "Kill Time - " + roomCompletionTime, "");
			infoBoxManager.addInfoBox(zebakInfoBox);
			resetZebak();
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
		if (!toaInside)
		{
			return;
		}

		NPC npc = event.getNpc();
		int npcId = npc.getId();

		switch (npcId)
		{
			case NpcID.BABA_11780: //Ba-Ba leaps to the top of the room and starts throwing boulders
				if (babaPhase1CompletionTime == 0)
				{
					babaPhase1CompletionTime = client.getTickCount() - babaStartTick;
				}
				else
				{
					babaPhase2CompletionTime = client.getTickCount() - babaStartTick - babaPhase1CompletionTime - babaBoulder1CompletionTime;
				}
				break;
			case NpcID.BABA: //end of first boulder phase
				babaBoulder1CompletionTime = client.getTickCount() - babaStartTick - babaPhase1CompletionTime;
				break;
			case NpcID.BABA_11779: //end of second boulder phase
				babaBoulder2CompletionTime = client.getTickCount() - babaStartTick - babaPhase1CompletionTime - babaBoulder1CompletionTime - babaPhase2CompletionTime;
				break;
			case NpcID.KEPHRI_11720: //Kephri's shield is depleted and Scarab Swarm phase starts
				if (kephriFirstShieldDown)
				{
					kephriPhase1CompletionTime = client.getTickCount() - kephriStartTick;
				}
				else
				{
					kephriPhase2CompletionTime = client.getTickCount() - kephriPhase2StartTick;
				}
				break;
			case NpcID.KEPHRI: //Kephri starts attacking again after regaining her shield
				if (kephriFirstShieldDown)
				{
					kephriFirstDownHealing = kephriTotalHealing;
					kephriFirstShieldDown = false;
					kephriPhase2StartTick = client.getTickCount();
				}
				else
				{
					kephriSecondDownHealing = kephriTotalHealing - kephriFirstDownHealing;
					kephriPhase3StartTick = client.getTickCount();
				}
				break;
			case NpcID.KEPHRI_11721: //Kephri's green health bar becomes exposed
				kephriPhase3CompletionTime = client.getTickCount() - kephriPhase3StartTick;
				kephriPhase4StartTick = client.getTickCount();
				break;
			case NpcID.AKKHA_11795:
				akkhaEnrageStartTick = client.getTickCount();
				break;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!toaInside)
		{
			return;
		}

		NPC npc = event.getNpc();
		int npcId = npc.getId();

		switch (npcId)
		{
			case NpcID.AKKHAS_SHADOW:
				if (akkhasShadowOneStartTick == 0)
				{
					akkhasShadowOneStartTick = client.getTickCount();
				}
				else if (akkhasShadowTwoStartTick == 0 && akkha80PercentStartTick > 0)
				{
					akkhasShadowTwoStartTick = client.getTickCount();
				}
				else if (akkhasShadowThreeStartTick == 0 && akkha60PercentStartTick > 0)
				{
					akkhasShadowThreeStartTick = client.getTickCount();
				}
				else if (akkhasShadowFourStartTick == 0 && akkha40PercentStartTick > 0)
				{
					akkhasShadowFourStartTick = client.getTickCount();
				}
				break;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (!toaInside)
		{
			return;
		}

		NPC npc = event.getNpc();
		int npcId = npc.getId();

		switch (npcId)
		{
			case NpcID.AKKHAS_SHADOW:
				if (akkha80PercentStartTick == 0)
				{
					akkha80PercentStartTick = client.getTickCount();
				}
				else if (akkha60PercentStartTick == 0 && akkhasShadowTwoStartTick > 0)
				{
					akkha60PercentStartTick = client.getTickCount();
				}
				else if (akkha40PercentStartTick == 0 && akkhasShadowThreeStartTick > 0)
				{
					akkha40PercentStartTick = client.getTickCount();
				}
				else if (akkha20PercentStartTick == 0 && akkhasShadowFourStartTick > 0)
				{
					akkha20PercentStartTick = client.getTickCount();
				}
				break;
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
		if (!toaInside)
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

		if (npcName == null || !(ENEMY_NAMES.contains(npcName)))
		{
			return;
		}

		if (npcName.contains("Scarab"))
		{
			npcName = "Scarabs"; //group all Scarab damage under one name
		}

		Hitsplat hitsplat = event.getHitsplat();

		if (isWardensP2Hitsplat(hitsplat))
		{
			npcName = npcName + " Shielded"; //save shield damage as separate enemy name due to damage Wardens receive from attacking the Core being saved already under Wardens' names
		}

		if (hitsplat.isMine() || hitsplat.getHitsplatType() == WARDENS_P2_DAMAGE_ME_HITSPLAT_ID || hitsplat.getHitsplatType() == WARDENS_P2_DAMAGE_MAX_ME_HITSPLAT_ID)
		{
			int myDmg = personalDamage.getOrDefault(npcName, 0);
			int totalDmg = totalDamage.getOrDefault(npcName, 0);
			myDmg += hitsplat.getAmount();
			totalDmg += hitsplat.getAmount();
			personalDamage.put(npcName, myDmg);
			totalDamage.put(npcName, totalDmg);
		}
		else if (hitsplat.isOthers() || hitsplat.getHitsplatType() == WARDENS_P2_DAMAGE_OTHER_HITSPLAT_ID)
		{
			int totalDmg = totalDamage.getOrDefault(npcName, 0);
			totalDmg += hitsplat.getAmount();
			totalDamage.put(npcName, totalDmg);

			if ((npcName.equals("Elidinis' Warden") || npcName.equals("Tumeken's Warden")) && (wardensP3StartTick >= 0) && (event.getActor().getInteracting() == null))
			{
				//if a Warden receives damage in P3 but no player is currently interacting with the Warden, attribute damage to Energy Siphons
				energySiphonBossDamage += hitsplat.getAmount();
			}
		}
		else if (hitsplat.getHitsplatType() == HitsplatID.HEAL)
		{
			int healed = totalHealing.getOrDefault(npcName, 0);
			healed += hitsplat.getAmount();
			totalHealing.put(npcName, healed);

			if (npcName.equals("Elidinis' Warden") || npcName.equals("Tumeken's Warden"))
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
		else if (hitsplat.getHitsplatType() == KEPHRI_SHIELDED_HEALING_HITSPLAT_ID && npcName.equals("Kephri")) //Hitsplat ID is shared with Palm of Resourcefulness
		{
			kephriTotalHealing += hitsplat.getAmount();
		}
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

	private void resetBaba()
	{
		babaStartTick = -1;
		babaPhase1CompletionTime = 0;
		babaBoulder1CompletionTime = 0;
		babaPhase2CompletionTime = 0;
		babaBoulder2CompletionTime = 0;
		personalDamage.remove("Ba-Ba");
		totalDamage.remove("Ba-Ba");
	}

	private void resetKephri()
	{
		kephriStartTick = -1;
		kephriPhase1CompletionTime = 0;
		kephriPhase2StartTick = 0;
		kephriPhase2CompletionTime = 0;
		kephriPhase3StartTick = 0;
		kephriPhase3CompletionTime = 0;
		kephriPhase4StartTick = 0;
		kephriFirstDownHealing = 0;
		kephriSecondDownHealing = 0;
		kephriTotalHealing = 0;
		kephriFirstShieldDown = true;
		personalDamage.remove("Kephri");
		totalDamage.remove("Kephri");
		personalDamage.remove("Scarabs");
		totalDamage.remove("Scarabs");
	}

	private void resetAkkha()
	{
		akkhaStartTick = -1;
		personalDamage.remove("Akkha");
		totalDamage.remove("Akkha");
		personalDamage.remove("Akkha's Shadow");
		totalDamage.remove("Akkha's Shadow");
		akkhasShadowOneStartTick = 0;
		akkha80PercentStartTick = 0;
		akkhasShadowTwoStartTick = 0;
		akkha60PercentStartTick = 0;
		akkhasShadowThreeStartTick = 0;
		akkha40PercentStartTick = 0;
		akkhasShadowFourStartTick = 0;
		akkha20PercentStartTick = 0;
		akkhaEnrageStartTick = 0;
	}

	private void resetZebak()
	{
		zebakStartTick = -1;
		personalDamage.remove("Zebak");
		totalDamage.remove("Zebak");
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
		personalDamage.clear();
		totalDamage.clear();
		totalHealing.clear();
	}

	private void resetAll()
	{
		resetBaba();
		resetKephri();
		resetAkkha();
		resetZebak();
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
