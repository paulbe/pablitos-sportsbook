package com.pablitosb.sportsbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pablitosb.sportsbook.navigation.AppNav
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.PablitosSportsbookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PablitosSportsbookTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = NavyBlack) {
                    AppNav()
                }
            }
        }
    }
}
