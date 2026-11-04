import com.healersjourney.app.domain.*;
import com.healersjourney.app.data.backup.StateJson;
import java.util.*;

/** Regressions found during the app audit. Uses isolated state only. */
public final class AuditRegressionChecks {
    static final GameEngine e = GameEngine.INSTANCE;
    static final Dates d = Dates.INSTANCE;
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static GameState fresh() { GameState s = new GameState(); s.setCareRoutinesConfigured(true); e.ensureDefaults(s); return s; }
    static void reject(String text) {
        try { StateJson.INSTANCE.decode(text); throw new AssertionError("Accepted invalid backup: " + text); }
        catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) {
        reject("{}"); reject("{\"unrelated\":true}"); reject("{\"state\":false,\"totalXp\":40}");
        reject("{\"totalXp\":\"bad\",\"playerName\":\"Player\"}");
        reject("{\"totalXp\":40,\"actionHistory\":false}");
        String day = d.gameDayKey(Calendar.getInstance()), previous = d.addDays(day, -2);
        GameState xp = fresh();
        ActionEntry first = e.addXp(xp, "medicineXp", 80, "A", day);
        e.addXp(xp, "medicineXp", 80, "B", day); e.addXp(xp, "medicineXp", 80, "C", day);
        e.undoActionById(xp, first.getId());
        check(xp.getTotalXp() == 160 && xp.getMedicineXp() == 160, "Undo did not release tier XP");
        check(e.weeklyXp(xp, "medicineXp") == 160, "Weekly XP not recalculated");
        check(xp.getActionHistory().stream().mapToInt(ActionEntry::getXp).sum() == 160, "History and totals disagree");
        GameState care = new GameState(); e.ensureDefaults(care);
        String baseline = care.getPersonalCareLast().get("shower");
        e.completePersonalCare(care, "shower", previous);
        String older = care.getActionHistory().get(0).getId();
        e.completePersonalCare(care, "shower", day);
        String newer = care.getActionHistory().get(0).getId();
        e.undoActionById(care, older);
        check(day.equals(care.getPersonalCareLast().get("shower")), "Older undo removed newer care");
        e.undoActionById(care, newer);
        check(baseline.equals(care.getPersonalCareLast().get("shower")), "Care baseline resurrected deleted action");
        e.completePersonalCare(care, "shower", day); e.completePersonalCare(care, "shower", previous);
        check(day.equals(care.getPersonalCareLast().get("shower")), "Backdating replaced latest care");
        String backdated = care.getActionHistory().get(0).getId();
        e.undoActionById(care, care.getActionHistory().get(1).getId());
        check(previous.equals(care.getPersonalCareLast().get("shower")), "Backdated remaining care ignored");
        e.undoActionById(care, backdated);
        check(baseline.equals(care.getPersonalCareLast().get("shower")), "Reverse-order care baseline wrong");
        GameState frequency = new GameState(); e.ensureDefaults(frequency);
        frequency.getPersonalCareLast().put("shower", previous); e.ensureTodoTasks(frequency);
        e.saveCareRoutine(frequency, "shower", "Shower", 7); e.ensureTodoTasks(frequency);
        check(frequency.getTodoTasks().stream().noneMatch(t -> t.getId().equals(e.careTaskId("shower", day))), "Not-due care task remained");
        e.saveCareRoutine(frequency, "shower", "Shower", 1); e.ensureTodoTasks(frequency);
        check(frequency.getTodoTasks().stream().filter(t -> t.getId().equals(e.careTaskId("shower", day))).count() == 1, "Due care task not recreated");
        GameState history = fresh(); e.addTodoTask(history, "Keep this trophy", "medicineXp", 40);
        String taskId = history.getTodoTasks().get(0).getId(); e.completeTodoTask(history, taskId);
        for (int i = 0; i < 4000; i++) e.addXp(history, "knowledgeXp", 0, "Entry " + i, day);
        check(history.getActionHistory().size() == 4001, "Old history truncated");
        check(DailyShowcase.INSTANCE.entries(history, day).size() == 4001, "Old trophy lost");
        GameState restored = StateJson.INSTANCE.decode(StateJson.INSTANCE.backupEnvelope(history));
        e.ensureDefaults(restored);
        check(restored.getActionHistory().size() == 4001, "Backup lost history");
        check(e.undoTodoTask(restored, taskId) instanceof Outcome.Applied, "Old task cannot be undone");
        check(restored.getTotalXp() == 0 && restored.getCoins() == 40000, "Old task rewards not reversed");
        GameState creation = fresh();
        e.addPlannedActivity(creation, "Water the plants", "socialXp", 20, true, Collections.emptySet());
        check(creation.getTodoTasks().stream().anyMatch(t -> t.getName().equals("Water the plants")), "Typed task missing");
        check(e.defsFor(creation, "socialXp", "other").stream().anyMatch(a -> a.getName().equals("Water the plants")), "Reusable activity missing");
        System.out.println("AuditRegressionChecks passed");
    }
}
