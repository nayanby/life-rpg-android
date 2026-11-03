# Life RPG Android

An Android life RPG where real-life activities grow your character, earn coins, and unlock outfits, companions and room backdrops. Package: `com.life.rpg`.

The Kotlin / Jetpack Compose project lives in `HealersJourney/`. Its four main tabs are Home, Today, Store and Journal. Home has an outlined name plate with the level, XP bar and badge on its right. It animates into a compact level-and-XP tile on other screens, hiding the name and badge; Today and Store also show the coin balance. Artwork uses individual pixel PNGs, while text stays clear and readable.

## Current experience

- Home centers your character and companion against the equipped backdrop. Swipe the five-category XP graph through Yesterday, Day, Week, Month, Year and Lifetime. It opens on Day; swipe right for Yesterday. Tap any category icon or label on the graph for its description and activity examples. Social keeps its name and includes household errands and responsibilities alongside relationships. Tap your character to open Care.
- Today lets you plan tasks from saved activities or create new ones. Completed tasks remain visible with a strike-through. Undoing or removing a completed task reverses its linked XP and coin rewards. Long-press saved activities to edit or delete them.
- A compact dated display holds six small trophies per row, growing to four rows before pagination, plus earned coins and category bonuses. Tap a trophy for its activity, date, XP and coins, or browse the full collection. Date navigation slides the shelf between days.
- Today has no task timers or separate repeating-task section. Saved activities own their weekday schedules. Category tags appear in two visible rows in the picker. XP is selected from 20, 40, 60 and 80, with no custom XP entry. Existing stored rewards stay unchanged until edited.
- Recurring tasks appear on selected weekdays, beginning when their schedule is created. Open task copies are editable; removed copies stay removed, and schedule changes preserve completed history. Repeating weekdays are edited with each saved activity. Today’s settings icon sets the game-day reset hour (5 AM by default).
- Personal goals in Today and Journal â†’ Goals & milestones count completions for a calendar month, either across one category or for an exact activity name. Choose this or next month with a readable month selector. Done today records one 0-XP, 10-coin completion if no matching activity has already counted today; its check-in can be undone. Reaching a target earns a commemorative trophy. Undo and deletion update goal progress and its trophy.
- Store offers outfits, companions and backdrops for coins. Successful purchases open an enlarged preview of the equipped item; tap an owned item to view it enlarged again. Male and female characters have matching outfit tiers, with care states that change their appearance, expression and pose.
- Care starts with Shower and Nails. Choose other common routines or add a custom routine, set its frequency, and long-press to edit or remove it. Due routines automatically appear in Today under Social, with linked completion/undo, 40 base XP and the usual coins. Appearance uses the average missed intervals across selected routines, so it scales with any routine count.
- Journal contains history and Goals & milestones, with My notes and Money & goals together under More tools. My notes uses a notebook PNG and plain text, with one automatically saved note per calendar date and no custom title field. Existing thoughts entries are preserved. Notes stay on-device and are included in local saves and backup exports. The Achievements page has been removed. Goals and milestones have separate tabs. Goals have active/achieved filters; milestones appear together in one list. Milestones can be edited and pinned to Home. Journal's Material settings icon opens character preferences, backup, sync information, feedback settings and data reset. First launch asks for a username and character.
- Tasks, saved activities and recurring tasks use four XP choices: 20, 40, 60 and 80. Older zero-XP records remain compatible with normal coins, trophies and goal progress. The activity picker carries typed search text into new-task and new-preset forms; Save as a new activity stores the typed task for reuse.

## Rewards

Each completed activity in Study, Work, Fitness, Social or Hobbies earns 10 coins. Completing activities in any three categories earns an additional 30 coins; all five categories earns another 50 coins. Undo reverses linked rewards. If coins were already spent, later earnings repay the difference.

Each category's first 200 base XP per game day earns 100%, the next 200 earns 50%, and the rest earns 25%. Cumulative rounding prevents extra rewards from splitting an activity. The game day resets at the chosen hour (5 AM by default), shared by tasks, XP budgets and daily bonuses.

## Build

Use Android SDK 36 and JDK 17 or a compatible Android Studio runtime. Configure `ANDROID_HOME` or an untracked `HealersJourney/local.properties`.

```powershell
cd HealersJourney
.\gradlew.bat assembleDebug
```

Release signing uses `LIFERPG_UPLOAD_STORE_FILE` and `LIFERPG_UPLOAD_PASSWORD` environment variables. Signing keys, passwords and machine-specific configuration are excluded from Git.

The isolated regression checks in `HealersJourney/tools/` cover task undo, coins, daily category bonuses, care routines, planning and the daily shelf. `PlanningChecks.java` checks recurring copies, focus recovery, goal progress, undo, backup round-trips and older saves. They do not modify installed user data.

## Google Play Games and privacy

Google Play Games Services v2 (`22.1.0`) is initialized at app startup. Settings displays the connected player name and offers sign-in. The app does not upload activity logs, care records, finances or game saves to Play Games. Google Drive sync is unavailable until its separate OAuth configuration is supplied.

Public integration identifiers are recorded in `HealersJourney/store-listing/play-games-setup.json`. Debug and Play-distributed builds require matching registered signing certificates and an eligible Play Games tester account while the service is in testing.

See [PRIVACY.md](PRIVACY.md) for local storage, SDK collection and deletion instructions. Google Play's updated Data safety declaration was published on 27 October 2026.

## Artwork and store listing

Runtime artwork is in `HealersJourney/app/src/main/res/`. Design sources and generation prompts are under `HealersJourney/design/`. Store descriptions, graphics, screenshots and their tooling are under `HealersJourney/store-listing/`; generated application bundles and private publishing files are excluded.

The five trophies use transparent PNG artwork with dimensional pixel-art shading. Original generated images and prompts are in `HealersJourney/design/trophies/`; `tools/export_trophy_pngs.py` creates optimized 512-pixel runtime PNGs. Shelf, collection and personal goals use the same cached images. Character artwork and level badges retain their existing style.
