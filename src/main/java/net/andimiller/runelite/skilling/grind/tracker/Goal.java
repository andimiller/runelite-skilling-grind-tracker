package net.andimiller.runelite.skilling.grind.tracker;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Represents a Goal that the User has
 *
 * types - List of the item ids they must travel through
 * goal - Number of the final item they want
 */
@AllArgsConstructor
@Value
public class Goal {
    private List<Integer> types;
    private Integer goal;

    public HashMap<Integer, Integer> computeRequired(Map<Integer, Integer> counts) {
        HashMap<Integer, Integer> required = new HashMap<>();
        int runningTotal = goal;
        for (Integer type : Lists.reverse(types)) {
            required.put(type, Math.max(runningTotal, 0));
            runningTotal -= counts.getOrDefault(type, 0);
        }
        return required;
    }
}
