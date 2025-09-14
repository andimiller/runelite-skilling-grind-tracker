package net.andimiller.runelite.skilling.grind.tracker;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GoalParserTest {

    @Test
    public void shouldParseEmptyString() {
        assertThat(
                GoalParser.parseGoals("")
        ).hasSize(0);
    }

    @Test
    public void shouldParseOneGoal() {
        assertThat(
                GoalParser.parseGoals("453:1000")
        ).hasSize(1)
                .contains(new Goal(List.of(453), 1000));
    }

    @Test
    public void shouldParseAFlow() {
        assertThat(
                GoalParser.FLOW.parse("123->456->789")
        ).isEqualTo(
                List.of(123, 456, 789)
        );
    }

    @Test
    public void shouldParseManyGoals() {
        assertThat(
                GoalParser.parseGoals("453:1000,1205:50,2434:250")
        ).hasSize(3)
                .contains(new Goal(List.of(453), 1000))
                .contains(new Goal(List.of(1205), 50))
                .contains(new Goal(List.of(2434), 250));
    }


}
