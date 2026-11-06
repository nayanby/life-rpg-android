import com.healersjourney.app.domain.*;
import java.util.*;

/** Current-period cards include empty days/months and never double-count boundary weeks. */
public final class TrophyCollectionChecks {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        GameState state = new GameState(); state.setCareRoutinesConfigured(true);
        GameEngine engine = GameEngine.INSTANCE; engine.ensureDefaults(state);
        TrophyCollection collection = TrophyCollection.INSTANCE;
        String today = "2026-01-01";
        check(collection.cards(state, TrophyCollection.Range.Week, today).size() == 7, "Empty week must show seven days");
        check(collection.cards(state, TrophyCollection.Range.Year, today).size() == 12, "Empty year must show twelve months");
        engine.addXp(state, "medicineXp", 40, "December task", "2025-12-31");
        ActionEntry januaryWork = null;
        for (String skill : Rules.INSTANCE.getSKILL_KEYS()) {
            ActionEntry action = engine.addXp(state, skill, 40, "January " + skill, today);
            if (skill.equals("medicineXp")) januaryWork = action;
        }
        List<TrophyCollection.Period> days = collection.cards(state, TrophyCollection.Range.Week, today);
        check(days.size() == 7 && days.get(0).getStart().equals("2025-12-29") && days.get(6).getStart().equals("2026-01-04"), "Week must be Monday through Sunday across years");
        check(days.get(0).getEntries().isEmpty() && days.get(2).getEntries().size() == 1 && days.get(3).getEntries().size() == 5, "Daily trophy grouping wrong");
        check(days.stream().mapToInt(TrophyCollection.Period::getCoins).sum() == 140, "Daily bonuses duplicated or lost");
        List<TrophyCollection.Period> weeks = collection.cards(state, TrophyCollection.Range.Month, today);
        check(weeks.size() == 5 && weeks.get(0).getStart().equals("2026-01-01") && weeks.get(4).getEnd().equals("2026-01-31"), "Month's partial weeks wrong");
        check(weeks.stream().mapToInt(TrophyCollection.Period::getCoins).sum() == 130, "Previous month leaked into first week card");
        for (int i = 1; i < weeks.size(); i++) check(Dates.INSTANCE.addDays(weeks.get(i-1).getEnd(), 1).equals(weeks.get(i).getStart()), "Month has overlapping or missing days");
        List<TrophyCollection.Period> months = collection.cards(state, TrophyCollection.Range.Year, today);
        check(months.size() == 12 && months.get(0).getStart().equals("2026-01-01") && months.get(11).getEnd().equals("2026-12-31"), "Year's month cards wrong");
        check(months.get(0).getCoins() == 130 && months.get(11).getCoins() == 0, "Year includes another year's rewards");
        for (String skill : Rules.INSTANCE.getSKILL_KEYS()) check(months.get(0).count(skill) == 1, "Category count wrong");
        engine.undoActionById(state, januaryWork.getId());
        TrophyCollection.Period january = collection.cards(state, TrophyCollection.Range.Year, today).get(0);
        check(january.getCoins() == 70 && january.getXp() == 160 && january.count("medicineXp") == 0, "Undo left stale totals");
        List<TrophyCollection.Period> leapWeeks = collection.cards(state, TrophyCollection.Range.Month, "2024-02-10");
        check(leapWeeks.get(leapWeeks.size()-1).getEnd().equals("2024-02-29"), "Leap day missing");
        check(collection.cards(state, TrophyCollection.Range.Year, "2024-02-10").get(1).getEnd().equals("2024-02-29"), "Leap month summary incorrect");
        System.out.println("TrophyCollectionChecks passed");
    }
}
