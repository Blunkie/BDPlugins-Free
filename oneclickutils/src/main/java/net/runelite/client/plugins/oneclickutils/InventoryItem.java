package net.runelite.client.plugins.oneclickutils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class InventoryItem {
    int itemID;
    int amount;
    boolean stackable;
}
