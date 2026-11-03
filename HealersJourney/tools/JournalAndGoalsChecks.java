import com.healersjourney.app.domain.*;
import com.healersjourney.app.data.backup.StateJson;
import java.util.*;

/** No installed data touched: covers dated text, 0 XP, preset saving and idempotent check-ins. */
public final class JournalAndGoalsChecks {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        GameEngine engine = GameEngine.INSTANCE;
        Planning plan = Planning.INSTANCE;
        StateJson json = StateJson.INSTANCE;
        GameState state = new GameState();
        state.setCareRoutinesConfigured(true); // These checks isolate planned tasks; care integration has its own suite.
        engine.ensureDefaults(state);
        String today = Dates.INSTANCE.todayKey();
        String day = Dates.INSTANCE.gameDayKey(Calendar.getInstance());
        String text = "A quiet morning.\n\nThings I want to remember: 🌻";
        check(Thoughts.INSTANCE.save(state, today, text) instanceof Outcome.Applied, "Thought save");
        String thoughtId = state.getThoughts().get(0).getId();
        Thoughts.INSTANCE.save(state, today, text + "\nAnother thought.");
        check(state.getThoughts().size() == 1 && state.getTotalXp() == 0 && state.getCoins() == 0, "Thought duplicated or awarded rewards");
        GameState restored = json.decode(json.backupEnvelope(state));
        check(restored.getThoughts().get(0).getId().equals(thoughtId), "Thought ID changed after backup");
        check(restored.getThoughts().get(0).getText().equals(text + "\nAnother thought."), "Thought text changed");
        GameState local = new GameState();
        json.restoreLocalExtras(local, json.encodeLocalExtras(state).toString());
        check(local.getThoughts().size() == 1 && local.getThoughts().get(0).getText().contains("🌻"), "Room extras lost thought");
        check(Thoughts.INSTANCE.save(state, "2026-02-31", "bad") instanceof Outcome.Failure, "Invalid journal date accepted");
        check(json.decode("{}").getThoughts().isEmpty(), "Old backup failed");

        check(engine.addPlannedActivity(state, "  Drink water  ", "strengthXp", 0, true, Collections.emptySet()) instanceof Outcome.Applied, "Zero-XP task/preset rejected");
        TodoTask task = state.getTodoTasks().get(0);
        check(task.getXp() == 0 && task.getName().equals("Drink water"), "Typed name or XP lost");
        ActivityDef preset = state.getActivityDefs().get("strengthXp").getOther().stream().filter(d -> d.getName().equals("Drink water")).findFirst().orElseThrow();
        check(preset.getXp() == 0, "Saved preset XP lost");
        check(engine.updateActivityById(state, "strengthXp", preset.getId(), "Drink water", 0) instanceof Outcome.Applied, "Cannot edit preset to 0 XP");
        engine.completeTodoTask(state, task.getId());
        check(state.getTotalXp() == 0 && state.getCoins() == 10 && state.getTodoTasks().get(0).getCompleted(), "Zero-XP task did not complete normally");
        check(DailyShowcase.INSTANCE.entries(state, day).size() == 1, "0 XP lost trophy");
        engine.undoTodoTask(state, task.getId());
        check(state.getCoins() == 0 && state.getActionHistory().isEmpty(), "Zero-XP undo failed");
        int presetsBefore = state.getActivityDefs().get("strengthXp").getOther().size();
        check(engine.addPlannedActivity(state, "Drink water", "strengthXp", 0, true, Collections.emptySet()) instanceof Outcome.Failure, "Duplicate task accepted");
        check(state.getActivityDefs().get("strengthXp").getOther().size() == presetsBefore, "Failed task added a preset");
        check(engine.addPlannedActivity(state, "Invalid", "strengthXp", -1, true, Collections.emptySet()) instanceof Outcome.Failure, "Negative XP accepted");
        plan.saveRecurring(state, null, "Take a break", "medicineXp", 0, Collections.singleton(plan.weekday(day)));
        check(json.decode(json.backupEnvelope(state)).getRecurringTasks().get(0).getXp() == 0, "Backup raised 0-XP recurrence to 1");

        plan.saveGoal(state, null, "Drink water twice", "strengthXp", "Drink water", 2, day.substring(0,7));
        PersonalGoal goal = state.getPersonalGoals().get(0);
        check(plan.checkInGoal(state, goal.getId()) instanceof Outcome.Applied, "Done today failed");
        plan.checkInGoal(state, goal.getId());
        check(plan.goalActions(state, goal).size() == 1 && state.getCoins() == 10, "Repeated check-in duplicated progress or coins");
        plan.saveGoal(state, goal.getId(), "Changed goal", "strengthXp", "Different activity", 2, day.substring(0,7));
        plan.checkInGoal(state, goal.getId());
        check(state.getCoins() == 10, "Editing a goal allowed another same-day check-in");
        plan.saveGoal(state, goal.getId(), goal.getTitle(), goal.getSkill(), goal.getActivity(), goal.getTarget(), day.substring(0,7));
        restored = json.decode(json.backupEnvelope(state));
        plan.checkInGoal(restored, goal.getId());
        check(plan.goalActions(restored, goal).size() == 1 && restored.getCoins() == 10, "Reload duplicated check-in");
        plan.undoGoalCheckIn(restored, goal.getId());
        check(plan.goalActions(restored, goal).isEmpty() && restored.getCoins() == 0, "Goal undo retained reward");
        engine.completeTodoTask(restored, task.getId());
        plan.checkInGoal(restored, goal.getId());
        check(plan.goalActions(restored, goal).size() == 1, "Check-in duplicated a logged activity today");

        engine.saveMilestone(state, null, "First 5K", "", true, true);
        Milestone milestone = state.getMilestones().get(0);
        engine.saveMilestone(state, milestone.getId(), "My first 5K", "A morning to remember", true, false);
        check(state.getMilestones().size() == 1 && !state.getMilestones().get(0).getShowOnMain(), "Milestone edit or pin failed");
        check(state.getMilestones().get(0).getCompletedDate().equals(milestone.getCompletedDate()), "Editing changed milestone date");
        System.out.println("JournalAndGoalsChecks passed");
    }
}
