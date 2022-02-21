package net.runelite.client.plugins.oneclickutils;

import com.google.common.base.Splitter;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.queries.BankItemQuery;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.BankUtils;
import net.runelite.client.plugins.iutils.ObjectUtils;
import net.runelite.client.plugins.iutils.iUtils;
import net.runelite.client.plugins.iutils.Banks;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.rs.api.RSClient;
import org.pf4j.Extension;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

import static net.runelite.api.ItemID.*;
import static net.runelite.api.ItemID.RING_OF_DUELING7;

@Extension
@PluginDescriptor(
        name = "BD One Click Utils",
        description = "BD Utils for one click plugins"
)

@Slf4j
@Singleton
@PluginDependency(iUtils.class)
public class OneClickUtilsPlugin extends Plugin {
    @Inject
    private ConfigManager configManager;
    @Inject
    private InventoryUtils inventory;
    @Inject
    private BankUtils bankUtils;
    @Inject
    private ItemManager itemManager;
    @Inject
    private ObjectUtils objectUtils;
    @Inject
    private ChatMessageManager chatMessageManager;
    @Inject
    private Client client;
    @Inject
    private WorldService worldService;


    private static final Splitter NEWLINE_SPLITTER = Splitter.on("\n").omitEmptyStrings().trimResults();
    private static final Set<Integer> duelingRings = Set.of(RING_OF_DUELING8, RING_OF_DUELING1, RING_OF_DUELING2, RING_OF_DUELING3, RING_OF_DUELING4, RING_OF_DUELING5, RING_OF_DUELING6, RING_OF_DUELING7);
    private static final Set<Integer> maxCape = Set.of(MAX_CAPE_13342);
    private static final Set<Integer> craftingCape = Set.of(CRAFTING_CAPE, CRAFTING_CAPET);
    Set<String> foodMenuOption = Set.of("Drink","Eat");
    Set<Integer> prayerPotionIDs = Set.of(PRAYER_POTION3,PRAYER_POTION2,PRAYER_POTION1,
            PRAYER_POTION4,SUPER_RESTORE4,SUPER_RESTORE3,SUPER_RESTORE2,SUPER_RESTORE1,
            ZAMORAK_BREW3,ZAMORAK_BREW2,ZAMORAK_BREW1,ZAMORAK_BREW4);
    Set<Integer> foodBlacklist = Set.of(PRAYER_POTION3,PRAYER_POTION2,PRAYER_POTION1,
            PRAYER_POTION4,SUPER_RESTORE4,SUPER_RESTORE3,SUPER_RESTORE2,SUPER_RESTORE1,
            ZAMORAK_BREW3,ZAMORAK_BREW2,ZAMORAK_BREW1,ZAMORAK_BREW4);
    private long lastXP = 0;
    private int ticksSinceLastXpDrop = -1;





    @Provides
    OneClickUtilsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OneClickUtilsConfig.class);
    }

    @Override
    protected void startUp() {
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event) {
        if(event.getGroup().equals("oneclickutils")) {
        }
    }

    public void sendGameMessage(String message) {
        String chatMessage = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append(message)
                .build();

        chatMessageManager
                .queue(QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(chatMessage)
                        .build());
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (client.getOverallExperience() == lastXP){
            ticksSinceLastXpDrop++;
        }else{
            ticksSinceLastXpDrop = 0;
            lastXP = client.getOverallExperience();
        }
    }


    public int getTicksSinceLastXpDrop(){
        return ticksSinceLastXpDrop;
    }

    public LegacyMenuEntry teleToBank(BankTele bankTeleMethod){
        ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipmentContainer != null) {
            Item[] items = equipmentContainer.getItems();
            for (Item item : items) {
                switch (bankTeleMethod){
                    case CASTLE_WARS:
                        if (duelingRings.contains(item.getId())) {
                            return new LegacyMenuEntry("Castle Wars",
                                    "Ring of dueling",
                                    3,
                                    MenuAction.CC_OP,
                                    -1,
                                    WidgetInfo.EQUIPMENT_RING.getId(),
                                    false);
                        }
                        continue;
                        //todo: add crafting cape
                    case CRAFTING_CAPE:

                        return null;
                    case MAX_CAPE:
                        if (item.getId() == MAX_CAPE_13342){
                            return new LegacyMenuEntry("Crafting Guild",
                                    "Max Cape",
                                    4,
                                    MenuAction.CC_OP,
                                    -1,
                                    WidgetInfo.EQUIPMENT_CAPE.getId(),
                                    false);
                        }
                        continue;
                }
            }
        }
        log.info("One Click Utils: couldn't find a bank teleport method");
        sendGameMessage("One Click Utils: couldn't find a bank teleport method");
        return null;
    }

    public LegacyMenuEntry maxCapeTeleToPOH(){
        ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipmentContainer != null) {
            Item[] items = equipmentContainer.getItems();
            for (Item item : items) {
                if (item.getId() == MAX_CAPE_13342) {
                    return new LegacyMenuEntry("Tele to POH",
                            "Max Cape",
                            5,
                            MenuAction.CC_OP,
                            -1,
                            WidgetInfo.EQUIPMENT_CAPE.getId(),
                            false);
                }
            }
        }
        log.info("One Click Utils: couldn't tele to POH with max cape");
        sendGameMessage("One Click Utils: couldn't tele to POH with max cape");
        return null;
    }

    public LegacyMenuEntry logout(){
        return new LegacyMenuEntry("Logout",
                "",
                1,
                MenuAction.CC_OP,
                -1,
                11927560,
                false);
    }



    //Always consume the event when using this
    public void walkTile(int x, int y) {
        RSClient rsClient = (RSClient) client;
        rsClient.setSelectedSceneTileX(x);
        rsClient.setSelectedSceneTileY(y);
        rsClient.setViewportWalking(true);
        rsClient.setCheckClick(false);
    }

    public LegacyMenuEntry eatFood(){
        return eatFood(this.foodMenuOption, this.foodBlacklist);
    }

    public LegacyMenuEntry eatFood(Set<String> fMO, Set<Integer> fBL) {
        WidgetItem food = getItemMenu(fMO,fBL);
        if (food == null){
            log.info("One Click Utils: couldn't find food");
            sendGameMessage("One Click Utils: couldn't find food");
            return null;
        }
        String[] foodMenuOptions = itemManager.getItemComposition(food.getId()).getInventoryActions();
        return new LegacyMenuEntry(foodMenuOptions[0],
                Integer.toString(food.getId()),
                food.getId(),
                MenuAction.ITEM_FIRST_OPTION,
                food.getIndex(),
                WidgetInfo.INVENTORY.getId(),
                false);
    }

    public boolean hasFood(Set<String> fMO, Set<Integer> fBL){
        WidgetItem food = getItemMenu(fMO,fBL);
        return food != null;
    }

    public boolean hasFood(){
        return hasFood(this.foodMenuOption, this.foodBlacklist);
    }

    public LegacyMenuEntry dropItem(WidgetItem item) {
        if (item != null){
            return new LegacyMenuEntry(
                    "Drop Item",
                    Integer.toString(item.getId()),
                    item.getId(),
                    MenuAction.ITEM_FIFTH_OPTION,
                    item.getIndex(),
                    WidgetInfo.INVENTORY.getId(),
                    false);
        }
        return null;
    }

    public LegacyMenuEntry dropItem(int itemID){
        return dropItem(getWidgetItem(itemID));
    }

    public Queue<LegacyMenuEntry> dropItems(HashSet<Integer> dropIDs, String dropOrderString) {
        HashMap<Integer, Integer> dropOrder = new HashMap<>();
        int order = 0;
        Set<Integer> uniquieIndexes = new HashSet<>();
        if (dropOrderString == null) {
            //Use default order
            dropOrderString = "1,2,5,6,9,10,13,14,17,18,21,22,25,26,3,4,7,8,11,12,15,16,19,20,23,24,27,28";
        }
        for (String s : Text.COMMA_SPLITTER.split(dropOrderString)) {
            try {
                int inventoryIndex = Integer.parseInt(s) - 1;
                //check if inx is out of bounds or already used
                if (inventoryIndex > 27 || inventoryIndex < 0 || uniquieIndexes.contains(inventoryIndex)) {
                    continue;
                }
                uniquieIndexes.add(inventoryIndex);
                dropOrder.put(order, inventoryIndex);
                order++;
            } catch (Exception ignored) {
                log.info("One Click Utils: error parsing drop order string");
                sendGameMessage("One Click Utils: error parsing drop order string");
                return null;
            }
        }
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        List<WidgetItem> matchedItems = new ArrayList<>();
        if (inventoryWidget != null) {
            for (int i = 0; i <= 27; i++) {
                int index = dropOrder.get(i);
                WidgetItem item = inventoryWidget.getWidgetItem(index);
                //drop all case
                if(item != null && dropIDs == null){
                    matchedItems.add(item);
                }else if (item != null && dropIDs.contains(item.getId())) {
                    matchedItems.add(item);
                }
            }
        }
        Queue<LegacyMenuEntry> dropQueue = new LinkedList<LegacyMenuEntry>();
        for (WidgetItem dropItem : matchedItems){
            dropQueue.add(dropItem(dropItem));
        }
        return dropQueue;
    }


    public List<WidgetItem> idsToWidgetItems(Collection<Integer> ids) {
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        List<WidgetItem> matchedItems = new ArrayList<>();
        if (inventoryWidget != null) {
            Collection<WidgetItem> items = inventoryWidget.getWidgetItems();
            for(WidgetItem widgetItem : items) {
                if (widgetItem != null && ids.contains(widgetItem.getId())) {
                    matchedItems.add(widgetItem);
                }
            }
            return matchedItems;
        }
        return null;
    }

    public WidgetItem getItemMenu(Collection<String>menuOptions, Collection<Integer> ignoreIDs) {
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        if (inventoryWidget != null) {
            Collection<WidgetItem> items = inventoryWidget.getWidgetItems();
            for (WidgetItem item : items) {
                if (ignoreIDs.contains(item.getId())) {
                    continue;
                }
                String[] menuActions = itemManager.getItemComposition(item.getId()).getInventoryActions();
                for (String action : menuActions) {
                    if (action != null && menuOptions.contains(action)) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public LegacyMenuEntry useItemOnItem(int highlightedItemID, int targetItemID){
        return useItemOnItem(getWidgetItem(highlightedItemID), getWidgetItem(targetItemID));
    }

    public LegacyMenuEntry useItemOnItem(WidgetItem highlightedWidgetItem, WidgetItem targetWidetItem){
        if(highlightedWidgetItem == null || targetWidetItem == null) {
            return null;
        }else{
            return new LegacyMenuEntry("Use",
                    "Item -> Item",
                    targetWidetItem.getId(),
                    MenuAction.ITEM_USE_ON_WIDGET_ITEM,
                    targetWidetItem.getIndex(),
                    9764864,
                    false);
        }
    }

    public WidgetItem getWidgetItem(int id) {
        return getWidgetItem(Set.of(id));
    }

    public WidgetItem getWidgetItem(Collection<Integer> ids) {
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        if (inventoryWidget != null) {
            Collection<WidgetItem> items = inventoryWidget.getWidgetItems();
            for (WidgetItem item : items) {
                if (ids.contains(item.getId())) {
                    return item;
                }
            }
        }
        return null;
    }

    public ArrayList<InventoryItem> parseInventoryItems(String desiredInventoryString){
        ArrayList<InventoryItem> desiredInventory = new ArrayList<InventoryItem>();
        for (String line : NEWLINE_SPLITTER.split(desiredInventoryString)){
            try {
                String[] split = line.split(",");
                desiredInventory.add(new InventoryItem(Integer.parseInt(split[0].trim()),
                        Integer.parseInt(split[1].trim()),
                        split[2].trim().equalsIgnoreCase("true")));
            }catch(ArrayIndexOutOfBoundsException e) {
                log.info("One Click Utils: Inventory parsing error, your syntax is probably wrong");
                sendGameMessage("One Click Utils: Inventory parsing error, your syntax is probably wrong");
                return null;
            }
        }
        return desiredInventory;
    }

    public HashSet<Integer> parseIOs(String idString){
        HashSet<Integer> ids = new HashSet<>();
        for (String s : Text.COMMA_SPLITTER.split(idString)) {
            try {
                ids.add(Integer.parseInt(s));
            }
            catch (NumberFormatException ignored) {
                log.info("One Click Utils: Ids list parsing error, your syntax is probably wrong");
                sendGameMessage("One Click Utils: Id list parsing error, your syntax is probably wrong");
            }
        }
        return ids;
    }

    private ArrayList<Integer> parseConfig(String desiredEquipmentString) {
        ArrayList<Integer> desiredEquipment = new ArrayList<Integer>();
        for (String s : Text.COMMA_SPLITTER.split(desiredEquipmentString)){
            try{
                desiredEquipment.add(Integer.parseInt(s));
            }catch (NumberFormatException e){
                log.info("One Click Utils: Equipment parsing error, your syntax is probably wrong");
                sendGameMessage("One Click Utils: Equipment parsing error, your syntax is probably wrong");
                return null;
            }
        }
        return desiredEquipment;
    }

    public LegacyMenuEntry openBank(){
        GameObject bankTarget = objectUtils.findNearestBank();
        if (bankTarget != null) {
            return new LegacyMenuEntry("Open",
                            "Bank",
                            bankTarget.getId(),
                            getBankMenuOpcode(bankTarget.getId()),
                            bankTarget.getSceneMinLocation().getX(),
                            bankTarget.getSceneMinLocation().getY(),
                            false);
        }
        return null;
    }

    public GameObject getGameObject(int id){
        return new GameObjectQuery()
                .idEquals(id)
                .result(client)
                .nearestTo(client.getLocalPlayer());
    }

    public LegacyMenuEntry equipItemFromInventory(int itemID){
        return equipItemFromInventory(getWidgetItem(itemID));
    }

    public LegacyMenuEntry equipItemFromInventory(WidgetItem widgetItem){
        if (widgetItem != null){
            return new LegacyMenuEntry( "Wear",
                    "Necklace",
                    widgetItem.getId(),
                    MenuAction.ITEM_SECOND_OPTION,
                    widgetItem.getIndex(),
                    WidgetInfo.INVENTORY.getId(),
                    false);
        }
        return null;
    }

    private MenuAction getBankMenuOpcode(int bankID) {
        return Banks.BANK_CHECK_BOX.contains(bankID) ? MenuAction.GAME_OBJECT_FIRST_OPTION:
                MenuAction.GAME_OBJECT_SECOND_OPTION;
    }

    public Queue<LegacyMenuEntry> resolveEquipmentFromBankWithdraw(Collection<Integer> itemIds){
        Queue<LegacyMenuEntry> queue = new LinkedList<LegacyMenuEntry>();
        ArrayList<Integer> scheduledEquip = new ArrayList<Integer>();

        for (Integer itemID : itemIds){
            if (!isItemEquipped(Set.of(itemID)) && !inventory.containsItem(itemID)){
                queue.add(withdrawItemAmount(itemID, 1));
                scheduledEquip.add(itemID);
            }
        }
        return queue;
    }

    public Queue<LegacyMenuEntry> resolveEquipmentFromBankEquip(Collection<Integer> itemIds){
        Queue<LegacyMenuEntry> queue = new LinkedList<LegacyMenuEntry>();
        ArrayList<Integer> scheduledEquip = new ArrayList<Integer>();
        //Equip stuff already in your inventory
        for (Integer itemID : itemIds){
            if (!isItemEquipped(Set.of(itemID)) && inventory.containsItem(itemID)){
                queue.add(equipItemFromBank(itemID));
            }
        }
        return queue;
    }

    public LegacyMenuEntry drinkPotionFromBank(int itemID){
        if ( inventory.getWidgetItem(itemID) != null) {
            return new LegacyMenuEntry("Eat/Drink",
                    "Item",
                    9,
                    MenuAction.CC_OP,
                    inventory.getWidgetItem(itemID).getIndex(),
                    983043,
                    false);
        }
        return null;
    }


    public LegacyMenuEntry equipItemFromBank(int itemID){
        return new LegacyMenuEntry("Equip",
                "Item",
                9,
                MenuAction.CC_OP_LOW_PRIORITY,
                inventory.getWidgetItem(itemID).getIndex(),
                983043,
                false);
    }

    public boolean isItemEquipped(Collection<Integer> itemIds) {
        assert client.isClientThread();
        ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipmentContainer != null) {
            Item[] items = equipmentContainer.getItems();
            for (Item item : items) {
                if (itemIds.contains(item.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Queue<LegacyMenuEntry> resolveInventory(ArrayList<InventoryItem> desiredInventory, boolean exact){
        Queue<LegacyMenuEntry> queue = new LinkedList<LegacyMenuEntry>();
        ArrayList<Integer> scheduledWithdraws = new ArrayList<Integer>();
        if (exact){
            ArrayList<WidgetItem> itemsToBeDeposited = getUnexpectedItemID(desiredInventory);
            for (WidgetItem widgetItem : itemsToBeDeposited){
                queue.add(depositAllOfItem(widgetItem));
            }

            //if theres too many or too few of an expected item, deposit all and withdraw the right amount
            //for items set to amount -1, just check that we have at least 1 here. will fill at the end
            for(WidgetItem widgetItem : inventory.getAllItems()){
                for(InventoryItem desiredItem : desiredInventory){
                    if (widgetItem.getId() == desiredItem.getItemID()){
                        if(!inventory.containsItemAmount(widgetItem.getId(), desiredItem.getAmount() == -1 ? 1 : desiredItem.getAmount(), desiredItem.isStackable(), desiredItem.getAmount() != -1)){
                            if (!scheduledWithdraws.contains(desiredItem.getItemID())){
                                queue.add(depositAllOfItem(desiredItem.itemID));
                                queue.add(withdrawItemAmount(desiredItem.getItemID(), desiredItem.getAmount()));
                                scheduledWithdraws.add(desiredItem.getItemID());
                            }
                        }
                    }
                }
            }
        }
        //withdraw missing items, account for "fill" -1?
        for (InventoryItem item : desiredInventory){
            if(!inventory.containsItem(item.getItemID())){
                queue.add(withdrawItemAmount(item.getItemID(), item.getAmount()));
                scheduledWithdraws.add(item.getItemID());
            }
        }

        //fill all -1 stackable items;
        for (InventoryItem item : desiredInventory){
            if(item.isStackable() && item.getAmount() == -1 && bankUtils.contains(item.getItemID(), 1)){
                if (!scheduledWithdraws.contains(item.getItemID())) {
                    queue.add(withdrawAllItem(item.getItemID()));
                }
            }
        }

        //fill the first -1 non stackable item
        for (InventoryItem item : desiredInventory){
            if(!item.isStackable() && item.getAmount() == -1 && !inventory.isFull()){
                if (!scheduledWithdraws.contains(item.getItemID())) {
                    queue.add(withdrawAllItem(item.getItemID()));
                    break;
                }
            }
        }
        return queue;
    }

    public LegacyMenuEntry worldHop(int worldID){
        WorldResult worldResult = worldService.getWorlds();
        World world = worldResult.findWorld(worldID);
        if (world == null) {
            log.debug("Couldn't find world " + worldID);
            return null;
        }
        return new LegacyMenuEntry("",
                "",
                1,
                MenuAction.CC_OP,
                worldID,
                4522000,
                false);
    }

    public LegacyMenuEntry depositAllOfItem(WidgetItem item) {
        if (!bankUtils.isOpen() && !bankUtils.isDepositBoxOpen()) {
            return null;
        }
        boolean depositBox = bankUtils.isDepositBoxOpen();
        return new LegacyMenuEntry("DepositAll", Integer.toString(item.getId()), (depositBox) ? 1 : 8, MenuAction.CC_OP, item.getIndex(),
                (depositBox) ? 12582914 : 983043, false);
    }

    public LegacyMenuEntry depositAllOfItem(int itemID) {
        if (!bankUtils.isOpen() && !bankUtils.isDepositBoxOpen()) {
            return null;
        }
        return depositAllOfItem(inventory.getWidgetItem(itemID));
    }

    public LegacyMenuEntry depositOneOfItem(WidgetItem item) {
        if (!bankUtils.isOpen() && !bankUtils.isDepositBoxOpen() || item == null) {
            return null;
        }
        boolean depositBox = bankUtils.isDepositBoxOpen();
        return new LegacyMenuEntry("Deposit-One", Integer.toString(item.getId()), (client.getVarbitValue(6590) == 0) ? 2 : 3, MenuAction.CC_OP, item.getIndex(),
                (depositBox) ? 12582914 : 983043, false);
    }

    public LegacyMenuEntry depositOneOfItem(int itemID) {
        if (!bankUtils.isOpen() && !bankUtils.isDepositBoxOpen()) {
            return null;
        }
        return depositOneOfItem(inventory.getWidgetItem(itemID));
    }

    public LegacyMenuEntry depositAll(){
        Widget depositInventoryWidget = client.getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);
        if (bankUtils.isDepositBoxOpen()) {
            return new LegacyMenuEntry("Deposit All", "", 1, MenuAction.CC_OP, -1, 12582916, false);
        }
        return new LegacyMenuEntry("Deposit All", "", 1, MenuAction.CC_OP, -1, 786474, false);
    }

    public LegacyMenuEntry genericClickItemFirstOption(int itemID){
        return genericClickItemFirstOption(getWidgetItem(itemID));
    }

    public LegacyMenuEntry genericClickItemFirstOption(WidgetItem item){
        if (item != null){
            return new LegacyMenuEntry("",
                    "",
                    item.getId(),
                    MenuAction.ITEM_FIRST_OPTION,
                    item.getIndex(),
                    9764864,
                    false);
        }
        return null;
    }

    public boolean inventoryMatches(ArrayList<InventoryItem> desiredInventory, boolean exact){
        for (InventoryItem item : desiredInventory){
            //if it sets to -1 or "all", make sure we have at least 1
            //if item is not stackable, we should have no open inventory spaces
            if(item.getAmount() == -1){
                if (!item.isStackable()){
                    if(!inventory.isFull()){
                        return false;
                    }
                }
                if(!inventory.containsItemAmount(item.getItemID(), 1, item.isStackable(), false)){
                    return false;
                }
            }else if(!inventory.containsItemAmount(item.getItemID(), item.getAmount(), item.isStackable(), exact)){
                return false;
            }
        }
        if(exact){
            if (getUnexpectedItemID(desiredInventory).size() != 0){
                return false;
            }
        }
        return true;
    }

    private ArrayList<WidgetItem> getUnexpectedItemID(ArrayList<InventoryItem> desiredInventory){
        Collection<WidgetItem> inventoryItems = inventory.getAllItems();
        ArrayList<WidgetItem> unexpectedItems = new ArrayList<WidgetItem>();
        for (WidgetItem inventoryItem : inventoryItems) {
            boolean acceptableItem = false;
            for(InventoryItem desiredItem : desiredInventory){
                if (inventoryItem.getId() == desiredItem.getItemID()){
                    acceptableItem = true;
                    break;
                }
            }
            if(!acceptableItem){
                unexpectedItems.add(inventoryItem);
            }
        }
        return unexpectedItems;
    }

    public LegacyMenuEntry withdrawAllItem(Widget bankItemWidget) {
        return new LegacyMenuEntry("Withdraw-All",
                "",
                7,
                MenuAction.CC_OP,
                bankItemWidget.getIndex(),
                WidgetInfo.BANK_ITEM_CONTAINER.getId(),
                false);
    }

    public LegacyMenuEntry withdrawAllItem(int bankItemID) {
        Widget item = getBankItemWidget(bankItemID);
        if (item != null) {
            return withdrawAllItem(item);
        }else{
            log.debug("Withdraw all item not found.");
            return null;
        }
    }

    public LegacyMenuEntry withdrawItemAmount(int bankItemID, int amount){
        return withdrawItemAmount(bankItemID, amount, -1);
    }

    public LegacyMenuEntry withdrawItemAmount(int bankItemID, int amount, int postActionTickDelay) {
        Widget item = getBankItemWidget(bankItemID);
        if (item != null) {
            int identifier;
            switch (amount) {
                case -1:
                    return withdrawAllItem(bankItemID);
                case 1:
                    identifier = (client.getVarbitValue(6590) == 0) ? 1 : 2;
                    break;
                case 5:
                    identifier = 3;
                    break;
                case 10:
                    identifier = 4;
                    break;
                default:
                    identifier = (client.getVarbitValue(3960) == amount) ? 5 : 6;
                    break;
            }
            return new LegacyMenuEntry("Withdraw " + amount,
                    "ID " + item.getId(),
                    identifier,
                    MenuAction.CC_OP,
                    item.getIndex(),
                    WidgetInfo.BANK_ITEM_CONTAINER.getId(),
                    false,
                    postActionTickDelay);
        }
        return null;
    }

    private Widget getBankItemWidget(int id) {
        if (!bankUtils.isOpen()) {
            return null;
        }

        WidgetItem bankItem = new BankItemQuery().idEquals(id).result(client).first();
        if (bankItem != null) {
            return bankItem.getWidget();
        } else {
            return null;
        }
    }

    public int getObjectParam0(Locatable gameObject) {
        if (gameObject instanceof GameObject){
            return ((GameObject) gameObject).getSceneMinLocation().getX();
        }
        return(gameObject.getLocalLocation().getSceneX());
    }

    public int getObjectParam1(Locatable gameObject) {
        if (gameObject instanceof GameObject) {
            return ((GameObject) gameObject).getSceneMinLocation().getY();
        }
        return(gameObject.getLocalLocation().getSceneY());
    }

    public void combineQueues(Queue<LegacyMenuEntry> destinationQueue, Queue<LegacyMenuEntry> extraQueue){
        if (extraQueue == null){
            log.info("Extra queue is null");
            return;
        }
        if(destinationQueue == null){
            log.info("Destination queue is null");
            return;
        }
        while (!extraQueue.isEmpty()){
            LegacyMenuEntry event = extraQueue.poll();
            if(event != null){
                destinationQueue.add(event);
            }
        }
    }

    public void sanitizeEnqueue(LegacyMenuEntry menuEntry, Queue<LegacyMenuEntry> actionQueue, String errorMessage) {
        if (menuEntry != null){
            actionQueue.add(menuEntry);
            //log.info("Adding to queue: " + menuEntry.getOption() + ", " + menuEntry.getTarget());
        }else{
            log.info(errorMessage);
        }
    }
}