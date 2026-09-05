package com.pablitosb.sportsbook.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pablitosb.sportsbook.ui.dfs.DfsLineupsScreen
import com.pablitosb.sportsbook.ui.fdproj.FdProjScreen
import com.pablitosb.sportsbook.ui.home.HomeScreen
import com.pablitosb.sportsbook.ui.hr.HrProbabilityScreen
import com.pablitosb.sportsbook.ui.models.ModelsScreen
import com.pablitosb.sportsbook.ui.props.UnderdogPropsScreen
import com.pablitosb.sportsbook.ui.settings.SettingsScreen
import com.pablitosb.sportsbook.ui.starters.StartersScreen
import com.pablitosb.sportsbook.ui.tb.TbScreen
import com.pablitosb.sportsbook.ui.toppicks.TopPicksScreen

enum class Dest(val route: String) {
    Home("home"),
    Starters("starters"),
    HrProb("hr"),
    Dfs("dfs"),
    FdProj("fdproj"),
    Props("props"),
    TopPicks("toppicks"),
    TotalBases("tb"),
    Models("models"),
    Settings("settings"),
}

@Composable
fun AppNav(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    val back = { nav.popBackStack(); Unit }
    val open = { dest: Dest -> nav.navigate(dest.route) }

    NavHost(
        navController = nav,
        startDestination = Dest.Home.route,
        modifier = modifier,
    ) {
        composable(Dest.Home.route) { HomeScreen(onOpen = open) }
        composable(Dest.Starters.route) { StartersScreen(onBack = back) }
        composable(Dest.HrProb.route) { HrProbabilityScreen(onBack = back) }
        composable(Dest.Dfs.route) { DfsLineupsScreen(onBack = back) }
        composable(Dest.FdProj.route) { FdProjScreen(onBack = back) }
        composable(Dest.Props.route) { UnderdogPropsScreen(onBack = back) }
        composable(Dest.TopPicks.route) { TopPicksScreen(onBack = back) }
        composable(Dest.TotalBases.route) { TbScreen(onBack = back) }
        composable(Dest.Models.route) { ModelsScreen(onBack = back) }
        composable(Dest.Settings.route) { SettingsScreen(onBack = back) }
    }
}
