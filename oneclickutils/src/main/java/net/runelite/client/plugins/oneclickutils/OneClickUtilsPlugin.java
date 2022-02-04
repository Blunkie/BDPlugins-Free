package net.runelite.client.plugins.oneclickutils;

import com.google.common.base.Splitter;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.queries.BankItemQuery;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.*;
import org.pf4j.Extension;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Extension
@PluginDescriptor(
        name = "BD One Click Utils",
        description = "BD Utils for one click plugins",
        enabledByDefault = true
)

@Slf4j
@Singleton
@PluginDependency(iUtils.class)
public class OneClickUtilsPlugin extends Plugin {
    @Inject
    private OneClickUtilsConfig config;
    @Inject
    private ConfigManager configManager;
    @Inject
    private InventoryUtils inventory;
    @Inject
    private BankUtils bankUtils;
    @Inject
    private ObjectUtils objectUtils;
    @Inject
    private ChatMessageManager chatMessageManager;
    @Inject
    private Client client;


    private static final Splitter NEWLINE_SPLITTER = Splitter.on("\n").omitEmptyStrings().trimResults();
    ArrayList<InventoryItem> desiredInventory = new ArrayList<InventoryItem>();
    ArrayList<Integer> desiredEquipment = new ArrayList<Integer>();
    Queue<LegacyMenuEntry> actionQueue = new LinkedList<LegacyMenuEntry>();


    @Provides
    OneClickUtilsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OneClickUtilsConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick tick) {

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
    }

    @Override
    protected void startUp() {
        parseConfig();
        actionQueue.clear();
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event) {
        if(event.getGroup().equals("oneclickutils")) {
            parseConfig();
        }
    }

    private void addToActionQueue(Queue<LegacyMenuEntry> queue){
        for (LegacyMenuEntry entry : queue){
            actionQueue.add(entry);
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

    private void parseConfig() {
        desiredInventory.clear();
        desiredEquipment.clear();
        for (String line : NEWLINE_SPLITTER.split(config.desiredInventory())){
            try {
                String[] split = line.split(",");
                desiredInventory.add(new InventoryItem(Integer.parseInt(split[0].trim()),
                        Integer.parseInt(split[1].trim()),
                        split[2].trim().equalsIgnoreCase("true")));
            }catch(ArrayIndexOutOfBoundsException e) {
                sendGameMessage("One Click Utils: Inventory parsing error, your syntax is probably wrong");
            }
        }
        for (String s : Text.COMMA_SPLITTER.split(config.desiredEquipment())){
            try{
                desiredEquipment.add(Integer.parseInt(s));
            }catch (NumberFormatException e){
                sendGameMessage("One Click Utils: Equipment parsing error, your syntax is probably wrong");
            }
        }
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


    public LegacyMenuEntry equipItemFromBank(int itemID){
        return new LegacyMenuEntry("Equip",
                "Item",
                9,
                MenuAction.CC_OP_LOW_PRIORITY,
                inventory.getWidgetItem(itemID).getIndex(),
                983043,
                false);
    }

    private boolean isItemEquipped(Collection<Integer> itemIds) {
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

    public LegacyMenuEntry withdrawItemAmount(int bankItemID, int amount) {
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
                    "",
                    identifier,
                    MenuAction.CC_OP,
                    item.getIndex(),
                    WidgetInfo.BANK_ITEM_CONTAINER.getId(),
                    false);
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
}