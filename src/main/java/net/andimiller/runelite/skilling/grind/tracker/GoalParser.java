package net.andimiller.runelite.skilling.grind.tracker;

import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;

import java.util.List;

public class GoalParser {

    private static final Parser<Integer> NUMBER = Scanners.INTEGER.map(Integer::parseInt);

    public static final Parser<List<Integer>> FLOW = NUMBER.sepBy(Scanners.string("->"));

    private static final Parser<Goal> GOAL =
            Parsers.tuple(
                    FLOW.followedBy(Scanners.isChar(':')),
                    NUMBER
            ).map(t -> new Goal(t.a, t.b));

    public static final Parser<List<Goal>> GOALS = GOAL.sepBy(Scanners.isChar(','));

    public static List<Goal> parseGoals(String input) {
        return GOALS.parse(input);
    }

}
