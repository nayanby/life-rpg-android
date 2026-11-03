import com.healersjourney.app.domain.*;
import com.healersjourney.app.data.backup.StateJson;
import java.util.*;

/** Isolated regressions: scheduling, removed copies, focus recovery, goals and backup compatibility. */
public final class PlanningChecks {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        Planning plan = Planning.INSTANCE;
        GameEngine engine = GameEngine.INSTANCE;
        GameState state = new GameState();
        state.setCareRoutinesConfigured(true); // These checks isolate planned tasks; care integration has its own suite.
        engine.ensureDefaults(state);
        String day = state.getTodoDate();
        int weekday = plan.weekday(day);
        Set<Integer> days = Collections.singleton(weekday);
        check(plan.saveRecurring(state, null, "Exercise", "strengthXp", 25, days) instanceof Outcome.Applied, "Create schedule");
        engine.ensureTodoTasks(state);
        engine.ensureTodoTasks(state);
        check(state.getTodoTasks().size() == 1, "Duplicate recurring copy");
        String removedId = state.getTodoTasks().get(0).getId();
        engine.deleteTodoTask(state, removedId);
        engine.ensureTodoTasks(state);
        check(state.getTodoTasks().isEmpty(), "Removed task reappeared");
        state.setTodoViewOffset(7);
        check(engine.getTodoListForView(state, true).getList().size() == 1, "Next selected weekday missing");
        state.setTodoViewOffset(1);
        check(engine.getTodoListForView(state, true).getList().isEmpty(), "Wrong weekday scheduled");
        state.setTodoViewOffset(0);
        engine.addTodoTask(state, "Read a chapter", "knowledgeXp", 30);
        TodoTask task = state.getTodoTasks().get(0);
        check(plan.saveGoal(state, null, "Read once", "knowledgeXp", "Read a chapter", 1, day.substring(0,7)) instanceof Outcome.Applied, "Create goal");
        PersonalGoal goal = state.getPersonalGoals().get(0);
        check(plan.goalActions(state, goal).isEmpty(), "Planning awarded progress");
        long now = System.currentTimeMillis();
        check(plan.startFocus(state, task, 1, now) instanceof Outcome.Applied, "Start focus");
        check(plan.startFocus(state, task, 1, now) instanceof Outcome.Failure, "Concurrent session allowed");
        check(plan.finishFocus(state, now + 1000) instanceof Outcome.Failure, "Early focus reward");
        plan.pauseFocus(state, now + 20000);
        check(state.getFocusSession().remaining(now + 90000) == 40000, "Pause did not freeze timer");
        GameState restored = StateJson.INSTANCE.decode(StateJson.INSTANCE.backupEnvelope(state));
        check(restored.getFocusSession() != null && !restored.getFocusSession().getRunning(), "Focus backup lost");
        check(restored.getPersonalGoals().size() == 1 && restored.getRecurringTasks().size() == 1, "Planning backup lost");
        engine.ensureDefaults(restored);
        check(restored.getTodoTasks().size() == 1, "Removed recurring copy restored");
        plan.resumeFocus(restored, now + 100000);
        check(plan.finishFocus(restored, now + 140001) instanceof Outcome.Applied, "Finish timer");
        check(restored.getTotalXp() == 30 && restored.getCoins() == 10, "Focus reward differs from normal task");
        check(plan.goalActions(restored, goal).size() == 1, "Goal did not progress");
        check(plan.finishFocus(restored, now + 150000) instanceof Outcome.Failure, "Focus rewarded twice");
        engine.completeTodoTask(restored, task.getId());
        check(restored.getCoins() == 10 && restored.getTotalXp() == 30, "Done rewarded twice");
        engine.undoTodoTask(restored, task.getId());
        check(plan.goalActions(restored, goal).isEmpty() && restored.getCoins() == 0, "Undo retained goal or coins");
        GameState local = new GameState();
        StateJson.INSTANCE.restoreLocalExtras(local, StateJson.INSTANCE.encodeLocalExtras(restored).toString());
        check(local.getPersonalGoals().size() == 1 && local.getScheduledOccurrences().contains(removedId), "Room extras lost planning state");
        GameState legacy = StateJson.INSTANCE.decode("{\"playerName\":\"Legacy\"}");
        check(legacy.getPersonalGoals().isEmpty() && legacy.getFocusSession() == null, "Old save compatibility");
        GameState rollover = new GameState();
        String yesterday = Dates.INSTANCE.addDays(day, -1);
        rollover.setTodoDate(yesterday);
        rollover.getTodoTasks().add(new TodoTask("todo-midnight", "Late focus", "medicineXp", 20, yesterday, false, ""));
        rollover.setFocusSession(new FocusSession("todo-midnight", yesterday, 60000, 0, now-1, true));
        check(plan.finishFocus(rollover, now) instanceof Outcome.Applied, "Rollover lost focus task");
        check(rollover.getTodoHistory().get(yesterday).get(0).getCompleted(), "Rollover completed wrong day");
        check(rollover.getActionHistory().get(0).getDate().equals(yesterday), "Rollover reward date changed");
        engine.ensureTodoTasks(restored);
        RecurringTask rule = restored.getRecurringTasks().get(0);
        plan.saveRecurring(restored, rule.getId(), "Updated exercise", "strengthXp", 50, days);
        check(restored.getTodoTasks().size() == 1, "Editing schedule recreated deliberately removed task");
        System.out.println("PlanningChecks passed");
    }
}
