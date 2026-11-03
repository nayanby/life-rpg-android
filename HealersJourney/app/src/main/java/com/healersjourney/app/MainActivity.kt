package com.healersjourney.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween

import com.healersjourney.app.ui.screens.ExploreScreen
import com.healersjourney.app.ui.screens.OnboardingScreen
import com.healersjourney.app.data.games.PlayGamesConnection
import com.healersjourney.app.data.games.PlayGamesUiState
import androidx.compose.ui.graphics.toArgb
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sanitizer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import com.healersjourney.app.ui.components.AppIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.healersjourney.app.ui.components.AppLayout
import androidx.compose.ui.unit.dp
import com.healersjourney.app.domain.GameEngine
import com.healersjourney.app.domain.GameState
import com.healersjourney.app.domain.Rules
import com.healersjourney.app.ui.GameSnapshot
import com.healersjourney.app.ui.GameViewModel
import com.healersjourney.app.ui.components.PillNavigation
import com.healersjourney.app.ui.components.Art
import com.healersjourney.app.ui.screens.CareScreen
import com.healersjourney.app.ui.screens.DataScreen
import com.healersjourney.app.ui.screens.DashboardScreen
import com.healersjourney.app.ui.screens.RenameDialog
import com.healersjourney.app.ui.screens.DailyScreen
import com.healersjourney.app.ui.screens.FinanceScreen
import com.healersjourney.app.ui.screens.FitnessScreen
import com.healersjourney.app.ui.screens.HobbiesScreen
import com.healersjourney.app.ui.screens.LogsScreen
import com.healersjourney.app.ui.screens.MedicineScreen
import com.healersjourney.app.ui.screens.MilestonesScreen
import com.healersjourney.app.ui.screens.SleepScreen
import com.healersjourney.app.ui.screens.SocialScreen
import com.healersjourney.app.ui.screens.StudyScreen
import com.healersjourney.app.ui.screens.TitleShelfDialog
import com.healersjourney.app.ui.screens.TodoScreen
import com.healersjourney.app.ui.screens.SettingsScreen
import com.healersjourney.app.ui.screens.MyNotesScreen
import com.healersjourney.app.ui.theme.HealersJourneyTheme
import com.healersjourney.app.ui.theme.ThemePrefs
import com.healersjourney.app.ui.theme.isDark
import kotlinx.coroutines.launch

enum class Destination(val label: String, val icon: ImageVector) {
    Dashboard("World", Icons.Filled.Home),
    Growth("Growth", Icons.AutoMirrored.Filled.MenuBook),
    More("Journal", Icons.Filled.Menu),
    Settings("Settings", Icons.Filled.Settings),
    LearningProgress("Study journey", Icons.AutoMirrored.Filled.MenuBook),
    WorkProgress("Work journey", Icons.Filled.MedicalServices),
    FitnessProgress("Fitness journey", Icons.Filled.FitnessCenter),
    SocialProgress("Social journey", Icons.Filled.Groups),
    CreativityProgress("Creative journey", Icons.Filled.MusicNote),
    RestProgress("Rest & recovery", Icons.Filled.Bedtime),
    Daily("Quick activity", Icons.Filled.Bolt),
    Study("Study", Icons.AutoMirrored.Filled.MenuBook),
    Medicine("Work", Icons.Filled.MedicalServices),
    Fitness("Fitness", Icons.Filled.FitnessCenter),
    Social("Social", Icons.Filled.Groups),
    Sleep("Sleep", Icons.Filled.Bedtime),
    Hobbies("Hobbies", Icons.Filled.MusicNote),
    Finance("Finance", Icons.Filled.AccountBalanceWallet),
    Notes("My notes", Icons.AutoMirrored.Filled.MenuBook),
    Character("Store", Icons.Filled.Sanitizer),
    Todo("Today", Icons.Filled.Checklist),
    Care("Personal care", Icons.Filled.Sanitizer),
    Milestones("Goals & milestones", Icons.Filled.Flag),
    // Retained only so older saved navigation state can be redirected safely.
    Achievements("Journal", Icons.Filled.Menu),
    Logs("Logs & history", Icons.AutoMirrored.Filled.ReceiptLong),
    Data("Data & sync", Icons.Filled.CloudSync)
}

class MainActivity : ComponentActivity() {

    private val playGames by lazy { PlayGamesConnection(this) }

    private val themePrefs: ThemePrefs by lazy { (application as HealersJourneyApp).container.themePrefs }

    private val viewModel: GameViewModel by viewModels {
        GameViewModel.Factory((application as HealersJourneyApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val signIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.let(viewModel::handleDriveSignInResult)
        }
        setContent {
            val themeMode by themePrefs.mode.collectAsState()
            HealersJourneyTheme(themeMode) {
                val colors = MaterialTheme.colorScheme
                val dark = themeMode.isDark()
                androidx.compose.runtime.SideEffect {
                    window.statusBarColor = colors.background.toArgb()
                    window.navigationBarColor = colors.surface.toArgb()
                    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                val gamesState by playGames.state.collectAsState()
                AppRoot(viewModel, onSignIn = { signIn.launch(viewModel.driveSignInIntent()) },
                    gamesState = gamesState, onGamesSignIn = playGames::signIn, themePrefs = themePrefs)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        playGames.refresh()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: GameViewModel, onSignIn: () -> Unit,
            gamesState: PlayGamesUiState, onGamesSignIn: () -> Unit, themePrefs: ThemePrefs) {
    val snapshot by viewModel.snapshot.collectAsState()
    val message by viewModel.message.collectAsState()
    val drive by viewModel.drive.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    if (loaded && !snapshot.state.onboardingCompleted) {
        OnboardingScreen(viewModel)
        return
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var destination by rememberSaveable { mutableStateOf(Destination.Dashboard) }
    var tab by rememberSaveable { mutableStateOf(Destination.Dashboard) }
    var todaySkill by rememberSaveable { mutableStateOf<String?>(null) }
    var showTitles by rememberSaveable { mutableStateOf(false) }
    var todaySettings by rememberSaveable { mutableStateOf(false) }
    val primary = listOf(Destination.Dashboard, Destination.Todo, Destination.Character, Destination.More)
    val retiredPages = setOf(Destination.Achievements, Destination.LearningProgress, Destination.WorkProgress, Destination.FitnessProgress, Destination.SocialProgress, Destination.CreativityProgress, Destination.RestProgress)
    val open: (Destination) -> Unit = { next ->
        val target = when {
            next in retiredPages -> Destination.Dashboard
            else -> when(next) {
            Destination.Growth -> Destination.Dashboard
            Destination.Care -> Destination.Care
            Destination.Character -> Destination.Character
            else -> next
            }
        }
        if (target == Destination.Todo && next == Destination.Todo) {
            todaySkill = null
            viewModel.runQuiet { GameEngine.openTodoWindow(it) }
        }
        destination = target
        if (target in primary) tab = target
    }
    LaunchedEffect(Unit) {
        if (destination == Destination.Growth || destination in retiredPages) destination = Destination.Dashboard
        if (tab == Destination.Growth) tab = Destination.Dashboard
    }
    BackHandler(enabled = destination !in primary) { destination = tab }
    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.showMessage(null) }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { PillNavigation(tab) { open(it) } },
        topBar = {
            TopAppBar(
                expandedHeight = if (destination == Destination.Dashboard) 88.dp else 64.dp,
                title = {
                    if (destination == Destination.Dashboard) {
                        val plateFont = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.papyrus))
                        androidx.compose.material3.Surface(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(snapshot.state.playerName, style = MaterialTheme.typography.titleMedium.copy(fontFamily = plateFont),
                                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text("Level ${Rules.level(snapshot.state.totalXp)} | ${snapshot.state.totalXp % Rules.XP_PER_LEVEL} / ${Rules.XP_PER_LEVEL} XP",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = plateFont), maxLines = 1)
                                    com.healersjourney.app.ui.components.SimpleProgress(
                                        { (snapshot.state.totalXp % Rules.XP_PER_LEVEL).toFloat() / Rules.XP_PER_LEVEL },
                                        Modifier.fillMaxWidth().height(4.dp))
                                }
                                Image(painterResource(Art.badgeFor(Rules.title(Rules.level(snapshot.state.totalXp)))) ,
                                    "View levels and titles", Modifier.size(68.dp).clickable { showTitles = true })
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (destination !in primary) IconButton(onClick = { destination = tab }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (destination == Destination.More) IconButton(onClick = { open(Destination.Settings) }) {
                        androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    if (destination == Destination.Character || destination == Destination.Todo) Row(Modifier.padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Image(com.healersjourney.app.ui.components.gameArtPainter(R.drawable.gameart_icon_coin),
                            contentDescription = "Coins", modifier = Modifier.size(28.dp))
                        Text("${snapshot.state.coins}", style = MaterialTheme.typography.titleMedium)
                    }
                    if (destination == Destination.Todo) IconButton(onClick = { todaySettings = true }) {
                        androidx.compose.material3.Icon(Icons.Filled.Settings, "Day reset settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (!loaded) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else AnimatedContent(targetState = destination, modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val from = primary.indexOf(initialState)
                val to = primary.indexOf(targetState)
                val direction = if (from >= 0 && to >= 0 && to < from) -1 else 1
                (slideInHorizontally(tween(260)) { it * direction } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it * direction / 3 } + fadeOut(tween(150)))
            }, label = "Main screen slide") { page ->
            val screenScroll = rememberScrollState()
            var scrollViewportTopPx by remember { mutableStateOf(0f) }
            var scrollViewportHeightPx by remember { mutableStateOf(0f) }
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            // The keyboard slides over the bottom bar, so the taller of the two insets replaces it rather than stacking.
            val bottomInset = maxOf(padding.calculateBottomPadding(),
                with(density) { WindowInsets.ime.getBottom(this).toDp() })
            Column(Modifier.fillMaxSize()
                .padding(top = padding.calculateTopPadding(), start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection), bottom = bottomInset)
                .onGloballyPositioned {
                    scrollViewportTopPx = it.positionInRoot().y
                    scrollViewportHeightPx = it.size.height.toFloat()
                }
                .verticalScroll(screenScroll).padding(start = 20.dp, end = 20.dp, bottom = if (page == Destination.Notes) 0.dp else 12.dp,
                    // The planner opens with its own day header, so it needs no gap under the app bar.
                    top = if (page == Destination.Todo) 0.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(bottom = if (page == Destination.Notes) 0.dp else 20.dp)) {
                    when (page) {
                        Destination.Achievements -> ExploreScreen(snapshot, viewModel, open)
                        Destination.Dashboard -> DashboardScreen(snapshot, viewModel, open)
                        Destination.Growth -> DashboardScreen(snapshot, viewModel, open)
                        Destination.LearningProgress -> DashboardScreen(snapshot, viewModel, open)
                        Destination.WorkProgress -> DashboardScreen(snapshot, viewModel, open)
                        Destination.FitnessProgress -> DashboardScreen(snapshot, viewModel, open)
                        Destination.SocialProgress -> DashboardScreen(snapshot, viewModel, open)
                        Destination.CreativityProgress -> DashboardScreen(snapshot, viewModel, open)
                        Destination.RestProgress -> DashboardScreen(snapshot, viewModel, open)
                        Destination.Character -> com.healersjourney.app.ui.screens.CharacterScreen(snapshot, viewModel)
                        Destination.More -> ExploreScreen(snapshot, viewModel, open)
                        Destination.Settings -> SettingsScreen(snapshot, viewModel, drive, onSignIn, gamesState, onGamesSignIn, themePrefs)
                        Destination.Daily -> DailyScreen(snapshot, viewModel)
                        Destination.Study -> StudyScreen(snapshot, viewModel)
                        Destination.Medicine -> MedicineScreen(snapshot, viewModel)
                        Destination.Fitness -> FitnessScreen(snapshot, viewModel)
                        Destination.Social -> SocialScreen(snapshot, viewModel)
                        Destination.Sleep -> SleepScreen(snapshot, viewModel)
                        Destination.Hobbies -> HobbiesScreen(snapshot, viewModel)
                        Destination.Finance -> FinanceScreen(snapshot, viewModel)
                        Destination.Notes -> MyNotesScreen(snapshot, viewModel, screenScroll, scrollViewportTopPx, scrollViewportHeightPx)
                        Destination.Todo -> TodoScreen(snapshot, viewModel, todaySkill, screenScroll, scrollViewportTopPx, todaySettings, { todaySettings = false }) { open(Destination.Sleep) }
                        Destination.Care -> CareScreen(snapshot, viewModel)
                        Destination.Milestones -> MilestonesScreen(snapshot, viewModel)
                        Destination.Logs -> LogsScreen(snapshot, viewModel)
                        Destination.Data -> DataScreen(snapshot, viewModel, drive, onSignIn)
                    }
                }
            }
        }
    }
    if (showTitles) TitleShelfDialog(Rules.level(snapshot.state.totalXp)) { showTitles = false }
    com.healersjourney.app.ui.components.RewardCelebration(viewModel)

}
