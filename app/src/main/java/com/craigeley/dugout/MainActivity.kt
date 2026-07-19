package com.craigeley.dugout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.craigeley.dugout.ui.BoxScoreScreen
import com.craigeley.dugout.ui.Navbar
import com.craigeley.dugout.ui.ScoresScreen
import com.craigeley.dugout.ui.StandingsScreen
import com.craigeley.dugout.ui.theme.DugoutTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DugoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableImmersive()
        setContent {
            DugoutTheme {
                DugoutApp(viewModel)
            }
        }
    }

    // Hide the status bar AND the bottom gesture pill for a true full-screen
    // look (the native equivalent of vandamd's expo-navigation-bar "hidden");
    // an edge swipe shows the bars transiently. Re-applied on focus so it sticks.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersive()
    }

    private fun enableImmersive() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
fun DugoutApp(viewModel: DugoutViewModel) {
    val state by viewModel.state.collectAsState()
    // Hoisted above the screen switch so scroll positions survive
    // navigating into the box score and back.
    val scoresListState = rememberLazyListState()
    val standingsListState = rememberLazyListState()

    BackHandler(enabled = state.selected != null) { viewModel.closeDetail() }
    BackHandler(enabled = state.selected == null && state.tab == Tab.STANDINGS) {
        viewModel.setTab(Tab.SCORES)
    }

    if (state.selected != null) {
        BoxScoreScreen(viewModel)
        return
    }

    LaunchedEffect(Unit) { viewModel.loadInitial() }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (state.tab) {
                Tab.SCORES -> ScoresScreen(viewModel, scoresListState)
                Tab.STANDINGS -> StandingsScreen(viewModel, standingsListState)
            }
        }
        Navbar(current = state.tab, onSelect = { viewModel.setTab(it) })
    }
}
