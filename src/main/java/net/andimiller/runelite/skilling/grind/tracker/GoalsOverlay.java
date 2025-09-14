package net.andimiller.runelite.skilling.grind.tracker;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.util.List;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class GoalsOverlay extends OverlayPanel {

    private HashMap<Integer, HashMap<Integer, Integer>> counters;
    private List<Goal> goals;
    private Map<Integer, String> names;

    GoalsOverlay(
            HashMap<Integer, HashMap<Integer, Integer>> _counters,
            List<Goal> _goals,
            Map<Integer, String> _names
    ) {
        this.counters = _counters;
        this.goals = _goals;
        this.names = _names;
    }

    public void updateGoals(List<Goal> _goals) {
        this.goals = _goals;
    }

    private Color pickColourFromPercentage(Double percentage) {
        if (percentage > 100.0) {
            return Color.GREEN;
        } else if (percentage == 0.0) {
            return Color.LIGHT_GRAY;
        } else {
            return Color.WHITE;
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(TitleComponent.builder().text("Item Grinds").build());

        int maxWidth = 0;

        int staleCount = 0;

        for (Goal goal : goals) {
            Map<Integer, Integer> owned = goal.getTypes().stream().map(i ->
                    Map.entry(
                            i,
                            counters.getOrDefault(i, new HashMap<>()).values().stream().mapToInt(x -> x).sum()
                    )
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Map<Integer, Integer> required = goal.computeRequired(owned);

            for (Integer itemId : goal.getTypes()) {
                String name = names.getOrDefault(itemId, "Unknown");

                Integer o = owned.getOrDefault(itemId, 0);
                Integer r = required.getOrDefault(itemId, 0);

                Integer remaining = Math.max(r - o, 0);

                boolean stale = counters.getOrDefault(itemId, new HashMap<>()).size() != 2;

                Color colour;
                if (stale) {
                   staleCount += 1;
                   colour = Color.RED;
                } else if (remaining == 0) {
                    colour = Color.GREEN;
                } else if (o == 0) {
                    colour = Color.LIGHT_GRAY;
                } else {
                    colour = Color.WHITE;
                }

                int stringWidth = graphics.getFontMetrics().stringWidth(String.format("%s  %d / %d", name, o, r));
                maxWidth = Integer.max(stringWidth, maxWidth);

                panelComponent.getChildren().add(
                        LineComponent.builder()
                                .left(name)
                                .leftColor(colour)
                                .right(String.format("%d / %d", o, r))
                                .rightColor(colour)
                                .build()
                );
            }
        }
        if (staleCount > 0) {
            String STALE_WARNING= "Stale items in red require you to check your bank to update counts";
            maxWidth = Integer.max(maxWidth, graphics.getFontMetrics().stringWidth(STALE_WARNING));
            panelComponent.getChildren().add(
                    TitleComponent.builder().text(STALE_WARNING).build()
            );
        }

        panelComponent.setPreferredSize(
                new Dimension(maxWidth + 30, 0)
        );

        return super.render(graphics);
    }
}
