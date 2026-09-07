package com.pablitosb.sportsbook.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.pablitosb.sportsbook.navigation.Dest
import com.pablitosb.sportsbook.ui.nfl.NflEdgeScreen
import kotlinx.coroutines.launch

/**
 * Swipe wrapper only. Page 0 is the unchanged baseball [HomeScreen].
 * Page 1 is NFL Edge. Does not edit baseball internals.
 */
@Composable
fun SportHubScreen(onOpen: (Dest) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
    ) { page ->
        when (page) {
            0 -> HomeScreen(onOpen = onOpen)
            else -> NflEdgeScreen(
                onOpen = onOpen,
                onShowBaseball = { scope.launch { pagerState.animateScrollToPage(0) } },
            )
        }
    }
}
