import com.healersjourney.app.domain.*;
import java.util.*;

/** Collection periods cross calendar boundaries and follow reward reversals. */
public final class TrophyCollectionChecks {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        GameState state = new GameState(); state.setCareRoutinesConfigured(true);
        GameEngine engine = GameEngine.INSTANCE; engine.ensureDefaults(state);
        check(TrophyCollection.INSTANCE.periods(state, false).isEmpty(), "Empty collection has cards");
        engine.addXp(state, "medicineXp", 40, "December task", "2025-12-31");
        ActionEntry januaryWork = null;
        for (String skill : Rules.INSTANCE.getSKILL_KEYS()) {
            ActionEntry action = engine.addXp(state, skill, 40, "January " + skill, "2026-01-01");
            if (skill.equals("medicineXp")) januaryWork = action;
        }
        List<TrophyCollection.Period> weeks = TrophyCollection.INSTANCE.periods(state, false);
        check(weeks.size() == 1 && weeks.get(0).getStart().equals("2025-12-29"), "Week split across years");
        check(weeks.get(0).getEnd().equals("2026-01-04") && weeks.get(0).getEntries().size() == 6, "Week dates/count wrong");
        check(weeks.get(0).getActiveDays() == 2 && weeks.get(0).getXp() == 240, "Week summary wrong");
        check(weeks.get(0).getCoins() == 140, "Weekly category bonuses not included exactly once");
        List<TrophyCollection.Period> months = TrophyCollection.INSTANCE.periods(state, true);
        check(months.size() == 2 && months.get(0).getStart().equals("2026-01-01"), "Months not newest first");
        check(months.get(0).getEnd().equals("2026-01-31") && months.get(1).getEnd().equals("2025-12-31"), "Month boundaries wrong");
        check(months.get(0).getCoins() == 130 && months.get(1).getCoins() == 10, "Coins crossed month boundary");
        for (String skill : Rules.INSTANCE.getSKILL_KEYS()) check(months.get(0).count(skill) == 1, "Category count wrong");
        engine.undoActionById(state, januaryWork.getId());
        TrophyCollection.Period january = TrophyCollection.INSTANCE.periods(state, true).get(0);
        check(january.getCoins() == 70 && january.getXp() == 160 && january.count("medicineXp") == 0, "Undo left stale monthly totals");
        System.out.println("TrophyCollectionChecks passed");
    }
}
