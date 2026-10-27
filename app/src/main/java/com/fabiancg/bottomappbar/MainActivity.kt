package com.fabiancg.bottomappbar

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fabiancg.bottomappbar.data.LocalStore
import com.fabiancg.bottomappbar.ui.AppNavigation
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.theme.BottomAppBarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalStore.init(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            BottomAppBarTheme {
                val vm: AppViewModel = viewModel()
                AppNavigation(vm)
            }
        }
    }
}
