import com.healersjourney.app.domain.*;
import com.healersjourney.app.data.backup.StateJson;

public final class CoinChecks {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        GameState state = new GameState();
        WorldRules.INSTANCE.reconcile(state);
        check(state.getCoins() == 0, "new balance");
        GameEngine.INSTANCE.addXp(state, "knowledgeXp", 20, "Read", "2026-10-25");
        check(state.getCoins() == 10, "first activity");
        GameEngine.INSTANCE.addXp(state, "knowledgeXp", 20, "Read again", "2026-10-25");
        check(state.getCoins() == 20, "repeat category still earns");
        GameEngine.INSTANCE.addXp(state, "medicineXp", 20, "Work", "2026-10-25");
        check(state.getCoins() == 30, "second category");
        GameEngine.INSTANCE.addXp(state, "knowledgeXp", 20, "Read tomorrow", "2026-10-26");
        check(state.getCoins() == 40, "next day activity");
        ActionEntry fifth = GameEngine.INSTANCE.addXp(state, "socialXp", 20, "Talk", "2026-10-26");
        check(state.getCoins() == 50, "every completion earns ten");
        int price = WorldRules.INSTANCE.coinPrice(WorldRules.INSTANCE.getItems().stream()
            .filter(item -> item.getId().equals("forest_cloak")).findFirst().orElseThrow());
        check(price == 50, "cloak price");
        check(WorldRules.INSTANCE.buyAndEquip(state, "forest_cloak") == Outcome.Applied.INSTANCE, "purchase");
        check(state.getCoins() == 0 && state.getWorldUnlocks().contains("forest_cloak"), "purchase balance and ownership");
        check(GameEngine.INSTANCE.undoActionById(state, fifth.getId()), "undo completion");
        check(state.getCoins() == 0 && state.getCoinDebt() == 10, "spent coins must become debt on undo");
        GameEngine.INSTANCE.addXp(state, "socialXp", 20, "Talk again", "2026-10-26");
        check(state.getCoins() == 0 && state.getCoinDebt() == 0, "new reward repays debt");
        GameEngine.INSTANCE.addXp(state, "hobbiesXp", 20, "Create", "2026-10-26");
        check(state.getCoins() == 40, "third category earns ten plus thirty bonus");
        check(DailyCoinBonus.INSTANCE.earnedToday(state, "2026-10-26") == 60, "shelf includes daily bonus");
        GameEngine.INSTANCE.addXp(state, "medicineXp", 20, "Work today", "2026-10-26");
        ActionEntry fifthCategory = GameEngine.INSTANCE.addXp(state, "strengthXp", 20, "Workout", "2026-10-26");
        check(state.getCoins() == 110, "fifth category earns ten plus fifty bonus");
        check(DailyCoinBonus.INSTANCE.earnedToday(state, "2026-10-26") == 130, "shelf includes both bonuses");
        check(GameEngine.INSTANCE.undoActionById(state, fifthCategory.getId()), "undo fifth category");
        check(state.getCoins() == 50, "undo removes activity and fifty-coin bonus");
        check(DailyCoinBonus.INSTANCE.earnedToday(state, "2026-10-26") == 70, "shelf removes undone rewards");
        int knight = WorldRules.INSTANCE.coinPrice(WorldRules.INSTANCE.getItems().stream()
            .filter(item -> item.getId().equals("golden_knight")).findFirst().orElseThrow());
        int dragon = WorldRules.INSTANCE.coinPrice(WorldRules.INSTANCE.getItems().stream()
            .filter(item -> item.getId().equals("dragon")).findFirst().orElseThrow());
        check(knight + dragon == 3500 && (knight + dragon) / 20 == 175, "six-month final-item pacing");
        GameState restored = new GameState();
        StateJson.INSTANCE.restoreLocalExtras(restored, StateJson.INSTANCE.encodeLocalExtras(state).toString());
        check(restored.getCoins() == 50 && restored.getCoinAwardedActionIds().size() == 7, "local coin persistence");
        GameState backup = StateJson.INSTANCE.decode(StateJson.INSTANCE.backupEnvelope(state));
        check(backup.getCoins() == 50 && backup.getWorldUnlocks().contains("forest_cloak"), "backup coin persistence");
        System.out.println("CoinChecks passed");
    }
}
