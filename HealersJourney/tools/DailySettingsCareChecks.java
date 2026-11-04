import com.healersjourney.app.domain.*;
import com.healersjourney.app.data.backup.StateJson;
import java.util.*;

/** Isolated state: reset boundaries, normalized care, auto tasks, rewards, undo and backup. */
public final class DailySettingsCareChecks {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        GameEngine e = GameEngine.INSTANCE;
        Dates dates = Dates.INSTANCE;
        GameState s = new GameState();
        e.ensureDefaults(s);
        String day = s.getTodoDate();
        check(s.getTodoTasks().size() == 2, "Due defaults missing");
        check(s.getTodoTasks().stream().allMatch(t -> t.getSkill().equals("socialXp")), "Care is not Social");
        check(s.getTodoTasks().stream().allMatch(t -> t.getXp() == 40), "Care task XP is not 40");
        e.ensureTodoTasks(s);
        check(s.getTodoTasks().size() == 2, "Care tasks duplicated");
        int original = e.personalCareStats(s).getScore();
        for (Rules.CareRule rule : new ArrayList<>(s.getCareRoutines())) {
            if (!rule.getKey().equals("shower")) e.removeCareRoutine(s, rule.getKey());
        }
        check(e.personalCareStats(s).getScore() == original, "Score depends on routine count rather than proportion");
        e.ensureTodoTasks(s);
        check(s.getTodoTasks().size() == 1, "Removed routine left an open task");
        String taskId = s.getTodoTasks().get(0).getId();
        e.addXp(s, "knowledgeXp", 25, "Study", day);
        e.addXp(s, "medicineXp", 25, "Work", day);
        int coins = s.getCoins();
        check(e.completeTodoTask(s, taskId) instanceof Outcome.Applied, "Care completion failed");
        check(e.personalCareStats(s).getScore() == 60, "Completion did not refresh character");
        check(s.getCoins() == coins + 40, "Care did not earn 10 coins plus three-category bonus");
        ActionEntry care = s.getActionHistory().get(0);
        check(care.getXp() == 40 && s.getTotalXp() == 90 && s.getSocialXp() == 40, "Care did not earn 40 Social XP");
        check(care.getSkill().equals("socialXp") && "shower".equals(care.getCareKey()), "Care action linkage lost");
        check(FriendlyLog.INSTANCE.action(care).equals("Took a shower"), "Care log is not friendly");
        check(DailyShowcase.INSTANCE.entries(s, day).size() == 3, "Social care trophy missing");
        e.completePersonalCare(s, "shower", day);
        check(s.getCoins() == coins + 40 && s.getTotalXp() == 90, "Repeated care awarded twice");
        check(e.undoTodoTask(s, taskId) instanceof Outcome.Applied, "Care undo failed");
        check(s.getCoins() == coins && s.getTotalXp() == 50, "Undo did not reverse only care coins/bonus");
        check(e.personalCareStats(s).getScore() == original, "Undo did not restore care appearance");
        e.ensureTodoTasks(s);
        check(s.getTodoTasks().size() == 1 && !s.getTodoTasks().get(0).getCompleted(), "Undo duplicated task");
        e.completeTodoTask(s, taskId);
        e.deleteTodoTask(s, taskId);
        check(s.getCoins() == coins && s.getTodoTasks().isEmpty(), "Removing care retained rewards");
        e.ensureTodoTasks(s);
        check(s.getTodoTasks().isEmpty(), "Removed care task reappeared");
        GameState custom = new GameState(); custom.setCareRoutinesConfigured(true);
        e.ensureDefaults(custom); e.saveCareRoutine(custom, null, "Wash face", 1); e.ensureTodoTasks(custom);
        String customKey = custom.getCareRoutines().get(0).getKey();
        String customTask = custom.getTodoTasks().get(0).getId();
        int customScore = e.personalCareStats(custom).getScore();
        e.completeTodoTask(custom, customTask); e.undoTodoTask(custom, customTask);
        check("".equals(custom.getPersonalCareLast().get(customKey)) && e.personalCareStats(custom).getScore() == customScore,
            "Undo changed a never-completed custom routine baseline");
        e.setDayResetHour(s, 9);
        Calendar time = Calendar.getInstance();
        time.set(2026, Calendar.NOVEMBER, 3, 8, 59, 0);
        check(dates.gameDayKey(time).equals("2026-11-02"), "Before custom reset boundary");
        time.set(Calendar.HOUR_OF_DAY, 9); time.set(Calendar.MINUTE, 0);
        check(dates.gameDayKey(time).equals("2026-11-03"), "At custom reset boundary");
        check(e.setDayResetHour(s, 24) instanceof Outcome.Failure, "Invalid reset accepted");
        GameState restored = StateJson.INSTANCE.decode(StateJson.INSTANCE.backupEnvelope(s));
        check(restored.getDayResetHour() == 9, "Backup lost reset hour");
        GameState local = new GameState();
        StateJson.INSTANCE.restoreLocalExtras(local, StateJson.INSTANCE.encodeLocalExtras(s).toString());
        check(local.getDayResetHour() == 9, "Local save lost reset hour");
        check(StateJson.INSTANCE.decode("{\"playerName\":\"Legacy\"}").getDayResetHour() == 5, "Old backup changed default reset");
        e.setDayResetHour(s, 0);
        time.set(2026, Calendar.NOVEMBER, 3, 0, 0, 0);
        check(dates.gameDayKey(time).equals("2026-11-03"), "Midnight reset failed");
        e.setDayResetHour(s, 5);
        System.out.println("DailySettingsCareChecks passed");
    }
}
