package com.openagnes.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.openagnes.app.ui.MainScreen
import com.openagnes.app.viewmodel.EditViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = androidx.compose.ui.graphics.Color.Black) {
                    val viewModel = viewModel(
                        viewModelFactory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return EditViewModel(application) as T
                            }
                        }
                    )
                    MainScreen(viewModel)
                }
            }
        }
    }
}
