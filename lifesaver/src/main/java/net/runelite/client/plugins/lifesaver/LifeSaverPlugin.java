package net.runelite.client.plugins.lifesaver;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.oneclickutils.OneClickUtilsPlugin;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Random;
import java.util.Set;

import static net.runelite.api.ItemID.*;

@SuppressWarnings("ALL")
@Extension
@PluginDescriptor(
	name = "BD Life Saver",
	description = "BD life Saver Plugin"
)

@Slf4j
@PluginDependency(OneClickUtilsPlugin.class)
public class LifeSaverPlugin extends Plugin {
	@Inject
	private OneClickUtilsPlugin oneClickUtilsPlugin;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Client client;
	@Inject
	private LifeSaverConfig config;
	@Inject
	private InventoryUtils inventory;
	@Inject
	OverlayManager overlayManager;
	@Inject
	private LifeSaverOverlay overlay;

	@Provides
	LifeSaverConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(LifeSaverConfig.class);
	}

	enum State{
		RUNNING,
		BREAKING,
		STOPPED
	}

	private static final WorldArea prifSpawnArea = new WorldArea(3253,6072,25,25,0);
	private int max = 35;
	private int min = 9;
	Random random = new Random();
	private int randomCoinPouchSize = 28;
	Set<Integer> pouches = Set.of(COIN_POUCH_22531,COIN_POUCH_22532,COIN_POUCH_22523,COIN_POUCH_22534);
	String openPouch = "BD Open Pouch";
	public Instant startTime;
	public Instant nextBreakStartTime;
	public Instant nextResumeStartTime;
	String timeFormat;
	State state;
	String stopReason;
	int totalBreaks;
	int ticksCache;

	@Override
	protected void startUp() {
		resetBreaks();
		overlayManager.add(overlay);
		randomizePouch();
		startTime = Instant.now();
	}

	@Override
	protected void shutDown() {
		overlayManager.remove(overlay);
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals("BDLifeSaverPlugin")) {
			return;
		}

		if (event.getKey().equals("takeBreaks")){
			resetBreaks();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		handleClick(event);
	}

	private void handleClick(MenuOptionClicked event) {
		if (prifSpawnArea.contains(client.getLocalPlayer().getWorldLocation())){
			event.setMenuEntry(oneClickUtilsPlugin.maxCapeTeleToPOH());
			stop("Found you in prif");
			return;
		}

		if (event.getMenuOption().contains(openPouch)){
			event.setMenuEntry(oneClickUtilsPlugin.clickItem(inventory.getWidgetItem(pouches)));
			return;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (state == State.STOPPED){
			return;
		}

		if (oneClickUtilsPlugin.getTicksSinceLastXpDrop() > config.watchDogTickTimer() && config.watchDogTickTimer() > 0 && state == state.RUNNING){
			stop("XP Tick Watchdog went off");
			return;
		}

		if (config.takeBreaks()){
			if(shouldBreak()){
				state = State.BREAKING;
				ticksCache = oneClickUtilsPlugin.getTicksSinceLastXpDrop();
				setClickerEnabled(false);
				nextBreakStartTime = null;
				scheduleResumeTime();
				totalBreaks++;
				if (config.stopAfterBreaks() > 0 && totalBreaks >= config.stopAfterBreaks()){
					stop("Stopping after " + totalBreaks + " breaks");
				}
			}else if(shouldResume()){
				state = State.RUNNING;
				oneClickUtilsPlugin.setTicksSinceLastXpDrop(ticksCache);
				setClickerEnabled(true);
				nextResumeStartTime = null;
				scheduleBreakTime();
			}
		}
	}



	@Subscribe
	private void onChatMessage(ChatMessage event){
		if (event.getMessage().contains("You are out of food")){
			stop(event.getMessage());
		}
		if (event.getMessage().contains("You open all of the pouches")){
			randomizePouch();
		}
	}

	@Subscribe
	private void onClientTick(ClientTick event) {
		if(inventory.containsItemAmount(pouches, Math.min(randomCoinPouchSize,28), true, false)){
			client.insertMenuItem(openPouch,"", MenuAction.UNKNOWN.getId(),0,0,0,false);
		}
	}

	private void randomizePouch(){
		randomCoinPouchSize = random.nextInt(max - min + 1) + min;
	}

	private boolean shouldBreak() {
		return nextBreakStartTime == null ? false : nextBreakStartTime.isBefore(Instant.now());
	}

	private boolean shouldResume(){
		return nextResumeStartTime == null ? false : nextResumeStartTime.isBefore(Instant.now());
	}

	private void scheduleBreakTime(){
		if (config.breakMinMinutes() > config.breakMaxMinutes()){
			stop("Config min larger than config max");
			return;
		}
		long randomTime =  random.nextInt(config.runMaxMinutes()*60000 - config.runMinMinutes()*60000 + 1) + config.runMinMinutes()*60000;
		nextBreakStartTime = Instant.ofEpochMilli(System.currentTimeMillis() + randomTime);
	}

	private void scheduleResumeTime(){
		if (config.runMinMinutes() > config.runMaxMinutes()){
			stop("Config min larger than config max");
			return;
		}
		long randomTime =  random.nextInt(config.breakMaxMinutes()*60000 - config.breakMinMinutes()*60000 + 1) + config.breakMinMinutes()*60000;
		nextResumeStartTime = Instant.ofEpochMilli(System.currentTimeMillis() + randomTime);
	}

	private void stop(String reason){
		state = State.STOPPED;
		setClickerEnabled(false);
		log.info("Life Saver Stopping: " + reason);
		oneClickUtilsPlugin.sendGameMessage("Life Saver Stopping: " + reason);
		stopReason = reason;
	}

	private void resetBreaks(){
		state = state.RUNNING;
		oneClickUtilsPlugin.setTicksSinceLastXpDrop(0);
		setClickerEnabled(true);
		if (config.takeBreaks()){
			totalBreaks = 0;
			scheduleBreakTime();
			nextResumeStartTime = null;
		}else{
			totalBreaks = -1;
			nextBreakStartTime = null;
			nextResumeStartTime = null;
		}
	}

	private void setClickerEnabled(boolean enabled){
		configManager.setConfiguration("autoprayflick", "onlyInNmz", !enabled);
	}
}