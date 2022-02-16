package net.runelite.client.plugins.testplugin;


import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.queries.NPCQuery;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.oneclickutils.BankTele;
import net.runelite.client.plugins.oneclickutils.LegacyMenuEntry;
import net.runelite.client.plugins.oneclickutils.OneClickUtilsPlugin;
import org.pf4j.Extension;
import net.runelite.rs.api.RSClient;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@Extension
@PluginDescriptor(
	name = "BD Test Plugin",
	description = "BD Test Plugin"
)

@Slf4j
@PluginDependency(OneClickUtilsPlugin.class)
public class TestPlugin extends Plugin
{
	// Injects our config
	@Inject
	private TestConfig config;
	@Inject
	private OneClickUtilsPlugin oneClickUtilsPlugin;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Client client;

	private static final WorldArea prifSpawnArea = new WorldArea(3253,6072,25,25,0);
	Set<String> herbMenuOption = Set.of("Clean");

	// Provides our config
	@Provides
	TestConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TestConfig.class);
	}

	private Queue<LegacyMenuEntry> actionQueue = new LinkedList<LegacyMenuEntry>();

	@Override
	protected void startUp() {
		actionQueue.clear();
	}

	@Override
	protected void shutDown() {
	}


	@Subscribe
	private void onClientTick(ClientTick event) {
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		handleClick(event);
		log.info(event.getMenuOption() + ", "
				+ event.getMenuTarget() + ", "
				+ event.getId() + ", "
				+ event.getMenuAction().name() + ", "
				+ event.getParam0() + ", "
				+ event.getParam1());
	}

	private void handleClick(MenuOptionClicked event) {
		//Only intercept cancel or oneclicktest options
		if (!event.getMenuOption().equals("Cancel")) {
			return;
		}
		log.info("In handle click");

//		WidgetItem herbs = oneClickUtilsPlugin.getItemMenu(herbMenuOption, Set.of());
//		if (herbs == null){
//
//		}

	}




	@Subscribe
	private void onGameTick(GameTick gameTick) {
		if(oneClickUtilsPlugin.getTicksSinceLastXpDrop() > 100){
			configManager.setConfiguration("autoprayflick", "onlyInNmz", true);
		}else{
			configManager.setConfiguration("autoprayflick", "onlyInNmz", false);
		}
	}

	private void testLogout(MenuOptionClicked event) {
		if(actionQueue.isEmpty()) {
			actionQueue.add(oneClickUtilsPlugin.logout());
		}
		if(!actionQueue.isEmpty()){
			log.info(actionQueue.toString());
			event.setMenuEntry(actionQueue.poll());
		}
	}

	private void testMaxCapePOHTele(MenuOptionClicked event){
		if(actionQueue.isEmpty()) {
			actionQueue.add(oneClickUtilsPlugin.maxCapeTeleToPOH());
		}
		if(!actionQueue.isEmpty()){
			log.info(actionQueue.toString());
			event.setMenuEntry(actionQueue.poll());
		}
	}

	private void testWalk(MenuOptionClicked event){
		event.consume();
		LocalPoint point = LocalPoint.fromWorld(client, new WorldPoint(2630,3365,client.getPlane()));
		oneClickUtilsPlugin.walkTile(point.getSceneX(), point.getSceneY());
	}

	private void testCastleWarsTele(MenuOptionClicked event){
		if(actionQueue.isEmpty()) {
			actionQueue.add(oneClickUtilsPlugin.teleToBank(BankTele.CASTLE_WARS));
		}
		if(!actionQueue.isEmpty()){
			log.info(actionQueue.toString());
			event.setMenuEntry(actionQueue.poll());
		}
	}

	private void testMaxCapeBankTele(MenuOptionClicked event){
		if(actionQueue.isEmpty()) {
			actionQueue.add(oneClickUtilsPlugin.teleToBank(BankTele.MAX_CAPE));
		}
		if(!actionQueue.isEmpty()){
			log.info(actionQueue.toString());
			event.setMenuEntry(actionQueue.poll());
		}
	}

	private void testEat(MenuOptionClicked event){
		if(actionQueue.isEmpty()) {
			actionQueue.add(oneClickUtilsPlugin.eatFood());
		}
		if(!actionQueue.isEmpty()){
			log.info(actionQueue.toString());
			event.setMenuEntry(actionQueue.poll());
		}
	}

	private void testDrop(MenuOptionClicked event){
		if(actionQueue.isEmpty()){
			log.info("In handle click1");
			oneClickUtilsPlugin.combineQueues(actionQueue, oneClickUtilsPlugin.dropItems(oneClickUtilsPlugin.parseIOs(config.dropIDs()),
					"1,5,2,6,3,7,4,8,9,13,10,14,11,15,12,16,17,21,18,22,19,23,20,24,25,26,27,28"));
		}
		if(!actionQueue.isEmpty()){
			log.info("In handle click2");
			log.info(actionQueue.toString());
			event.setMenuEntry(actionQueue.poll());
		}
	}
}