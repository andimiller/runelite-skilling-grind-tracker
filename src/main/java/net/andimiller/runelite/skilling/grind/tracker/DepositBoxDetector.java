package net.andimiller.runelite.skilling.grind.tracker;

import net.runelite.api.gameval.InventoryID;

import java.util.HashMap;

public class DepositBoxDetector {

    static void handleDeposit(HashMap<Integer, HashMap<Integer, Integer>> before, HashMap<Integer, HashMap<Integer, Integer>> after) {

        for (Integer itemId : before.keySet()) {
            int beforeCount = before.getOrDefault(itemId, new HashMap<>()).values().stream().mapToInt(i -> i).sum();
            int afterCount = after.getOrDefault(itemId, new HashMap<>()).values().stream().mapToInt(i -> i).sum();

            int depositCount = beforeCount - afterCount;

            if (depositCount != 0) {
                after.get(itemId).compute(InventoryID.BANK, (k, v) -> v + depositCount);
            }
        }

    }

}
