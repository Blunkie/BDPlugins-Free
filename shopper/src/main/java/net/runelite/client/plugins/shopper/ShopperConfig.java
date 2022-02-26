package net.runelite.client.plugins.shopper;


import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("BD Shopper")
public interface ShopperConfig extends Config {
    @ConfigItem(
            keyName = "shopQuantityThreshold",
            name = "Quantity Threshold",
            description = "At what threshold to stop buying",
            position = 0
    )
    default int shopQuantityThreshold() {
        return 50;
    }

    @ConfigItem(
            keyName = "goldOre",
            name = "BF Gold Ore",
            description = "Check this if buying gold ore at BF",
            position = 1
    )
    default boolean goldOre() {
        return false;
    }

    @ConfigItem(
            keyName = "npcID",
            name = "NPC ID",
            description = "NPC of the shop, ignored if doing gold ore",
            position = 2
    )
    default int npcID() {
        return 0;
    }

    @ConfigItem(
            keyName = "itemID",
            name = "Item ID",
            description = "Item ID to buy, ignored if doing gold ore",
            position = 3
    )
    default int itemID() {
        return 0;
    }

    @ConfigItem(
            keyName = "stackable",
            name = "Stackable",
            description = "Check this if the item you are buying is not stackable",
            position = 4
    )
    default boolean stackable() {
        return false;
    }
}