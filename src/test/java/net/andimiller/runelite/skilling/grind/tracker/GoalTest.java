package net.andimiller.runelite.skilling.grind.tracker;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class GoalTest {

    @Test
    public void shouldFlowOneItem() {
        Goal goal = new Goal(List.of(1), 500); // we want 500 of item 1
        assertThat(
                goal.computeRequired(
                        Map.of(1, 200) // we have 200 of item 1
                )
        )
                .hasSize(1)
                .containsEntry(1, 300); // we need 300 more of item 1
    }


    @Test
    public void shouldFlowThreeItems() {
        Goal goal = new Goal(List.of(1, 2, 3), 500); // we want 500 of the final item 3
        assertThat(
                goal.computeRequired(
                        Map.of(
                                3, 100, // we've got 100 of the final item 3
                                2, 50, // we've got 50 of the intermediate item 2
                                1, 4000 // we've got 4000 of the first item 1
                        )
                )
        ).hasSize(3)
                .containsEntry(3, 400)
                .containsEntry(2, 350)
                .containsEntry(1, 0);

    }
}
