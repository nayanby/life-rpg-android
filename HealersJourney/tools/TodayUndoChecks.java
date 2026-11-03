import com.healersjourney.app.domain.*;

/** Isolated check: Today undo and removal reverse only the linked task reward. */
public final class TodayUndoChecks {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        GameEngine engine = GameEngine.INSTANCE;
        GameState state = new GameState();
        state.setCareRoutinesConfigured(true); // These checks isolate planned tasks; care integration has its own suite.
        String day = Dates.INSTANCE.gameDayKey(java.util.Calendar.getInstance());
        engine.ensureDefaults(state);
        check(engine.addTodoTask(state, "Read", "knowledgeXp", 30) instanceof Outcome.Applied, "Add task");
        String id = state.getTodoTasks().get(0).getId();
        check(engine.completeTodoTask(state, id) instanceof Outcome.Applied, "Complete task");
        check(state.getTotalXp() == 30 && DailyShowcase.INSTANCE.entries(state, day).size() == 1, "Task reward");
        engine.addXp(state, "medicineXp", 20, "Work", day);
        check(engine.undoTodoTask(state, id) instanceof Outcome.Applied, "Undo task");
        check(!state.getTodoTasks().get(0).getCompleted() && state.getTotalXp() == 20, "Undo failed to reverse XP");
        check(DailyShowcase.INSTANCE.entries(state, day).size() == 1, "Undo touched unrelated trophy");
        check(engine.completeTodoTask(state, id) instanceof Outcome.Applied, "Recomplete task");
        check(engine.deleteTodoTask(state, id) instanceof Outcome.Applied, "Remove task");
        check(state.getTodoTasks().isEmpty() && state.getTotalXp() == 20, "Remove retained task XP");
        check(DailyShowcase.INSTANCE.entries(state, day).size() == 1, "Remove touched unrelated trophy");
        System.out.println("TodayUndoChecks passed");
    }
}
